/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.strimzi.kafka.bridge.quarkus.config;

import io.smallrye.config.ConfigSourceInterceptorContext;
import io.smallrye.config.ConfigValue;
import io.smallrye.config.RelocateConfigSourceInterceptor;
import io.strimzi.kafka.bridge.quarkus.tracing.TracingManager;

import java.util.function.Function;

/**
 * Bridge relocate configuration interceptor
 * It is used for two main goals:
 * - double quoting all Kafka common/admin/producer/consumer properties because Quarkus doesn't allow to have
 *   properties with "." in the names after the configuration prefix when using Map(s) {@link io.strimzi.kafka.bridge.quarkus.config.KafkaConfig}
 *   (i.e. kafka.foo.bar has to be mapped to kafka."foo.bar" so that foo.bar will be the key in the Map to access the value)
 *   For example, it will relocate a kafka.foo.bar to kafka."foo.bar" which will be the key in the corresponding
 *   Map in the {@link io.strimzi.kafka.bridge.quarkus.config.KafkaConfig} class.
 * - relocate the quarkus.http.* properties to corresponding http.* to allow users continuing to use our prefix
 */
@SuppressWarnings("NPathComplexity")
public class BridgeRelocateConfigInterceptor extends RelocateConfigSourceInterceptor {

    private static final long serialVersionUID = 1L;

    public BridgeRelocateConfigInterceptor() {
        super(new Function<String, String>() {
            @Override
            public String apply(final String name) {
                if (name.startsWith(KafkaConfig.KAFKA_CONSUMER_CONFIG_PREFIX)) {
                    return withQuotes(KafkaConfig.KAFKA_CONSUMER_CONFIG_PREFIX, name);
                }
                if (name.startsWith(KafkaConfig.KAFKA_PRODUCER_CONFIG_PREFIX)) {
                    return withQuotes(KafkaConfig.KAFKA_PRODUCER_CONFIG_PREFIX, name);
                }
                if (name.startsWith(KafkaConfig.KAFKA_ADMIN_CONFIG_PREFIX)) {
                    return withQuotes(KafkaConfig.KAFKA_ADMIN_CONFIG_PREFIX, name);
                }
                if (name.startsWith(KafkaConfig.KAFKA_CONFIG_PREFIX)) {
                    return withQuotes(KafkaConfig.KAFKA_CONFIG_PREFIX, name);
                }
                if (name.equals("quarkus.http.port")) {
                    return "http.port";
                }
                if (name.equals("quarkus.http.host")) {
                    return "http.host";
                }
                if (name.equals("quarkus.http.cors")) {
                    return "http.cors.enabled";
                }
                if (name.equals("quarkus.http.cors.origins")) {
                    return "http.cors.allowedOrigins";
                }
                if (name.equals("quarkus.http.cors.methods")) {
                    return "http.cors.allowedMethods";
                }
                return name;
            }
        });
    }
    private static String withQuotes(String prefix, String name) {
        String key = name.substring(prefix.length());
        return key.charAt(0) == '"' && key.charAt(key.length() - 1) == '"' ?
               name :
               String.format("%s\"%s\"", prefix, key);
    }

    @Override
    public ConfigValue getValue(ConfigSourceInterceptorContext context, String name) {
        // NOTE: OpenTelemetry cannot be disabled at runtime so the workaround is about turning on and off the sampler
        //       Traces generation is always enabled but then the traces are sent or not based on the sampler.
        //       Anyway for backward compatibility we want users still using the bridge.tracing property to enable tracing.
        //       This implementation allow to relocate the quarkus.opentelemetry.tracer.sampler set to "on" or "off" based
        //       on the value of the "bridge.tracing"
        if (name.equals("quarkus.opentelemetry.tracer.sampler")) {
            ConfigValue bridgeTracing = context.proceed("bridge.tracing");

            String value = bridgeTracing != null && bridgeTracing.getValue().equals(TracingManager.OPENTELEMETRY) ? "on" : "off";
            ConfigValue sampler;
            // creating a new ConfigValue respecting the data from the referring one.
            // Using the bridge.tracing one if enabled otherwise the original quarkus.opentelemetry.tracer.sampler.
            // No way to easy close, so creating from scratch. Opened issue: https://github.com/smallrye/smallrye-config/issues/902
            if (value.equals("on")) {
                sampler = ConfigValue.builder()
                        .withName("quarkus.opentelemetry.tracer.sampler")
                        .withValue(value)
                        .withRawValue(value)
                        .withConfigSourceName(bridgeTracing.getConfigSourceName())
                        .withConfigSourceOrdinal(bridgeTracing.getConfigSourceOrdinal())
                        .withConfigSourcePosition(bridgeTracing.getConfigSourcePosition())
                        .withProfile(bridgeTracing.getProfile())
                        .build();
            } else {
                ConfigValue original = context.proceed("quarkus.opentelemetry.tracer.sampler");
                sampler = ConfigValue.builder()
                        .withName(original.getName())
                        .withValue(value)
                        .withRawValue(value)
                        .withConfigSourceName(original.getConfigSourceName())
                        .withConfigSourceOrdinal(original.getConfigSourceOrdinal())
                        .withConfigSourcePosition(original.getConfigSourcePosition())
                        .withProfile(original.getProfile())
                        .withLineNumber(original.getLineNumber())
                        .build();
            }
            return sampler;
        }
        return super.getValue(context, name);
    }
}
