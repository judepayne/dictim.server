# syntax=docker/dockerfile:1

FROM clojure:temurin-8-tools-deps-1.11.1.1435-jammy

ARG PORT
ARG SSLPORT

RUN mkdir /app
WORKDIR /app
COPY . /app

# build the standalone dictim server jar
RUN clj -T:build uber

# install terrastruct d2
RUN curl -fsSL https://d2lang.com/install.sh | sh -s --

RUN curl -fsSL https://d2lang.com/install.sh | sh -s -- --tala

# For https, comment the ${PORT} line below and uncomment the ${SSLPORT} line.
EXPOSE ${PORT}/tcp
# EXPOSE ${SSLPORT}/tcp

COPY docker/entrypoint.sh /sbin/entrypoint.sh
RUN chmod 755 /sbin/entrypoint.sh

ENTRYPOINT ["/sbin/entrypoint.sh"]
