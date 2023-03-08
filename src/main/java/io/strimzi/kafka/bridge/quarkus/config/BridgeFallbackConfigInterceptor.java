/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.strimzi.kafka.bridge.quarkus.config;

import io.smallrye.config.FallbackConfigSourceInterceptor;
import io.strimzi.kafka.bridge.config.KafkaAdminConfig;
import io.strimzi.kafka.bridge.config.KafkaConfig;
import io.strimzi.kafka.bridge.config.KafkaConsumerConfig;
import io.strimzi.kafka.bridge.config.KafkaProducerConfig;

import java.util.function.Function;

public class BridgeFallbackConfigInterceptor extends FallbackConfigSourceInterceptor {
    public BridgeFallbackConfigInterceptor() {
        super(new Function<String, String>() {
            @Override
            public String apply(final String name) {
                if (name.startsWith(KafkaConsumerConfig.KAFKA_CONSUMER_CONFIG_PREFIX)) {
                    return removeQuotes(KafkaConsumerConfig.KAFKA_CONSUMER_CONFIG_PREFIX, name);
                }
                if (name.startsWith(KafkaProducerConfig.KAFKA_PRODUCER_CONFIG_PREFIX)) {
                    return removeQuotes(KafkaProducerConfig.KAFKA_PRODUCER_CONFIG_PREFIX, name);
                }
                if (name.startsWith(KafkaAdminConfig.KAFKA_ADMIN_CONFIG_PREFIX)) {
                    return removeQuotes(KafkaAdminConfig.KAFKA_ADMIN_CONFIG_PREFIX, name);
                }
                if (name.startsWith(io.strimzi.kafka.bridge.config.KafkaConfig.KAFKA_CONFIG_PREFIX)) {
                    return removeQuotes(KafkaConfig.KAFKA_CONFIG_PREFIX, name);
                }
                return name;
            }
        });
    }

    private static String removeQuotes(String prefix, String name) {
        String key = name.substring(prefix.length());
        return key.charAt(0) == '"' && key.charAt(key.length() - 1) == '"' ?
               prefix + key.substring(1, key.length() - 1) :
               name;
    }
}
