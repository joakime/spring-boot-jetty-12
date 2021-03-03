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

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.util.CollectionUtils;

/**
 * {@link InitializingBean} base class that performs database initialization using DDL and
 * DML scripts.
 *
 * @author Andy Wilkinson
 * @since 2.5.0
 */
public abstract class AbstractScriptDatabaseInitializer implements InitializingBean, ResourceLoaderAware {

	private final DatabaseInitializationSettings settings;

	private volatile ResourceLoader resourceLoader;

	/**
	 * Creates a new {@link AbstractScriptDatabaseInitializer} that will initialize the
	 * database using the given settings.
	 * @param settings initialization settings
	 */
	protected AbstractScriptDatabaseInitializer(DatabaseInitializationSettings settings) {
		this.settings = settings;
	}

	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		initializeDatabase();
	}

	/**
	 * Initializes the database by running DDL and DML scripts.
	 * @return {@code true} if one or more scripts were applied to the database, otherwise
	 * {@code false}
	 */
	public boolean initializeDatabase() {
		ScriptLocationResolver locationResolver = new ScriptLocationResolver(this.resourceLoader);
		boolean initialized = applyDdlScripts(locationResolver);
		initialized = applyDmlScripts(locationResolver) || initialized;
		return initialized;
	}

	private boolean applyDdlScripts(ScriptLocationResolver locationResolver) {
		return applyScripts(this.settings.getDdlScriptLocations(), "DDL", locationResolver);
	}

	private boolean applyDmlScripts(ScriptLocationResolver locationResolver) {
		return applyScripts(this.settings.getDmlScriptLocations(), "DML", locationResolver);
	}

	private boolean applyScripts(List<String> locations, String type, ScriptLocationResolver locationResolver) {
		List<Resource> scripts = getScripts(locations, type, locationResolver);
		if (!scripts.isEmpty()) {
			runScripts(scripts);
		}
		return !scripts.isEmpty();
	}

	private List<Resource> getScripts(List<String> locations, String type, ScriptLocationResolver locationResolver) {
		if (CollectionUtils.isEmpty(locations)) {
			return Collections.emptyList();
		}
		List<Resource> resources = new ArrayList<>();
		for (String location : locations) {
			boolean optional = location.startsWith("optional:");
			if (optional) {
				location = location.substring("optional:".length());
			}
			for (Resource resource : doGetResources(location, locationResolver)) {
				if (resource.exists()) {
					resources.add(resource);
				}
				else if (!optional) {
					throw new IllegalStateException("No " + type + " scripts found at location '" + location + "'");
				}
			}
		}
		return resources;
	}

	private List<Resource> doGetResources(String location, ScriptLocationResolver locationResolver) {
		try {
			return locationResolver.resolve(location);
		}
		catch (Exception ex) {
			throw new IllegalStateException("Unable to load resources from " + location, ex);
		}
	}

	private void runScripts(List<Resource> resources) {
		if (resources.isEmpty()) {
			return;
		}
		applyScripts(resources, this.settings.isContinueOnError(), this.settings.getSeparator(),
				this.settings.getEncoding());
	}

	/**
	 * Applies the given scripts to the database.
	 * @param scripts scripts to apply
	 * @param continueOnError whether to continue when an error occurs
	 * @param separator statement separator in the scripts
	 * @param encoding encoding of the scripts
	 */
	protected abstract void applyScripts(List<Resource> scripts, boolean continueOnError, String separator,
			Charset encoding);

	private static class ScriptLocationResolver {

		private final ResourcePatternResolver resourcePatternResolver;

		ScriptLocationResolver(ResourceLoader resourceLoader) {
			this.resourcePatternResolver = ResourcePatternUtils.getResourcePatternResolver(resourceLoader);
		}

		private List<Resource> resolve(String location) throws IOException {
			List<Resource> resources = new ArrayList<>(
					Arrays.asList(this.resourcePatternResolver.getResources(location)));
			resources.sort((r1, r2) -> {
				try {
					return r1.getURL().toString().compareTo(r2.getURL().toString());
				}
				catch (IOException ex) {
					return 0;
				}
			});
			return resources;
		}

	}

}
