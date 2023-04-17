/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.strimzi.kafka.bridge.quarkus;

import io.micrometer.core.instrument.Meter;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.micrometer.prometheus.PrometheusNamingConvention;
import io.quarkus.runtime.Startup;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

/**
 * Used for scraping and reporting metrics in Prometheus format
 */
@Startup
public class MetricsReporter {

    @Inject
    JmxCollectorRegistry restJmxCollectorRegistry;

    @Inject
    PrometheusMeterRegistry meterRegistry;

    @PostConstruct
    void init() {
        meterRegistry.config().namingConvention(new MetricsReporter.MetricsNamingConvention());
    }

    /**
     * Scrape metrics on the provided registries returning them in the Prometheus format
     *
     * @return metrics in Prometheus format as String
     */
    public String scrape() {
        StringBuilder sb = new StringBuilder();
        if (restJmxCollectorRegistry != null) {
            sb.append(restJmxCollectorRegistry.scrape());
        }
        if (meterRegistry != null) {
            sb.append(meterRegistry.scrape());
        }
        return sb.toString();
    }

    private static class MetricsNamingConvention extends PrometheusNamingConvention {
        @Override
        public String name(String name, Meter.Type type, String baseUnit) {
            String metricName = name.startsWith("http.") ? "strimzi.bridge." + name : name;
            return super.name(metricName, type, baseUnit);
        }
    }
}
