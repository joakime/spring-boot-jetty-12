/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import org.springframework.aot.generator.DefaultGeneratedTypeContext;
import org.springframework.aot.generator.GeneratedType;
import org.springframework.aot.generator.GeneratedTypeReference;
import org.springframework.aot.hint.ExecutableMode;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.TypeReference;
import org.springframework.aot.nativex.FileNativeConfigurationGenerator;
import org.springframework.context.generator.ApplicationContextAotGenerator;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.javapoet.ClassName;
import org.springframework.javapoet.JavaFile;
import org.springframework.util.Assert;

/**
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 */
public class AotProcessor {

	private final Class<?> application;

	private final Path generatedSources;

	private final Path generatedResources;

	public AotProcessor(Class<?> application, Path generatedSources, Path generatedResources) {
		this.application = application;
		this.generatedSources = generatedSources;
		this.generatedResources = generatedResources;
	}

	public void process() {
		AotProcessingHook hook = new AotProcessingHook();
		SpringApplicationHooks.withHook(hook, this::callApplicationMainMethod);
		GenericApplicationContext applicationContext = hook.getApplicationContext();
		Assert.notNull(applicationContext, "No application context available after calling main method of '"
				+ this.application.getName() + "'. Does it run a SpringApplication?");
		performAotProcessing(applicationContext);
	}

	private void callApplicationMainMethod() {
		try {
			this.application.getMethod("main", String[].class).invoke(null, new Object[] { new String[0] });
		}
		catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	private void performAotProcessing(GenericApplicationContext applicationContext) {
		DefaultGeneratedTypeContext generationContext = new DefaultGeneratedTypeContext(
				this.application.getPackageName(), (packageName) -> GeneratedType.of(ClassName.get(packageName,
						this.application.getSimpleName() + "$$ApplicationContextInitializer")));
		ApplicationContextAotGenerator generator = new ApplicationContextAotGenerator();
		generator.generateApplicationContext(applicationContext, generationContext);

		// Register reflection hint for entry point as we access it via reflection
		generationContext.runtimeHints().reflection()
				.registerType(GeneratedTypeReference.of(generationContext.getMainGeneratedType().getClassName()),
						(hint) -> hint.onReachableType(TypeReference.of(this.application)).withConstructor(
								Collections.emptyList(),
								(constructorHint) -> constructorHint.setModes(ExecutableMode.INVOKE)));

		writeGeneratedSources(generationContext.toJavaFiles());
		writeGeneratedResources(generationContext.runtimeHints());
	}

	private void writeGeneratedSources(List<JavaFile> sources) {
		for (JavaFile source : sources) {
			try {
				source.writeTo(this.generatedSources);
			}
			catch (IOException ex) {
				throw new IllegalStateException("Failed to write " + source.typeSpec.name, ex);
			}
		}
	}

	private void writeGeneratedResources(RuntimeHints hints) {
		FileNativeConfigurationGenerator generator = new FileNativeConfigurationGenerator(this.generatedResources);
		generator.generate(hints);
	}

}
