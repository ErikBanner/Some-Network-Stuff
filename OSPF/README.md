                                                    README
-------------------------------------------------------------------------------------------------------------
===================================== Computer Networks Assignment III ======================================
-------------------------------------------------------------------------------------------------------------

                            Need GNU make 3.81 and javac 1.8 to make and run.
                            Undergrad machine: ubuntu1204-006 for nse and routers

*************************************************** Files ***************************************************

                                                router.java
                                                nse.sh
                                                router.sh
                                                Makefile

************************************************ Installation ***********************************************

                                                1. $ make clean
                                                2. $ make

************************************************* How to Run ************************************************

Run the following commands in seperate terminal:

1. $ ./nse.sh               <routers_host>: The host where the routers are running.
                            <nse_port>: The port number of the Network State Emulator.

The following command should be run 5 times, in different terminals:

2. $ ./router               <router_id>: An integer that represents the router id. It should be unique for each router.
                            <nse_host>: The host where the Network State Emulator is running.
                            <nse_port>: The port number of the Network State Emulator.
                            <router_port>: The router port.

*************************************************************************************************************
