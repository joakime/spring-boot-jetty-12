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

package org.springframework.boot.testsupport.testcontainers;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.exceptions.NoHostAvailableException;
import org.rnorth.ducttape.TimeoutException;
import org.rnorth.ducttape.unreliables.Unreliables;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.HostPortWaitStrategy;

/**
 * @author awilkinson
 */
public class TestContainers {

	@SuppressWarnings("resource")
	public static GenericContainer<?> redis() {
		return new GenericContainer<>("redis:4.0.6").withExposedPorts(6379);
	}

	@SuppressWarnings("resource")
	public static GenericContainer<?> cassandra() {
		return new GenericContainer<>("cassandra:3.11.1").withExposedPorts(9042)
				.waitingFor(new ConnectionVerifyingWaitStrategy());
	}

	private static class ConnectionVerifyingWaitStrategy extends HostPortWaitStrategy {

		@Override
		protected void waitUntilReady() {
			super.waitUntilReady();

			try {
				Unreliables.retryUntilTrue((int) this.startupTimeout.getSeconds(),
						TimeUnit.SECONDS, checkConnection());
			}
			catch (TimeoutException ex) {
				throw new IllegalStateException(ex);
			}
		}

		private Callable<Boolean> checkConnection() {
			return () -> {
				try (Cluster cluster = Cluster.builder()
						.addContactPoint(
								"localhost:" + this.container.getMappedPort(9042))
						.build()) {
					cluster.connect();
					return true;
				}
				catch (NoHostAvailableException ex) {
					return false;
				}
			};
		}

	}

}
