#! /bin/sh

if test -n "$4" ; then

echo "
Usage `basename $0` <release keystore file> <release keystore pass> <release keystore alias> <release alias pass>

Will generate custom-debug.keystore

Copy the new keystore into ~/.android/debug.keystore for Eclipse to build project using release keystore
"

exit 0

fi

keytool -importkeystore -v -srckeystore $1 -destkeystore custom-debug.keystore -srcstorepass $2 -deststorepass android -srcalias $3 -destalias androiddebugkey -srckeypass $4 -destkeypass android



