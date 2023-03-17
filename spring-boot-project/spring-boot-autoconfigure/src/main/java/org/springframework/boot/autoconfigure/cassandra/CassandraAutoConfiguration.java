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

package org.springframework.boot.autoconfigure.cassandra;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLContext;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.CqlSessionBuilder;
import com.datastax.oss.driver.api.core.config.DefaultDriverOption;
import com.datastax.oss.driver.api.core.config.DriverConfigLoader;
import com.datastax.oss.driver.api.core.config.DriverOption;
import com.datastax.oss.driver.api.core.config.ProgrammaticDriverConfigLoaderBuilder;
import com.datastax.oss.driver.internal.core.config.typesafe.DefaultDriverConfigLoader;
import com.datastax.oss.driver.internal.core.config.typesafe.DefaultProgrammaticDriverConfigLoaderBuilder;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.cassandra.CassandraProperties.Connection;
import org.springframework.boot.autoconfigure.cassandra.CassandraProperties.Controlconnection;
import org.springframework.boot.autoconfigure.cassandra.CassandraProperties.Request;
import org.springframework.boot.autoconfigure.cassandra.CassandraProperties.Throttler;
import org.springframework.boot.autoconfigure.cassandra.CassandraProperties.ThrottlerType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.Resource;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Cassandra.
 *
 * @author Julien Dubois
 * @author Phillip Webb
 * @author Eddú Meléndez
 * @author Stephane Nicoll
 * @author Steffen F. Qvistgaard
 * @author Ittay Stern
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @since 1.3.0
 */
@AutoConfiguration
@ConditionalOnClass({ CqlSession.class })
@EnableConfigurationProperties(CassandraProperties.class)
public class CassandraAutoConfiguration {

	private static final Config SPRING_BOOT_DEFAULTS;
	static {
		CassandraDriverOptions options = new CassandraDriverOptions();
		options.add(DefaultDriverOption.CONTACT_POINTS, Collections.singletonList("127.0.0.1:9042"));
		options.add(DefaultDriverOption.PROTOCOL_COMPRESSION, "none");
		options.add(DefaultDriverOption.CONTROL_CONNECTION_TIMEOUT, (int) Duration.ofSeconds(5).toMillis());
		SPRING_BOOT_DEFAULTS = options.build();
	}

	private final CassandraProperties properties;

	private final CassandraConnectionDetails connectionDetails;

	CassandraAutoConfiguration(CassandraProperties properties,
			ObjectProvider<CassandraConnectionDetails> connectionDetails) {
		this.properties = properties;
		this.connectionDetails = connectionDetails
			.getIfAvailable(() -> new PropertiesCassandraConnectionDetails(properties));
	}

	@Bean
	@ConditionalOnMissingBean
	@Lazy
	public CqlSession cassandraSession(CqlSessionBuilder cqlSessionBuilder) {
		return cqlSessionBuilder.build();
	}

	@Bean
	@ConditionalOnMissingBean
	@Scope("prototype")
	public CqlSessionBuilder cassandraSessionBuilder(DriverConfigLoader driverConfigLoader,
			ObjectProvider<CqlSessionBuilderCustomizer> builderCustomizers) {
		CqlSessionBuilder builder = CqlSession.builder().withConfigLoader(driverConfigLoader);
		configureAuthentication(builder);
		configureSsl(builder);
		builder.withKeyspace(this.properties.getKeyspaceName());
		builderCustomizers.orderedStream().forEach((customizer) -> customizer.customize(builder));
		return builder;
	}

	private void configureAuthentication(CqlSessionBuilder builder) {
		String username = this.connectionDetails.getUsername();
		if (username != null) {
			builder.withAuthCredentials(username, this.connectionDetails.getPassword());
		}
	}

	private void configureSsl(CqlSessionBuilder builder) {
		if (this.connectionDetails instanceof PropertiesCassandraConnectionDetails && this.properties.isSsl()) {
			try {
				builder.withSslContext(SSLContext.getDefault());
			}
			catch (NoSuchAlgorithmException ex) {
				throw new IllegalStateException("Could not setup SSL default context for Cassandra", ex);
			}
		}
	}

	@Bean(destroyMethod = "")
	@ConditionalOnMissingBean
	public DriverConfigLoader cassandraDriverConfigLoader(
			ObjectProvider<DriverConfigLoaderBuilderCustomizer> builderCustomizers) {
		ProgrammaticDriverConfigLoaderBuilder builder = new DefaultProgrammaticDriverConfigLoaderBuilder(
				() -> cassandraConfiguration(), DefaultDriverConfigLoader.DEFAULT_ROOT_PATH);
		builderCustomizers.orderedStream().forEach((customizer) -> customizer.customize(builder));
		return builder.build();
	}

	private Config cassandraConfiguration() {
		ConfigFactory.invalidateCaches();
		Config config = ConfigFactory.defaultOverrides();
		config = config.withFallback(mapConfig());
		if (this.properties.getConfig() != null) {
			config = config.withFallback(loadConfig(this.properties.getConfig()));
		}
		config = config.withFallback(SPRING_BOOT_DEFAULTS);
		config = config.withFallback(ConfigFactory.defaultReference());
		return config.resolve();
	}

	private Config loadConfig(Resource resource) {
		try {
			return ConfigFactory.parseURL(resource.getURL());
		}
		catch (IOException ex) {
			throw new IllegalStateException("Failed to load cassandra configuration from " + resource, ex);
		}
	}

	private Config mapConfig() {
		CassandraDriverOptions options = new CassandraDriverOptions();
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		map.from(this.properties.getSessionName())
			.whenHasText()
			.to((sessionName) -> options.add(DefaultDriverOption.SESSION_NAME, sessionName));
		map.from(this.connectionDetails.getUsername())
			.to((value) -> options.add(DefaultDriverOption.AUTH_PROVIDER_USER_NAME, value)
				.add(DefaultDriverOption.AUTH_PROVIDER_PASSWORD, this.connectionDetails.getPassword()));
		map.from(this.properties::getCompression)
			.to((compression) -> options.add(DefaultDriverOption.PROTOCOL_COMPRESSION, compression));
		mapConnectionOptions(options);
		mapPoolingOptions(options);
		mapRequestOptions(options);
		mapControlConnectionOptions(options);
		map.from(mapContactPoints())
			.to((contactPoints) -> options.add(DefaultDriverOption.CONTACT_POINTS, contactPoints));
		map.from(this.connectionDetails.getLocalDatacenter())
			.whenHasText()
			.to((localDatacenter) -> options.add(DefaultDriverOption.LOAD_BALANCING_LOCAL_DATACENTER, localDatacenter));
		return options.build();
	}

	private void mapConnectionOptions(CassandraDriverOptions options) {
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		Connection connectionProperties = this.properties.getConnection();
		map.from(connectionProperties::getConnectTimeout)
			.asInt(Duration::toMillis)
			.to((connectTimeout) -> options.add(DefaultDriverOption.CONNECTION_CONNECT_TIMEOUT, connectTimeout));
		map.from(connectionProperties::getInitQueryTimeout)
			.asInt(Duration::toMillis)
			.to((initQueryTimeout) -> options.add(DefaultDriverOption.CONNECTION_INIT_QUERY_TIMEOUT, initQueryTimeout));
	}

	private void mapPoolingOptions(CassandraDriverOptions options) {
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		CassandraProperties.Pool poolProperties = this.properties.getPool();
		map.from(poolProperties::getIdleTimeout)
			.asInt(Duration::toMillis)
			.to((idleTimeout) -> options.add(DefaultDriverOption.HEARTBEAT_TIMEOUT, idleTimeout));
		map.from(poolProperties::getHeartbeatInterval)
			.asInt(Duration::toMillis)
			.to((heartBeatInterval) -> options.add(DefaultDriverOption.HEARTBEAT_INTERVAL, heartBeatInterval));
	}

	private void mapRequestOptions(CassandraDriverOptions options) {
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		Request requestProperties = this.properties.getRequest();
		map.from(requestProperties::getTimeout)
			.asInt(Duration::toMillis)
			.to(((timeout) -> options.add(DefaultDriverOption.REQUEST_TIMEOUT, timeout)));
		map.from(requestProperties::getConsistency)
			.to(((consistency) -> options.add(DefaultDriverOption.REQUEST_CONSISTENCY, consistency)));
		map.from(requestProperties::getSerialConsistency)
			.to((serialConsistency) -> options.add(DefaultDriverOption.REQUEST_SERIAL_CONSISTENCY, serialConsistency));
		map.from(requestProperties::getPageSize)
			.to((pageSize) -> options.add(DefaultDriverOption.REQUEST_PAGE_SIZE, pageSize));
		Throttler throttlerProperties = requestProperties.getThrottler();
		map.from(throttlerProperties::getType)
			.as(ThrottlerType::type)
			.to((type) -> options.add(DefaultDriverOption.REQUEST_THROTTLER_CLASS, type));
		map.from(throttlerProperties::getMaxQueueSize)
			.to((maxQueueSize) -> options.add(DefaultDriverOption.REQUEST_THROTTLER_MAX_QUEUE_SIZE, maxQueueSize));
		map.from(throttlerProperties::getMaxConcurrentRequests)
			.to((maxConcurrentRequests) -> options.add(DefaultDriverOption.REQUEST_THROTTLER_MAX_CONCURRENT_REQUESTS,
					maxConcurrentRequests));
		map.from(throttlerProperties::getMaxRequestsPerSecond)
			.to((maxRequestsPerSecond) -> options.add(DefaultDriverOption.REQUEST_THROTTLER_MAX_REQUESTS_PER_SECOND,
					maxRequestsPerSecond));
		map.from(throttlerProperties::getDrainInterval)
			.asInt(Duration::toMillis)
			.to((drainInterval) -> options.add(DefaultDriverOption.REQUEST_THROTTLER_DRAIN_INTERVAL, drainInterval));
	}

	private void mapControlConnectionOptions(CassandraDriverOptions options) {
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		Controlconnection controlProperties = this.properties.getControlconnection();
		map.from(controlProperties::getTimeout)
			.asInt(Duration::toMillis)
			.to((timeout) -> options.add(DefaultDriverOption.CONTROL_CONNECTION_TIMEOUT, timeout));
	}

	private List<String> mapContactPoints() {
		return this.connectionDetails.getContactPoints()
			.stream()
			.map((node) -> node.host() + ":" + node.port())
			.toList();
	}

	private static class CassandraDriverOptions {

		private final Map<String, String> options = new LinkedHashMap<>();

		private CassandraDriverOptions add(DriverOption option, String value) {
			String key = createKeyFor(option);
			this.options.put(key, value);
			return this;
		}

		private CassandraDriverOptions add(DriverOption option, int value) {
			return add(option, String.valueOf(value));
		}

		private CassandraDriverOptions add(DriverOption option, Enum<?> value) {
			return add(option, value.name());
		}

		private CassandraDriverOptions add(DriverOption option, List<String> values) {
			for (int i = 0; i < values.size(); i++) {
				this.options.put(String.format("%s.%s", createKeyFor(option), i), values.get(i));
			}
			return this;
		}

		private Config build() {
			return ConfigFactory.parseMap(this.options, "Environment");
		}

		private static String createKeyFor(DriverOption option) {
			return String.format("%s.%s", DefaultDriverConfigLoader.DEFAULT_ROOT_PATH, option.getPath());
		}

	}

	private static class PropertiesCassandraConnectionDetails implements CassandraConnectionDetails {

		private final CassandraProperties properties;

		public PropertiesCassandraConnectionDetails(CassandraProperties properties) {
			this.properties = properties;
		}

		@Override
		public List<Node> getContactPoints() {
			List<String> contactPoints = this.properties.getContactPoints();
			return (contactPoints != null) ? contactPoints.stream().map(this::asNode).toList()
					: Collections.emptyList();
		}

		@Override
		public String getUsername() {
			return this.properties.getUsername();
		}

		@Override
		public String getPassword() {
			return this.properties.getPassword();
		}

		@Override
		public String getLocalDatacenter() {
			return this.properties.getLocalDatacenter();
		}

		private Node asNode(String contactPoint) {
			int i = contactPoint.lastIndexOf(':');
			if (i >= 0) {
				String portCandidate = contactPoint.substring(i + 1);
				Integer port = asPort(portCandidate);
				if (port != null) {
					return new Node(contactPoint.substring(0, i), port);
				}
			}
			return new Node(contactPoint, this.properties.getPort());
		}

		private Integer asPort(String value) {
			try {
				int i = Integer.parseInt(value);
				return (i > 0 && i < 65535) ? i : null;
			}
			catch (Exception ex) {
				return null;
			}
		}

	}

}
