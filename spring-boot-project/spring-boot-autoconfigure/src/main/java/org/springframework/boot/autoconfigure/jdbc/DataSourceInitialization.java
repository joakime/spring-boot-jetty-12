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

package org.springframework.boot.autoconfigure.jdbc;

import javax.sql.DataSource;

import org.springframework.beans.factory.InitializingBean;

/**
 * {@link InitializingBean} that performs {@link DataSource} initialization using DDL and
 * DML scripts.
 *
 * @author Andy Wilkinson
 * @since 2.5.0
 */
public class DataSourceInitialization implements InitializingBean {

	private final DataSource dataSource;

	private final DataSourceProperties properies;

	public DataSourceInitialization(DataSource dataSource, DataSourceProperties properies) {
		this.dataSource = dataSource;
		this.properies = properies;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		new DataSourceInitializer(this.dataSource, this.properies).initializeDataSource();
	}

	public DataSource getDataSource() {
		return this.dataSource;
	}

}
