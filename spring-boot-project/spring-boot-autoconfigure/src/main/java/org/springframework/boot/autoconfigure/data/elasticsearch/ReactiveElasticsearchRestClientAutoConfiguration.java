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

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import reactor.netty.http.client.HttpClient;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.reactive.ReactiveElasticsearchClient;
import org.springframework.data.elasticsearch.client.reactive.ReactiveRestClients;
import org.springframework.http.HttpHeaders;
import org.springframework.util.Assert;
import org.springframework.util.unit.DataSize;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Elasticsearch Reactive REST
 * clients.
 *
 * @author Brian Clozel
 * @since 2.2.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({ ReactiveRestClients.class, WebClient.class, HttpClient.class })
@EnableConfigurationProperties({ ElasticsearchProperties.class, ReactiveElasticsearchRestClientProperties.class })
public class ReactiveElasticsearchRestClientAutoConfiguration {

	private final ConsolidatedProperties properties;

	ReactiveElasticsearchRestClientAutoConfiguration(ElasticsearchProperties properties,
			ReactiveElasticsearchRestClientProperties reactiveProperties) {
		this.properties = new ConsolidatedProperties(properties, reactiveProperties);
	}

	@Bean
	@ConditionalOnMissingBean
	public ClientConfiguration clientConfiguration() {
		ClientConfiguration.MaybeSecureClientConfigurationBuilder builder = ClientConfiguration.builder()
				.connectedTo(this.properties.getEndpoints().toArray(new String[0]));
		if (this.properties.isUseSsl()) {
			builder.usingSsl();
		}
		configureTimeouts(builder);
		configureExchangeStrategies(builder);
		return builder.build();
	}

	private void configureTimeouts(ClientConfiguration.TerminalClientConfigurationBuilder builder) {
		PropertyMapper map = PropertyMapper.get();
		map.from(this.properties.getConnectionTimeout()).whenNonNull().to(builder::withConnectTimeout);
		map.from(this.properties.getSocketTimeout()).whenNonNull().to(builder::withSocketTimeout);
		map.from(this.properties.getCredentials()).whenNonNull().as(ConsolidatedProperties.Credentials::getUsername)
				.whenHasText().to((username) -> {
					HttpHeaders headers = new HttpHeaders();
					headers.setBasicAuth(username, this.properties.getCredentials().getPassword());
					builder.withDefaultHeaders(headers);
				});
	}

	private void configureExchangeStrategies(ClientConfiguration.TerminalClientConfigurationBuilder builder) {
		PropertyMapper map = PropertyMapper.get();
		builder.withWebClientConfigurer((webClient) -> {
			ExchangeStrategies exchangeStrategies = ExchangeStrategies.builder()
					.codecs((configurer) -> map.from(this.properties.getMaxInMemorySize()).whenNonNull()
							.asInt(DataSize::toBytes)
							.to((maxInMemorySize) -> configurer.defaultCodecs().maxInMemorySize(maxInMemorySize)))
					.build();
			return webClient.mutate().exchangeStrategies(exchangeStrategies).build();
		});
	}

	@Bean
	@ConditionalOnMissingBean
	public ReactiveElasticsearchClient reactiveElasticsearchClient(ClientConfiguration clientConfiguration) {
		return ReactiveRestClients.create(clientConfiguration);
	}

	private static final class ConsolidatedProperties {

		private final ElasticsearchProperties properties;

		private final ReactiveElasticsearchRestClientProperties reactiveProperties;

		private final List<URI> uris;

		private ConsolidatedProperties(ElasticsearchProperties properties,
				ReactiveElasticsearchRestClientProperties reactiveProperties) {
			this.properties = properties;
			this.reactiveProperties = reactiveProperties;
			this.uris = properties.getUris().stream().map((s) -> s.startsWith("http") ? s : "http://" + s)
					.map(URI::create).collect(Collectors.toList());
		}

		@SuppressWarnings("deprecation")
		private List<String> getEndpoints() {
			if (this.reactiveProperties.isCustomized()) {
				return this.reactiveProperties.getEndpoints();
			}
			return this.uris.stream().map((uri) -> uri.getHost() + ":" + uri.getPort()).collect(Collectors.toList());
		}

		@SuppressWarnings("deprecation")
		private Credentials getCredentials() {
			if (this.reactiveProperties.isCustomized()) {
				return new Credentials(this.reactiveProperties.getUsername(), this.reactiveProperties.getPassword());
			}
			Credentials propertyCredentials = Credentials.from(this.properties);
			Credentials uriCredentials = Credentials.from(this.properties.getUris());
			if (uriCredentials == null) {
				return propertyCredentials;
			}
			if (propertyCredentials != null && !uriCredentials.equals(propertyCredentials)) {
				throw new IllegalArgumentException(
						"Credentials from URI user info do not match those from spring.elasticsearch.username and "
								+ "spring.elasticsearch.password");
			}
			return uriCredentials;

		}

		@SuppressWarnings("deprecation")
		private Duration getConnectionTimeout() {
			return this.reactiveProperties.isCustomized() ? this.reactiveProperties.getConnectionTimeout()
					: this.properties.getConnectionTimeout();
		}

		@SuppressWarnings("deprecation")
		private Duration getSocketTimeout() {
			return this.reactiveProperties.isCustomized() ? this.reactiveProperties.getSocketTimeout()
					: this.properties.getSocketTimeout();
		}

		@SuppressWarnings("deprecation")
		private boolean isUseSsl() {
			if (this.reactiveProperties.isCustomized()) {
				return this.reactiveProperties.isUseSsl();
			}
			Set<String> schemes = this.uris.stream().map((uri) -> uri.getScheme()).collect(Collectors.toSet());
			Assert.isTrue(schemes.size() == 1, () -> "Configured Elasticsearch URIs have varying schemes");
			return schemes.iterator().next().equals("https");
		}

		private DataSize getMaxInMemorySize() {
			return this.reactiveProperties.getMaxInMemorySize();
		}

		private static final class Credentials {

			private final String username;

			private final String password;

			private Credentials(String username, String password) {
				this.username = username;
				this.password = password;
			}

			private String getUsername() {
				return this.username;
			}

			private String getPassword() {
				return this.password;
			}

			private static Credentials from(List<String> uris) {
				Set<String> userInfos = uris.stream().map(URI::create).map((uri) -> uri.getUserInfo())
						.collect(Collectors.toSet());
				Assert.isTrue(userInfos.size() == 1, () -> "Configured Elasticsearch URIs have varying user infos");
				String userInfo = userInfos.iterator().next();
				if (userInfo != null) {
					String[] parts = userInfo.split(":");
					return new Credentials(parts[0], (parts.length == 2) ? parts[1] : "");
				}
				return null;
			}

			private static Credentials from(ElasticsearchProperties properties) {
				String username = properties.getUsername();
				String password = properties.getPassword();
				if (username == null && password == null) {
					return null;
				}
				return new Credentials(username, password);
			}

			@Override
			public boolean equals(Object obj) {
				if (this == obj) {
					return true;
				}
				if (obj == null) {
					return false;
				}
				if (getClass() != obj.getClass()) {
					return false;
				}
				Credentials other = (Credentials) obj;
				if (this.password == null) {
					if (other.password != null) {
						return false;
					}
				}
				else if (!this.password.equals(other.password)) {
					return false;
				}
				if (this.username == null) {
					if (other.username != null) {
						return false;
					}
				}
				else if (!this.username.equals(other.username)) {
					return false;
				}
				return true;
			}

			@Override
			public int hashCode() {
				final int prime = 31;
				int result = 1;
				result = prime * result + ((this.password == null) ? 0 : this.password.hashCode());
				result = prime * result + ((this.username == null) ? 0 : this.username.hashCode());
				return result;
			}

		}

	}

}
