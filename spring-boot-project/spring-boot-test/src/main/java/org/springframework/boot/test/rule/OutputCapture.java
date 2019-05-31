/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.test.rule;

import java.util.ArrayList;
import java.util.List;

import org.hamcrest.Matcher;
import org.junit.Assert;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import static org.hamcrest.Matchers.allOf;

/**
 * JUnit {@code @Rule} to capture output from System.out and System.err.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 1.4.0
 */
public class OutputCapture implements TestRule {

	private final org.springframework.boot.test.io.OutputCapture delegate = new org.springframework.boot.test.io.OutputCapture();

	private List<Matcher<? super String>> matchers = new ArrayList<>();

	@Override
	public Statement apply(Statement base, Description description) {
		return new Statement() {
			@Override
			public void evaluate() throws Throwable {
				OutputCapture.this.delegate.push();
				try {
					base.evaluate();
				}
				finally {
					try {
						if (!OutputCapture.this.matchers.isEmpty()) {
							String output = OutputCapture.this.delegate.toString();
							Assert.assertThat(output, allOf(OutputCapture.this.matchers));
						}
					}
					finally {
						OutputCapture.this.delegate.pop();
					}
				}
			}
		};
	}

	/**
	 * Discard all currently accumulated output.
	 */
	public void reset() {
		OutputCapture.this.delegate.reset();
	}

	public void flush() {

	}

	@Override
	public String toString() {
		return this.delegate.toString();
	}

	/**
	 * Verify that the output is matched by the supplied {@code matcher}. Verification is
	 * performed after the test method has executed.
	 * @param matcher the matcher
	 */
	public void expect(Matcher<? super String> matcher) {
		this.matchers.add(matcher);
	}

}
