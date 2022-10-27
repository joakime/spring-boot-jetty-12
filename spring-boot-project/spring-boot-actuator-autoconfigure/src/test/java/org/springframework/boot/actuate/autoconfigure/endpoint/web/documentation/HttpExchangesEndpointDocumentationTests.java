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

package org.springframework.boot.actuate.autoconfigure.endpoint.web.documentation;

import java.net.URI;
import java.security.Principal;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.web.exchanges.HttpExchange;
import org.springframework.boot.actuate.web.exchanges.HttpExchangeRepository;
import org.springframework.boot.actuate.web.exchanges.HttpExchangesEndpoint;
import org.springframework.boot.actuate.web.exchanges.Include;
import org.springframework.boot.actuate.web.exchanges.SourceHttpRequest;
import org.springframework.boot.actuate.web.exchanges.SourceHttpResponse;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.restdocs.payload.JsonFieldType;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for generating documentation describing {@link HttpExchangesEndpoint}.
 *
 * @author Andy Wilkinson
 */
class HttpExchangesEndpointDocumentationTests extends MockMvcEndpointDocumentationTests {

	@MockBean
	private HttpExchangeRepository repository;

	@Test
	void httpExchanges() throws Exception {
		SourceHttpRequest request = mock(SourceHttpRequest.class);
		given(request.getUri()).willReturn(URI.create("https://api.example.com"));
		given(request.getMethod()).willReturn("GET");
		given(request.getHeaders())
				.willReturn(Collections.singletonMap(HttpHeaders.ACCEPT, Arrays.asList("application/json")));
		SourceHttpResponse response = mock(SourceHttpResponse.class);
		given(response.getStatus()).willReturn(200);
		given(response.getHeaders())
				.willReturn(Collections.singletonMap(HttpHeaders.CONTENT_TYPE, Arrays.asList("application/json")));
		Principal principal = mock(Principal.class);
		given(principal.getName()).willReturn("alice");
		HttpExchange exchange = HttpExchange.start(request).finish(response, () -> principal,
				() -> UUID.randomUUID().toString(), EnumSet.allOf(Include.class));
		given(this.repository.findAll()).willReturn(Arrays.asList(exchange));
		this.mockMvc.perform(get("/actuator/httpexchanges")).andExpect(status().isOk()).andDo(document("httpexchanges",
				responseFields(fieldWithPath("exchanges").description("An array of HTTP request-response exchanges."),
						fieldWithPath("exchanges.[].timestamp").description("Timestamp of when the exchange occurred."),
						fieldWithPath("exchanges.[].principal").description("Principal of the exchange, if any.")
								.optional(),
						fieldWithPath("exchanges.[].principal.name").description("Name of the principal.").optional(),
						fieldWithPath("exchanges.[].request.method").description("HTTP method of the request."),
						fieldWithPath("exchanges.[].request.remoteAddress")
								.description("Remote address from which the request was received, if known.").optional()
								.type(JsonFieldType.STRING),
						fieldWithPath("exchanges.[].request.uri").description("URI of the request."),
						fieldWithPath("exchanges.[].request.headers")
								.description("Headers of the request, keyed by header name."),
						fieldWithPath("exchanges.[].request.headers.*.[]").description("Values of the header"),
						fieldWithPath("exchanges.[].response.status").description("Status of the response"),
						fieldWithPath("exchanges.[].response.headers")
								.description("Headers of the response, keyed by header name."),
						fieldWithPath("exchanges.[].response.headers.*.[]").description("Values of the header"),
						fieldWithPath("exchanges.[].session")
								.description("Session associated with the exchange, if any.").optional(),
						fieldWithPath("exchanges.[].session.id").description("ID of the session."),
						fieldWithPath("exchanges.[].timeTaken")
								.description("Time, in milliseconds, taken to handle the exchange."))));
	}

	@Configuration(proxyBeanMethods = false)
	@Import(BaseDocumentationConfiguration.class)
	static class TestConfiguration {

		@Bean
		HttpExchangesEndpoint httpExchangesEndpoint(HttpExchangeRepository repository) {
			return new HttpExchangesEndpoint(repository);
		}

	}

}
