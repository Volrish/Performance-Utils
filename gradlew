#!/bin/sh
# Gradle wrapper script for UN*X
# https://gradle.org

APP_NAME="Gradle"
APP_BASE_NAME=${0##*/}
APP_HOME=$( cd "${0%"${0##*/}"}.." && pwd -P ) || exit

DEFAULT_JVM_OPTS='"-Xmx64m" "-Xms64m"'
CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar

if [ -n "$JAVA_HOME" ] ; then
    JAVACMD=$JAVA_HOME/bin/java
    [ -x "$JAVACMD" ] || { echo "ERROR: JAVA_HOME points to invalid dir: $JAVA_HOME"; exit 1; }
else
    JAVACMD=java
    command -v java >/dev/null 2>&1 || { echo "ERROR: No 'java' in PATH and JAVA_HOME not set."; exit 1; }
fi

exec "$JAVACMD" $DEFAULT_JVM_OPTS $JAVA_OPTS $GRADLE_OPTS \
    "-Dorg.gradle.appname=$APP_BASE_NAME" \
    -classpath "$CLASSPATH" \
    org.gradle.wrapper.GradleWrapperMain \
    "$@"
