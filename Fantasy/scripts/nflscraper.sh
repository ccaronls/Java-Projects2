#! /bin/sh

function fail() # msg
{
echo "
!!!!!!!!!!!!!!!!!

FAILED: $@

!!!!!!!!!!!!!!!!!
"
exit 1
}

function usage()
{
echo "
USAGE: `basename $0` [OPTIONS]

DESCRIPTION
This script will scape the html from nfl.com for player statistics 
into a set of tab delimited spreadsheets viewable from Excel.
There is one spreadsheet for each position.  The set of positions
parsed is:
`echo $POSITIONS | sed -e 's/.\{45\} /&\n/g'`

OPTIONS
-h                this message
-s <season>       set the season to parse (default: $SEASON)
-p <position>     set the position to parse (default: all)
-d <directory>    where to write spreadsheets (default $DESTDIR)
"
}


function fixEscape() # text
{
   echo "$1" | sed -e "s/&amp;/\&/g" \
                   -e "s/&lt;/</g" \
                   -e "s/&gt;/>/g" \
                   -e "s/&quot;/\"/g" \
                   -e "s/&apos;/\'/g"
}


# test
#fixEscape "&quot;This&quot; &amp; &lt;is&gt; some &apos;text&apos;"
#exit

URL=http://www.nfl.com/stats/categorystats

# example
#http://www.nfl.com/stats/categorystats?archive=false&conference=null&statisticPositionCategory=WIDE_RECEIVER&season=2009&seasonType=REG&experience=null&tabSeq=1&qualified=false&Submit=Go

POSITIONS="
QUARTERBACK
RUNNING_BACK 
WIDE_RECEIVER 
TIGHT_END 
DEFENSIVE_LINEMAN 
LINEBACKER
DEFENSIVE_BACK
KICKOFF_KICKER
KICK_RETURNER
PUNTER
PUNT_RETURNER
FIELD_GOAL_KICKER"

SEASON=2009
SEASON_TYPE=REG
PAGE=nfl.html
DESTDIR=.

while test -n "$1" ; do
   case $1 in
   -h) usage ; exit ;;
   -s) SEASON=$2 ; shift ;;
   -p) POSITIONS=$2 ; shift ;;
   -d) DESTDIR=$2 ; shift ;;
   -*) echo "Unknown option $1" ; usage ; exit 1 ;;
   *) echo "Unknown parameter $1" ; usage ; exit 1 ;;
   esac
   shift
done

if ! test -d "$DESTDIR" ; then 
   mkdir -p ${DESTDIR} || fail "Cannot create directory $DESTDIR"
fi


for pos in $POSITIONS ; do

   REQ="${URL}?archive=false&conference=null&statisticPositionCategory=${pos}&season=${SEASON}&seasonType=${SEASON_TYPE}&experience=null&tabSeq=1&qualified=false&Submit=Go"

   #echo "Fetching $REQ"
   echo "Fetching ${pos}"
   wget -q -O ${PAGE} ${REQ} || fail "Failed to fetch: $REQ  $?"

   if test "${TERM} != cygwin" ; then
      sed -i "s/\r$//" nfl.html
   fi

   # scrape the headers from the page
   grep -E "thd|${pos}" nfl.html | sed -e "s/<[^>]*//g" | grep "^>" | sed -e "s/>//g" -e "s/.*&.*//" -e '/./!d' > headers.txt
   
   sed 's/^[ \t]*//;s/[ \t]*$//' nfl.html | sed -e '/./!d' | grep -A 5000 "<tbody>" | grep -B 5000 "</tbody>" | sed -e "s/<[^>]*>//g" -e "s/.*&.*//" -e '/./!d' > data.txt

   # the destination spreadsheet file.  Only gets overwriten when there are changes.
   DEST=${DESTDIR}/${pos}.xls
   OUT=tmp.xls
   test -f ${OUT} && rm ${OUT}

   echo "Writing to ${OUT}"

   cols=0
   exec < headers.txt || fail $?
   while read header ; do
      (( cols += 1 ))
      echo -n -e "${header}\t" >> ${OUT}
   done

   echo "Found ${cols} columns"
   #echo >> ${OUT}
   
   row=0
   col=${cols}

   echo -n "Parsing Row: "
   exec < data.txt || fail $?
   while read data ; do
      if [[ col -ge cols ]] ; then
         (( row += 1 )) 
         echo -n "${row} "
         col=0
         echo >> ${OUT}
      fi
      echo -n -e `fixEscape "${data}"`"\t" >> ${OUT}
      (( col += 1 ))
   done
   echo

   if [[ col -ne cols ]] ; then
      echo "
!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
problem scraping ${pos}
!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
"
   else
      if ! test -e "${DEST}" ; then
         echo "${DEST} not found, copying from ${OUT}"
         mv ${OUT} ${DEST}
      elif ( ! cmp "${OUT}" "${DEST}" ); then
         echo "${DEST} has been changed, updating from ${OUT}"
         mv ${OUT} ${DEST}
      else
         echo "No change detected, not updating ${DEST}"
      fi
   fi

done
