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

package org.springframework.boot.autoconfigure.sql.init;

import java.nio.charset.Charset;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@link ConfigurationProperties Configuration properties} for initializing an SQL
 * database.
 *
 * @author Andy Wilkinson
 * @since 2.5.0
 */
@ConfigurationProperties("spring.sql.init")
public class SqlInitializationProperties {

	/**
	 * Locations of DDL (schema) scripts to apply to the database.
	 */
	private List<String> ddlScriptLocations;

	/**
	 * Locations of DML (data) scripts to apply to the database.
	 */
	private List<String> dmlScriptLocations;

	/**
	 * Platform to use in the default DDL or DML script locations schema-${platform}.sql
	 * and data-${platform}.sql.
	 */
	private String platform = "all";

	/**
	 * Username of the database to use when executing initialization scripts (if
	 * different).
	 */
	private String username;

	/**
	 * Password of the database to use when exexuting initialization scripts (if
	 * different).
	 */
	private String password;

	/**
	 * Whether initialization should continue when an error occurs.
	 */
	private boolean continueOnError = false;

	/**
	 * Statement separator in the DDL and DML scripts.
	 */
	private String separator = ";";

	/**
	 * Encoding of the DDL and DML scripts.
	 */
	private Charset encoding;

	public List<String> getDdlScriptLocations() {
		return this.ddlScriptLocations;
	}

	public void setDdlScriptLocations(List<String> ddlScriptLocations) {
		this.ddlScriptLocations = ddlScriptLocations;
	}

	public List<String> getDmlScriptLocations() {
		return this.dmlScriptLocations;
	}

	public void setDmlScriptLocations(List<String> dmlScriptLocations) {
		this.dmlScriptLocations = dmlScriptLocations;
	}

	public String getPlatform() {
		return this.platform;
	}

	public void setPlatform(String platform) {
		this.platform = platform;
	}

	public String getUsername() {
		return this.username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return this.password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public boolean isContinueOnError() {
		return this.continueOnError;
	}

	public void setContinueOnError(boolean continueOnError) {
		this.continueOnError = continueOnError;
	}

	public String getSeparator() {
		return this.separator;
	}

	public void setSeparator(String separator) {
		this.separator = separator;
	}

	public Charset getEncoding() {
		return this.encoding;
	}

	public void setEncoding(Charset encoding) {
		this.encoding = encoding;
	}

}
