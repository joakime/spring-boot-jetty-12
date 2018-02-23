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
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.jar.JarEntry;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

/**
 * @author awilkinson
 */
public class JarFile extends java.util.jar.JarFile {

	private static final String PROTOCOL_HANDLER = "java.protocol.handler.pkgs";

	private static final String HANDLERS_PACKAGE = "org.springframework.boot.loader";

	private final File file;

	private final ZipMetadata zipMetadata;

	private final String pathFromRoot;

	private URL url;

	public JarFile(File file) throws IOException {
		this(file, new RandomAccessDataFile(file), "");
	}

	private JarFile(File file, RandomAccessData data, String pathFromRoot)
			throws IOException {
		this(file, data, pathFromRoot, Function.identity());
	}

	private JarFile(File file, RandomAccessData data, String pathFromRoot,
			Function<byte[], byte[]> nameMapper) throws IOException {
		super(file);
		this.file = file;
		this.zipMetadata = new ZipMetadata(data, nameMapper);
		this.pathFromRoot = pathFromRoot;
	}

	File getFile() {
		return this.file;
	}

	String getPathFromRoot() {
		return this.pathFromRoot;
	}

	@Override
	public JarEntry getJarEntry(String name) {
		return this.zipMetadata.getEntry(name);
	}

	public JarEntry getJarEntry(CharSequence name) {
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

	public JarFile getNestedJarFile(ZipEntry entry) throws IOException {
		return this.zipMetadata.getNestedJarFile(entry);
	}

	@Override
	public Manifest getManifest() throws IOException {
		ZipEntry manifestEntry = getEntry("META-INF/MANIFEST.MF");
		if (manifestEntry == null) {
			try (java.util.jar.JarFile jarFile = new java.util.jar.JarFile(this.file)) {
				return jarFile.getManifest();
			}
		}
		InputStream inputStream = getInputStream(manifestEntry);
		return new Manifest(inputStream);
	}

	public InputStream getInputStream() throws IOException {
		return this.zipMetadata.getInputStream();
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

	public URL getUrl() throws MalformedURLException {
		if (this.url == null) {
			Handler handler = new Handler(this);
			String file = this.file.toURI() + this.pathFromRoot + "!/";
			file = file.replace("file:////", "file://"); // Fix UNC paths
			this.url = new URL("jar", "", -1, file, handler);
		}
		return this.url;
	}

	@Override
	public String toString() {
		return this.file.toString() + this.pathFromRoot;
	}

	/**
	 * Register a {@literal 'java.protocol.handler.pkgs'} property so that a
	 * {@link URLStreamHandler} will be located to deal with jar URLs.
	 */
	public static void registerUrlProtocolHandler() {
		String handlers = System.getProperty(PROTOCOL_HANDLER, "");
		System.setProperty(PROTOCOL_HANDLER, ("".equals(handlers) ? HANDLERS_PACKAGE
				: handlers + "|" + HANDLERS_PACKAGE));
		resetCachedUrlHandlers();
	}

	/**
	 * Reset any cached handlers just in case a jar protocol has already been used. We
	 * reset the handler by trying to set a null {@link URLStreamHandlerFactory} which
	 * should have no effect other than clearing the handlers cache.
	 */
	private static void resetCachedUrlHandlers() {
		try {
			URL.setURLStreamHandlerFactory(null);
		}
		catch (Error ex) {
			// Ignore
		}
	}

	private final class ZipMetadata {

		private final RandomAccessData data;

		private final EndRecord endRecord;

		private final CentralDirectory centralDirectory;

		private ZipMetadata(RandomAccessData data, Function<byte[], byte[]> nameMapper)
				throws IOException {
			this.endRecord = EndRecord.from(data);
			this.data = this.endRecord.archiveStartOffset == 0 ? data
					: data.subsection(this.endRecord.archiveStartOffset,
							data.length() - this.endRecord.archiveStartOffset);
			this.centralDirectory = readCentralDirectory(nameMapper);
		}

		private CentralDirectory readCentralDirectory(Function<byte[], byte[]> nameMapper)
				throws IOException {
			byte[] centralDirectory = new byte[(int) this.endRecord.centralDirectorySize];
			if (this.data.seekAndRead(this.endRecord.centralDirectoryOffset,
					centralDirectory, 0,
					centralDirectory.length) != this.endRecord.centralDirectorySize) {
				throw new IOException("Failed to read central directory");
			}
			return new CentralDirectory(centralDirectory, (int) this.endRecord.entries,
					nameMapper);
		}

		private JarEntry getEntry(CharSequence name) {
			return this.centralDirectory.getEntry(name);
		}

		private Enumeration<JarEntry> entries() {
			return this.centralDirectory.entries();
		}

		private InputStream getInputStream() throws IOException {
			return new RandomAccessDataInputStream(this.data);
		}

		private InputStream getInputStream(String name) throws IOException {
			return new RandomAccessDataInputStream(getEntryData(name));
		}

		private JarFile getNestedJarFile(ZipEntry entry) throws IOException {
			if (entry.isDirectory()) {
				byte[] rootBytes = this.centralDirectory.getNameAsBytes(entry.getName());
				return new JarFile(JarFile.this.file, this.data, JarFile.this.pathFromRoot
						+ "!/"
						+ entry.getName().substring(0, entry.getName().length() - 1),
						(name) -> {
							if (name.length <= rootBytes.length) {
								return null;
							}
							for (int i = 0; i < rootBytes.length; i++) {
								if (rootBytes[i] != name[i]) {
									return null;
								}
							}
							byte[] mapped = Arrays.copyOfRange(name, rootBytes.length,
									name.length);
							return mapped;
						});
			}
			return new JarFile(JarFile.this.file, getEntryData(entry.getName()),
					JarFile.this.pathFromRoot + "!/" + entry.getName());
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

		private final long archiveStartOffset;

		private EndRecord(long entries, long centralDirectorySize,
				long centralDirectoryOffset, long archiveStartOffset) {
			this.centralDirectorySize = centralDirectorySize;
			this.centralDirectoryOffset = centralDirectoryOffset;
			this.entries = entries;
			this.archiveStartOffset = archiveStartOffset;
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
						long actualCentralDirectoryOffset = data.length() - size
								- centralDirectorySize;
						return new EndRecord(entries, centralDirectorySize,
								centralDirectoryOffset,
								actualCentralDirectoryOffset - centralDirectoryOffset);
					}
				}
			}
			return null;
		}

	}

	private class CentralDirectory {

		private final byte[] data;

		private final Entries entries;

		private final Function<byte[], byte[]> nameMapper;

		private CentralDirectory(byte[] data, int entries,
				Function<byte[], byte[]> nameMapper) {
			this.data = data;
			this.entries = new Entries(entries);
			this.nameMapper = nameMapper;
			for (int i = 0; i < data.length;) {
				int nameLength = (int) Bytes.littleEndianValue(data, i + 28, 2);
				long extraFieldLength = Bytes.littleEndianValue(data, i + 30, 2);
				long commentLength = Bytes.littleEndianValue(data, i + 32, 2);
				byte[] name = name(i, nameLength);
				if (name != null) {
					int nameHash = hash(name, 0, name.length);
					this.entries.put(nameHash, i);
				}
				i += (46 + nameLength + extraFieldLength + commentLength);
			}
		}

		private JarEntry getEntry(CharSequence name) {
			Integer offset = getOffset(name);
			return offset == null ? null : createEntry(name, offset);
		}

		private byte[] getNameAsBytes(CharSequence name) {
			CharsetEncoder encoder = StandardCharsets.UTF_8.newEncoder();
			CharBuffer input = CharBuffer.wrap(name);
			byte[] output = new byte[name.length() * (int) encoder.maxBytesPerChar()];
			ByteBuffer outputBuffer = ByteBuffer.wrap(output);
			encoder.encode(input, outputBuffer, true);
			encoder.flush(outputBuffer);
			return Arrays.copyOfRange(output, 0, outputBuffer.position());
		}

		private Integer getOffset(CharSequence name) {
			return this.entries.get(getNameAsBytes(name));
		}

		private JarEntry createEntry(CharSequence name, int offset) {
			JarEntry jarEntry = new JarEntry(name.toString());
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
			byte[] name = Arrays.copyOfRange(this.data, offset + 46,
					offset + 46 + length);
			return this.nameMapper.apply(name);
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

			private int size;

			private int entryIndex = 0;

			private Entries(int expectedSize) {
				this.table = new int[expectedSize / 2];
				Arrays.fill(this.table, -1);
				this.entries = new int[expectedSize * 3];
				Arrays.fill(this.entries, -1);
			}

			private void put(int nameHash, int offset) {
				int tableIndex = (nameHash & 0x7FFFFFFF) % this.table.length;
				int existingEntryIndex = this.table[tableIndex];
				this.table[tableIndex] = this.entryIndex;
				addEntry(nameHash, existingEntryIndex, offset);
			}

			private Integer get(byte[] name) {
				Integer offset = doGet(name);
				if (offset == null) {
					byte[] nameWithSlash = new byte[name.length + 1];
					System.arraycopy(name, 0, nameWithSlash, 0, name.length);
					nameWithSlash[name.length] = '/';
					offset = doGet(nameWithSlash);
				}
				return offset;
			}

			private Integer doGet(byte[] name) {
				int nameHash = hash(name, 0, name.length);
				int tableIndex = (nameHash & 0x7FFFFFFF) % this.table.length;
				int entryIndex = this.table[tableIndex];
				while (entryIndex != -1) {
					int entryNameHash = this.entries[entryIndex];
					if (entryNameHash == nameHash) {
						int offset = this.entries[entryIndex + 2];
						int entryNameLength = CentralDirectory.this
								.entryNameLength(offset);
						byte[] centralDirectoryName = CentralDirectory.this.name(offset,
								entryNameLength);
						if (Arrays.equals(name, centralDirectoryName)) {
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
				this.size++;
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

	/**
	 * An {@link InputStream} that reads from {@code RandomAccessData}.
	 */
	private static final class RandomAccessDataInputStream extends InputStream {

		private final RandomAccessData entryData;

		private int position = 0;

		private RandomAccessDataInputStream(RandomAccessData entryData) {
			this.entryData = entryData;
		}

		@Override
		public int read() throws IOException {
			int read = this.entryData.seekAndRead(this.position);
			this.position++;
			return read;
		}

		@Override
		public int read(byte[] b, int off, int len) throws IOException {
			int read = this.entryData.seekAndRead(this.position, b, off, len);
			this.position += read;
			return read;
		}

	}

}
