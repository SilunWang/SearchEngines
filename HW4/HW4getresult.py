input = open('tmpout', 'r')
qrynum = 0;
win = 0;
loss = 0;
basic = [0.0180,  0.0013,  0.0146,  0.0585,  0.2721,  0.1555,  0.0183,  0.0014,  0.0000,  0.0729,  0.1118,  0.0043,  0.0336,  0.0481,  0.0288,  0.0615,  0.0862,  0.0000,  0.0175,  0.0006]
p10=''
p20=''
p30=''
mymap=''
for lines in input.readlines():
	tmp = lines.split(' ')
	qryscore = tmp[-1].strip().split('\t')
	#print tmp
	#print qryscore
	if(tmp[0]=='map'):
		#print qrynum,
		if(qryscore[0]!='all'):
			#print float(qryscore[1]),
			#print float(basic[qrynum])
			if float(qryscore[1]) > float(basic[qrynum]):

				win = win + 1
			elif float(qryscore[1]) < basic[qrynum]:
				loss = loss + 1;
		qrynum = qrynum +1
	if(len(tmp) > 2 and qryscore[0]=='all'):
		if(tmp[0]=='P10'):
			p10 = qryscore[1]
		elif(tmp[0]=='P20'):
			p20 = qryscore[1]
		elif(tmp[0]=='P30'):
			p30 = qryscore[1]
		elif(tmp[0]=='map'):
			mymap = qryscore[1]
print p10
print p20
print p30
print mymap
print str(win) + '/' + str(loss)


