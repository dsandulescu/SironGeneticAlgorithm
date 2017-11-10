# name=ReadSironFlow
# displayinmenu=true
# displaytouser=true
# displayinselector=false
#  Extract the outflow data from the Siron model, 
#print them on the console and save them in a file 

#import modules
from hec.script import *
from hec.heclib.dss import *
from hec.heclib.util import *
from java import *

try:  
#  Open the Siron.dss file with a string that represents the time window
  dssFile = HecDss.open("C:\Users\Dragos\Documents\Water models\Siron-new\Siron\RunNew.dss", "20APR1987 0600, 21APR1987 0500")
#  Get the particular dataset using a pathname string (can be copied and pasted from the HECDSSVue) 
# The dataset is in a TimeSeriesContainer object named outflowTS 
# //SUBBASIN-1/FLOW/01APR1987/30MIN/RUN:RUNNEW/


  outflowTS = dssFile.get("//SUBBASIN-1/FLOW-OBSERVED/01APR1987/30MIN/RUN:RUNNEW/")
# Make a hecTime object so that dates and time scan be formatted  
  hecTime=HecTime()
#Make a loop that goes through all the values of outflowTS and prints the date/times and the values  
  i=0
  for value in outflowTS.values :
#Set the date/time of the hecTime object to be the one from the outflowTS list member
    hecTime.set(outflowTS.times[i])
#Print the date/times using the hecTime object and the values directly from outflowTS
    print hecTime.toString() + '    ' + str(outflowTS.values[i]) + '\n'
    i+=1
#open a file where the same data will be written
  outfile=open('c:\HEC-HMS\Siron\myOutfile.txt','w')
#Write a title
  outfile. writelines('Siron model outflow\n')
#Make the similar loop as above to write the data in the file
  i=0
  for value in outflowTS.values :
    hecTime.set(outflowTS.times[i])
    outfile.writelines(hecTime.toString() + '    ' + str(outflowTS.values[i]) + '\n')
    outfile.flush()
    i+=1
#close the output file
  outfile.close()
#release the file 'dssFile'
  dssFile.done()
  
except java.lang.Exception, e :
  #  Take care of any missing data or errors
   MessageBox.showError(e.getMessage(), "Error reading data")
