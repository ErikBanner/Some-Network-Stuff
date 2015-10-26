import java.io.*;
import java.net.*;

class Server {

	private static int requestCode;
	private static int randomPort;
	private static int negotiationPort;

	public static void main(String[] args) throws Exception {
		try{
			if (args.length < 1) {
				throw new IllegalArgumentException("Missing argument");
			}
			else {
				requestCode = Integer.parseInt(args[0]);
			}
		}
		catch (NumberFormatException nfe) {
			System.out.println("Please enter an integer");
			System.exit(1);
		}
		catch (IllegalArgumentException iae) {
			System.out.println("Missing argument");
			System.out.println("Usage: int: <req_code>");
			System.exit(1);
		}
		finally {
			negotiate(); // Begin stage I
		}
	}

	private static void negotiate() throws Exception {
		System.out.println("running");
		ServerSocket serverSocket = new ServerSocket(0);
		negotiationPort = serverSocket.getLocalPort();
		System.out.println("SERVER_PORT=" + negotiationPort);

		while(true) {

			Socket clientSocket = serverSocket.accept();

			BufferedReader inFromClient = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			DataOutputStream outToClient = new DataOutputStream(clientSocket.getOutputStream());
            
			int clientRequestCode = Integer.parseInt(inFromClient.readLine()); // Receive client request code
			if (clientRequestCode != requestCode) { // Unknown request code
				outToClient.writeBytes("-1" + '\n');
				randomPort = -1;
				clientSocket.close(); // Server closes TCP connection
			}
			else { // Negotiation succeeded.
				randomPort = new ServerSocket(0).getLocalPort();
				outToClient.writeBytes(String.valueOf(randomPort) + '\n'); // Send random port to client
				
				transaction(randomPort); // Begin stage II
			}
		}

	}

	// The following method code is from lecture slides, with some modification
	private static void transaction(int randomPort) throws Exception {
		DatagramSocket serverSocket = new DatagramSocket(randomPort);

		byte[] receiveData = new byte[1024];
		byte[] sendData = new byte[1024];

		DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
		serverSocket.receive(receivePacket);
		
		String sentence = new String(receivePacket.getData());
		InetAddress IPAddress = receivePacket.getAddress();
		int port = receivePacket.getPort();
		
		String reversedSentence = new StringBuilder(sentence).reverse().toString();
		sendData = reversedSentence.getBytes();
		DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port);
		serverSocket.send(sendPacket);
	}

}