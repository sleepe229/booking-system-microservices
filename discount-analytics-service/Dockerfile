FROM bellsoft/liberica-openjdk-alpine:21 AS builder
WORKDIR /application

RUN apk add --no-cache maven

COPY . .

RUN mvn clean install -DskipTests -Dmaven.compiler.parameters=True


FROM bellsoft/liberica-openjre-alpine:21 AS layers
WORKDIR /application

COPY --from=builder /application/discount-analytics-service/target/*.jar app.jar
RUN java -Djarmode=layertools -jar app.jar extract

FROM bellsoft/liberica-openjre-alpine:21
VOLUME /tmp /home/local-storage

RUN addgroup -S spring && adduser -S spring-user -G spring && \
    mkdir -p /home/local-storage && \
    chown spring-user:spring /home/local-storage

USER spring-user

COPY --from=layers /application/dependencies/ ./
COPY --from=layers /application/spring-boot-loader/ ./
COPY --from=layers /application/snapshot-dependencies/ ./
COPY --from=layers /application/application/ ./
COPY --from=layers /application/application/ ./

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]