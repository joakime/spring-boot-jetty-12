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
import java.io.RandomAccessFile;

/**
 * @author awilkinson
 */
class RandomAccessDataFile implements RandomAccessData {

	private final RandomAccessFile file;

	private final long offset;

	private final long length;

	RandomAccessDataFile(File file) throws IOException {
		this(new RandomAccessFile(file, "r"), 0, file.length());
	}

	private RandomAccessDataFile(RandomAccessFile file, long offset, long length) {
		this.file = file;
		this.offset = offset;
		this.length = length;
	}

	@Override
	public int seekAndRead(long position, byte[] buffer, int offset, int length)
			throws IOException {
		synchronized (this.file) {
			this.file.seek(this.offset + position);
			int read = this.file.read(buffer, offset, length);
			return read;
		}
	}

	@Override
	public int seekAndRead(long position) throws IOException {
		synchronized (this.file) {
			this.file.seek(position);
			return this.file.read();
		}
	}

	@Override
	public long length() throws IOException {
		return this.length;
	}

	@Override
	public RandomAccessData subsection(long offset, long length) {
		return new RandomAccessDataFile(this.file, this.offset + offset, length);
	}

}
