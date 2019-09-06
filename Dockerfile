FROM alpine/git as clone
WORKDIR /app
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
CMD ["java -jar jira-bot-jar-with-dependencies.jar"]