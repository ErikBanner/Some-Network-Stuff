import java.io.*;
import java.net.*;

class Client {
	
	private static int randomPort;
    private static InetAddress serverAddress;
    private static int negotiationPort;
    private static int requestCode;
    private static String message;

    public static void main(String[] args) throws Exception {
        try {
            if (args.length < 4) {
                throw new IllegalArgumentException("Missing argument");
            }
        }
        catch (IllegalArgumentException e) {
            System.out.println("Missing argument");
            System.out.println("Usage: <server_address> <n_port> <req_code> <msg>");
            System.exit(1);
        } 
        
        try {
            serverAddress = InetAddress.getByName(args[0]);
        }
        catch (UnknownHostException uhe) {
            System.out.println("Host for IP not found");
            System.exit(1);
        }  
        catch (SecurityException se) {
            System.out.println("Blocked by firewall. Please change firewall settings");
            System.exit(1);
        }
        try {
            negotiationPort = Integer.parseInt(args[1]);
            if (negotiationPort < 0 || negotiationPort > 65535) {
                throw new IllegalArgumentException("Invalid negotiation port");
            }
        }
        catch (NumberFormatException nfe) {
            System.out.println("Plaese enter an integer for <n_port>");
            System.exit(1);
        }
        catch (IllegalArgumentException iae) {
            System.out.println("<n_port> range: 0 - 65535");
            System.exit(1);
        }
        try {
            requestCode = Integer.parseInt(args[2]);
        }
        catch (NumberFormatException nfe) {
            System.out.println("Plaese enter an integer for <req_code>");
            System.exit(1);
        }

        String message = args[3];

        negotiate(); // Begin stage I
        
        if (randomPort != -1){
            transaction(serverAddress, message); // Begin stage II
        }
    }

    private static void negotiate() throws Exception {
		Socket clientSocket = new Socket(serverAddress, negotiationPort);

    	BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
    	DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
        
    	outToServer.writeBytes(String.valueOf(requestCode) + '\n'); // Send request code to server

        randomPort = Integer.parseInt(inFromServer.readLine()); // Receive random port from server

    	clientSocket.close();

    	return;
    }

    // The following method code is from lecture slides, with some modification
    private static void transaction(InetAddress serverAddress, String message) throws Exception {
    	DatagramSocket clientSocket = new DatagramSocket();
    	
    	byte[] sendData = new byte[1024];
    	byte[] receiveData = new byte[1024];

    	sendData = message.getBytes();
    	
    	DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, randomPort);
    	clientSocket.send(sendPacket); // Send message to reverse to server
    	
    	DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
    	clientSocket.receive(receivePacket); // Receive reversed message from sever
    	String reversedMessage = new String(receivePacket.getData());
    	System.out.println("Reversed Message: " + reversedMessage);
    	clientSocket.close();
    }
}