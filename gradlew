#!/bin/sh
#
# Gradle start up script for POSIX generated for raw_edit v2.
#
APP_HOME=$( cd "${0%/*}" && pwd -P ) || exit
DEFAULT_JVM_OPTS="-Xmx64m -Xms64m"
WRAPPER_JAR="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"

if [ -n "$JAVA_HOME" ] ; then
    JAVACMD="$JAVA_HOME/bin/java"
else
    JAVACMD="java"
fi

if [ -f "$WRAPPER_JAR" ] ; then
    exec "$JAVACMD" $DEFAULT_JVM_OPTS $JAVA_OPTS $GRADLE_OPTS -classpath "$WRAPPER_JAR" org.gradle.wrapper.GradleWrapperMain "$@"
elif command -v gradle >/dev/null 2>&1 ; then
    exec gradle "$@"
else
    echo "ERROR: gradle-wrapper.jar not found (excluded per zero-binary constraint) and 'gradle' is not on PATH."
    echo "Run 'gradle wrapper --gradle-version 8.13' once or open the project in Android Studio to bootstrap the wrapper."
    exit 1
fi
