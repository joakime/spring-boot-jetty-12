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

package org.springframework.boot.ssl;

/**
 * Configured {@link SslBundle}s that can be retrieved by name.
 *
 * @author Scott Frederick
 * @since 3.1.0
 */
public interface SslBundles {

	// TODO Rename to SslBundleRegistry?
	// Now I've looked at auto-configure, it has SslBundleRegistry that implements
	// SslBundles. Should that implementation move into spring-boot? As things stand,
	// using SslBundles requires spring-boot-autoconfigure or writing your own
	// implementation

	/**
	 * Return an {@link SslBundle} with the provided name.
	 * @param name the bundle name
	 * @return the bundle
	 */
	// TODO Behavior when a bundle with the given name isn't registered.
	// SslBundleRegistry throws IllegalArgumentException. Should it be
	// IllegalStateException, a custom exception, or should we just return null. FWIW,
	// BootstrapRegistry returns null.
	SslBundle getBundle(String name);

}
