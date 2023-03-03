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

package org.springframework.boot.test.testcontainers;

import java.util.Map;

import org.testcontainers.containers.PostgreSQLContainer;

import org.springframework.boot.test.context.properties.PropertiesExtractor;

/**
 * {@link PropertiesExtractor} for {@link PostgreSQLContainer}.
 *
 * @author Andy Wilkinson
 */
@SuppressWarnings("rawtypes")
class PostgreSqlContainerPropertiesExtractor implements PropertiesExtractor<PostgreSQLContainer> {

	@Override
	public Map<String, Object> extract(PostgreSQLContainer container) {
		// TODO JDBC, Flyway, and Liquibase
		return Map.of("spring.r2dbc.url", r2dbcUrl(container), "spring.r2dbc.username", container.getUsername(),
				"spring.r2dbc.password", container.getPassword());
	}

	@Override
	public Class<PostgreSQLContainer> sourceType() {
		return PostgreSQLContainer.class;
	}

	private static String r2dbcUrl(PostgreSQLContainer postgreSql) {
		return String.format("r2dbc:postgresql://%s:%s/%s", postgreSql.getHost(),
				postgreSql.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT), postgreSql.getDatabaseName());
	}

}
