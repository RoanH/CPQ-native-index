# syntax=docker/dockerfile:1
FROM eclipse-temurin:17 AS compile
LABEL maintainer="roan@roanh.dev"
WORKDIR /CPQ-native\ Index
ADD CPQ-native\ Index/gradle/wrapper/ /index/gradle/wrapper/
ADD CPQ-native\ Index/src/ /index/src/
ADD CPQ-native\ Index/build.gradle /index/
ADD CPQ-native\ Index/gradlew /index/
ADD CPQ-native\ Index/settings.gradle /index/
RUN chmod -R 755 ./
RUN apt-get update && apt-get -y install gcc cmake
RUN ./gradlew :compileNatives
RUN ./gradlew :shadowJar

FROM eclipse-temurin:17
LABEL maintainer="roan@roanh.dev"
WORKDIR /CPQ-native\ Index
COPY --from=compile /index/build/libs/Index.jar ./Index.jar
COPY --from=compile /index/lib/libnauty.so ./lib/libnauty.so
ENTRYPOINT ["java", "-jar", "Index.jar"]