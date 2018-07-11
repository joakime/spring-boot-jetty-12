/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.autoconfigure.jdbc;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import javax.sql.DataSource;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.io.ResourceLoader;

/**
 * A registry for {@link DataSource} beans that may have their schema created and
 * initialized.
 *
 * @author Andy Wilkinson
 */
class StandardDataSourceInitializationRegistry
		implements DataSourceInitializationRegistry, BeanPostProcessor, Ordered,
		ApplicationListener<DataSourceSchemaCreatedEvent> {

	private final Map<DataSource, DataSourceInitializer> initializers = new HashMap<>();

	private final ApplicationEventPublisher eventPublisher;

	private final ResourceLoader resourceLoader;

	StandardDataSourceInitializationRegistry(ApplicationEventPublisher eventPublisher,
			ResourceLoader resourceLoader) {
		this.eventPublisher = eventPublisher;
		this.resourceLoader = resourceLoader;

	}

	@Override
	public void register(DataSource dataSource, DataSourceProperties properties) {
		this.initializers.put(dataSource,
				new DataSourceInitializer(dataSource, properties, this.resourceLoader));
	}

	@Override
	public boolean replace(DataSource existingDataSource, DataSource newDataSource) {
		return doIfRegistered(existingDataSource, (initializer) -> this.initializers.put(
				newDataSource,
				new DataSourceInitializer(newDataSource, initializer.getProperties())));
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName)
			throws BeansException {
		doIfRegistered(bean, this::createSchema);
		return bean;
	}

	private void createSchema(DataSourceInitializer initializer) {
		if (initializer.createSchema()) {
			this.eventPublisher.publishEvent(
					new DataSourceSchemaCreatedEvent(initializer.getDataSource()));
		}
	}

	@Override
	public void onApplicationEvent(DataSourceSchemaCreatedEvent event) {
		doIfRegistered(event.getSource(), DataSourceInitializer::initSchema);
	}

	private boolean doIfRegistered(Object candidate,
			Consumer<DataSourceInitializer> consumer) {
		DataSourceInitializer initializer = this.initializers.get(candidate);
		if (initializer != null) {
			consumer.accept(initializer);
			return true;
		}
		return false;
	}

	@Override
	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE;
	}

}
