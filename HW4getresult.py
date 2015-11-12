input = open('tmpout', 'r')
qrynum = 0;
win = 0;
loss = 0;
basic = [0.4923,  0.0157,  0.0170,  0.3535, 0.2721, 0.3866, 0.1002, 0.0340, 0.0165, 0.2887, 0.3836, 0.0669, 0.2897, 0.6498, 0.1391, 0.0615, 0.0344, 0.0007, 0.1168, 0.0432]
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
			else:
				print "tie"
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


