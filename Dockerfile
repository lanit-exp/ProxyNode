FROM openjdk:8-jdk-alpine as build

ARG MVN_VERSION=3.9.6
ARG USER_HOME_DIR="/root"
ARG BASE_URL=https://apache.osuosl.org/maven/maven-3/${MVN_VERSION}/binaries

RUN apk add --no-cache curl \
    && mkdir -p /usr/share/maven /usr/share/maven/ref \
    && curl -fsSL -o /tmp/apache-maven.tar.gz ${BASE_URL}/apache-maven-${MVN_VERSION}-bin.tar.gz \
    && tar -xzf /tmp/apache-maven.tar.gz -C /usr/share/maven --strip-components=1 \
    && rm -f /tmp/apache-maven.tar.gz \
    && ln -s /usr/share/maven/bin/mvn /usr/bin/mvn

ENV MAVEN_HOME /usr/share/maven
ENV MAVEN_CONFIG "$USER_HOME_DIR/.m2"

WORKDIR /data

COPY / /data

RUN mvn package

FROM openjdk:8-jdk-alpine

ARG APP_VERSION="1.0.2"
ARG APP_PORT=8100
ARG APP_OPT="-Dfile.encoding=UTF-8"
ARG APP_XMX_OPT="-XX:MaxRAMPercentage=75.0"

LABEL name="ProxyNode"
LABEL version=$APP_VERSION

ENV APP_USER app_user
ENV APP_GROUP app_group
ENV APP_XMX_OPT $APP_XMX_OPT
ENV APP_OPT $APP_OPT
ENV APP_PORT $APP_PORT
ENV APP_HOME /home/$APP_USER
ENV APP_NAME app

RUN apk add --no-cache curl \
    && addgroup $APP_GROUP && adduser -S -u 1000 -G $APP_GROUP $APP_USER \
    && echo "${APP_USER}:${APP_GROUP}" | chpasswd

COPY --from=build /data/target/*.jar $APP_HOME/$APP_NAME.jar
COPY entrypoint $APP_HOME/

WORKDIR $APP_HOME

RUN chmod +x entrypoint && ln -s entrypoint /usr/local/bin/entrypoint

USER $APP_USER

EXPOSE $APP_PORT

HEALTHCHECK --interval=6s --timeout=5s --start-period=7s CMD curl --fail http://localhost:$APP_PORT/health || exit 1

ENTRYPOINT ["sh", "entrypoint"]
