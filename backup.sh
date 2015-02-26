#! /bin/sh

for ext in `cat extensions` ; do
   #SIZE=`find . -type f -name *.$ext -exec du -k "{}" | awk '{SUM+=$1}END{print SUM}'`
   
SIZE=`find . -name *.$ext -exec du -k "{}" \; | awk '{SUM += $1} END {print SUM}'`

echo "$ext=$SIZE" 
done

