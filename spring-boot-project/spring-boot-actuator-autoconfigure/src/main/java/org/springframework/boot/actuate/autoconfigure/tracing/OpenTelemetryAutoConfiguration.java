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

package org.springframework.boot.actuate.autoconfigure.tracing;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import io.micrometer.tracing.SamplerFunction;
import io.micrometer.tracing.otel.bridge.DefaultHttpClientAttributesGetter;
import io.micrometer.tracing.otel.bridge.DefaultHttpServerAttributesExtractor;
import io.micrometer.tracing.otel.bridge.EventListener;
import io.micrometer.tracing.otel.bridge.EventPublishingContextWrapper;
import io.micrometer.tracing.otel.bridge.OtelBaggageManager;
import io.micrometer.tracing.otel.bridge.OtelCurrentTraceContext;
import io.micrometer.tracing.otel.bridge.OtelHttpClientHandler;
import io.micrometer.tracing.otel.bridge.OtelHttpServerHandler;
import io.micrometer.tracing.otel.bridge.OtelPropagator;
import io.micrometer.tracing.otel.bridge.OtelTracer;
import io.micrometer.tracing.otel.bridge.OtelTracer.EventPublisher;
import io.micrometer.tracing.otel.bridge.Slf4JBaggageEventListener;
import io.micrometer.tracing.otel.bridge.Slf4JEventListener;
import io.micrometer.tracing.otel.propagation.BaggageTextMapPropagator;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.baggage.propagation.W3CBaggagePropagator;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.ContextStorage;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.extension.trace.propagation.B3Propagator;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SdkTracerProviderBuilder;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import org.slf4j.MDC;

import org.springframework.boot.SpringBootVersion;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for OpenTelemetry.
 *
 * @author Moritz Halbritter
 * @since 3.0.0
 */
@AutoConfiguration(before = MicrometerTracingAutoConfiguration.class)
@ConditionalOnEnabledTracing
@ConditionalOnClass({ OtelTracer.class, SdkTracerProvider.class, OpenTelemetry.class })
@EnableConfigurationProperties(TracingProperties.class)
public class OpenTelemetryAutoConfiguration {

	/**
	 * Default value for application name if {@code spring.application.name} is not set.
	 */
	private static final String DEFAULT_APPLICATION_NAME = "application";

	@Bean
	@ConditionalOnMissingBean
	OpenTelemetry openTelemetry(SdkTracerProvider sdkTracerProvider, ContextPropagators contextPropagators) {
		return OpenTelemetrySdk.builder().setTracerProvider(sdkTracerProvider).setPropagators(contextPropagators)
				.build();
	}

	@Bean
	@ConditionalOnMissingBean
	SdkTracerProvider otelSdkTracerProvider(Environment environment, List<SpanProcessor> spanProcessors,
			Sampler sampler) {
		String applicationName = environment.getProperty("spring.application.name", DEFAULT_APPLICATION_NAME);
		SdkTracerProviderBuilder builder = SdkTracerProvider.builder().setSampler(sampler)
				.setResource(Resource.create(Attributes.of(ResourceAttributes.SERVICE_NAME, applicationName)));
		for (SpanProcessor spanProcessor : spanProcessors) {
			builder.addSpanProcessor(spanProcessor);
		}
		return builder.build();
	}

	@Bean
	@ConditionalOnMissingBean
	ContextPropagators otelContextPropagators(List<TextMapPropagator> textMapPropagators) {
		return ContextPropagators.create(TextMapPropagator.composite(textMapPropagators));
	}

	@Bean
	@ConditionalOnMissingBean
	Sampler otelSampler(TracingProperties properties) {
		return Sampler.traceIdRatioBased(properties.getSampling().getProbability());
	}

	@Bean
	SpanProcessor otelSpanProcessor(List<SpanExporter> spanExporter) {
		return SpanProcessor.composite(spanExporter.stream()
				.map((exporter) -> BatchSpanProcessor.builder(exporter).build()).collect(Collectors.toList()));
	}

	@Bean
	@ConditionalOnMissingBean
	Tracer otelTracer(OpenTelemetry openTelemetry) {
		return openTelemetry.getTracer("org.springframework.boot", SpringBootVersion.getVersion());
	}

	@Bean
	@ConditionalOnMissingBean
	OtelTracer micrometerOtelTracer(Tracer tracer, EventPublisher eventPublisher,
			OtelCurrentTraceContext otelCurrentTraceContext, TracingProperties properties) {
		return new OtelTracer(tracer, otelCurrentTraceContext, eventPublisher,
				new OtelBaggageManager(otelCurrentTraceContext, properties.getBaggage().getRemoteFields(), List.of()));
	}

	@Bean
	@ConditionalOnMissingBean
	OtelPropagator otelPropagator(ContextPropagators contextPropagators, Tracer tracer) {
		return new OtelPropagator(contextPropagators, tracer);
	}

	@Bean
	@ConditionalOnMissingBean
	EventPublisher otelTracerEventPublisher(List<EventListener> eventListeners) {
		return new OTelEventPublisher(eventListeners);
	}

	@Bean
	@ConditionalOnMissingBean
	OtelCurrentTraceContext otelCurrentTraceContext(EventPublisher publisher) {
		ContextStorage.addWrapper(new EventPublishingContextWrapper(publisher));
		return new OtelCurrentTraceContext();
	}

	@Bean
	@ConditionalOnMissingBean
	OtelHttpClientHandler otelHttpClientHandler(OpenTelemetry openTelemetry) {
		return new OtelHttpClientHandler(openTelemetry, null, null, SamplerFunction.deferDecision(),
				new DefaultHttpClientAttributesGetter());
	}

	@Bean
	@ConditionalOnMissingBean
	OtelHttpServerHandler otelHttpServerHandler(OpenTelemetry openTelemetry) {
		return new OtelHttpServerHandler(openTelemetry, null, null, Pattern.compile(""),
				new DefaultHttpServerAttributesExtractor());
	}

	@Configuration(proxyBeanMethods = false)
	static class PropagationConfiguration {

		@Configuration(proxyBeanMethods = false)
		@ConditionalOnClass(B3Propagator.class)
		static class B3NoBaggagePropagatorConfiguration {

			@Bean
			@ConditionalOnMissingBean
			@ConditionalOnProperty(value = "management.tracing.propagation.type", havingValue = "B3")
			B3Propagator b3TextMapPropagator() {
				return B3Propagator.injectingSingleHeader();
			}

		}

		@Configuration(proxyBeanMethods = false)
		@ConditionalOnProperty(value = "management.tracing.baggage.enabled", havingValue = "false")
		static class W3CNoBaggagePropagatorConfiguration {

			@Bean
			@ConditionalOnMissingBean
			@ConditionalOnProperty(value = "management.tracing.propagation.type", havingValue = "W3C",
					matchIfMissing = true)
			W3CTraceContextPropagator w3cTextMapPropagatorWithoutBaggage() {
				return W3CTraceContextPropagator.getInstance();
			}

		}

		@Configuration(proxyBeanMethods = false)
		@ConditionalOnProperty(value = "management.tracing.baggage.enabled", matchIfMissing = true)
		static class W3CBaggagePropagatorConfiguration {

			@Bean
			@ConditionalOnMissingBean
			@ConditionalOnProperty(value = "management.tracing.propagation.type", havingValue = "W3C",
					matchIfMissing = true)
			TextMapPropagator w3cTextMapPropagatorWithBaggage() {
				return TextMapPropagator.composite(W3CTraceContextPropagator.getInstance(),
						W3CBaggagePropagator.getInstance());
			}

		}

	}

	@Configuration(proxyBeanMethods = false)
	static class MicrometerTracingPropagationConfiguration {

		@Configuration(proxyBeanMethods = false)
		@ConditionalOnProperty(value = "management.tracing.baggage.enabled", matchIfMissing = true)
		static class BaggagePropagatorConfiguration {

			@Bean
			@ConditionalOnProperty(value = "management.tracing.propagation.type", havingValue = "B3")
			BaggageTextMapPropagator b3BaggageTextMapPropagator(TracingProperties properties,
					OtelCurrentTraceContext otelCurrentTraceContext) {
				return new BaggageTextMapPropagator(properties.getBaggage().getRemoteFields(), new OtelBaggageManager(
						otelCurrentTraceContext, properties.getBaggage().getRemoteFields(), List.of()));
			}

			@Configuration(proxyBeanMethods = false)
			@ConditionalOnClass(MDC.class)
			static class Slf4jConfiguration {

				@Bean
				@ConditionalOnMissingBean
				@ConditionalOnProperty(value = "management.tracing.baggage.correlation.enabled", matchIfMissing = true)
				Slf4JBaggageEventListener otelSlf4JBaggageEventListener(TracingProperties tracingProperties) {
					return new Slf4JBaggageEventListener(tracingProperties.getBaggage().getCorrelation().getFields());
				}

			}

		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(MDC.class)
	static class Slf4jConfiguration {

		@Bean
		@ConditionalOnMissingBean
		Slf4JEventListener otelSlf4JEventListener() {
			return new Slf4JEventListener();
		}

	}

	static class OTelEventPublisher implements EventPublisher {

		private final List<EventListener> listeners;

		OTelEventPublisher(List<EventListener> listeners) {
			this.listeners = listeners;
		}

		@Override
		public void publishEvent(Object event) {
			for (EventListener listener : this.listeners) {
				listener.onEvent(event);
			}
		}

	}

}
