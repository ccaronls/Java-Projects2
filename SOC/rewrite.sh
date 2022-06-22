#! /bin/sh

STRINGS=../SOCAndroid/res/values/strings.xml

function fail() {
  echo $1
  exit 1
}

function findStr() {
  arg="$1"
  echo "arg=$arg"
  result="$(grep "$arg\"" $STRINGS | sed -e s/.*\"\>//g -e s/\<.*// -e s/'\\n'/\n/g)"
  #echo "$result"
  # | sed -e "s/.*\">//" -e "s/<\/ .*//"
}

test -f "$STRINGS" || fail "Cannot find $STRINGS"


while test -n "$1" ; do
  FILE=$1
  rm /tmp/x
  shift
  echo "Searching file $FILE"

  sed -e 's/,/\
  /g' $FILE | grep "R.string" | sed -e "s/.*R.string.//g" | sed -e "s/[^a-zA-Z0-9_].*//" | sort -r > /tmp/x

  for s in `cat /tmp/x` ; do
    findStr $s
    #echo "$result"

    sed -ie 's/sr.getString\(R\.string\."$s"\)/\"$result\"/g' $FILE
    sed -ie s/"R\.string\.$s"/\""$result"\"/1 $FILE
#    sed -ie 's/xxx/\\n/g' $FILE

    #cat $FILE | sed -e s/R\.string\."$s"/\""$result"\"/g

  done
done
