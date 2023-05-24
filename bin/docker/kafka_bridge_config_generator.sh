#!/usr/bin/env bash

if [ -n "$STRIMZI_TRACING" ]; then
    BRIDGE_TRACING="bridge.tracing=${STRIMZI_TRACING}"
fi

BRIDGE_PROPERTIES=$(cat <<-EOF
#Bridge configuration
bridge.id=${KAFKA_BRIDGE_ID}
${BRIDGE_TRACING}
EOF
)

SECURITY_PROTOCOL=PLAINTEXT

if [ "$KAFKA_BRIDGE_TLS" = "true" ]; then
    SECURITY_PROTOCOL="SSL"

    if [ -n "$KAFKA_BRIDGE_TRUSTED_CERTS" ]; then
        TLS_CONFIGURATION=$(cat <<EOF
#TLS/SSL
kafka.ssl.truststore.location=/tmp/strimzi/bridge.truststore.p12
kafka.ssl.truststore.password=${CERTS_STORE_PASSWORD}
kafka.ssl.truststore.type=PKCS12
EOF
)
    fi

    if [ -n "$KAFKA_BRIDGE_TLS_AUTH_CERT" ] && [ -n "$KAFKA_BRIDGE_TLS_AUTH_KEY" ]; then
        TLS_AUTH_CONFIGURATION=$(cat <<EOF
kafka.ssl.keystore.location=/tmp/strimzi/bridge.keystore.p12
kafka.ssl.keystore.password=${CERTS_STORE_PASSWORD}
kafka.ssl.keystore.type=PKCS12
EOF
)
    fi
fi

if [ -n "$KAFKA_BRIDGE_SASL_MECHANISM" ]; then
    if [ "$SECURITY_PROTOCOL" = "SSL" ]; then
        SECURITY_PROTOCOL="SASL_SSL"
    else
        SECURITY_PROTOCOL="SASL_PLAINTEXT"
    fi

    if [ "x$KAFKA_BRIDGE_SASL_MECHANISM" = "xplain" ]; then
        PASSWORD=$(cat /opt/strimzi/bridge-password/$KAFKA_BRIDGE_SASL_PASSWORD_FILE)
        SASL_MECHANISM="PLAIN"
        JAAS_CONFIG="org.apache.kafka.common.security.plain.PlainLoginModule required username=\"${KAFKA_BRIDGE_SASL_USERNAME}\" password=\"${PASSWORD}\";"
    elif [ "x$KAFKA_BRIDGE_SASL_MECHANISM" = "xscram-sha-512" ]; then
        PASSWORD=$(cat /opt/strimzi/bridge-password/$KAFKA_BRIDGE_SASL_PASSWORD_FILE)
        SASL_MECHANISM="SCRAM-SHA-512"
        JAAS_CONFIG="org.apache.kafka.common.security.scram.ScramLoginModule required username=\"${KAFKA_BRIDGE_SASL_USERNAME}\" password=\"${PASSWORD}\";"
    elif [ "x$KAFKA_BRIDGE_SASL_MECHANISM" = "xoauth" ]; then
        SASL_MECHANISM="OAUTHBEARER"

        if [ ! -z "$KAFKA_BRIDGE_OAUTH_ACCESS_TOKEN" ]; then
            OAUTH_ACCESS_TOKEN="oauth.access.token=\"$KAFKA_BRIDGE_OAUTH_ACCESS_TOKEN\""
        fi

        if [ ! -z "$KAFKA_BRIDGE_OAUTH_REFRESH_TOKEN" ]; then
            OAUTH_REFRESH_TOKEN="oauth.refresh.token=\"$KAFKA_BRIDGE_OAUTH_REFRESH_TOKEN\""
        fi

        if [ ! -z "$KAFKA_BRIDGE_OAUTH_CLIENT_SECRET" ]; then
            OAUTH_CLIENT_SECRET="oauth.client.secret=\"$KAFKA_BRIDGE_OAUTH_CLIENT_SECRET\""
        fi

        if [ ! -z "$KAFKA_BRIDGE_OAUTH_PASSWORD_GRANT_PASSWORD" ]; then
            OAUTH_PASSWORD_GRANT_PASSWORD="oauth.password.grant.password=\"$KAFKA_BRIDGE_OAUTH_PASSWORD_GRANT_PASSWORD\""
        fi

        if [ -f "/tmp/strimzi/oauth.truststore.p12" ]; then
            OAUTH_TRUSTSTORE="oauth.ssl.truststore.location=\"/tmp/strimzi/oauth.truststore.p12\" oauth.ssl.truststore.password=\"${CERTS_STORE_PASSWORD}\" oauth.ssl.truststore.type=\"PKCS12\""
        fi

        JAAS_CONFIG="org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginModule required ${KAFKA_BRIDGE_OAUTH_CONFIG} ${OAUTH_CLIENT_SECRET} ${OAUTH_REFRESH_TOKEN} ${OAUTH_ACCESS_TOKEN} ${OAUTH_PASSWORD_GRANT_PASSWORD} ${OAUTH_TRUSTSTORE};"
        OAUTH_CALLBACK_CLASS="kafka.sasl.login.callback.handler.class=io.strimzi.kafka.oauth.client.JaasClientOauthLoginCallbackHandler"
    fi

    SASL_AUTH_CONFIGURATION=$(cat <<EOF
kafka.sasl.mechanism=${SASL_MECHANISM}
kafka.sasl.jaas.config=${JAAS_CONFIG}
${OAUTH_CALLBACK_CLASS}
EOF
)
fi

KAFKA_PROPERTIES=$(cat <<-EOF
#Kafka common properties
kafka.bootstrap.servers=${KAFKA_BRIDGE_BOOTSTRAP_SERVERS}
kafka.security.protocol=${SECURITY_PROTOCOL}
${TLS_CONFIGURATION}
${TLS_AUTH_CONFIGURATION}
${SASL_AUTH_CONFIGURATION}
EOF
)

PRODUCER_PROPERTIES="#Apache Kafka Producer"

for i in $KAFKA_BRIDGE_PRODUCER_CONFIG; do

        key="kafka.producer.$(echo $i | cut -d'=' -f1)"
        value="$(echo -n $i | cut -d'=' -f2)"
        PRODUCER_PROPERTIES=$(cat <<EOF
$PRODUCER_PROPERTIES
${key}=${value}
EOF
)
done


CONSUMER_PROPERTIES="#Apache Kafka Consumer"
for i in $KAFKA_BRIDGE_CONSUMER_CONFIG; do
        key="kafka.consumer.$(echo $i | cut -d'=' -f1)"
        value="$(echo -n $i | cut -d'=' -f2)"
        CONSUMER_PROPERTIES=$(cat <<EOF
$CONSUMER_PROPERTIES
${key}=${value}
EOF
)
done

# Get broker rack if it's enabled from the file $KAFKA_HOME/init/rack.id (if it exists). This file is generated by the
# init-container used when rack awareness is enabled.
if [ -e "$STRIMZI_HOME/init/rack.id" ]; then
  STRIMZI_RACK_ID=$(cat "$STRIMZI_HOME/init/rack.id")
  CONSUMER_PROPERTIES=$(cat <<EOF
$CONSUMER_PROPERTIES
kafka.consumer.client.rack=${STRIMZI_RACK_ID}
EOF
)
fi

ADMIN_CLIENT_PROPERTIES="#Apache Kafka AdminClient"
for i in $KAFKA_BRIDGE_ADMIN_CLIENT_CONFIG; do
        key="kafka.admin.$(echo $i | cut -d'=' -f1)"
        value="$(echo -n $i | cut -d'=' -f2)"
        ADMIN_CLIENT_PROPERTIES=$(cat <<EOF
$ADMIN_CLIENT_PROPERTIES
${key}=${value}
EOF
)
done

HTTP_PROPERTIES=$(cat <<-EOF
#HTTP configuration
http.host=${KAFKA_BRIDGE_HTTP_HOST}
http.port=${KAFKA_BRIDGE_HTTP_PORT}
http.cors.enabled=${KAFKA_BRIDGE_CORS_ENABLED}
EOF
)

if [ "${KAFKA_BRIDGE_CORS_ALLOWED_ORIGINS}" ]; then
        HTTP_PROPERTIES=$(cat <<EOF
        $HTTP_PROPERTIES
        http.cors.allowedOrigins=${KAFKA_BRIDGE_CORS_ALLOWED_ORIGINS}
EOF
)
fi

if [ "${KAFKA_BRIDGE_CORS_ALLOWED_METHOD}" ]; then
        HTTP_PROPERTIES=$(cat <<EOF
        $HTTP_PROPERTIES
        http.cors.allowedMethods=${KAFKA_BRIDGE_CORS_ALLOWED_METHODS}
EOF
)
fi

PROPERTIES=$(cat <<EOF
$BRIDGE_PROPERTIES

$KAFKA_PROPERTIES

$ADMIN_CLIENT_PROPERTIES

$PRODUCER_PROPERTIES

$CONSUMER_PROPERTIES

$HTTP_PROPERTIES
EOF
)

cat <<EOF
$PROPERTIES
EOF