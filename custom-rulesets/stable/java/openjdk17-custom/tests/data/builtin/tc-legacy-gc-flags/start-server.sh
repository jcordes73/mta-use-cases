#!/bin/bash
# Legacy JDK 8 startup options for the openjdk17-custom jvm-flags rules
JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseConcMarkSweepGC -XX:MaxPermSize=256m"
exec java $JAVA_OPTS -jar app.jar
