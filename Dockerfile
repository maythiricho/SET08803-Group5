FROM openjdk:18
COPY target/Assessmentforgp5-1.0.0-SNAPSHOT.jar /tmp
WORKDIR /tmp
ENTRYPOINT ["java", "-jar", "Assessmentforgp5-1.0.0-SNAPSHOT.jar"]