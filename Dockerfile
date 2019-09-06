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
COPY --from=build /app/target/jira-bot.jar /app
EXPOSE 8080
ENTRYPOINT ["sh", "-c"]
CMD ["java -jar jira-bot.jar"]