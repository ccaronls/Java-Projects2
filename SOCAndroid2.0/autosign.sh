#! /bin/sh

# ccaron - 11/12/10
# Script will auto sign an apk file from generated keys

CARRIER=$1
VERSION=$2

function usage()
{
cat << EOF
Usage: `basename $0` <carrier> <version>
EOF
}

function fail()
{
cat << EOF



!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
   FAILED
!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
EOF
exit 1
}

if test "x$VERSION" == "x" ; then
   usage
   exit 1
fi

if test "x$CARRIER" == "x" ; then
   usage
   exit 1
fi 

DIR=dist/${CARRIER}-xstream-android-${VERSION}
SRC=${DIR}/xstream-android-unsigned.apk
ALIAS=xstream-${CARRIER}
#${CARRIER}-xstream-android
DEST=${DIR}/${ALIAS}-${VERSION}.apk
KEYSTORE=keystore/xstream-${CARRIER}-key.keystore
KEYS=keys/${CARRIER}-keys.txt

echo "
DIR=$DIR
SRC=$SRC
ALIAS=$ALIAS
DEST=$DEST
KEYSTORE=$KEYSTORE
KEYS=$KEYS
"

# sign
echo "

-------------------------------
Signing ..."
cat ${KEYS} | jarsigner -keystore ${KEYSTORE} ${SRC} ${ALIAS} || fail 
#1> /dev/null || fail $?

# verify 
echo "

-------------------------------
Verifying ..."
jarsigner -verbose -verify -certs ${SRC} | tail -n 10 || fail $?

if test -f "${DEST}" ; then
   rm ${DEST} || fail "Failed to delete ${DEST}"
fi

# align
echo "

-------------------------------
Alligning..."
zipalign -v 4 ${SRC} ${DEST} || fail $?

echo "SUCCESS!!!

Signed, Sealed and Delivered: ${DEST}
"

