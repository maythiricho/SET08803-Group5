FROM amazoncorretto:17
COPY ./target/devops.jar /tmp/devops.jar
WORKDIR /tmp
ENTRYPOINT ["java", "-jar", "devops.jar", "db:3306", "30000"]