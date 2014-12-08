FROM docker-registry.hackweek.aws.zalando/tsarnowski/zalando-java:8u25-1

ADD target/aws-overlord.jar .

EXPOSE 8080

CMD java -jar /aws-overlord.jar