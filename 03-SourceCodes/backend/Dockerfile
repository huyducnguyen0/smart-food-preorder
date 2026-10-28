# Image chạy backend (Spring Boot). Build hai bước: bước 1 có JDK + Maven Wrapper để đóng gói jar, bước 2 chỉ có JRE
# để chạy, nên image cuối nhỏ hơn và không chứa mã nguồn. Test không chạy ở đây: test chạy bằng `./mvnw verify`
# trước khi merge (xem 03-SourceCodes/README.md).

FROM eclipse-temurin:25-jdk AS build
WORKDIR /app
# Chép file cấu hình Maven trước để Docker giữ lại lớp thư viện đã tải khi chỉ mã nguồn thay đổi.
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN sh mvnw -B -q dependency:go-offline
COPY config config
COPY src src
RUN sh mvnw -B -q -DskipTests package && cp target/preorder-backend-*.jar app.jar

FROM eclipse-temurin:25-jre
WORKDIR /app
ENV TZ=Asia/Ho_Chi_Minh
# Không chạy bằng root trong container.
RUN groupadd --system app && useradd --system --gid app app
COPY --from=build /app/app.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
