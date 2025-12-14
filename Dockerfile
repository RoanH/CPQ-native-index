# syntax=docker/dockerfile:1
ARG version=0.0

FROM eclipse-temurin:21 AS compile
LABEL maintainer="roan@roanh.dev"
ARG version
WORKDIR /Index
ADD ["CPQ-native Index/gradle/wrapper/", "/Index/gradle/wrapper/"]
ADD ["CPQ-native Index/src/", "/Index/src/"]
ADD ["CPQ-native Index/build.gradle", "/Index/"]
ADD ["CPQ-native Index/gradlew", "/Index/"]
ADD ["CPQ-native Index/settings.gradle", "/Index/"]
ADD ["CPQ-native Index/native/", "/Index/native/"]
RUN chmod -R 755 ./
RUN apt-get update && apt-get -y install gcc cmake
RUN ./gradlew -PrefName=v$version :compileNatives
RUN ./gradlew -PrefName=v$version :shadowJar

FROM eclipse-temurin:21
LABEL maintainer="roan@roanh.dev"
ARG version
WORKDIR /Index
COPY --from=compile /Index/build/libs/Index-v$version.jar ./Index.jar
COPY --from=compile /Index/lib/libnauty.so ./lib/libnauty.so
ENTRYPOINT ["java", "-jar", "Index.jar"]