#! /bin/sh

echo "Deploying fantasy.war to resin ..."

SUDO="sudo -u lsadm"
SUDOROOT=sudo

if test -z "$RESIN_HOME" ; then
   echo "Please define RESIN_HOME (e.g. /user/local/resin)"
   exit 1
fi

if test ${TERM} == cygwin ; then
   CYGWIN=true
   SUDO=
   SUDOROOT=
   PREFIX=C:
fi

${SUDO} ${RESIN_HOME}/bin/httpd.sh stop

# build the war file, no unit tests
echo "Building ..."
ant war || exit 1

FANDIR=${PREFIX}/fan
LOGDIR=${FANDIR}/log

echo "RESIN_HOME=${RESIN_HOME}"
echo "JAVA_HOME=${JAVA_HOME}"
echo "ANT_HOME=${ANT_HOME}"
echo "FANDIR=${FANDIR}"
echo "LOGDIR=${LOGDIR}"

if ! test -d ${FANDIR} ; then
   ${SUDO} mkdir -p ${FANDIR} || exit 1
fi

if test -z "${CYGWIN}" ; then
   ${SUDO} chown -R lsadm:lsurf ${FANDIR}/* || exit 1
fi

if ! test -d ${LOGDIR} ; then
   ${SUDO} mkdir -p ${LOGDIR} || exit 1
fi   

${SUDO} rm -rf ${LOGDIR}/*
${SUDO} touch ${LOGDIR}/fantasy.log

sleep 2
${SUDOROOT} rm -rf ${RESIN_HOME}/webapps/fantasy*
${SUDO} cp dist/fantasy.war ${RESIN_HOME}/webapps || exit 1

# To Debug, or not debug ???
#EXTRA_ARGS="-Xdebug -Xnoagent -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=9999"

#if test -n "${CLASSPATH}" ; then
#  EXTRA_ARGS="${EXTRA_ARGS} -classpath ${CLASSPATH}"
#fi

#export CLASSPATH=${CLASSPATH}:/lsurf/resin/webapps/fantasy/WEB-INF

if test -z "${CYGWIN}" ; then
   EXTRA_ARGS="-Xdebug -Xnoagent -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=9999"	
   LOG_ARGS="-stderr ${LOGDIR}/resin_stderr -stdout ${LOGDIR}/resin_stdout"
   cd /lsurf
else
   cp conf/fantasy-resin.xml ${RESIN_HOME}/conf/ || exit 1   
   LOG_ARGS="-log-directory ${LOGDIR}"
fi

if test -f "${RESIN_HOME}/conf/fantasy-resin.xml" ; then
   CONF="-conf ${RESIN_HOME}/conf/fantasy-resin.xml"
   echo "Found ${CONF}"
fi

cmd="${SUDO} ${RESIN_HOME}/bin/httpd.sh ${CONF}  ${EXTRA_ARGS} ${LOG_ARGS} start" 
echo "$cmd"
${cmd} || exit 1

sleep 3

echo "Tailing the logs ..."

tail -f ${LOGDIR}/*
