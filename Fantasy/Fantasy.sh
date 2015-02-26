#! /bin/sh


libs=`ls lib`

if test "${TERM}" == "cygwin" ; then
   SEP=";"
else
   SEP=":"
fi

ant jar || exit 1

CP=.${SEP}dist/fantasy.jar

for lib in ${libs} ; do

  CP=${CP}${SEP}lib/${lib} 

done

#echo "CP=$CP"

java -cp "$CP" cc.fantasy.swing.Fantasy $@
