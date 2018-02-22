/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.loader.jar;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.NoSuchElementException;
import java.util.jar.JarEntry;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

/**
 * @author awilkinson
 */
public class BootJarFile extends java.util.jar.JarFile {

	private final File file;

	private final ZipMetadata zipMetadata;

	public BootJarFile(File file) throws IOException {
		this(file, new RandomAccessDataFile(file));
	}

	private BootJarFile(File file, RandomAccessData data) throws IOException {
		super(file);
		this.file = file;
		this.zipMetadata = new ZipMetadata(data);
	}

	@Override
	public JarEntry getJarEntry(String name) {
		return this.zipMetadata.getEntry(name);
	}

	@Override
	public ZipEntry getEntry(String name) {
		return getJarEntry(name);
	}

	@Override
	public Enumeration<JarEntry> entries() {
		return this.zipMetadata.entries();
	}

	public BootJarFile getNestedEntry(String name) throws IOException {
		return this.zipMetadata.getNestedEntry(name);
	}

	@Override
	public Manifest getManifest() throws IOException {
		InputStream inputStream = getInputStream(getEntry("META-INF/MANIFEST.MF"));
		return new Manifest(inputStream);
	}

	@Override
	public synchronized InputStream getInputStream(ZipEntry ze) throws IOException {
		InputStream inputStream = this.zipMetadata.getInputStream(ze.getName());
		if (ze.getMethod() == ZipEntry.DEFLATED) {
			return new ZipInflaterInputStream(inputStream, (int) ze.getSize());
		}
		else {
			return inputStream;
		}
	}

	private final class ZipMetadata {

		private final CentralDirectory centralDirectory;

		private final RandomAccessData data;

		private final EndRecord endRecord;

		private ZipMetadata(RandomAccessData data) throws IOException {
			this.data = data;
			this.endRecord = EndRecord.from(data);
			this.centralDirectory = readCentralDirectory();
		}

		private CentralDirectory readCentralDirectory() throws IOException {
			byte[] centralDirectory = new byte[(int) this.endRecord.centralDirectorySize];
			if (this.data.seekAndRead(this.endRecord.centralDirectoryOffset,
					centralDirectory, 0,
					centralDirectory.length) != this.endRecord.centralDirectorySize) {
				throw new IOException("Failed to read central directory");
			}
			return new CentralDirectory(centralDirectory, (int) this.endRecord.entries);
		}

		private JarEntry getEntry(String name) {
			return this.centralDirectory.getEntry(name);
		}

		private Enumeration<JarEntry> entries() {
			return this.centralDirectory.entries();
		}

		private InputStream getInputStream(String name) throws IOException {
			RandomAccessData entryData = getEntryData(name);
			return new InputStream() {

				int position = 0;

				@Override
				public int read() throws IOException {
					int read = entryData.seekAndRead(this.position);
					this.position++;
					return read;
				}

				@Override
				public int read(byte[] b, int off, int len) throws IOException {
					int read = entryData.seekAndRead(this.position, b, off, len);
					this.position += read;
					return read;
				}

			};
		}

		private BootJarFile getNestedEntry(String name) throws IOException {
			return new BootJarFile(BootJarFile.this.file, getEntryData(name));
		}

		private RandomAccessData getEntryData(String name) throws IOException {
			Integer localHeaderOffset = this.centralDirectory.localHeaderOffset(name);
			if (localHeaderOffset == null) {
				return null;
			}
			byte[] header = new byte[30];
			this.data.seekAndRead(localHeaderOffset, header, 0, 30);
			int nameLength = (int) Bytes.littleEndianValue(header, 26, 2);
			int extraLength = (int) Bytes.littleEndianValue(header, 28, 2);
			long length = Bytes.littleEndianValue(header, 18, 4);
			RandomAccessData entryData = this.data.subsection(
					localHeaderOffset + 30 + nameLength + extraLength, length);
			return entryData;
		}

	}

	private static class EndRecord {

		private static final int MINIMUM_SIZE = 22;

		private static final int MAXIMUM_COMMENT_LENGTH = 0xFFFF;

		private static final int MAXIMUM_SIZE = MINIMUM_SIZE + MAXIMUM_COMMENT_LENGTH;

		private final long entries;

		private final long centralDirectorySize;

		private final long centralDirectoryOffset;

		private EndRecord(long entries, long centralDirectorySize,
				long centralDirectoryOffset) {
			this.centralDirectorySize = centralDirectorySize;
			this.centralDirectoryOffset = centralDirectoryOffset;
			this.entries = entries;
		}

		private static EndRecord from(RandomAccessData data) throws IOException {
			byte[] buffer = new byte[256];
			int size = MINIMUM_SIZE;
			while (size < MAXIMUM_SIZE) {
				long position = data.length() - buffer.length - size + MINIMUM_SIZE;
				int read = data.seekAndRead(position, buffer, 0, buffer.length);
				for (int i = read - MINIMUM_SIZE; i > 0; i--) {
					if (buffer[i] == 'P' && buffer[i + 1] == 'K' && buffer[i + 2] == 5
							&& buffer[i + 3] == 6) {
						long entries = Bytes.littleEndianValue(buffer, i + 10, 2);
						long centralDirectorySize = Bytes.littleEndianValue(buffer,
								i + 12, 4);
						long centralDirectoryOffset = Bytes.littleEndianValue(buffer,
								i + 16, 4);
						return new EndRecord(entries, centralDirectorySize,
								centralDirectoryOffset);
					}
				}
			}
			return null;
		}

	}

	private class CentralDirectory {

		private final byte[] data;

		private final Entries entries;

		private CentralDirectory(byte[] data, int entries) {
			this.data = data;
			this.entries = new Entries(entries);
			for (int i = 0; i < data.length;) {
				int nameLength = (int) Bytes.littleEndianValue(data, i + 28, 2);
				long extraFieldLength = Bytes.littleEndianValue(data, i + 30, 2);
				long commentLength = Bytes.littleEndianValue(data, i + 32, 2);
				int nameHash = hash(data, i + 46, nameLength);
				this.entries.put(nameHash, i);
				i += (46 + nameLength + extraFieldLength + commentLength);
			}
		}

		private JarEntry getEntry(String name) {
			Integer offset = getOffset(name);
			if (offset == null) {
				return null;
			}
			return createEntry(name, offset);
		}

		private Integer getOffset(String name) {
			CharsetEncoder encoder = StandardCharsets.UTF_8.newEncoder();
			CharBuffer input = CharBuffer.wrap(name.toCharArray());
			byte[] output = new byte[name.length() * (int) encoder.maxBytesPerChar()];
			ByteBuffer outputBuffer = ByteBuffer.wrap(output);
			encoder.encode(input, outputBuffer, true);
			encoder.flush(outputBuffer);
			return this.entries
					.get(Arrays.copyOfRange(output, 0, outputBuffer.position()));
		}

		private JarEntry createEntry(String name, int offset) {
			JarEntry jarEntry = new JarEntry(name);
			jarEntry.setCompressedSize(compressedSize(offset));
			jarEntry.setMethod(method(offset));
			jarEntry.setCrc(crc32(offset));
			jarEntry.setSize(uncompressedSize(offset));
			jarEntry.setExtra(extra(offset));
			jarEntry.setComment(new AsciiBytes(comment(offset)).toString());
			jarEntry.setTime(time(offset));
			return jarEntry;
		}

		private JarEntry createEntry(int offset) {
			return createEntry(
					new AsciiBytes(name(offset, entryNameLength(offset))).toString(),
					offset);
		}

		public Integer localHeaderOffset(String name) throws IOException {
			Integer offset = getOffset(name);
			if (offset == null) {
				return null;
			}
			return (int) localHeaderOffset(offset);
		}

		private Enumeration<JarEntry> entries() {
			return this.entries.enumeration();
		}

		private int hash(byte[] bytes, int offset, int length) {
			int hash = 1;
			for (int i = 0; i < length; i++) {
				hash = 31 * hash + bytes[i + offset];
			}
			return hash;
		}

		private int method(int offset) {
			return (int) Bytes.littleEndianValue(this.data, offset + 10, 2);
		}

		public long time(int offset) {
			long date = Bytes.littleEndianValue(this.data, offset + 14, 2);
			long time = Bytes.littleEndianValue(this.data, offset + 12, 2);
			return decodeMsDosFormatDateTime(date, time).getTimeInMillis();
		}

		/**
		 * Decode MS-DOS Date Time details. See
		 * <a href="http://mindprod.com/jgloss/zip.html">mindprod.com/jgloss/zip.html</a>
		 * for more details of the format.
		 * @param date the date part
		 * @param time the time part
		 * @return a {@link Calendar} containing the decoded date.
		 */
		private Calendar decodeMsDosFormatDateTime(long date, long time) {
			int year = (int) ((date >> 9) & 0x7F) + 1980;
			int month = (int) ((date >> 5) & 0xF) - 1;
			int day = (int) (date & 0x1F);
			int hours = (int) ((time >> 11) & 0x1F);
			int minutes = (int) ((time >> 5) & 0x3F);
			int seconds = (int) ((time << 1) & 0x3E);
			return new GregorianCalendar(year, month, day, hours, minutes, seconds);
		}

		private long crc32(int offset) {
			return Bytes.littleEndianValue(this.data, offset + 16, 4);
		}

		private long compressedSize(int offset) {
			return Bytes.littleEndianValue(this.data, offset + 20, 4);
		}

		private long uncompressedSize(int offset) {
			return Bytes.littleEndianValue(this.data, offset + 24, 4);
		}

		private int entryNameLength(int offset) {
			return (int) Bytes.littleEndianValue(this.data, offset + 28, 2);
		}

		private int extraLength(int offset) {
			return (int) Bytes.littleEndianValue(this.data, offset + 30, 2);
		}

		private int commentLength(int offset) {
			return (int) Bytes.littleEndianValue(this.data, offset + 32, 2);
		}

		private long localHeaderOffset(int offset) {
			return Bytes.littleEndianValue(this.data, offset + 42, 4);
		}

		private byte[] name(int offset, int length) {
			return Arrays.copyOfRange(this.data, offset + 46, offset + 46 + length);
		}

		private byte[] extra(int offset) {
			int length = extraLength(offset);
			int extraOffset = offset + 46 + entryNameLength(offset);
			return Arrays.copyOfRange(this.data, extraOffset, extraOffset + length);
		}

		private byte[] comment(int offset) {
			int length = commentLength(offset);
			int commentOffset = offset + 46 + entryNameLength(offset)
					+ commentLength(offset);
			return Arrays.copyOfRange(this.data, commentOffset, commentOffset + length);
		}

		private class Entries {

			private final int[] table;

			private final int[] entries;

			private final int size;

			private int entryIndex = 0;

			private Entries(int size) {
				this.size = size;
				this.table = new int[size / 2];
				Arrays.fill(this.table, -1);
				this.entries = new int[size * 3];
				Arrays.fill(this.entries, -1);
			}

			private void put(int nameHash, int offset) {
				int tableIndex = (nameHash & 0x7FFFFFFF) % this.table.length;
				int existingEntryIndex = this.table[tableIndex];
				this.table[tableIndex] = this.entryIndex;
				addEntry(nameHash, existingEntryIndex, offset);
			}

			private Integer get(byte[] name) {
				int nameHash = hash(name, 0, name.length);
				int tableIndex = (nameHash & 0x7FFFFFFF) % this.table.length;
				int entryIndex = this.table[tableIndex];
				while (entryIndex != -1) {
					int entryNameHash = this.entries[entryIndex];
					if (entryNameHash == nameHash) {
						int offset = this.entries[entryIndex + 2];
						int entryNameLength = CentralDirectory.this
								.entryNameLength(offset);
						if (entryNameLength == name.length && Arrays.equals(name,
								CentralDirectory.this.name(offset, entryNameLength))) {
							return offset;
						}
					}
					entryIndex = this.entries[entryIndex + 1];
				}
				return null;
			}

			private void addEntry(int nameHash, int existingEntryIndex, int offset) {
				this.entries[this.entryIndex++] = nameHash;
				this.entries[this.entryIndex++] = existingEntryIndex;
				this.entries[this.entryIndex++] = offset;
			}

			private int hash(byte[] bytes, int offset, int length) {
				int hash = 1;
				for (int i = 0; i < length; i++) {
					hash = 31 * hash + bytes[i + offset];
				}
				return hash;
			}

			private Enumeration<JarEntry> enumeration() {
				return new Enumeration<JarEntry>() {

					private int entryIndex = 0;

					@Override
					public boolean hasMoreElements() {
						return this.entryIndex < Entries.this.size;
					}

					@Override
					public JarEntry nextElement() {
						if (this.entryIndex >= Entries.this.size) {
							throw new NoSuchElementException();
						}
						int offset = Entries.this.entries[(this.entryIndex * 3) + 2];
						this.entryIndex++;
						return createEntry(offset);
					}

				};
			}

		}

	}

}
