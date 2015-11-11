                                                    README
-------------------------------------------------------------------------------------------------------------
====================================== Computer Networks Assignment II ======================================
-------------------------------------------------------------------------------------------------------------

                            Need GNU make 3.81 and javac 1.8 to make and run.
                            Undergrad machine: ubuntu1204-006 for emulator
                                               ubuntu1204-004 for receiver
                                               ubuntu1204-002 for sender

*************************************************** Files ***************************************************

                                                Sender.java
                                                Receiver.java
                                                packet.java
                                                Makefile

************************************************ Installation ***********************************************

                                                1. $ make clean
                                                2. $ make

************************************************* How to Run ************************************************

Type in the following commands in three different machines to get meaningful results:

1. $ ./nEmulator-linux386   <emulator's receiving UDP port number in the forward(sender) direction>
                            <receiver’s network address>
                            <receiver’s receiving UDP port number>
                            <emulator's receiving UDP port number in the backward(receiver) direction>
                            <sender’s network address>
                            <sender’s receiving UDP port number>
                            <maximum delay of the link in units of millisecond>
                            <packet discard probability>
                            <verbose-mode>

2. $ java Receiver          <hostname for the network emulator>
                            <UDP port number used by the link emulator to receive ACKs from the receiver>
                            <UDP port number used by the receiver to receive data from the emulator>
                            <name of the file into which the received data is written>

3. $ java Sender            <host address of the network emulator>
                            <UDP port number used by the emulator to receive data from the sender>
                            <UDP port number used by the sender to receive ACKs from the emulator>
                            <name of the file to be transferred>

*************************************************************************************************************
