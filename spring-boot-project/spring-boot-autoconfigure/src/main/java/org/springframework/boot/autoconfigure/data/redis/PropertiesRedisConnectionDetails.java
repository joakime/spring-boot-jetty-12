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

package org.springframework.boot.autoconfigure.data.redis;

import java.util.List;

import org.springframework.boot.autoconfigure.data.redis.RedisConnectionConfiguration.ConnectionInfo;

/**
 * Adapts {@link RedisProperties} to {@link RedisConnectionDetails}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class PropertiesRedisConnectionDetails implements RedisConnectionDetails {

	private final RedisProperties properties;

	PropertiesRedisConnectionDetails(RedisProperties properties) {
		this.properties = properties;
	}

	@Override
	public String getUsername() {
		if (this.properties.getUrl() != null) {
			ConnectionInfo connectionInfo = connectionInfo(this.properties.getUrl());
			String userInfo = connectionInfo.getUri().getUserInfo();
			if (userInfo != null) {
				int index = userInfo.indexOf(':');
				if (index != -1) {
					return userInfo.substring(0, index);
				}
			}
		}
		return this.properties.getUsername();
	}

	@Override
	public String getPassword() {
		if (this.properties.getUrl() != null) {
			ConnectionInfo connectionInfo = connectionInfo(this.properties.getUrl());
			String userInfo = connectionInfo.getUri().getUserInfo();
			if (userInfo != null) {
				int index = userInfo.indexOf(':');
				if (index != -1) {
					return userInfo.substring(index + 1);
				}
			}
		}
		return this.properties.getPassword();
	}

	@Override
	public Standalone getStandalone() {
		if (this.properties.getUrl() != null) {
			ConnectionInfo connectionInfo = connectionInfo(this.properties.getUrl());
			return Standalone.of(connectionInfo.getUri().getHost(), connectionInfo.getUri().getPort(),
					this.properties.getDatabase());
		}
		return Standalone.of(this.properties.getHost(), this.properties.getPort(), this.properties.getDatabase());
	}

	private ConnectionInfo connectionInfo(String url) {
		if (url == null) {
			return null;
		}
		return RedisConnectionConfiguration.parseUrl(url);
	}

	@Override
	public Sentinel getSentinel() {
		org.springframework.boot.autoconfigure.data.redis.RedisProperties.Sentinel sentinel = this.properties
			.getSentinel();
		if (sentinel == null) {
			return null;
		}
		return new Sentinel() {

			@Override
			public int getDatabase() {
				return PropertiesRedisConnectionDetails.this.properties.getDatabase();
			}

			@Override
			public String getMaster() {
				return sentinel.getMaster();
			}

			@Override
			public List<Node> getNodes() {
				return sentinel.getNodes().stream().map(PropertiesRedisConnectionDetails.this::asNode).toList();
			}

			@Override
			public String getUsername() {
				return sentinel.getUsername();
			}

			@Override
			public String getPassword() {
				return sentinel.getPassword();
			}

		};
	}

	@Override
	public Cluster getCluster() {
		org.springframework.boot.autoconfigure.data.redis.RedisProperties.Cluster cluster = this.properties
			.getCluster();
		if (cluster == null) {
			return null;
		}
		return new Cluster() {

			@Override
			public List<Node> getNodes() {
				return cluster.getNodes().stream().map(PropertiesRedisConnectionDetails.this::asNode).toList();
			}

		};
	}

	private Node asNode(String node) {
		String[] components = node.split(":");
		return new Node(components[0], Integer.parseInt(components[1]));
	}

}