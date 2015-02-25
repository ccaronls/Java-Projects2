#! /bin/sh

libs=`ls lib`

if test "${TERM}" == "cygwin" ; then
   SEP=";"
else
   SEP=":"
fi

ant jar || exit 1

#CP=.${SEP}dist/soc.jar
CP=.${SEP}antbuild

for lib in ${libs} ; do

  CP=${CP}${SEP}lib/${lib} 

done

echo "CP=$CP"

#java -cp "$CP" cc.game.soc.net.StandaloneServer $@
java -cp "$CP" cc.game.soc.netx.SimpleServer $@
