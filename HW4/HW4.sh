#1
echo "problem1"
java -cp ".:lucene-4.3.0/*" QryEval p1.txt
perl fetchUrl1.perl.txt > tmpout1
python HW4getresult1.py
<<COMMENT
#2
echo "problem2"
java -cp ".:lucene-4.3.0/*" QryEval p2.txt
perl fetchUrl2.perl.txt > tmpout2
python HW4getresult2.py
#3
echo "problem3"
perl reference.perl.txt > tmpout
python HW4getresult.py
#4
echo "problem4"
java -cp ".:lucene-4.3.0/*" QryEval p4.txt
perl fetchUrl4.perl.txt > tmpout4
python HW4getresult4.py
#5
echo "problem5"
java -cp ".:lucene-4.3.0/*" QryEval p5.txt
perl fetchUrl5.perl.txt > tmpout5
python HW4getresult5.py
COMMENT