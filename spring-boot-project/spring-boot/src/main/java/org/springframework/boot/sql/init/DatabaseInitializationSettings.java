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

package org.springframework.boot.sql.init;

import java.nio.charset.Charset;
import java.util.List;

/**
 * Settings for initializing an SQL database.
 *
 * @author Andy Wilkinson
 * @since 2.5.0
 */
public class DatabaseInitializationSettings {

	/**
	 * Locations of DDL (schema) scripts to apply to the database.
	 */
	private List<String> ddlScriptLocations;

	/**
	 * Locations of DML (data) scripts to apply to the database.
	 */
	private List<String> dmlScriptLocations;

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
