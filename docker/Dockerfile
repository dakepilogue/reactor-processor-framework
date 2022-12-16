FROM wz2cool/maven3-aliyun-jdk8:2

MAINTAINER zmz8323802@163.com
# Step : package jar
WORKDIR /app
COPY ./ ./

# Step : deploy jar
RUN mvn clean deploy -DskipTests