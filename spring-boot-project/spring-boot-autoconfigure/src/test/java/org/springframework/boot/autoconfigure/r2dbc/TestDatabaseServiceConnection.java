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

package org.springframework.boot.autoconfigure.r2dbc;

import org.springframework.boot.autoconfigure.database.DatabaseServiceConnection;
import org.springframework.boot.origin.Origin;

/**
 * {@link DatabaseServiceConnection} used in tests.
 *
 * @author Moritz Halbritter
 */
class TestDatabaseServiceConnection implements DatabaseServiceConnection {

	@Override
	public DatabaseType getType() {
		return DatabaseType.POSTGRESQL;
	}

	@Override
	public String getHostname() {
		return "postgres.example.com";
	}

	@Override
	public int getPort() {
		return 12345;
	}

	@Override
	public String getUsername() {
		return "user-1";
	}

	@Override
	public String getPassword() {
		return "password-1";
	}

	@Override
	public String getDatabase() {
		return "database-1";
	}

	@Override
	public String getName() {
		return "test-service-connection";
	}

	@Override
	public Origin getOrigin() {
		return null;
	}

}
