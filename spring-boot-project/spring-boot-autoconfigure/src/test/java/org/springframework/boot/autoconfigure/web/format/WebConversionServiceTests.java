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

package org.springframework.boot.autoconfigure.web.format;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Date;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link WebConversionService}.
 *
 * @author Brian Clozel
 * @author Madhura Bhave
 */
class WebConversionServiceTests {

	@Test
	void customDateFormatWithJavaUtilDate() {
		customDateFormat(Date.from(ZonedDateTime.of(2018, 1, 1, 20, 30, 0, 0, ZoneId.systemDefault()).toInstant()));
	}

	@Test
	void customDateFormatWithJavaTime() {
		customDateFormat(java.time.LocalDate.of(2018, 1, 1));
	}

	@Test
	void customTimeFormatWithJavaTime() {
		customTimeFormat(LocalTime.of(13, 37, 42));
	}

	@Test
	void customDateTimeFormatWithJavaTime() {
		customDateTimeFormat(LocalDateTime.of(2019, 10, 28, 13, 37, 42));
	}

	@Test
	void convertFromStringToDate() {
		WebConversionService conversionService = new WebConversionService("yyyy-MM-dd", null, null, false);
		java.time.LocalDate date = conversionService.convert("2018-01-01", java.time.LocalDate.class);
		assertThat(date).isEqualTo(java.time.LocalDate.of(2018, 1, 1));
	}

	@Test
	void defaultDateFormat() {
		WebConversionService conversionService = new WebConversionService(null, null, null, false);
		LocalDate date = LocalDate.of(2020, 4, 26);
		assertThat(conversionService.convert(date, String.class))
				.isEqualTo(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).format(date));
	}

	@Test
	void defaultIsoDateFormat() {
		WebConversionService conversionService = new WebConversionService(null, null, null, true);
		LocalDate date = LocalDate.of(2020, 4, 26);
		assertThat(conversionService.convert(date, String.class)).isEqualTo(DateTimeFormatter.ISO_DATE.format(date));
	}

	@Test
	void defaultTimeFormat() {
		WebConversionService conversionService = new WebConversionService(null, null, null, false);
		LocalTime time = LocalTime.of(12, 45, 23);
		assertThat(conversionService.convert(time, String.class))
				.isEqualTo(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).format(time));
	}

	@Test
	void defaultIsoTimeFormat() {
		WebConversionService conversionService = new WebConversionService(null, null, null, true);
		LocalTime time = LocalTime.of(12, 45, 23);
		assertThat(conversionService.convert(time, String.class)).isEqualTo(DateTimeFormatter.ISO_TIME.format(time));
	}

	@Test
	void defaultDateTimeFormat() {
		WebConversionService conversionService = new WebConversionService(null, null, null, false);
		LocalDateTime dateTime = LocalDateTime.of(2020, 4, 26, 12, 45, 23);
		assertThat(conversionService.convert(dateTime, String.class))
				.isEqualTo(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).format(dateTime));
	}

	@Test
	void defaultIsoDateTimeFormat() {
		WebConversionService conversionService = new WebConversionService(null, null, null, true);
		LocalDateTime dateTime = LocalDateTime.of(2020, 4, 26, 12, 45, 23);
		assertThat(conversionService.convert(dateTime, String.class))
				.isEqualTo(DateTimeFormatter.ISO_DATE_TIME.format(dateTime));
	}

	private void customDateFormat(Object input) {
		WebConversionService conversionService = new WebConversionService("dd*MM*yyyy", null, null, false);
		assertThat(conversionService.convert(input, String.class)).isEqualTo("01*01*2018");
	}

	private void customTimeFormat(Object input) {
		WebConversionService conversionService = new WebConversionService(null, "HH*mm*ss", null, false);
		assertThat(conversionService.convert(input, String.class)).isEqualTo("13*37*42");
	}

	private void customDateTimeFormat(Object input) {
		WebConversionService conversionService = new WebConversionService(null, null, "dd*MM*yyyy HH*mm*ss", false);
		assertThat(conversionService.convert(input, String.class)).isEqualTo("28*10*2019 13*37*42");
	}

}
