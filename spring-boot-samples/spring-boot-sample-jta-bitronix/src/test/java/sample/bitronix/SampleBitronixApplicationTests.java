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

package sample.bitronix;

import bitronix.tm.resource.jms.PoolingConnectionFactory;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.io.CapturedOutput;
import org.springframework.boot.test.io.OutputCaptureExtension;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Basic integration tests for demo application.
 *
 * @author Phillip Webb
 */
@ExtendWith(OutputCaptureExtension.class)
class SampleBitronixApplicationTests {

	@Test
	void testTransactionRollback(CapturedOutput capturedOutput) throws Exception {
		SampleBitronixApplication.main(new String[] {});
		assertThat(capturedOutput.toString()).has(substring(1, "---->"))
				.has(substring(1, "----> josh")).has(substring(2, "Count is 1"))
				.has(substring(1, "Simulated error"));
	}

	@Test
	void testExposesXaAndNonXa() {
		ApplicationContext context = SpringApplication
				.run(SampleBitronixApplication.class);
		Object jmsConnectionFactory = context.getBean("jmsConnectionFactory");
		Object xaJmsConnectionFactory = context.getBean("xaJmsConnectionFactory");
		Object nonXaJmsConnectionFactory = context.getBean("nonXaJmsConnectionFactory");
		assertThat(jmsConnectionFactory).isSameAs(xaJmsConnectionFactory);
		assertThat(jmsConnectionFactory).isInstanceOf(PoolingConnectionFactory.class);
		assertThat(nonXaJmsConnectionFactory)
				.isNotInstanceOf(PoolingConnectionFactory.class);
	}

	private Condition<String> substring(int times, String substring) {
		return new Condition<String>(
				"containing '" + substring + "' " + times + " times") {

			@Override
			public boolean matches(String value) {
				int i = 0;
				while (value.contains(substring)) {
					int beginIndex = value.indexOf(substring) + substring.length();
					value = value.substring(beginIndex);
					i++;
				}
				return i == times;
			}

		};
	}

}
