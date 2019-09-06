# Alpine Linux with OpenJDK JRE
FROM openjdk:8-jre-alpine

COPY target/bit.jar /bot.jar

# run application with this command line 
CMD ["/usr/bin/java", "-jar", "-DlogLevel=info", "/bot.jar"]
