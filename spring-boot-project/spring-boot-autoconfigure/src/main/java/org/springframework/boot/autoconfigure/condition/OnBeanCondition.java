/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.autoconfigure.condition;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.springframework.boot.autoconfigure.AutoConfigurationMetadata;
import org.springframework.boot.autoconfigure.condition.ConditionMessage.Style;
import org.springframework.boot.autoconfigure.condition.OnBeanRegistrationPredicate.Spec;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.ConfigurationCondition;
import org.springframework.core.Ordered;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotation.Adapt;
import org.springframework.core.annotation.MergedAnnotationCollectors;
import org.springframework.core.annotation.MergedAnnotationPredicates;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.annotation.Order;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.core.type.MethodMetadata;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * {@link Condition} that checks for the presence or absence of specific beans.
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @author Jakub Kubrynski
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 * @see ConditionalOnBean
 * @see ConditionalOnMissingBean
 * @see ConditionalOnSingleCandidate
 */
@Order(Ordered.LOWEST_PRECEDENCE)
class OnBeanCondition extends FilteringRegistrationPredicateCondition implements ConfigurationCondition {

	@Override
	public ConfigurationPhase getConfigurationPhase() {
		return ConfigurationPhase.REGISTER_BEAN;
	}

	@Override
	protected final ConditionOutcome[] getOutcomes(String[] autoConfigurationClasses,
			AutoConfigurationMetadata autoConfigurationMetadata) {
		ConditionOutcome[] outcomes = new ConditionOutcome[autoConfigurationClasses.length];
		for (int i = 0; i < outcomes.length; i++) {
			String autoConfigurationClass = autoConfigurationClasses[i];
			if (autoConfigurationClass != null) {
				Set<String> onBeanTypes = autoConfigurationMetadata.getSet(autoConfigurationClass, "ConditionalOnBean");
				outcomes[i] = getOutcome(onBeanTypes, ConditionalOnBean.class);
				if (outcomes[i] == null) {
					Set<String> onSingleCandidateTypes = autoConfigurationMetadata.getSet(autoConfigurationClass,
							"ConditionalOnSingleCandidate");
					outcomes[i] = getOutcome(onSingleCandidateTypes, ConditionalOnSingleCandidate.class);
				}
			}
		}
		return outcomes;
	}

	private ConditionOutcome getOutcome(Set<String> requiredBeanTypes, Class<? extends Annotation> annotation) {
		List<String> missing = filter(requiredBeanTypes, ClassNameFilter.MISSING, getBeanClassLoader());
		if (!missing.isEmpty()) {
			ConditionMessage message = ConditionMessage.forCondition(annotation)
					.didNotFind("required type", "required types").items(Style.QUOTE, missing);
			return ConditionOutcome.noMatch(message);
		}
		return null;
	}

	@Override
	public boolean evaluate(ConditionContext context, AnnotatedTypeMetadata metadata) {
		MergedAnnotations annotations = metadata.getAnnotations();
		Spec beanSpec = null;
		Spec singleCandidateSpec = null;
		Spec missingBeanSpec = null;
		if (annotations.isPresent(ConditionalOnBean.class)) {
			beanSpec = createSpec(context, metadata, annotations, ConditionalOnBean.class, Function.identity());
		}
		if (metadata.isAnnotated(ConditionalOnSingleCandidate.class.getName())) {
			singleCandidateSpec = createSpec(context, metadata, annotations, ConditionalOnSingleCandidate.class,
					(types) -> {
						types.removeAll(Arrays.asList("", Object.class.getName()));
						return types;
					});
			Set<String> types = singleCandidateSpec.getTypes();
			Assert.isTrue(types.size() == 1,
					() -> "@ConditionalOnSingleCandidate annotations must specify only one type (got "
							+ StringUtils.collectionToCommaDelimitedString(types) + ")");
		}
		if (metadata.isAnnotated(ConditionalOnMissingBean.class.getName())) {
			missingBeanSpec = createSpec(context, metadata, annotations, ConditionalOnMissingBean.class,
					Function.identity());
		}
		return new OnBeanRegistrationPredicate(getLocation(metadata), beanSpec, singleCandidateSpec, missingBeanSpec)
				.test(new ConditionContextRegistrationContext(context));
	}

	private Spec createSpec(ConditionContext context, AnnotatedTypeMetadata metadata,
			MergedAnnotations mergedAnnotations, Class<? extends Annotation> annotationType,
			Function<Set<String>, Set<String>> typesMapper) {
		MergedAnnotation<?> annotation = mergedAnnotations.get(annotationType);
		MultiValueMap<String, Object> attributes = mergedAnnotations.stream(annotationType)
				.filter(MergedAnnotationPredicates.unique(MergedAnnotation::getMetaTypes))
				.collect(MergedAnnotationCollectors.toMultiValueMap(Adapt.CLASS_TO_STRING));
		Set<String> names = extract(attributes, "name");
		Set<String> annotations = extract(attributes, "annotation");
		Set<String> ignoredTypes = extract(attributes, "ignored", "ignoredType");
		Set<Class<?>> parameterizedContainers = resolveWhenPossible(extract(attributes, "parameterizedContainer"),
				context.getClassLoader());
		SearchStrategy strategy = annotation.getValue("search", SearchStrategy.class).orElse(null);
		Set<String> types = typesMapper.apply(extractTypes(attributes));
		BeanTypeDeductionException deductionException = null;
		if (types.isEmpty() && names.isEmpty()) {
			try {
				types = deducedBeanType(context, metadata, parameterizedContainers);
			}
			catch (BeanTypeDeductionException ex) {
				deductionException = ex;
			}
		}
		String predicateName = "@" + ClassUtils.getShortName(annotationType);
		validate(deductionException, types, names, annotations, predicateName);
		return new Spec(names, types, annotations, ignoredTypes, parameterizedContainers, strategy, predicateName);
	}

	private Set<String> extract(MultiValueMap<String, Object> attributes, String... attributeNames) {
		if (attributes.isEmpty()) {
			return Collections.emptySet();
		}
		Set<String> result = new LinkedHashSet<>();
		for (String attributeName : attributeNames) {
			List<Object> values = attributes.getOrDefault(attributeName, Collections.emptyList());
			for (Object value : values) {
				if (value instanceof String[]) {
					merge(result, (String[]) value);
				}
				else if (value instanceof String) {
					merge(result, (String) value);
				}
			}
		}
		return result.isEmpty() ? Collections.emptySet() : result;
	}

	private void merge(Set<String> result, String... additional) {
		Collections.addAll(result, additional);
	}

	private Set<Class<?>> resolveWhenPossible(Set<String> classNames, ClassLoader classLoader) {
		if (classNames.isEmpty()) {
			return Collections.emptySet();
		}
		Set<Class<?>> resolved = new LinkedHashSet<>(classNames.size());
		for (String className : classNames) {
			try {
				resolved.add(resolve(className, classLoader));
			}
			catch (ClassNotFoundException | NoClassDefFoundError ex) {
			}
		}
		return resolved;
	}

	protected Set<String> extractTypes(MultiValueMap<String, Object> attributes) {
		return extract(attributes, "value", "type");
	}

	private Set<String> deducedBeanType(ConditionContext context, AnnotatedTypeMetadata metadata,
			Set<Class<?>> parameterizedContainers) {
		if (metadata instanceof MethodMetadata && metadata.isAnnotated(Bean.class.getName())) {
			return deducedBeanTypeForBeanMethod(context, (MethodMetadata) metadata, parameterizedContainers);
		}
		return Collections.emptySet();
	}

	private Set<String> deducedBeanTypeForBeanMethod(ConditionContext context, MethodMetadata metadata,
			Set<Class<?>> parameterizedContainers) {
		try {
			Class<?> returnType = getReturnType(context, metadata, parameterizedContainers);
			return Collections.singleton(returnType.getName());
		}
		catch (Throwable ex) {
			throw new BeanTypeDeductionException(metadata.getDeclaringClassName(), metadata.getMethodName(), ex);
		}
	}

	private Class<?> getReturnType(ConditionContext context, MethodMetadata metadata,
			Set<Class<?>> parameterizedContainers) throws ClassNotFoundException, LinkageError {
		// Safe to load at this point since we are in the REGISTER_BEAN phase
		ClassLoader classLoader = context.getClassLoader();
		Class<?> returnType = resolve(metadata.getReturnTypeName(), classLoader);
		if (isParameterizedContainer(returnType, parameterizedContainers)) {
			returnType = getReturnTypeGeneric(metadata, classLoader);
		}
		return returnType;
	}

	private boolean isParameterizedContainer(Class<?> type, Set<Class<?>> parameterizedContainers) {
		for (Class<?> parameterizedContainer : parameterizedContainers) {
			if (parameterizedContainer.isAssignableFrom(type)) {
				return true;
			}
		}
		return false;
	}

	private Class<?> getReturnTypeGeneric(MethodMetadata metadata, ClassLoader classLoader)
			throws ClassNotFoundException, LinkageError {
		Class<?> declaringClass = resolve(metadata.getDeclaringClassName(), classLoader);
		Method beanMethod = findBeanMethod(declaringClass, metadata.getMethodName());
		return ResolvableType.forMethodReturnType(beanMethod).resolveGeneric();
	}

	private Method findBeanMethod(Class<?> declaringClass, String methodName) {
		Method method = ReflectionUtils.findMethod(declaringClass, methodName);
		if (isBeanMethod(method)) {
			return method;
		}
		Method[] candidates = ReflectionUtils.getAllDeclaredMethods(declaringClass);
		for (Method candidate : candidates) {
			if (candidate.getName().equals(methodName) && isBeanMethod(candidate)) {
				return candidate;
			}
		}
		throw new IllegalStateException("Unable to find bean method " + methodName);
	}

	private boolean isBeanMethod(Method method) {
		return method != null && MergedAnnotations.from(method, MergedAnnotations.SearchStrategy.TYPE_HIERARCHY)
				.isPresent(Bean.class);
	}

	protected void validate(BeanTypeDeductionException ex, Set<String> types, Set<String> names,
			Set<String> annotations, String predicateName) {
		if (!hasAtLeastOneElement(types, names, annotations)) {
			String message = predicateName + " did not specify a bean using type, name or annotation";
			if (ex == null) {
				throw new IllegalStateException(message);
			}
			throw new IllegalStateException(message + " and the attempt to deduce the bean's type failed", ex);
		}
	}

	private boolean hasAtLeastOneElement(Set<?>... sets) {
		for (Set<?> set : sets) {
			if (!set.isEmpty()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Exception thrown when the bean type cannot be deduced.
	 */
	static final class BeanTypeDeductionException extends RuntimeException {

		private BeanTypeDeductionException(String className, String beanMethodName, Throwable cause) {
			super("Failed to deduce bean type for " + className + "." + beanMethodName, cause);
		}

	}

}
