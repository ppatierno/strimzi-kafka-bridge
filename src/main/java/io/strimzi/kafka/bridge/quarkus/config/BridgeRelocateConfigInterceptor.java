/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.strimzi.kafka.bridge.quarkus.config;

import io.smallrye.config.RelocateConfigSourceInterceptor;
import io.strimzi.kafka.bridge.config.KafkaAdminConfig;
import io.strimzi.kafka.bridge.config.KafkaConfig;
import io.strimzi.kafka.bridge.config.KafkaConsumerConfig;
import io.strimzi.kafka.bridge.config.KafkaProducerConfig;

import java.util.function.Function;

/**
 * Bridge configuration interceptor
 * It is used for two main goals:
 * - double quoting all Kafka common/admin/producer/consumer properties because Quarkus doesn't allow to have
 *   properties with "." in the names after the configuration prefix
 *   (i.e. kafka.consumer.auto.offset.reset has to be mapped to kafka.consumer."auto.offset.reset")
 * - relocate the http.* properties to corresponding quarkus.http.* to allow users continuing to use our prefix
 */
@SuppressWarnings("NPathComplexity")
public class BridgeRelocateConfigInterceptor extends RelocateConfigSourceInterceptor {
    public BridgeRelocateConfigInterceptor() {
        super(new Function<String, String>() {
            @Override
            public String apply(final String name) {
                if (name.startsWith(KafkaConsumerConfig.KAFKA_CONSUMER_CONFIG_PREFIX)) {
                    return withQuotes(KafkaConsumerConfig.KAFKA_CONSUMER_CONFIG_PREFIX, name);
                }
                if (name.startsWith(KafkaProducerConfig.KAFKA_PRODUCER_CONFIG_PREFIX)) {
                    return withQuotes(KafkaProducerConfig.KAFKA_PRODUCER_CONFIG_PREFIX, name);
                }
                if (name.startsWith(KafkaAdminConfig.KAFKA_ADMIN_CONFIG_PREFIX)) {
                    return withQuotes(KafkaAdminConfig.KAFKA_ADMIN_CONFIG_PREFIX, name);
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
                if (name.equals("quarkus.http.cors.enabled")) {
                    return "http.cors";
                }
                if (name.equals("quarkus.http.cors.allowedOrigins")) {
                    return "http.cors.origins";
                }
                if (name.equals("quarkus.http.cors.allowedMethods")) {
                    return "http.cors.methods";
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
}
