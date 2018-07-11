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

import javax.sql.DataSource;

/**
 * A registry for {@link DataSource} beans that wish to have their schema created and
 * initialized.
 *
 * @author Andy Wilkinson
 * @since 2.1.0
 */
public interface DataSourceInitializationRegistry {

	/**
	 * Registers the given {@code dataSource} for initialization. The initialization will
	 * be configured using the given {@code properties}.
	 * @param dataSource the data source
	 * @param properties the properties
	 */
	void register(DataSource dataSource, DataSourceProperties properties);

	/**
	 * Replaces the registration for the given {@code existingDataSource} with the given
	 * {@code newDataSource}, reusing the {@link DataSourceProperties} from the existing
	 * registration. Typically used when post-processing of a {@link DataSource} means
	 * that the original {@code DataSource} and the pre-processed {@code DataSource} will
	 * no longer be {@link Object#equals(Object) equal}.
	 * @param existingDataSource the existing data source to replace
	 * @param newDataSource the new data source to use as a replacement
	 * @return {@code true} if the data source was registered and has been replaced,
	 * otherwise {@code false}
	 */
	boolean replace(DataSource existingDataSource, DataSource newDataSource);

}
