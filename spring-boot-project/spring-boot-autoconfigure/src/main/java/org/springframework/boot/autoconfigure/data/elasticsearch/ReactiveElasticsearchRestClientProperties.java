/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.autoconfigure.data.elasticsearch;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.DeprecatedConfigurationProperty;
import org.springframework.util.unit.DataSize;

/**
 * Configuration properties for Elasticsearch Reactive REST clients.
 *
 * @author Brian Clozel
 * @since 2.2.0
 */
@ConfigurationProperties(prefix = "spring.data.elasticsearch.client.reactive")
public class ReactiveElasticsearchRestClientProperties {

	/**
	 * Comma-separated list of the Elasticsearch endpoints to connect to.
	 */
	private List<String> endpoints = new ArrayList<>(Collections.singletonList("localhost:9200"));

	/**
	 * Whether the client should use SSL to connect to the endpoints.
	 */
	private boolean useSsl = false;

	/**
	 * Credentials username.
	 */
	private String username;

	/**
	 * Credentials password.
	 */
	private String password;

	/**
	 * Connection timeout.
	 */
	private Duration connectionTimeout;

	/**
	 * Read and Write Socket timeout.
	 */
	private Duration socketTimeout;

	/**
	 * Limit on the number of bytes that can be buffered whenever the input stream needs
	 * to be aggregated.
	 */
	private DataSize maxInMemorySize;

	private boolean customized = false;

	@Deprecated
	@DeprecatedConfigurationProperty(replacement = "spring.elasticsearch.uris")
	public List<String> getEndpoints() {
		return this.endpoints;
	}

	@Deprecated
	@DeprecatedConfigurationProperty(replacement = "spring.elasticsearch.uris")
	public void setEndpoints(List<String> endpoints) {
		this.customized = true;
		this.endpoints = endpoints;
	}

	@Deprecated
	@DeprecatedConfigurationProperty(reason = "Use of SSL should be indicated through an https URI scheme")
	public boolean isUseSsl() {
		return this.useSsl;
	}

	@Deprecated
	public void setUseSsl(boolean useSsl) {
		this.customized = true;
		this.useSsl = useSsl;
	}

	@Deprecated
	@DeprecatedConfigurationProperty(replacement = "spring.elasticsearch.username")
	public String getUsername() {
		return this.username;
	}

	@Deprecated
	public void setUsername(String username) {
		this.customized = true;
		this.username = username;
	}

	@Deprecated
	@DeprecatedConfigurationProperty(replacement = "spring.elasticsearch.password")
	public String getPassword() {
		return this.password;
	}

	@Deprecated
	public void setPassword(String password) {
		this.customized = true;
		this.password = password;
	}

	@Deprecated
	@DeprecatedConfigurationProperty(replacement = "spring.elasticsearch.connection-timeout")
	public Duration getConnectionTimeout() {
		return this.connectionTimeout;
	}

	@Deprecated
	public void setConnectionTimeout(Duration connectionTimeout) {
		this.customized = true;
		this.connectionTimeout = connectionTimeout;
	}

	@Deprecated
	@DeprecatedConfigurationProperty(replacement = "spring.elasticsearch.socket-timeout")
	public Duration getSocketTimeout() {
		return this.socketTimeout;
	}

	@Deprecated
	public void setSocketTimeout(Duration socketTimeout) {
		this.customized = true;
		this.socketTimeout = socketTimeout;
	}

	public DataSize getMaxInMemorySize() {
		return this.maxInMemorySize;
	}

	public void setMaxInMemorySize(DataSize maxInMemorySize) {
		this.customized = true;
		this.maxInMemorySize = maxInMemorySize;
	}

	boolean isCustomized() {
		return this.customized;
	}

}
