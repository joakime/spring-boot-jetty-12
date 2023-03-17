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

package org.springframework.boot.test.autoconfigure.service.connection;

import java.lang.annotation.Annotation;

import org.testcontainers.containers.GenericContainer;

import org.springframework.boot.autoconfigure.service.connection.ConnectionDetails;
import org.springframework.boot.autoconfigure.service.connection.ConnectionDetailsFactory;
import org.springframework.boot.origin.Origin;
import org.springframework.boot.origin.OriginProvider;
import org.springframework.core.ResolvableType;
import org.springframework.util.Assert;

/**
 * Base class for {@link ConnectionDetailsFactory} implementations that provide
 * {@link ConnectionDetails} from a {@link ServiceConnection @ServiceConnection}
 * meta-annotated {@link GenericContainer} field.
 *
 * @param <A> the source annotation type. The annotation will be mergable to a
 * {@link ServiceConnection @ServiceConnection}.
 * @param <D> the connection details type
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 3.1.0
 */
public abstract class ContainerConnectionDetailsFactory<A extends Annotation, D extends ConnectionDetails>
		implements ConnectionDetailsFactory<ServiceConnectedContainer<A>, D> {

	@Override
	public final D getConnectionDetails(ServiceConnectedContainer<A> source) {
		Class<?> annotationType = ResolvableType.forClass(ContainerConnectionDetailsFactory.class, getClass())
			.resolveGeneric();
		if (!annotationType.isInstance(source.getAnnotation())) {
			return null;
		}
		return getContainerConnectionDetails(source);
	}

	/**
	 * Get the {@link ConnectionDetails} from the given {@link ServiceConnectedContainer}
	 * {@code source}. May return {@code null} if no connection can be created. Result
	 * types should consider extending {@link ContainerConnectionDetails}.
	 * @param source the source
	 * @return the service connection or {@code null}.
	 */
	protected abstract D getContainerConnectionDetails(ServiceConnectedContainer<A> source);

	/**
	 * Convenient base class for {@link ConnectionDetails} results that are backed by a
	 * {@link ServiceConnectedContainer}.
	 */
	protected static class ContainerConnectionDetails implements ConnectionDetails, OriginProvider {

		private final Origin origin;

		/**
		 * Create a new {@link ContainerConnectionDetails} instance.
		 * @param source the source {@link ServiceConnectedContainer}
		 */
		protected ContainerConnectionDetails(ServiceConnectedContainer<?> source) {
			Assert.notNull(source, "Source must not be null");
			this.origin = source.getOrigin();
		}

		@Override
		public Origin getOrigin() {
			return this.origin;
		}

	}

}
