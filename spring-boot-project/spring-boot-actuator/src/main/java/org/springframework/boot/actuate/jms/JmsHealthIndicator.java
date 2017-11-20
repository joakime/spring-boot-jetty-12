/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.actuate.jms;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;

/**
 * {@link HealthIndicator} for a JMS {@link ConnectionFactory}.
 *
 * @author Stephane Nicoll
 * @since 2.0.0
 */
public class JmsHealthIndicator extends AbstractHealthIndicator {

	private final Log logger = LogFactory.getLog(JmsHealthIndicator.class);

	private final ConnectionFactory connectionFactory;

	public JmsHealthIndicator(ConnectionFactory connectionFactory) {
		this.connectionFactory = connectionFactory;
	}

	@Override
	protected void doHealthCheck(Health.Builder builder) throws Exception {
		CountDownLatch started = new CountDownLatch(1);
		try (Connection connection = this.connectionFactory.createConnection()) {
			new Thread(() -> {
				try {
					if (!started.await(5, TimeUnit.SECONDS)) {
						this.logger.warn("Connection failed to start within 5 seconds "
								+ "and will be closed.");
						connection.close();
					}
				}
				catch (InterruptedException ex) {
					Thread.currentThread().interrupt();
				}
				catch (Exception ex) {
					// Continue
				}
			}).start();
			connection.start();
			started.countDown();
			builder.up().withDetail("provider",
					connection.getMetaData().getJMSProviderName());
		}
	}

}
