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

package org.springframework.boot.autoconfigure.aop;

import org.aspectj.weaver.Advice;

import org.springframework.aop.config.AopConfigUtils;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.BeanRegistrar;
import org.springframework.boot.autoconfigure.BeanRegistry;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * @author awilkinson
 */
public class AopAutoConfigurationBeanRegistrar implements BeanRegistrar {

	@Override
	public void registerBeans(BeanRegistry registry) {
		registry.conditional(AopAutoConfigurationBeanRegistrar.class.getName(), (on) -> {
			on.property((property) -> property.prefix("spring.aop").name("auto").havingValue("true").matchIfMissing());
		}, () -> {
			registry.conditional("$AspectJAutoProxyingConfiguration", (on) -> on.type(() -> Advice.class),
					() -> aspectJAutoProxyingConfiguration(registry));
			registry.conditional("$ClassProxyingConfiguration", (on) -> {
				on.missingType(() -> Advice.class);
				on.property((property) -> property.prefix("spring.aop").name("proxy-target-class").havingValue("true")
						.matchIfMissing());
			}, () -> classProxyingConfiguration(registry));
		});
	}

	private void aspectJAutoProxyingConfiguration(BeanRegistry registry) {
		registry.conditional("$JdkDynamicAutoProxyConfiguration",
				(on) -> on.property(
						(property) -> property.prefix("spring.aop").name("proxy-target-class").havingValue("false")),
				() -> registry.registerBean(JdkDynamicAutoProxyConfiguration.class));
		registry.conditional("$CglibAutoProxyConfiguration",
				(on) -> on.property((property) -> property.prefix("spring.aop").name("proxy-target-class")
						.havingValue("true").matchIfMissing()),
				() -> registry.registerBean(CglibAutoProxyConfiguration.class));
	}

	private void classProxyingConfiguration(BeanRegistry registry) {
		ConfigurableListableBeanFactory beanFactory = registry.getRegistrationContext().getBeanFactory();
		if (beanFactory instanceof BeanDefinitionRegistry) {
			BeanDefinitionRegistry definitionRegistry = (BeanDefinitionRegistry) beanFactory;
			AopConfigUtils.registerAutoProxyCreatorIfNecessary(definitionRegistry);
			AopConfigUtils.forceAutoProxyCreatorToUseClassProxying(definitionRegistry);
		}
	}

	@EnableAspectJAutoProxy(proxyTargetClass = false)
	static class JdkDynamicAutoProxyConfiguration {

	}

	@EnableAspectJAutoProxy(proxyTargetClass = true)
	static class CglibAutoProxyConfiguration {

	}

}
