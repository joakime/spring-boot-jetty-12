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

package org.springframework.boot.docker.compose.lifecycle;

/**
 * Docker compose lifecycle management.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 3.1.0
 */
public enum LifecycleManagement {

	/**
	 * Don't start up or shut down docker compose.
	 */
	NONE(false, false),

	/**
	 * Start up docker compose if it's not running.
	 */
	START_UP_ONLY(true, false),

	/**
	 * Start up docker compose if it's not running and shut it down afterwards.
	 */
	START_UP_AND_SHUT_DOWN(true, true);

	private final boolean startUp;

	private final boolean shutDown;

	LifecycleManagement(boolean startUp, boolean shutDown) {
		this.startUp = startUp;
		this.shutDown = shutDown;
	}

	/**
	 * Return whether docker compose should be started up.
	 * @return whether docker compose should be started up
	 */
	boolean shouldStartUp() {
		return this.startUp;
	}

	/**
	 * Return whether docker compose should be shut down.
	 * @return whether docker compose should be shut down
	 */
	boolean shouldShutDown() {
		return this.shutDown;
	}

}
