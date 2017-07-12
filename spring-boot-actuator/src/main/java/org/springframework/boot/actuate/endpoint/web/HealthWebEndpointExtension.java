/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.actuate.endpoint.web;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.actuate.endpoint.HealthEndpoint;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.endpoint.ReadOperation;
import org.springframework.boot.endpoint.web.WebEndpointExtension;
import org.springframework.boot.endpoint.web.WebEndpointResponse;
import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;

/**
 * {@link WebEndpointExtension} for the {@link HealthEndpoint}.
 *
 * @author Christian Dupuis
 * @author Dave Syer
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author Eddú Meléndez
 * @author Madhura Bhave
 * @since 2.0.0
 */
@WebEndpointExtension(endpoint = HealthEndpoint.class)
public class HealthWebEndpointExtension {

	private Map<String, Integer> statusMapping = new HashMap<>();

	private final HealthEndpoint delegate;

	public HealthWebEndpointExtension(HealthEndpoint delegate) {
		this.delegate = delegate;
		setupDefaultStatusMapping();
	}

	private void setupDefaultStatusMapping() {
		addStatusMapping(Status.DOWN, 503);
		addStatusMapping(Status.OUT_OF_SERVICE, 503);
	}

	/**
	 * Set specific status mappings.
	 * @param statusMapping a map of status code to {@link HttpStatus}
	 */
	public void setStatusMapping(Map<String, Integer> statusMapping) {
		Assert.notNull(statusMapping, "StatusMapping must not be null");
		this.statusMapping = new HashMap<>(statusMapping);
	}

	/**
	 * Add specific status mappings to the existing set.
	 * @param statusMapping a map of status code to {@link HttpStatus}
	 */
	public void addStatusMapping(Map<String, Integer> statusMapping) {
		Assert.notNull(statusMapping, "StatusMapping must not be null");
		this.statusMapping.putAll(statusMapping);
	}

	/**
	 * Add a status mapping to the existing set.
	 * @param status the status to map
	 * @param httpStatus the http status
	 */
	public void addStatusMapping(Status status, Integer httpStatus) {
		Assert.notNull(status, "Status must not be null");
		Assert.notNull(httpStatus, "HttpStatus must not be null");
		addStatusMapping(status.getCode(), httpStatus);
	}

	/**
	 * Add a status mapping to the existing set.
	 * @param statusCode the status code to map
	 * @param httpStatus the http status
	 */
	public void addStatusMapping(String statusCode, Integer httpStatus) {
		Assert.notNull(statusCode, "StatusCode must not be null");
		Assert.notNull(httpStatus, "HttpStatus must not be null");
		this.statusMapping.put(statusCode, httpStatus);
	}

	@ReadOperation
	public WebEndpointResponse<Health> getHealth() {
		Health health = this.delegate.getHealth();
		Integer status = getStatus(health);
		return new WebEndpointResponse<Health>(health, status);
	}

	private int getStatus(Health health) {
		String code = getUniformValue(health.getStatus().getCode());
		if (code != null) {
			return this.statusMapping.keySet().stream()
					.filter((key) -> code.equals(getUniformValue(key)))
					.map(this.statusMapping::get).findFirst().orElse(200);
		}
		return 200;
	}

	private String getUniformValue(String code) {
		if (code == null) {
			return null;
		}
		StringBuilder builder = new StringBuilder();
		for (char ch : code.toCharArray()) {
			if (Character.isAlphabetic(ch) || Character.isDigit(ch)) {
				builder.append(Character.toLowerCase(ch));
			}
		}
		return builder.toString();
	}

}
