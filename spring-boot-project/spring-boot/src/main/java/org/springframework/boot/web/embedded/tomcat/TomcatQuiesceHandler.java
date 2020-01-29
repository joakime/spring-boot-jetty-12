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

package org.springframework.boot.web.embedded.tomcat;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

import org.apache.catalina.Service;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;
import org.apache.coyote.AbstractProtocol;
import org.apache.tomcat.util.net.AbstractEndpoint;

import org.springframework.boot.web.server.QuiesceHandler;

/**
 * A {@link QuiesceHandler} for {@link Tomcat}.
 *
 * @author Andy Wilkinson
 */
class TomcatQuiesceHandler implements QuiesceHandler {

	private final Tomcat tomcat;

	private final long period;

	private volatile boolean quiescing = false;

	TomcatQuiesceHandler(Tomcat tomcat, long period) {
		this.tomcat = tomcat;
		this.period = period;
	}

	@Override
	public boolean quiesce() {
		List<ConnectorQuiescer> connectorQuiesces = getConnectorQuiescers();
		for (ConnectorQuiescer quiescer : connectorQuiesces) {
			quiescer.initiate();
		}
		this.quiescing = true;
		try {
			long end = System.currentTimeMillis() + this.period;
			for (ConnectorQuiescer quiescer : connectorQuiesces) {
				while (!quiescer.isQuiesced() && System.currentTimeMillis() < end) {
					Thread.sleep(100);
				}
				if (!quiescer.isQuiesced()) {
					return false;
				}
			}
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
		finally {
			this.quiescing = false;
		}
		return true;
	}

	private List<ConnectorQuiescer> getConnectorQuiescers() {
		List<ConnectorQuiescer> quiescers = new ArrayList<>();
		for (Service service : this.tomcat.getServer().findServices()) {
			for (Connector connector : service.findConnectors()) {
				quiescers.add(ConnectorQuiescer.of(connector));
			}
		}
		return quiescers;
	}

	@Override
	public boolean isQuiescing() {
		return this.quiescing;
	}

	/**
	 * Base class for quiescing a {@link Connector}. Merely pauses the connector to
	 * prevent new requests and then assumes that quiesce is complete. Only to be used
	 * when more sophisticated quiesce is not possible.
	 */
	private static class ConnectorQuiescer {

		private final Connector connector;

		protected ConnectorQuiescer(Connector connector) {
			this.connector = connector;
		}

		protected void initiate() {
			this.connector.pause();
		}

		protected boolean isQuiesced() {
			return true;
		}

		private static ConnectorQuiescer of(Connector connector) {
			try {
				AbstractProtocol<?> abstractProtocol = (AbstractProtocol<?>) connector.getProtocolHandler();
				Method getEndpoint = AbstractProtocol.class.getDeclaredMethod("getEndpoint");
				getEndpoint.setAccessible(true);
				return new EndpointConnectorQuieser(connector,
						(AbstractEndpoint<?, ?>) getEndpoint.invoke(abstractProtocol));
			}
			catch (Exception ex) {
				// Continue
			}
			try {
				ExecutorService executorService = (ExecutorService) connector.getProtocolHandler().getExecutor();
				return new ExecutorServiceConnectorQuieser(connector, executorService);
			}
			catch (Exception ex) {
				// Continue
			}
			return new ConnectorQuiescer(connector);
		}

	}

	/**
	 * Quiesces a {@link Connector} by pausing it and then checking if its endpoint has no
	 * connections. Waits for keep-alive connections to be closed via a
	 * {@code Connection: close} header which Tomcat will send in response to the first
	 * request received on the connection after quiescing begins.
	 */
	private static final class EndpointConnectorQuieser extends ConnectorQuiescer {

		private final AbstractEndpoint<?, ?> endpoint;

		private EndpointConnectorQuieser(Connector connector, AbstractEndpoint<?, ?> endpoint) {
			super(connector);
			this.endpoint = endpoint;
		}

		@Override
		protected boolean isQuiesced() {
			return this.endpoint.getConnections().isEmpty();
		}

	}

	/**
	 * Quiesces a {@link Connector} by pausing it, shutting down its executor, and then
	 * checking that the exector is terminated which indicates that there are no active
	 * requests. Does not consider keep-alive connections or async requests.
	 */
	private static final class ExecutorServiceConnectorQuieser extends ConnectorQuiescer {

		private final ExecutorService exector;

		private ExecutorServiceConnectorQuieser(Connector connector, ExecutorService exector) {
			super(connector);
			this.exector = exector;
		}

		@Override
		protected void initiate() {
			super.initiate();
			this.exector.shutdown();
		}

		@Override
		protected boolean isQuiesced() {
			return this.exector.isTerminated();
		}

	}

}
