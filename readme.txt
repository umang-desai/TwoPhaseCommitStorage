Two Phase Commit Storage Protocol. Author:Umang Desai

Instructions of use:
1) Compile all files. 
Make sure to keep them all in the same folder. This syntax is for UNIX machines. Windows has it a little bit different with the semicolon instead of the colon.

javac -cp ".:sqlite-jdbc-3.21.0.jar" *.java      


2) Launch Master
The master class takes a lot of arguments. 
First argument is the port number on which the master will listen for RMI calls. 
Second argument, logfile name with EXTENSION (.txt)
Third argument, database name with EXTENSION (.txt)

java -cp ".:sqlite-jdbc-3.21.0.jar" Master <MasterPort> <log.txt> <dbname.db>

e.g :
java -cp ".:sqlite-jdbc-3.21.0.jar" Master 3000 log.txt test.db

P.S.: Master will broadcast its own IP address, if needed while launching client and replica.

3) Launch Client(s).
This is pretty straight forward. Two arguments, 
First is IP address of master.
Second is port of master.

java -cp ".:sqlite-jdbc-3.21.0.jar" Client <MasterIP> <MasterPort>


4) Launch Replica(s).
First argument, IP address of master.
Second argument, Port of master.
Third argument, Port for this replica.
Fourth argument, logfile name with EXTENSION (.txt)
Fifth argument, database name with EXTENSION (.txt)

java -cp ".:sqlite-jdbc-3.21.0.jar" Replica <Master IP> <MasterPort> <SelfPort> <log1.txt> <test1.db>

e.g.: 

java -cp ".:sqlite-jdbc-3.21.0.jar" Replica localhost 3000 3001 log1.txt test1.db


----------------------------------------------------------------------------------------------
