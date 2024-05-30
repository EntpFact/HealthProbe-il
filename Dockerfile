FROM openjdk:17
EXPOSE 8080
ADD target/healthconfigmap-gke.jar healthconfigmap-gke.jar
ENTRYPOINT ["java","-jar","/healthconfigmap-gke.jar"]