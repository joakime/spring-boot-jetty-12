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

package org.springframework.boot.actuate.endpoint;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.springframework.boot.actuate.endpoint.http.ApiVersion;
import org.springframework.boot.actuate.endpoint.invoke.OperationInvoker;
import org.springframework.util.Assert;

/**
 * The context for the {@link OperationInvoker invocation of an operation}.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 2.0.0
 */
public class InvocationContext {

	private final Map<String, Object> arguments;

	private final List<OperationArgumentResolver> argumentResolvers;

	/**
	 * Creates a new context for an operation being invoked by the given
	 * {@code securityContext} with the given available {@code arguments}.
	 * @param securityContext the current security context. Never {@code null}
	 * @param arguments the arguments available to the operation. Never {@code null}
	 */
	public InvocationContext(SecurityContext securityContext, Map<String, Object> arguments) {
		this(null, securityContext, arguments);
	}

	/**
	 * Creates a new context for an operation being invoked by the given
	 * {@code securityContext} with the given available {@code arguments}.
	 * @param apiVersion the API version or {@code null} to use the latest
	 * @param securityContext the current security context. Never {@code null}
	 * @param arguments the arguments available to the operation. Never {@code null}
	 * @since 2.2.0
	 * @deprecated since 2.5.0 in favor of
	 * {@link #InvocationContext(SecurityContext, Map, List)}
	 */
	@Deprecated
	public InvocationContext(ApiVersion apiVersion, SecurityContext securityContext, Map<String, Object> arguments) {
		this(securityContext, arguments, Arrays.asList(new FixedValueArgumentResolver<>(ApiVersion.class, apiVersion)));
	}

	/**
	 * Creates a new context for an operation being invoked by the given
	 * {@code securityContext} with the given available {@code arguments}.
	 * @param securityContext the current security context. Never {@code null}
	 * @param arguments the arguments available to the operation. Never {@code null}
	 * @param argumentResolvers resolvers for additional arguments should be available to
	 * the operation.
	 */
	public InvocationContext(SecurityContext securityContext, Map<String, Object> arguments,
			List<OperationArgumentResolver> argumentResolvers) {
		Assert.notNull(securityContext, "SecurityContext must not be null");
		Assert.notNull(arguments, "Arguments must not be null");
		this.arguments = arguments;
		this.argumentResolvers = new ArrayList<>();
		if (argumentResolvers != null) {
			this.argumentResolvers.addAll(argumentResolvers);
		}
		this.argumentResolvers.add(new FixedValueArgumentResolver<>(SecurityContext.class, securityContext));
		this.argumentResolvers.add(new SuppliedValueArgumentResolver<>(Principal.class, securityContext::getPrincipal));
		this.argumentResolvers.add(new FixedValueArgumentResolver<>(ApiVersion.class, ApiVersion.LATEST));
	}

	/**
	 * Return the API version in use.
	 * @return the apiVersion the API version
	 * @since 2.2.0
	 */
	public ApiVersion getApiVersion() {
		return resolveArgument(ApiVersion.class);
	}

	/**
	 * Return the security context to use for the invocation.
	 * @return the security context
	 * @deprecated since 2.5.0 in favor of {@link #resolveArgument(Class)}
	 */
	@Deprecated
	public SecurityContext getSecurityContext() {
		return resolveArgument(SecurityContext.class);
	}

	/**
	 * Return the invocation arguments.
	 * @return the arguments
	 */
	public Map<String, Object> getArguments() {
		return this.arguments;
	}

	/**
	 * Resolves an argument with the given {@code argumentType}.
	 * @param <T> type of the argument
	 * @param argumentType type of the argument
	 * @return resolved argument of the required type or {@code null}
	 * @since 2.5.0
	 * @see #canResolve(Class)
	 */
	public <T> T resolveArgument(Class<T> argumentType) {
		for (OperationArgumentResolver argumentResolver : this.argumentResolvers) {
			if (argumentResolver.canResolve(argumentType)) {
				T result = argumentResolver.resolve(argumentType);
				if (result != null) {
					return result;
				}
			}
		}
		return null;
	}

	/**
	 * Returns whether or not the context is capable of resolving an argument of the given
	 * {@code type}. Note that, even when {@code true} is returned,
	 * {@link #resolveArgument argument resolution} will return {@code null} if no
	 * argument of the required type is available.
	 * @param type argument type
	 * @return {@code true} if resolution of arguments of the given type is possible,
	 * otherwise {@code false}.
	 * @since 2.5.0
	 * @see #resolveArgument(Class)
	 */
	public boolean canResolve(Class<?> type) {
		for (OperationArgumentResolver argumentResolver : this.argumentResolvers) {
			if (argumentResolver.canResolve(type)) {
				return true;
			}
		}
		return false;
	}

	private static final class FixedValueArgumentResolver<T> implements OperationArgumentResolver {

		private final Class<T> argumentType;

		private final T value;

		private FixedValueArgumentResolver(Class<T> argumentType, T value) {
			this.argumentType = argumentType;
			this.value = value;
		}

		@SuppressWarnings("unchecked")
		@Override
		public <U> U resolve(Class<U> type) {
			return (U) this.value;
		}

		@Override
		public boolean canResolve(Class<?> type) {
			return this.argumentType.equals(type);
		}

	}

	private static final class SuppliedValueArgumentResolver<T> implements OperationArgumentResolver {

		private final Class<T> argumentType;

		private final Supplier<T> value;

		private SuppliedValueArgumentResolver(Class<T> argumentType, Supplier<T> value) {
			this.argumentType = argumentType;
			this.value = value;
		}

		@SuppressWarnings("unchecked")
		@Override
		public <U> U resolve(Class<U> type) {
			return (U) this.value.get();
		}

		@Override
		public boolean canResolve(Class<?> type) {
			return this.argumentType.equals(type);
		}

	}

}
