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

package org.springframework.boot.actuate.endpoint2;

import java.lang.reflect.Method;

public class EndpointOperationInfo {

	private final String beanName;

	private final Method operationMethod;

	private final EndpointOperationType type;

	public EndpointOperationInfo(String beanName, Method method,
			EndpointOperationType type) {
		this.beanName = beanName;
		this.operationMethod = method;
		this.type = type;
	}

	public String getBeanName() {
		return this.beanName;
	}

	public Method getOperationMethod() {
		return this.operationMethod;
	}

	public EndpointOperationType getType() {
		return this.type;
	}

}
