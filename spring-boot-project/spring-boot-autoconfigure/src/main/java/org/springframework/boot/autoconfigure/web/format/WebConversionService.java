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

import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.function.Consumer;

import org.springframework.format.datetime.DateFormatter;
import org.springframework.format.datetime.DateFormatterRegistrar;
import org.springframework.format.datetime.standard.DateTimeFormatterRegistrar;
import org.springframework.format.number.NumberFormatAnnotationFormatterFactory;
import org.springframework.format.number.money.CurrencyUnitFormatter;
import org.springframework.format.number.money.Jsr354NumberFormatAnnotationFormatterFactory;
import org.springframework.format.number.money.MonetaryAmountFormatter;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * {@link org.springframework.format.support.FormattingConversionService} dedicated to web
 * applications for formatting and converting values to/from the web.
 * <p>
 * This service replaces the default implementations provided by
 * {@link org.springframework.web.servlet.config.annotation.EnableWebMvc @EnableWebMvc}
 * and {@link org.springframework.web.reactive.config.EnableWebFlux @EnableWebFlux}.
 *
 * @author Brian Clozel
 * @since 2.0.0
 */
public class WebConversionService extends DefaultFormattingConversionService {

	private static final boolean JSR_354_PRESENT = ClassUtils.isPresent("javax.money.MonetaryAmount",
			WebConversionService.class.getClassLoader());

	private final String dateFormat;

	private final String timeFormat;

	private final String dateTimeFormat;

	private final boolean useIso;

	/**
	 * Create a new WebConversionService that configures formatters with the provided date
	 * format, or register the default ones if no custom format is provided.
	 * @param dateFormat the custom date format to use for date conversions
	 * @deprecated since 2.3.0 in favor of
	 * {@link #WebConversionService(String, String, String, boolean)}
	 */
	@Deprecated
	public WebConversionService(String dateFormat) {
		this(dateFormat, null, null, false);
	}

	/**
	 * Create a new WebConversionService that configures formatters with the provided
	 * date, time, and date-time formats, or registers the default ones if no custom
	 * formats are provided.
	 * @param dateFormat the custom date format to use for date conversions, or
	 * {@code null} to use the default
	 * @param timeFormat the custom time format to use for time conversions, or
	 * {@code null} to use the default
	 * @param dateTimeFormat the custom date-time format to use for date-time conversions,
	 * or {@code null} to use the default
	 * @param useIso whether the default date, time, and date-time formatters should use
	 * their corresponding ISO format
	 * @since 2.3.0
	 */
	public WebConversionService(String dateFormat, String timeFormat, String dateTimeFormat, boolean useIso) {
		super(false);
		this.dateFormat = nullIfEmpty(dateFormat);
		this.timeFormat = nullIfEmpty(timeFormat);
		this.dateTimeFormat = nullIfEmpty(dateTimeFormat);
		this.useIso = useIso;
		if (dateFormat != null || timeFormat != null || dateTimeFormat != null || useIso) {
			addFormatters();
		}
		else {
			addDefaultFormatters(this);
		}
	}

	private void addFormatters() {
		addFormatterForFieldAnnotation(new NumberFormatAnnotationFormatterFactory());
		if (JSR_354_PRESENT) {
			addFormatter(new CurrencyUnitFormatter());
			addFormatter(new MonetaryAmountFormatter());
			addFormatterForFieldAnnotation(new Jsr354NumberFormatAnnotationFormatterFactory());
		}
		registerJsr310();
		registerJavaDate();
	}

	private void registerJsr310() {
		DateTimeFormatterRegistrar dateTime = new DateTimeFormatterRegistrar();
		configure(this.dateFormat, dateTime::setDateFormatter);
		configure(this.timeFormat, dateTime::setTimeFormatter);
		configure(this.dateTimeFormat, dateTime::setDateTimeFormatter);
		dateTime.setUseIsoFormat(this.useIso);
		dateTime.registerFormatters(this);
	}

	private void configure(String format, Consumer<DateTimeFormatter> formatter) {
		if (format != null) {
			formatter.accept(DateTimeFormatter.ofPattern(format).withResolverStyle(ResolverStyle.SMART));
		}
	}

	private void registerJavaDate() {
		DateFormatterRegistrar dateFormatterRegistrar = new DateFormatterRegistrar();
		if (this.dateFormat != null) {
			DateFormatter dateFormatter = new DateFormatter(this.dateFormat);
			dateFormatterRegistrar.setFormatter(dateFormatter);
		}
		dateFormatterRegistrar.registerFormatters(this);
	}

	private static String nullIfEmpty(String format) {
		return StringUtils.hasText(format) ? format : null;
	}

}
