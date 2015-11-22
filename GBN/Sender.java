import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.*;

public class Sender {
	private static InetAddress emulatorHostAddr;
	private static int emulatorPortForSenderData;
	private static int senderPortForEmulatorACKs;
	private static String transferredFileName;
	private volatile static int base = 0;
	private volatile static int mult = 0;
	private volatile static int nextSeq = 0;
	private static long totalPackets;
	private static final int WINDOWSIZE = 10;
	private static final int DATALENGTH = 500; // This is the size of a packet data. Maximum size 500 enforce by class Packet.
	private static final int MOD = 32;
	private static final int BLOCKDURATION = 200;
	private static final String SEQNUMLOG = "seqnum.log";
	private static final String ACKLOG = "ack.log";
	private volatile static boolean EOT = false;

	// Thread for sending packet.
	static class Send extends Thread {
		public void run() {
			try {
				FileOutputStream logSeq = new FileOutputStream(SEQNUMLOG, false);
				RandomAccessFile raf = new RandomAccessFile(transferredFileName, "r");
				DatagramSocket senderSocket = new DatagramSocket();
				boolean eotSent = false;
				// Compute number of packets to be sent.
				long remainder = raf.length() % DATALENGTH;
				if (remainder == 0) {
					totalPackets = raf.length() / DATALENGTH;
				}
				else {
					totalPackets = raf.length() / DATALENGTH + 1;
				}

				while (true) {
					if (EOT == true) {
						logSeq.close();
						raf.close();
						return;
					}
					if (nextSeq < base + mult*MOD + WINDOWSIZE) { // Check if window is full.
						byte[] data = new byte[DATALENGTH];
						raf.seek(nextSeq*DATALENGTH); // Set the file pointer according to the window position.
						if (raf.read(data) != -1) { // Check if reaches EOF.
							// Send the packet.
							packet pkt = packet.createPacket(nextSeq, new String(data));
							byte[] sendData = pkt.getUDPdata();
							DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, emulatorHostAddr, emulatorPortForSenderData);
							senderSocket.send(sendPacket); 
							// Write to seqnum.log
							logSeq.write(String.valueOf(nextSeq % MOD).getBytes());
							logSeq.write('\n');
							// Increment nextSeq.
							nextSeq++;
						}
						else { // EOF
							if ((base + mult * MOD) >= totalPackets && !eotSent) { // All packets sent received ACKs.
								// Send EOT
								packet eot = packet.createEOT(999999); // sequence number for EOT doesn't matter.
								byte[] eotbytes = eot.getUDPdata();
								DatagramPacket sendEOT = new DatagramPacket(eotbytes, eotbytes.length, emulatorHostAddr, emulatorPortForSenderData);
								senderSocket.send(sendEOT);
								eotSent = true;
							}
						}
					}
				}
			}
			catch (Exception e) {
				System.err.println(e);
			}
		}
	}

	static class Receive extends Thread {
		public void run() {
			try {
				FileOutputStream logAck = new FileOutputStream(ACKLOG, false);
				DatagramSocket receiveSocket = new DatagramSocket(senderPortForEmulatorACKs);
				receiveSocket.setSoTimeout(BLOCKDURATION);
				while (true) {
			    	byte[] receiveData = new byte[1024];
			    	DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
					try {
						receiveSocket.receive(receivePacket);
						packet ackPacket = packet.parseUDPdata(receivePacket.getData());
						if (ackPacket.getType() == 0) { // The packet received is an ACK.
							// Write to ack.log
							logAck.write(String.valueOf(ackPacket.getSeqNum()).getBytes());
							logAck.write('\n');
							// Shift window.
							if (ackPacket.getSeqNum() >= base && ackPacket.getSeqNum() < base + WINDOWSIZE) {
							    base = ackPacket.getSeqNum() + 1;
							    if (base == MOD) {
									mult++;
									base = 0;
							    }
							}
							else if (ackPacket.getSeqNum() < base) {
							    if (base > MOD-WINDOWSIZE && base < MOD) {
									if (ackPacket.getSeqNum() < (base + WINDOWSIZE) % MOD) {
									    base = ackPacket.getSeqNum() + 1;
									    mult++;
									}
							    }
							}
						}
						else if (ackPacket.getType() == 2) { // The packet received is an EOT.
							System.out.println("Transfer finished. Close now");
							EOT = true;
							logAck.close();
							return;
						}
					}
					catch (SocketTimeoutException ste) {
						System.out.println("Timeout. Resending…");
						nextSeq = base + mult*MOD; // Resend all not acknowledged packet upon timeout.
					}
				}
			}
			catch (Exception e) {
				System.err.println(e);
			}
		}
	}

	public static void main(String[] args) throws Exception {
		checkInput(args);
		// Start two threads.
		Send sendThread = new Send();
		Receive receiveThread = new Receive();
		sendThread.start();
		receiveThread.start();
	}

	private static void checkInput(String[] args) {
		try {
			if (args.length < 4) {
				throw new IllegalArgumentException("Missing arguments");
			}
		}
		catch (IllegalArgumentException iae) {
			System.err.println("Missing arguments");
			System.err.println("Usage: <emulator host addr>, <emulator's port number for sender data>, <sender's port number for emulator ACKs>, <name of file to be transferred>");
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
	        emulatorPortForSenderData = Integer.parseInt(args[1]);
	        if (emulatorPortForSenderData < 0 || emulatorPortForSenderData > 65535) {
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
	        senderPortForEmulatorACKs = Integer.parseInt(args[2]);
	        if (senderPortForEmulatorACKs < 0 || senderPortForEmulatorACKs > 65535) {
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

	    try {
	    	File f = new File(args[3]);
	    	if (f.exists() && !f.isDirectory()) {
	    		transferredFileName = args[3];
	    	}
	    	else {
	    		throw new FileNotFoundException("File not found");
	    	}
	    } 
	    catch (FileNotFoundException fnfe) {
	        System.err.println("File not found");
	        System.exit(1);
	    }
	}
}


