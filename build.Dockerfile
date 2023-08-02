FROM eclipse-temurin:17.0.7_7-jdk-alpine as builder
ARG repoUsername
ARG repoPassword
ENV ORG_GRADLE_PROJECT_repoUsername=$repoUsername
ENV ORG_GRADLE_PROJECT_repoPassword=$repoPassword

WORKDIR /build
COPY . ./
RUN apk --no-cache add bash # for git-version plugin

RUN ./gradlew -x test publish

RUN rm build/libs/*-plain.jar

FROM scratch
COPY --from=builder /build/build/libs/*.jar /build/