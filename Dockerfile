# syntax=docker/dockerfile:1
ARG version=0.0

FROM eclipse-temurin:25 AS compile
LABEL maintainer="roan@roanh.dev"
ARG version
WORKDIR /Index
COPY ["CPQ-native Index/", "/Index/"]
RUN chmod -R 755 ./
RUN apt-get update && apt-get -y install gcc cmake
RUN ./gradlew -PrefName=v$version :compileNatives
RUN ./gradlew -PrefName=v$version cli:shadowJar

FROM eclipse-temurin:25
LABEL maintainer="roan@roanh.dev"
ARG version
WORKDIR /Index
COPY --from=compile /Index/build/libs/*.jar ./Index.jar
COPY --from=compile /Index/lib/libnauty.so ./lib/libnauty.so
ENTRYPOINT ["java", "-jar", "Index.jar"]