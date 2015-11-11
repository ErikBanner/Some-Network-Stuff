			    		   README
-----------------------------------------------------------
============== Computer Networks Assignment I =============
-----------------------------------------------------------

Need GNU make 3.81 and javac 1.8 to make and run.
Undergrad machine: ubuntu1204-004

************************** Files **************************
						Client.java
						Server.java
						client.sh
						server.sh
						Makefile

*********************** Installation **********************
1. $ make clean
2. $ make

************************ Arguments ************************
String <server_address>:
The IP address of the host server.

int <n_port>:
An integer between 0 to 65535, used as negotiation port.

int <req_code>:
An integer used specifying the request for random port.

String <msg>:
The message to be reversed.

************************ How to Run ***********************
3. $ ./server.sh <req_code>
4. $ ./client.sh <server_address> <n_port> <req_code> <msg>