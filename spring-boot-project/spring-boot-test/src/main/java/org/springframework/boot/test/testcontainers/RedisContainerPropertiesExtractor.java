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

import org.testcontainers.containers.GenericContainer;

import org.springframework.boot.test.context.properties.PropertiesExtractor;

/**
 * {@link PropertiesExtractor} for a container running Redis.
 *
 * @author Andy Wilkinson
 */
@SuppressWarnings("rawtypes")
class RedisContainerPropertiesExtractor implements PropertiesExtractor<GenericContainer> {

	@Override
	public Map<String, Object> extract(GenericContainer container) {
		if (container.getDockerImageName().startsWith("redis:")) {
			return Map.of("spring.data.redis.host", container.getHost(), "spring.data.redis.port",
					container.getFirstMappedPort());
		}
		else {
			return null;
		}
	}

	@Override
	public Class<GenericContainer> sourceType() {
		return GenericContainer.class;
	}

}
