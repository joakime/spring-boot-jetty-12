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

import org.testcontainers.containers.Neo4jContainer;

import org.springframework.boot.test.context.properties.PropertiesExtractor;

/**
 * {@link PropertiesExtractor} for {@link Neo4jContainer}.
 *
 * @author Andy Wilkinson
 */
@SuppressWarnings("rawtypes")
class Neo4jContainerPropertiesExtractor implements PropertiesExtractor<Neo4jContainer> {

	@Override
	public Map<String, Object> extract(Neo4jContainer container) {
		return Map.of("spring.neo4j.uri", container.getBoltUrl(), "spring.neo4j.authentication.username", "neo4j",
				"spring.neo4j.authentication.password", container.getAdminPassword());
	}

	@Override
	public Class<Neo4jContainer> sourceType() {
		return Neo4jContainer.class;
	}

}
