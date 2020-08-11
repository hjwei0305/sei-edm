# Docker for java  sei-edm

# 基础镜像
FROM openoffice-tesseract-jre8-zh-lm:v1.0.0

# 作者
LABEL maintainer="hua.feng@changhong.com"

# 环境变量
## JAVA_OPTS：JAVA启动参数
## APP_NAME：应用名称（各项目需要修改）
ENV JAVA_OPTS=""  APP_NAME="sei-edm"  SERVER_NAME="edm-service"

# 添加应用
ADD $APP_NAME-service/build/libs/$APP_NAME.jar /usr/app/$SERVER_NAME.jar

# 开放8080端口
EXPOSE 8080

# 启动应用
ENTRYPOINT ["sh","-c","java -server -XX:InitialRAMPercentage=75.0  -XX:MaxRAMPercentage=75.0  $JAVA_OPTS -XX:+UseG1GC -jar /usr/app/$SERVER_NAME.jar --file.encoding=UTF-8 --server.servlet.context-path=/$APP_NAME --java.security.egd=file:/dev/./urandom"]
