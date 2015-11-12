#!/bin/bash
<<COMMENT
echo "Problem 1"
java -cp ".:lucene-4.3.0/*:./out/production/SearchEngine" -Xmx4096m QryEval q5-2 > /dev/null
perl fetchUrl2.perl > tmpout2
python HW4getresult2.py

echo "Problem 2"
java -cp ".:lucene-4.3.0/*:./out/production/SearchEngine" -Xmx4096m QryEval q10-2 > /dev/null
perl fetchUrl2.perl > tmpout2
python HW4getresult2.py
COMMENT

echo "Problem 3"
java -cp ".:lucene-4.3.0/*:./out/production/SearchEngine" -Xmx4096m QryEval q20-2 > /dev/null
perl fetchUrl2.perl > tmpout2
python HW4getresult2.py

echo "Problem 4"
java -cp ".:lucene-4.3.0/*:./out/production/SearchEngine" -Xmx4096m QryEval q30-2 > /dev/null
perl fetchUrl2.perl > tmpout2
python HW4getresult2.py

echo "Problem 5"
java -cp ".:lucene-4.3.0/*:./out/production/SearchEngine" -Xmx4096m QryEval q40-2 > /dev/null
perl fetchUrl2.perl > tmpout2
python HW4getresult2.py

echo "Problem 6"
java -cp ".:lucene-4.3.0/*:./out/production/SearchEngine" -Xmx4096m QryEval q50-2 > /dev/null
perl fetchUrl2.perl > tmpout2
python HW4getresult2.py
