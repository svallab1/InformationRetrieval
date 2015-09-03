#!/bin/bash


sed -i “” /^$/d $1/*.txt
cat $1/*.txt > $1/mergedFile.txt
sort $1/mergedFile.txt|uniq -c |sort -nr > $1/sortedByFreq.txt
sort -f $1/mergedFile.txt|uniq -c > $1/sortedAlphabetic.txt
rm $1/mergedFile.txt

echo "script completed"