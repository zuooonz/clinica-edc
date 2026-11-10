FROM docker.io/library/maven:3.9.9-eclipse-temurin-11@sha256:8d3b35643e52d707b16a3e9b52698be1b75c2b45beb5d0e37d35e881f0a18ced AS builder

WORKDIR /app
ARG SOURCE_REVISION=unversioned
ARG SOURCE_DATE=unknown

COPY . .

RUN \
    # cache downloaded dependencies
    --mount=type=cache,target=/root/.m2 \
    # build cache
    --mount=type=cache,target=/app/core/target \
    --mount=type=cache,target=/app/docs/target \
    --mount=type=cache,target=/app/odm/target \
    --mount=type=cache,target=/app/web/target \
    --mount=type=cache,target=/app/ws/target \
    --mount=type=secret,id=maven_settings \
    --mount=type=secret,id=java_truststore \
    set -eux; \
    set -- --batch-mode --no-transfer-progress --strict-checksums; \
    if [ -f /run/secrets/maven_settings ]; then \
        set -- "$@" --settings /run/secrets/maven_settings; \
    fi; \
    if [ -f /run/secrets/java_truststore ]; then \
        export MAVEN_OPTS="${MAVEN_OPTS:-} -Djavax.net.ssl.trustStore=/run/secrets/java_truststore"; \
    fi; \
    mvn "$@" -Dmaven.buildNumber.skip=true "-DchangeSet=$SOURCE_REVISION" "-DchangeSetDate=$SOURCE_DATE" package; \
    mv web/target/LibreClinica-web.war /;

############################################################
FROM tomcat:9-jdk11@sha256:cf13b145adf437e0c30ff3238016b09f9e904d2d75a714894ced0c8b7ea61554
ARG SOURCE_REVISION=unversioned
LABEL org.opencontainers.image.source="https://github.com/zuooonz/clinica-edc" \
      org.opencontainers.image.revision="$SOURCE_REVISION"

RUN set -eux; \
    # set up redirection to application when accessing tomcat root
    mkdir /usr/local/tomcat/webapps/ROOT; \
    echo '<html><head><meta http-equiv="refresh" content="0; URL=LibreClinica/" /></head></html>' \
        > /usr/local/tomcat/webapps/ROOT/index.html;

# set up volumes for data and logs
VOLUME \
    /usr/local/tomcat/libreclinica.data \
    /usr/local/tomcat/logs

# add config files
COPY \
    /docker/config/ \
    /usr/local/tomcat/libreclinica.config/

# add libre-clinica war file
COPY --from=builder \
    /LibreClinica-web.war \
    /usr/local/tomcat/webapps/LibreClinica.war
