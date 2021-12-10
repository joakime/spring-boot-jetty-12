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

package org.springframework.boot.sql.init.dependency;

import java.lang.reflect.AnnotatedElement;
import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;

/**
 * {@link DependsOnDatabaseInitializationDetector} that detects beans annotated with
 * {@link DependsOnDatabaseInitialization}.
 *
 * @author Andy Wilkinson
 */
class AnnotationDependsOnDatabaseInitializationDetector implements DependsOnDatabaseInitializationDetector {

	@Override
	public Set<String> detect(ConfigurableListableBeanFactory beanFactory) {
		Set<String> dependentBeans = new HashSet<>();
		for (String beanName : beanFactory.getBeanDefinitionNames()) {
			if (findAnnotation(beanName, beanFactory).isPresent()) {
				dependentBeans.add(beanName);
			}
		}
		return dependentBeans;
	}

	private MergedAnnotation<DependsOnDatabaseInitialization> findAnnotation(String beanName,
			ConfigurableListableBeanFactory beanFactory) {
		Class<?> beanType = beanFactory.getType(beanName, false);
		MergedAnnotation<DependsOnDatabaseInitialization> annotation = mergedAnnotation(beanType);
		if (annotation.isPresent()) {
			return annotation;
		}
		BeanDefinition definition = beanFactory.getBeanDefinition(beanName);
		if (definition instanceof RootBeanDefinition) {
			RootBeanDefinition rootDefinition = (RootBeanDefinition) definition;
			if (rootDefinition.hasBeanClass()) {
				annotation = mergedAnnotation(rootDefinition.getBeanClass());
				if (annotation.isPresent()) {
					return annotation;
				}
			}
			annotation = mergedAnnotation(rootDefinition.getResolvedFactoryMethod());
		}
		return annotation;
	}

	private MergedAnnotation<DependsOnDatabaseInitialization> mergedAnnotation(AnnotatedElement element) {
		if (element == null) {
			return MergedAnnotation.missing();
		}
		return MergedAnnotations.from(element, SearchStrategy.TYPE_HIERARCHY)
				.get(DependsOnDatabaseInitialization.class);
	}

}
