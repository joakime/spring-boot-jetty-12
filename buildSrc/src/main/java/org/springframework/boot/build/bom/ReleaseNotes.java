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

package org.springframework.boot.build.bom;

import java.io.IOException;
import java.net.URI;

import javax.inject.Inject;

import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.HttpClients;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

public abstract class ReleaseNotes extends DefaultTask {

	private final BomExtension bom;

	@Inject
	public ReleaseNotes(BomExtension bom) {
		this.bom = bom;
	}

	@TaskAction
	void releaseNotes() {
		RestTemplate restTemplate = new RestTemplate(new HttpComponentsClientHttpRequestFactory(HttpClients.custom()
				.setDefaultRequestConfig(RequestConfig.custom().setCookieSpec(CookieSpecs.IGNORE_COOKIES).build())
				.build()));
		restTemplate.setErrorHandler(new DefaultResponseErrorHandler() {

			@Override
			public void handleError(ClientHttpResponse response) throws IOException {
			}

		});
		for (Library library : this.bom.getLibraries()) {
			URI releaseNotes = library.getReleaseNotes(library.getVersion().getVersion());
			if (releaseNotes != null) {
				ResponseEntity<String> response = restTemplate.exchange(releaseNotes, HttpMethod.HEAD, null,
						String.class);
				System.out.println(library.getName() + ": " + releaseNotes + " " + response.getStatusCode());
			}
		}
	}

}
