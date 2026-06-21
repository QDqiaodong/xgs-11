# Build stage
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Use Aliyun Maven Mirror
RUN mkdir -p /root/.m2 && echo '<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd"><mirrors><mirror><id>aliyunmaven</id><mirrorOf>*</mirrorOf><name>aliyun maven</name><url>https://maven.aliyun.com/repository/public</url></mirror></mirrors></settings>' > /root/.m2/settings.xml

COPY pom.xml ./
RUN mvn -B -ntp dependency:go-offline
RUN mvn -B -ntp dependency:copy \
    -Dartifact=org.eclipse.jetty:jetty-runner:9.4.46.v20220331 \
    -DoutputDirectory=/app/deps \
    -Dmdep.stripVersion=true

COPY src ./src
RUN mvn -B -ntp package -DskipTests

# Run stage. Reuse the Maven/Temurin image already needed for builds, avoiding
# an extra Tomcat base image pull on machines with restricted registry access.
FROM maven:3.9-eclipse-temurin-17
WORKDIR /app
COPY pom.xml ./
COPY --from=build /root/.m2 /root/.m2
COPY --from=build /app/deps/jetty-runner.jar /app/jetty-runner.jar
COPY --from=build /app/target/todo-app.war /app/todo-app.war
ENV JAVA_OPTS="-Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8" \
    LANG=C.UTF-8
EXPOSE 8080
CMD ["java", "-Dfile.encoding=UTF-8", "-Dsun.jnu.encoding=UTF-8", "-jar", "/app/jetty-runner.jar", "--port", "8080", "--path", "/", "/app/todo-app.war"]
