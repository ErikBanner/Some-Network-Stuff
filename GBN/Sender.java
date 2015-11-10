import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.*;

public class Sender {
	private static InetAddress emulatorHostAddr;
	private static int emulatorPortForSenderData;
	private static int senderPortForEmulatorACKs;
	private static String transferredFileName;
	private static int base = 0;
	private static int mult = 0;
	private static int nextSeq = 0;
	private static final int windowSize = 10;
	private static long totalPackets;
	private static Logger logger = Logger.getLogger(Sender.class.getName());

	static class Send extends Thread {
		public void run() {
			try {
								int lost = 0;
								boolean delayed25 = false;
								boolean sent25 = false;

				FileOutputStream logSeq = new FileOutputStream("seqnum.log", false);
				RandomAccessFile raf = new RandomAccessFile(transferredFileName, "r");
				DatagramSocket senderSocket = new DatagramSocket();
				boolean eotSent = false;
				long remainder = raf.length() % 12;
				if (remainder == 0) {
					totalPackets = raf.length() / 12;
				}
				else {
					totalPackets = raf.length() / 12 + 1;
				}
				System.out.println("Total packets: " + totalPackets);
				while (true) {
					raf.seek(nextSeq()*12);
					System.out.println("seek " + nextSeq);
					if (nextSeq < base + mult*32 + windowSize) {
						//System.out.println("nextSeq < base + mult*32 + windowSize = " + nextSeq + " < " + base + " + " + mult + "*32 + " + windowSize);
						byte[] data = new byte[12];
						if (raf.read(data) != -1) {
							System.out.println("preparing packet" + nextSeq);
							if (nextSeq == 12 && lost < 1) {
								logSeq.write(String.valueOf(nextSeq % 32).getBytes());
						        logSeq.write('\n');
								lost++;
								nextSeq++;
							}
							else {
								if (nextSeq == 25 && !delayed25) {
									logSeq.write(String.valueOf(nextSeq % 32).getBytes());
						        	logSeq.write('\n');
									nextSeq++;
									System.out.println("packet 25 delayed");
									delayed25 = true;
									continue;
								}
								else if (nextSeq == 27 && !sent25) {
									packet pkt = packet.createPacket(25, new String("25xxxxxxxxxx"));
									byte[] sendData = pkt.getUDPdata();
									DatagramPacket hold = new DatagramPacket(sendData, sendData.length, emulatorHostAddr, emulatorPortForSenderData);
									senderSocket.send(hold);
									sent25 = true;
								}
								packet pkt = packet.createPacket(nextSeq, new String(data));
								byte[] sendData = pkt.getUDPdata();
								DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, emulatorHostAddr, emulatorPortForSenderData);
								senderSocket.send(sendPacket);
								System.out.println("packet " + nextSeq + " sent with content: " + new String(data));
						        logSeq.write(String.valueOf(nextSeq % 32).getBytes());
						        logSeq.write('\n');
								nextSeq++;
							}
						}
						else {
							//System.out.println("base: " + base + " mult*32: " + mult*32 + "eotSent: " + eotSent);
							if ((base + mult * 32) >= totalPackets && !eotSent) {
								packet eot = packet.createEOT(base);
								byte[] eotbytes = eot.getUDPdata();
								DatagramPacket sendEOT = new DatagramPacket(eotbytes, eotbytes.length, emulatorHostAddr, emulatorPortForSenderData);
								senderSocket.send(sendEOT);
								System.out.println("EOT sent. Received " + base + " many packets");
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
				FileOutputStream logAck = new FileOutputStream("ack.log", false);
				DatagramSocket receiveSocket = new DatagramSocket(senderPortForEmulatorACKs);
				receiveSocket.setSoTimeout(600);
				while (true) {
			    	byte[] receiveData = new byte[1024];
			    	DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
					try {
						receiveSocket.receive(receivePacket);
						packet ackPacket = packet.parseUDPdata(receivePacket.getData());
						if (ackPacket.getType() == 0) {
							System.out.println("get ACK for " + ackPacket.getSeqNum());
							logAck.write(String.valueOf(ackPacket.getSeqNum()).getBytes());
							logAck.write('\n');
							if (ackPacket.getSeqNum() >= base) {
								base = ackPacket.getSeqNum() + 1;
								if (base == 32) {
									mult++;
									base = 0;
								}
								if (base + mult*32 == nextSeq) {
									receiveSocket.setSoTimeout(0);
								}
								else {
									receiveSocket.setSoTimeout(600);
								}
							}
						}
						else if (ackPacket.getType() == 2) {
							System.out.println("transfer finished. Close now");
							System.exit(0);
						}
					}
					catch (SocketTimeoutException ste) {
						System.out.println("timeout");
						nextSeq = base;
						System.out.println("nextSeq is now " + nextSeq);
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
		System.out.println("emulatorHostAddr: " + emulatorHostAddr);
		System.out.println("emulatorPortForSenderData: " + emulatorPortForSenderData);
		System.out.println("senderPortForEmulatorACKs: " + senderPortForEmulatorACKs);
		System.out.println("transferredFileName: " + transferredFileName);

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


