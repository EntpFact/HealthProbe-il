FROM openjdk:17
EXPOSE 8080
RUN mkdir -p /opt/certs
ADD target/healthconfigmap-gke.jar healthconfigmap-gke.jar
COPY src/main/resources/root.crt /opt/certs/root.crt
ENV PGSSLROOTCERT /opt/cetrs/root.crt
ENTRYPOINT ["java","-jar","/healthconfigmap-gke.jar"]