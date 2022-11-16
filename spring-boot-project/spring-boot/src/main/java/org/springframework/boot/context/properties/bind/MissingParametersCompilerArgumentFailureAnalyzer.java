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

package org.springframework.boot.context.properties.bind;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.boot.diagnostics.FailureAnalyzer;

/**
 * {@link FailureAnalyzer} for {@link MissingParametersCompilerArgumentException}.
 *
 * @author Andy Wilkinson
 */
class MissingParametersCompilerArgumentFailureAnalyzer
		extends AbstractFailureAnalyzer<MissingParametersCompilerArgumentException> {

	@Override
	protected FailureAnalysis analyze(Throwable rootFailure, MissingParametersCompilerArgumentException cause) {
		return new FailureAnalysis(cause.getMessage(), "Recompile the classes with -parameters", cause);
	}

}
