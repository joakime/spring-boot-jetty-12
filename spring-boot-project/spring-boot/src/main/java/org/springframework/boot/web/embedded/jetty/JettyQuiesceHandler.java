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

package org.springframework.boot.web.embedded.jetty;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.StatisticsHandler;

import org.springframework.boot.web.server.QuiesceHandler;

/**
 * A {@link QuiesceHandler} for Jetty.
 *
 * @author Andy Wilkinson
 */
class JettyQuiesceHandler implements QuiesceHandler {

	private final Server server;

	private final StatisticsHandler statisticsHandler;

	private final long period;

	private volatile boolean quiescing = false;

	JettyQuiesceHandler(Server server, StatisticsHandler statisticsHandler, long period) {
		this.server = server;
		this.statisticsHandler = statisticsHandler;
		this.period = period;
	}

	@Override
	public boolean quiesce() {
		for (Connector connector : this.server.getConnectors()) {
			((ServerConnector) connector).setAccepting(false);
		}
		this.quiescing = true;
		long end = System.currentTimeMillis() + this.period;
		while (System.currentTimeMillis() < end && (this.statisticsHandler.getRequestsActive() > 0)) {
			try {
				Thread.sleep(100);
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
		}
		this.quiescing = false;
		return this.statisticsHandler.getRequestsActive() == 0;
	}

	@Override
	public boolean isQuiescing() {
		return this.quiescing;
	}

}
