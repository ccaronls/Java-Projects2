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
This script will scape the html from nhl.com for player statistics 
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

SEASON=2010
SEASON_TYPE=REG
PAGE=nfl.html
POSITIONS="LWING RWING CENTER DEFENDER GOALIE"
DESTDIR=.

while test -n "$1" ; do
   case $1 in
   -h) usage ; exit ;;
   -s) SEASON=$2 ; shift ;;
   -p) POSITION=$2 ; shift ;;
   -d) DESTDIR=$2 ; shift ;;
   -*) echo "Unknown option $1" ; usage ; exit 1 ;;
   *) echo "Unknown parameter $1" ; usage ; exit 1 ;;
   esac
   shift
done

if ! test -d "$DESTDIR" ; then 
   mkdir -p ${DESTDIR} || fail "Cannot create directory $DESTDIR"
fi

function getPositionIndex() # pos
{
   index=0
   for p in $POSITIONS ; do
      if test $p == $1 ; then
         break
      fi
      (( index += 1 ))
   done
   echo ${index}
}


fetchKey[0]="${SEASON}2ALLLALAll&viewName=summary&sort=points"
fetchKey[1]="${SEASON}2ALLRARAll&viewName=summary&sort=points"
fetchKey[2]="${SEASON}2ALLCACAll&viewName=summary&sort=points"
fetchKey[3]="${SEASON}2ALLDADAll&viewName=summary&sort=points"
fetchKey[4]="${SEASON}2ALLGAGAll&viewName=wlt&sort=wins"

#for item in ${POSITIONS} ; do
#	index=`getPositionIndex $item`
#	echo -n "Array[$index] = "
#	echo "${fetchKey[$index]}"
#done

#exit



function getURL() # page, position
{
   index=`getPositionIndex $2`
   echo "http://www.nhl.com/ice/app?service=page&page=playerstats&fetchKey=${fetchKey[$index]}&pg=$1"
}

for pos in ${POSITIONS} ; do

	if test -n "${POSITION}" ; then
		if test "${pos}" != "${POSITION}" ; then
			continue
		fi
	fi

	echo "Fetching position ${pos} ..."
		
	URL=`getURL 1 ${pos}`
	wget -O nhl.html "$URL"
	
	# parse the number of rows
	results=`grep -m 1 results nhl.html | sed -e "s/\s*//" -e "s/\s.*//"`   
	
	entries=0
	
	echo "Results: $results"
	
	# parse the headers
	echo "Rank" > headers.txt
	grep -A 10000 statsTableGoop nhl.html | grep -m 1 -B 10000 "</table" | sed -e "s/^\s*//" -e '/./!d' | grep -m 1 -A 1000 "<tr>" | sed -e "s/<[^>]*>//g" -e '/./!d' >> headers.txt
	
	DEST=${DESTDIR}/${pos}.xls
	OUT="tmp.xls"
	test -e "$OUT" && rm "$OUT"
	
	cols=0
	exec < headers.txt || fail $?
	while read header ; do
	   if test -z "${header}" ; then
	      continue
	   fi
	   (( cols += 1 ))
	   echo -n -e "${header}\t" >> ${OUT}
	done
	
	echo "Found ${cols} columns"
	
	page=1
	
	while true ; do
	
	   # now parse the data, need to do it for all pages
	   grep -m 1 -A 10000 "stats rows" nhl.html | grep -B 10000 -m 1 "</\table" | sed -e "s/^\s*//" -e '/./!d' -e "s/<[^>]*>//g" -e '/./!d' > data.txt
	
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
	      echo -n -e "${data}\t" >> ${OUT}
	      (( col += 1 ))
	   done
	   echo
	
	   if [[ col -ne cols ]] ; then
	      echo "
	!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
	problem scraping ${pos}
	!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
	"
	   fi
	
	   (( entries += row ))
	
	   echo "Entries so far : $entries"
	   
	   if [[ entries -ge results ]] ; then
	      break
	   fi
	   
	   (( page += 1 ))
	   URL=`getURL $page ${pos}`
	   wget -q -O nhl.html "$URL"
	
	done

	if ! test -e "${DEST}" ; then
		echo "${DEST} not found, copying from ${OUT}"
		mv ${OUT} ${DEST}
	elif ( ! cmp "${OUT}" "${DEST}" ); then
		echo "${DEST} has been changed, updating from ${OUT}"
		mv ${OUT} ${DEST}
	else
		echo "No change detected, not updating ${DEST}"
	fi

done

echo "SUCCESS!"


