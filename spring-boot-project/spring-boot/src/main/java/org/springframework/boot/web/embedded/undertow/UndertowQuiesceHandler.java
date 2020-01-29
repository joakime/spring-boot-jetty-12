/*
 * Copyright 2012-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.web.embedded.undertow;

import io.undertow.server.handlers.GracefulShutdownHandler;

import org.springframework.boot.web.server.QuiesceHandler;

/**
 * A {@link QuiesceHandler} for Undertow.
 *
 * @author Andy Wilkinson
 */
class UndertowQuiesceHandler implements QuiesceHandler {

	private final GracefulShutdownHandler gracefulShutdownHandler;

	private final long period;

	private volatile boolean quiescing;

	UndertowQuiesceHandler(GracefulShutdownHandler gracefulShutdownHandler, long period) {
		this.gracefulShutdownHandler = gracefulShutdownHandler;
		this.period = period;
	}

	@Override
	public boolean quiesce() {
		GracefulShutdownHandler gracefulShutdownHandler = this.gracefulShutdownHandler;
		if (this.gracefulShutdownHandler != null) {
			gracefulShutdownHandler.shutdown();
			this.quiescing = true;
			try {
				return gracefulShutdownHandler.awaitShutdown(this.period);
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
			this.quiescing = false;
		}
		return false;
	}

	@Override
	public boolean isQuiescing() {
		return this.quiescing;
	}

}
