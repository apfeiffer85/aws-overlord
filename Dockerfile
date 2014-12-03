FROM dockerfile/java:oracle-java8

ADD target/aws-overlord.jar .

EXPOSE 8080

CMD ["/usr/bin/java",  "-jar", "/aws-overlord.jar"]