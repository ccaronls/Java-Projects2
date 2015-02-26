#! /bin/sh

export CATALINA_HOME=/cygdrive/c/tomcat
export JPDA_OPTS="-Xdebug -Xnoagent -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=12345"
export TMP=/cygdrive/c/tmp
export CATALINA_PID=${TMP}/fantasy/run/fantasy.pid

if ! test -d ${TMP} ; then
   mkdir -p ${TMP} || exit 1
fi

if ! test -d ${TMP}/fantasy/run ; then
   mkdir -p ${TMP}/fantasy/run || exit 1
fi
   

${CATALINA_HOME}/bin/catalina.sh stop -force

ant war || exit 1

cp dist/fantasy.war ${CATALINA_HOME}/webapps/ || exit 1

${CATALINA_HOME}/bin/startup.sh
