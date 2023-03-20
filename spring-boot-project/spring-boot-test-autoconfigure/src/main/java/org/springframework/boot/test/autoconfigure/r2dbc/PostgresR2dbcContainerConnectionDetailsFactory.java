/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.test.autoconfigure.r2dbc;

import io.r2dbc.spi.ConnectionFactoryOptions;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.PostgreSQLR2DBCDatabaseContainer;

import org.springframework.boot.autoconfigure.r2dbc.R2dbcConnectionDetails;
import org.springframework.boot.autoconfigure.service.connection.ConnectionDetailsFactory;
import org.springframework.boot.test.autoconfigure.service.connection.ContainerConnectionDetailsFactory;
import org.springframework.boot.test.autoconfigure.service.connection.ServiceConnectedContainer;

/**
 * {@link ConnectionDetailsFactory} for {@link R2dbcConnection @R2dbcConnection} annotated
 * {@link PostgreSQLContainer} fields.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class PostgresR2dbcContainerConnectionDetailsFactory
		extends ContainerConnectionDetailsFactory<R2dbcConnection, R2dbcConnectionDetails, PostgreSQLContainer<?>> {

	@Override
	public R2dbcConnectionDetails getContainerConnectionDetails(
			ServiceConnectedContainer<R2dbcConnection, R2dbcConnectionDetails, PostgreSQLContainer<?>> source) {
		return new R2dbcDatabaseContainerConnectionDetails(source.getContainer());
	}

	private static final class R2dbcDatabaseContainerConnectionDetails implements R2dbcConnectionDetails {

		private final PostgreSQLContainer<?> container;

		private R2dbcDatabaseContainerConnectionDetails(PostgreSQLContainer<?> container) {
			this.container = container;
		}

		@Override
		public ConnectionFactoryOptions getConnectionFactoryOptions() {
			return PostgreSQLR2DBCDatabaseContainer.getOptions(this.container);
		}

	}

}
