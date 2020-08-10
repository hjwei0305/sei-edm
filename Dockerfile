# Docker for java  sei-edm

# 基础镜像
FROM openoffice-tesseract-jre8-zh-lm:v1.0.3

# 作者
LABEL maintainer="hua.feng@changhong.com"

# 环境变量
## JAVA_OPTS：JAVA启动参数
## APP_NAME：应用名称（各项目需要修改）
ENV JAVA_OPTS=""  APP_NAME="sei-edm"  SERVER_NAME="edm-service"

# 添加应用
ADD $APP_NAME-service/build/libs/$APP_NAME.jar /usr/app/$SERVER_NAME.jar

# 启动应用
CMD ["/usr/local/entrypoint.sh"]
