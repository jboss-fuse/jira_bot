FROM alpine/git
WORKDIR /app
RUN git clone https://github.com/heiko-braun/jira_bot.git

FROM maven:3.5-jdk-8-alpine
WORKDIR /app
COPY --from=0 /app/jira_bot /app

RUN mvn install
FROM openjdk:8-jre-alpine
WORKDIR /app

COPY --from=1 /app/target/jira-bot.jar /app
CMD ["java -jar jira-bot.jar -DlogLevel=info"]