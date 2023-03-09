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

package org.springframework.boot.autoconfigure.sql;

import org.springframework.boot.autoconfigure.serviceconnection.ServiceConnection;

/**
 * A connection to a SQL database service.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @since 3.1.0
 */
public interface SqlServiceConnection extends ServiceConnection {

	/**
	 * Hostname of the database.
	 * @return the hostname of the database.
	 */
	String getHostname();

	/**
	 * Port of the database.
	 * @return the port of the database
	 */
	int getPort();

	/**
	 * Hostname for the database.
	 * @return the username for the database
	 */
	String getUsername();

	/**
	 * Password for the database.
	 * @return the password for the database
	 */
	String getPassword();

	/**
	 * The name of the database to use.
	 * @return the name of the database to use.
	 */
	String getDatabase();

	/**
	 * The product name of the database.
	 * @return the product name
	 */
	String getProductName();

}
