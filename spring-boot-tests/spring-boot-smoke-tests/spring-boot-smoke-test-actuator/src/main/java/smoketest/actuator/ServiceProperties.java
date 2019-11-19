/*
 * Copyright 2012-2019 the original author or authors.
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

package smoketest.actuator;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "service", ignoreUnknownFields = false)
public class ServiceProperties {

	/**
	 * Name of the service.
	 */
	private String name = "World";

	private List<List<String>> stringList = new ArrayList<>();

	private Map<String, Map<String, String>> stringMap = new LinkedHashMap<>();

	private Map<String, String> simpleMap = new LinkedHashMap<>();

	private Map<String, Foo> fooMap = new LinkedHashMap<>();

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Map<String, Foo> getFooMap() {
		return fooMap;
	}

	public Map<String, Map<String, String>> getStringMap() {
		return this.stringMap;
	}

	public Map<String, String> getSimpleMap() {
		return simpleMap;
	}

	public List<List<String>> getStringList() {
		return stringList;
	}

	static class Foo {

		private Set<Bar> barSet = new LinkedHashSet<>();

		public Set<Bar> getBarSet() {
			return barSet;
		}

	}

	static class Bar {

		private String value;

		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}

	}

}
