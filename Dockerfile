# syntax=docker/dockerfile:1
FROM eclipse-temurin:17 AS compile
LABEL maintainer="roan@roanh.dev"
WORKDIR /Index
ADD ["CPQ-native Index/gradle/wrapper/", "/Index/gradle/wrapper/"]
ADD ["CPQ-native Index/src/", "/Index/src/"]
ADD ["CPQ-native Index/build.gradle", "/Index/"]
ADD ["CPQ-native Index/gradlew", "/Index/"]
ADD ["CPQ-native Index/settings.gradle", "/Index/"]
ADD ["CPQ-native Index/native/", "/Index/native/"]
RUN chmod -R 755 ./
RUN apt-get update && apt-get -y install gcc cmake
RUN ./gradlew :compileNatives
RUN ./gradlew :shadowJar

FROM eclipse-temurin:17
LABEL maintainer="roan@roanh.dev"
WORKDIR /Index
COPY --from=compile /Index/build/libs/Index-v1.0.jar ./Index.jar
COPY --from=compile /Index/lib/libnauty.so ./lib/libnauty.so
ENTRYPOINT ["java", "-jar", "Index.jar"]