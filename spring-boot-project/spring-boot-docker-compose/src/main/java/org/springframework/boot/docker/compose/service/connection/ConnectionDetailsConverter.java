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

package org.springframework.boot.docker.compose.service.connection;

import java.util.function.Function;

import org.springframework.boot.autoconfigure.service.connection.ConnectionDetails;

/**
 * {@link Function} that converts one type of {@link ConnectionDetails} to another.
 *
 * @author Andy Wilkinson
 * @param <I> the input type
 * @param <O> the output type
 * @since 3.1.0
 */
public interface ConnectionDetailsConverter<I extends ConnectionDetails, O extends ConnectionDetails> {

	/**
	 * Converts the given {@code input} to the new output type
	 * @param input the input to convert
	 * @return the converted output
	 */
	O convert(I input);

}
