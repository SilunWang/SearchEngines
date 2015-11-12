#!/bin/bash

echo "Problem 1"
java -cp ".:lucene-4.3.0/*:./out/production/SearchEngine" -Xmx4096m QryEval q5 > /dev/null
perl fetchUrl.perl > tmpout
python HW4getresult.py

echo "Problem 2"
java -cp ".:lucene-4.3.0/*:./out/production/SearchEngine" -Xmx4096m QryEval q10 > /dev/null
perl fetchUrl.perl > tmpout
python HW4getresult.py

echo "Problem 3"
java -cp ".:lucene-4.3.0/*:./out/production/SearchEngine" -Xmx4096m QryEval q20 > /dev/null
perl fetchUrl.perl > tmpout
python HW4getresult.py

echo "Problem 4"
java -cp ".:lucene-4.3.0/*:./out/production/SearchEngine" -Xmx4096m QryEval q30 > /dev/null
perl fetchUrl.perl > tmpout
python HW4getresult.py

echo "Problem 5"
java -cp ".:lucene-4.3.0/*:./out/production/SearchEngine" -Xmx4096m QryEval q40 > /dev/null
perl fetchUrl.perl > tmpout
python HW4getresult.py

echo "Problem 6"
java -cp ".:lucene-4.3.0/*:./out/production/SearchEngine" -Xmx4096m QryEval q50 > /dev/null
perl fetchUrl.perl > tmpout
python HW4getresult.py