## 박덕근 대표 작업버전에서 개인pc설정시 필요 작업

1. jdbc 연결 관련 설정 
경로 : icon-api\src\main\resources\application.properties
postgresql 접속 정보에 맞춰서 작성
spring.datasource.driver-class-name=org.postgresql.Driver
spring.datasource.url=jdbc:postgresql://localhost:5432/icon]
spring.datasource.username=postgres
spring.datasource.password=1234

2. JWT 키 설정
jwt.secret=swcIv0u99DX5uulYrkvzlGlYHnzwbAYPb7rQrvWO7QsQudGki4Q75c14BLA/wR1Cp3CBhrTb3uWW7WMoccOQ==


3. ai.chat연동정보 데몬에서 주석처리
경로 : icon-api\src\main\resources\application.properties
# AI Chat Webhook Configuration (n8n)
ai.chat.webhook.url
ai.chat.webhook.api-key

# 주석처리 대상 
icon-api\src\main\java\com\itmasters\icon\api\common\client\AiWebhookClient.java
@Value("${ai.webhook.api-key}") -> 주석처리

icon-api\src\main\java\com\itmasters\icon\api\aichat\application\service\N8nAiChatService.java
@Value("${ai.chat.webhook.url}") -> 주석처리

icon-api\src\main\java\com\itmasters\icon\api\rulesnapshot\application\service\RuleSnapshotService.java
@Value("${ai.rulesnapshot.webhook.url}") -> 주석처리


4. gradle wrapper 명령어 실행
- icon-backend 경로에서 명령어 실행 
- gradlew 파일 생성 확인


5. gradle빌드시 명령어
// api jar생성하여 기동
gradlew :icon-api:bootJar


// jar 기동 명령어
기동 파일 : icon-api-0.0.1-SNAPSHOT.jar
java -jar [icon-api-0.0.1-SNAPSHOT.jar] 

# 실행 또는 빌드시 에러 발생할경우 처리
1. icon-entity:compilejar 관련 에러
확인사항 : jdk 21버전 설치 여부

해결방법:
jdk 버전을 최소 17버전 이상설치하며, 17버전만 설치된 경우 JavaLanguageVersion.of(17)에 버전 17로 변경
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

icon-backend\icon-entity\build.gradle 파일의 아래내용 주석처리
//bootJar {
//    enabled = false
//}



2. gradlew build 했을때 에러 발생시 확인사항 
icon-backend\gradle\wrapper\gradle-wrapper.properties
#distributionUrl=https\://services.gradle.org/distributions/gradle-9.2.1-bin.zip --> 버전 문제로 build 안됨
distributionUrl=https\://services.gradle.org/distributions/gradle-8.10.2-bin.zip <-- 해당 버전으로 변경




