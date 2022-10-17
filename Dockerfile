# define base docker image
FROM adoptopenjdk/openjdk11:jdk-11.0.5_10-alpine
LABEL maintainer="telegram-bot"
ADD target/FeedingKittiesBot-0.0.1-SNAPSHOT.jar /src/feeding-kitties-bot.jar
WORKDIR /src
ENTRYPOINT ["java", "-jar", "feeding-kitties-bot.jar"]