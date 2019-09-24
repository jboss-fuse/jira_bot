FROM alpine/git as clone
WORKDIR /app

RUN apk --update add \
        fontconfig \
        ttf-dejavu

RUN ln -s /usr/lib/libfontconfig.so.1 /usr/lib/libfontconfig.so && \
    ln -s /lib/libuuid.so.1 /usr/lib/libuuid.so.1 && \
    ln -s /lib/libc.musl-x86_64.so.1 /usr/lib/libc.musl-x86_64.so.1
ENV LD_LIBRARY_PATH /usr/lib

RUN rm -rf /app/jira_bot
RUN git clone https://github.com/heiko-braun/jira_bot.git

FROM maven:3.5-jdk-8-alpine as build
WORKDIR /app
COPY --from=clone /app/jira_bot /app
RUN mvn clean install

FROM openjdk:8-jre-alpine as main
WORKDIR /app
RUN addgroup -S appuser && adduser -S -G appuser appuser
USER appuser

COPY --from=build /app/target/jira-bot-jar-with-dependencies.jar /app
EXPOSE 8080
ENTRYPOINT ["sh", "-c"]
CMD ["java -jar jira-bot-jar-with-dependencies.jar -Djava.awt.headless=true"]