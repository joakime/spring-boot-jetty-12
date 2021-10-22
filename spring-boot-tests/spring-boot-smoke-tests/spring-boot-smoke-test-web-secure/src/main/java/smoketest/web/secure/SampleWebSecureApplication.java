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

package smoketest.web.secure;

import java.util.Date;
import java.util.EnumSet;
import java.util.Map;

import javax.servlet.DispatcherType;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.filter.ErrorPageSecurityInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.LogoutConfigurer;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.WebInvocationPrivilegeEvaluator;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@SpringBootApplication
@Controller
public class SampleWebSecureApplication implements WebMvcConfigurer {

	@GetMapping("/")
	public String home(Map<String, Object> model) {
		model.put("message", "Hello World");
		model.put("title", "Hello Home");
		model.put("date", new Date());
		return "home";
	}

	@RequestMapping("/foo")
	public String foo() {
		throw new RuntimeException("Expected exception in controller");
	}

	@Override
	public void addViewControllers(ViewControllerRegistry registry) {
		registry.addViewController("/login").setViewName("login");
	}

	public static void main(String[] args) {
		new SpringApplicationBuilder(SampleWebSecureApplication.class).run(args);
	}

	@Bean
	public FilterRegistrationBean<ErrorPageSecurityInterceptor> fixItMaybe(
			WebInvocationPrivilegeEvaluator privilegeEvaluator) {
		ErrorPageSecurityInterceptor filter = new ErrorPageSecurityInterceptor(privilegeEvaluator);
		FilterRegistrationBean<ErrorPageSecurityInterceptor> registration = new FilterRegistrationBean<>(filter);
		registration.setDispatcherTypes(EnumSet.of(DispatcherType.ERROR));
		return registration;
	}

	@Configuration(proxyBeanMethods = false)
	protected static class ApplicationSecurity {

		@Bean
		SecurityFilterChain configure(HttpSecurity http) throws Exception {
			http.authorizeRequests((requests) -> {
				requests.requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll();
				requests.antMatchers("/public/**").permitAll();
				requests.anyRequest().fullyAuthenticated();
			});
			http.httpBasic();
			http.formLogin((form) -> {
				form.loginPage("/login");
				form.failureUrl("/login?error").permitAll();
			});
			http.logout(LogoutConfigurer::permitAll);
			DefaultSecurityFilterChain chain = http.build();
			// List<Filter> filters = chain.getFilters();
			// FilterSecurityInterceptor securityInterceptor = (FilterSecurityInterceptor)
			// filters.get(filters.size() - 1);
			// addErrorInterceptor(filters, securityInterceptor);
			return chain;
		}

		// private void addErrorInterceptor(List<Filter> filters,
		// FilterSecurityInterceptor securityInterceptor) {
		// ErrorPageSecurityInterceptor interceptor = new ErrorPageSecurityInterceptor();
		// interceptor.setMetadataSource(securityInterceptor.getSecurityMetadataSource());
		// interceptor.setAccessDecisionManager(securityInterceptor.getAccessDecisionManager());
		// filters.add(filters.size() - 1, interceptor);
		// }

	}

}
