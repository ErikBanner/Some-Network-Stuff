import java.io.*;
import java.net.*;

public class Receiver {
	private static InetAddress emulatorHostAddr;
	private static int emulatorPortForReceiverData;
	private static int receiverPortForEmulatorPackets;
	private static String savedFileName;
	private static int expectedSeq = 0;


	public static void main(String[] args) throws Exception {
		checkInput(args);
		System.out.println("emulatorHostAddr: " + emulatorHostAddr);
		System.out.println("emulatorPortForReceiverData: " + emulatorPortForReceiverData);
		System.out.println("receiverPortForEmulatorPackets: " + receiverPortForEmulatorPackets);
		System.out.println("savedFileName: " + savedFileName);

		DatagramSocket receiveSocket = new DatagramSocket(receiverPortForEmulatorPackets);
		FileOutputStream out = new FileOutputStream(savedFileName, false);
		FileOutputStream log = new FileOutputStream("arrival.log", false);

				boolean lost5 = false;
				boolean delayed27 = false;
				boolean sent27 = false;
		
		while (true) {
	    	byte[] receiveData = new byte[1024];
	    	DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
			receiveSocket.receive(receivePacket); 
			packet dataPacket = packet.parseUDPdata(receivePacket.getData());
			System.out.println("Receiver received packet with seqNum " + dataPacket.getSeqNum());

			if (dataPacket.getType() == 1) {
				log.write(String.valueOf(dataPacket.getSeqNum()).getBytes());
				log.write('\n');
				if (dataPacket.getSeqNum() == expectedSeq % 32) {
					// if (dataPacket.getSeqNum() == 5 && !lost) {
					// 	lost = true;
					// }
					// else {
					// 	try {
					// 	    Thread.sleep(200);                 //1000 milliseconds is one second.
					// 	} catch(InterruptedException ex) {
					// 	    Thread.currentThread().interrupt();
					// 	}
					// }
					if (expectedSeq == 5 && !lost5) {
						expectedSeq++;
						lost5 = true;
						continue;
					}
					else if (expectedSeq == 27 && !delayed27) {
						expectedSeq++;
						continue;
					}
					else if (expectedSeq == 29 && !sent27) {
						packet ack = packet.createACK(27);
						byte[] sendData = ack.getUDPdata();
						DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, emulatorHostAddr, emulatorPortForReceiverData);
						receiveSocket.send(sendPacket);
					}
					System.out.println("creating ACK with seqNum: " + dataPacket.getSeqNum());
					System.out.println("this is " + expectedSeq + " packet received");
					packet ack = packet.createACK(dataPacket.getSeqNum());
					byte[] sendData = ack.getUDPdata();
					DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, emulatorHostAddr, emulatorPortForReceiverData);
					receiveSocket.send(sendPacket);
					expectedSeq++;
					byte[] dataToWrite = dataPacket.getData();
					out.write(dataToWrite);
					continue;
					// }
				}
			}
			else if (dataPacket.getType() == 2) {
				byte[] eotbytes = dataPacket.getUDPdata();
				DatagramPacket sendBackEOT = new DatagramPacket(eotbytes, eotbytes.length, emulatorHostAddr, emulatorPortForReceiverData);
				receiveSocket.send(sendBackEOT);
				System.out.println("EOT sent back. Close now.");
				System.exit(0);
			}
			if (expectedSeq != 0) {
				packet ack = packet.createACK((expectedSeq-1));
				byte[] sendData = ack.getUDPdata();
				DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, emulatorHostAddr, emulatorPortForReceiverData);
				receiveSocket.send(sendPacket);
			}
		}
	}

	private static void checkInput(String[] args) {
		try {
			if (args.length < 4) {
				throw new IllegalArgumentException("Missing arguments");
			}
		}
		catch (IllegalArgumentException iae) {
			System.err.println("Missing arguments");
			System.err.println("Usage: <emulator host addr>, <emulator's port number for receiver data>, <receiver's port number for emulator packets>, <name of file to be written to>");
			System.exit(1);
		}

		try {
			emulatorHostAddr = InetAddress.getByName(args[0]);
		}
		catch (UnknownHostException uhe) {
	        System.err.println("Host for IP not found");
	        System.exit(1);
	    }  
	    catch (SecurityException se) {
	        System.err.println("Blocked by firewall. Please change firewall settings");
	        System.exit(1);
	    }

	    try {
	        emulatorPortForReceiverData = Integer.parseInt(args[1]);
	        if (emulatorPortForReceiverData < 0 || emulatorPortForReceiverData > 65535) {
	            throw new IllegalArgumentException("Invalid emulator port number");
	        }
	    }
	    catch (NumberFormatException nfe) {
	        System.err.println("Plaese enter an integer for <emulator's port number for sender data>");
	        System.exit(1);
	    }
	    catch (IllegalArgumentException iae) {
	        System.err.println("<emulator's port number for sender data> range: 0 - 65535");
	        System.exit(1);
	    }

	    try {
	        receiverPortForEmulatorPackets = Integer.parseInt(args[2]);
	        if (receiverPortForEmulatorPackets < 0 || receiverPortForEmulatorPackets > 65535) {
	            throw new IllegalArgumentException("Invalid sender port number");
	        }
	    }
	    catch (NumberFormatException nfe) {
	        System.err.println("Plaese enter an integer for <sender's port number for emulator ACKs>");
	        System.exit(1);
	    }
	    catch (IllegalArgumentException iae) {
	        System.err.println("<sender's port number for emulator ACKs> range: 0 - 65535");
	        System.exit(1);
	    }  

	    savedFileName = args[3];
	}
}