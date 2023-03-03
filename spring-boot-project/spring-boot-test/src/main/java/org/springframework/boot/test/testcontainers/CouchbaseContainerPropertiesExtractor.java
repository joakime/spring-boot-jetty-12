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

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import org.testcontainers.couchbase.BucketDefinition;
import org.testcontainers.couchbase.CouchbaseContainer;

import org.springframework.boot.test.context.properties.PropertiesExtractor;
import org.springframework.util.ReflectionUtils;

/**
 * {@link PropertiesExtractor} for {@link CouchbaseContainer}.
 *
 * @author Andy Wilkinson
 */
class CouchbaseContainerPropertiesExtractor implements PropertiesExtractor<CouchbaseContainer> {

	@Override
	public Map<String, Object> extract(CouchbaseContainer container) {
		return Map.of("spring.couchbase.connection-string", container.getConnectionString(),
				"spring.couchbase.username", container.getUsername(), "spring.couchbase.password",
				container.getPassword(), "spring.data.couchbase.bucket-name", firstBucketName(container));
	}

	@SuppressWarnings("unchecked")
	private Object firstBucketName(CouchbaseContainer container) {
		Field bucketsField = ReflectionUtils.findField(container.getClass(), "buckets");
		ReflectionUtils.makeAccessible(bucketsField);
		List<BucketDefinition> buckets = (List<BucketDefinition>) ReflectionUtils.getField(bucketsField, container);
		if (buckets.isEmpty()) {
			throw new IllegalStateException(
					"Property extraction from a CouchbaseContainer requires at least one bucket definition");
		}
		return buckets.get(0).getName();
	}

	@Override
	public Class<CouchbaseContainer> sourceType() {
		return CouchbaseContainer.class;
	}

}
