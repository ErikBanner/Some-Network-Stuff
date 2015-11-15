import java.io.*;
import java.net.*;

public class Receiver {
	private static InetAddress emulatorHostAddr;
	private static int emulatorPortForReceiverData;
	private static int receiverPortForEmulatorPackets;
	private static String savedFileName;
	private static int expectedSeq = 0;
	private static final String ARRIVALLOG = "arrival.log";
	private static final int MOD = 32;


	public static void main(String[] args) throws Exception {
		checkInput(args);

		DatagramSocket receiveSocket = new DatagramSocket(receiverPortForEmulatorPackets);
		FileOutputStream out = new FileOutputStream(savedFileName, false);
		FileOutputStream log = new FileOutputStream(ARRIVALLOG, false);

		while (true) {
			// Keep blocking waiting for incoming packets.
	    	byte[] receiveData = new byte[1024];
	    	DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
			receiveSocket.receive(receivePacket); 
			packet dataPacket = packet.parseUDPdata(receivePacket.getData());

			if (dataPacket.getType() == 1) { // Received a packet.
				// Write to arrival.log
				log.write(String.valueOf(dataPacket.getSeqNum()).getBytes());
				log.write('\n');

				if (dataPacket.getSeqNum() == expectedSeq % MOD) { // The packet is expected.
					// Create ACK and send.
					packet ack = packet.createACK(dataPacket.getSeqNum()); 
					byte[] sendData = ack.getUDPdata();
					DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, emulatorHostAddr, emulatorPortForReceiverData);
					receiveSocket.send(sendPacket);
					
					expectedSeq++; // Increment expected sequence number
					byte[] dataToWrite = dataPacket.getData(); 
					out.write(dataToWrite); // Write data to file
					continue;
				}
			}
			else if (dataPacket.getType() == 2) { // Received EOT
				byte[] eotbytes = dataPacket.getUDPdata();
				DatagramPacket sendBackEOT = new DatagramPacket(eotbytes, eotbytes.length, emulatorHostAddr, emulatorPortForReceiverData);
				receiveSocket.send(sendBackEOT);
				System.out.println("EOT sent back. Close now.");
				out.close();
				log.close();
				System.exit(0);
			}

			// Discard the unexpected packet and resend ACK for latest in-order packet.
			if (expectedSeq != 0) { // Do nothing is nothing has yet been received.
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