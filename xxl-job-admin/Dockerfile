# base image
#FROM openjdk:21-jdk-slim
FROM eclipse-temurin:17-jre

# maintainer
MAINTAINER xuxueli

# set params
ENV PARAMS=""

# set timezone
ENV TZ=UTC
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime \
    && echo $TZ > /etc/timezone \
    && groupadd --gid 10001 xxl-job \
    && useradd --uid 10001 --gid 10001 --no-create-home --shell /usr/sbin/nologin xxl-job \
    && mkdir -p /data/applogs/xxl-job \
    && chown -R 10001:10001 /data/applogs

# copy jar
COPY --chown=10001:10001 target/xxl-job-admin-*.jar /app.jar

USER 10001:10001

# command
# log home: -e LOG_HOME=/data/applogs
# jvm options: -e JAVA_OPTS="-Xms128m -Xmx128m"
# app params: -e PARAMS="--server.port=8080"
ENTRYPOINT ["sh","-c","exec java ${LOG_HOME:+-DLOG_HOME=$LOG_HOME} $JAVA_OPTS -jar /app.jar $PARAMS"]
