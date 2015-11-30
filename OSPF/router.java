import java.net.*;
import java.io.*;
import java.nio.*;
import java.util.*;

class router {
	private static final int NUMBER_ROUTER = 5;
	private static int RouterID;
	private static InetAddress nseAddress;
	private static int nsePort;
	private static int routerPort;
	private static CircuitDB[] linkStateDatabase = new CircuitDB[NUMBER_ROUTER];
	private static RIBEntry[] RIB = new RIBEntry[NUMBER_ROUTER];
	private static FileOutputStream log;
	private static String logStr;
	private static LinkCost[][] neighbours = new LinkCost[NUMBER_ROUTER][NUMBER_ROUTER]; // This is used to store neighbours of this router

	private static class RIBEntry {
		int destRouter;
		int thruRouter;
		int cost;
		RIBEntry(int destRouter) {
			this.destRouter = destRouter;
			this.thruRouter = Integer.MAX_VALUE;
			this.cost = Integer.MAX_VALUE;
		}

		RIBEntry(int destRouter, int thruRouter, int cost) {
			this.destRouter = destRouter;
			this.thruRouter = thruRouter;
			this.cost = cost;
		}

		public String stringRepr() {
			String ret = "R" + destRouter + " -> ";
			if (thruRouter == RouterID) {
				ret += "Local, 0";
			}
			else if (thruRouter == Integer.MAX_VALUE) {
				ret += "Inf, ";
				if (cost == Integer.MAX_VALUE) {
					ret += "Inf";
				}
				else {
					ret += cost;
				}
			}
			else{
				ret += "R" + thruRouter + ", ";
				if (cost == Integer.MAX_VALUE) {
					ret += "Inf";
				}
				else {
					ret += cost;
				}
			}
			return ret;
		}
	}

	private static class PacketHELLO {
		int routerID;							// id of the router who sends the HELLO PDU
		int linkID;								// id of the link through which the HELLO PDU is sent
		PacketHELLO(int routerID, int linkID) {
			this.routerID = routerID;
			this.linkID = linkID;
		}

		public String stringRepr() {
			return "HELLO: routerID " + routerID + " linkID " + linkID;
		}

		public byte[] getUDPdata() {
			ByteBuffer buffer = ByteBuffer.allocate(8);
			buffer.order(ByteOrder.LITTLE_ENDIAN);
			buffer.putInt(routerID);
			buffer.putInt(linkID);
			return buffer.array();
		}

		public static PacketHELLO parseUDPdata(byte[] data) {
			ByteBuffer buffer = ByteBuffer.wrap(data);
			buffer.order(ByteOrder.LITTLE_ENDIAN);
			int routerID = buffer.getInt();
			int linkID = buffer.getInt();
			return new PacketHELLO(routerID, linkID);
		}
	}

	private static class PacketLSPDU {
		int sender;								// sender of the LS PDU
		int routerID;							// router id
		int linkID;								// link id
		int cost;								// cost of the link
		int via;								// id of the link through which the LS PDU is sent
		PacketLSPDU(int sender, int routerID, int linkID, int cost, int via) {
			this.sender = sender;
			this.routerID = routerID;
			this.linkID = linkID;
			this.cost = cost;
			this.via = via;
		}

		public String stringRepr() {
			return "LS PDU: sender " + sender + " routerID " + routerID + " linkID " + linkID + " cost " + cost + " via " + via;
		}

		public byte[] getUDPdata() {
			ByteBuffer buffer = ByteBuffer.allocate(20);
			buffer.order(ByteOrder.LITTLE_ENDIAN);
			buffer.putInt(sender);
			buffer.putInt(routerID);
			buffer.putInt(linkID);
			buffer.putInt(cost);
			buffer.putInt(via);
			return buffer.array();
		}

		public static PacketLSPDU parseUDPdata(byte[] data) {
			ByteBuffer buffer = ByteBuffer.wrap(data);
			buffer.order(ByteOrder.LITTLE_ENDIAN);
			int sender = buffer.getInt();
			int routerID = buffer.getInt();
			int linkID = buffer.getInt();
			int cost = buffer.getInt();
			int via = buffer.getInt();
			return new PacketLSPDU(sender, routerID, linkID, cost, via);
		}
	}

	private static class PacketINIT {
		int routerID;							// id of the router that send the INIT PDU
		PacketINIT(int routerID) {
			this.routerID = routerID;
		}

		public byte[] getUDPdata() {
			ByteBuffer buffer = ByteBuffer.allocate(4);
			buffer.order(ByteOrder.LITTLE_ENDIAN);
			buffer.putInt(this.routerID);
			return buffer.array();
		}

		public String stringRepr() {
			return "INIT: routerID " + routerID;
		}
	}

	private static class LinkCost {
		int link;								// link id
		int cost;								// associated cost
		LinkCost(int link, int cost) {
			this.link = link;
			this.cost = cost;
		}
	}

	private static class CircuitDB {
		int numLink;							// number of links attached to a router
		LinkCost[] linkcost;					// assume at most NUMBER_ROUTER links are attached to each router

		CircuitDB() {
			this.numLink = 0;
			this.linkcost = new LinkCost[NUMBER_ROUTER];
		}

		private CircuitDB(int numLink, LinkCost[] linkcost) {
			this.numLink = numLink;
			this.linkcost = linkcost;
		}

		public static CircuitDB parseUDPdata(byte[] data) {
			ByteBuffer buffer = ByteBuffer.wrap(data);
			buffer.order(ByteOrder.LITTLE_ENDIAN);
			int numLink = buffer.getInt();
			LinkCost[] linkcost = new LinkCost[NUMBER_ROUTER];
			for (int i=0; i<numLink; i++){
				int link = buffer.getInt();
				int cost = buffer.getInt();
				linkcost[i] = new LinkCost(link, cost);
			}
			return new CircuitDB(numLink, linkcost);
		}

		public String stringRepr() {
			return "CircuitDB: numLink " + numLink;
		}

		public void addLinkCost(LinkCost lc) {
			linkcost[numLink++] = lc;
		}
	}

	public static void main(String[] args) throws Exception {
		try{
			RouterID = Integer.parseInt(args[0]);
			nseAddress = InetAddress.getByName(args[1]);
			nsePort = Integer.parseInt(args[2]);
			routerPort = Integer.parseInt(args[3]);
			log = new FileOutputStream("router" + RouterID + ".log", false);

			DatagramSocket socket = new DatagramSocket(routerPort);
			initPhase(socket); // Initialization phase
			// Main loop: keep waiting until new pkt_HELLO or pkt_LSPDU arrives
			while (true) {
				byte[] recv = new byte[1024];
				DatagramPacket receivedPacket = new DatagramPacket(recv, recv.length);
				socket.receive(receivedPacket);
				if (receivedPacket.getLength() == 8) { // The received packet is a pkt_HELLO
					PacketHELLO pHelloIn = PacketHELLO.parseUDPdata(recv);
					logStr = "R" + RouterID + " receives a " + pHelloIn.stringRepr();
					log.write(logStr.getBytes());
					log.write('\n');
					// Update the cost to neighbours
					RIB[pHelloIn.routerID-1].thruRouter = pHelloIn.routerID;
					CircuitDB cdb = linkStateDatabase[RouterID-1];
					for (int i=0; i<cdb.numLink; i++) {
						LinkCost lc = cdb.linkcost[i];
						if (lc.link == pHelloIn.linkID) {
							RIB[pHelloIn.routerID-1].cost = lc.cost;
						}
					}
					// Set neighbours since only neighbours can send pkt_HELLO to each other
					neighbours[RouterID-1][pHelloIn.routerID-1] = new LinkCost(pHelloIn.linkID, RIB[pHelloIn.routerID-1].cost);
					neighbours[pHelloIn.routerID-1][RouterID-1] = new LinkCost(pHelloIn.linkID, RIB[pHelloIn.routerID-1].cost);
					// Log RIB after updating
					logRIB();
					// Send LS PDU to those who have sent pkt_HELLO to this router
					sendLSPDU(socket, pHelloIn.linkID);
				}
				else { // The received packet is a pkt_LSPDU
					PacketLSPDU pLspdu = PacketLSPDU.parseUDPdata(recv);
					logStr = "R" + RouterID + " receives a " + pLspdu.stringRepr();
					log.write(logStr.getBytes());
					log.write('\n');
					boolean updateResult = updateLSDB(pLspdu);
					if (updateResult == true) { // The newly arrived LS PDU is unique in the LSDB and has been saved
						// Update neighbours
						for (int i=0; i<NUMBER_ROUTER; i++) {
							if (linkStateDatabase[i] != null) {
								CircuitDB cdb = linkStateDatabase[i];
								for (int j=0; j<cdb.numLink; j++) {
									if (cdb.linkcost[j].link == pLspdu.linkID) {
										neighbours[i][pLspdu.routerID-1] = new LinkCost(pLspdu.linkID, cdb.linkcost[j].cost);
										neighbours[pLspdu.routerID-1][i] = new LinkCost(pLspdu.linkID, cdb.linkcost[j].cost);
									}
								}
							}
						}
						// Then the LS PDU get forwarded to all the neighbours
						forwardLSPDU(socket, pLspdu);
						// Run the shortest path first algorithm (Dijkstra's Algorithm) to update the RIB if applicable
						spf();
						// Log the updated LSDB
						logLSDB();
					}
				}
			}
		}
		finally {
			log.close();
		}
	}

	private static void initPhase(DatagramSocket socket) throws Exception {
		// Initialize RIB
		RIBInit();
		// Log RIB
		logRIB();
		// Send pkt_INIT to nse
		PacketINIT initPacket = new PacketINIT(RouterID);
		byte[] dataInit = initPacket.getUDPdata();
		DatagramPacket packetInit = new DatagramPacket(dataInit, dataInit.length, nseAddress, nsePort);
		socket.send(packetInit);
		logStr = "R" + RouterID + " sends an " + initPacket.stringRepr();
		log.write(logStr.getBytes());
		log.write('\n');
		// Receive the initial circuit_DB from nse
		byte[] recv = new byte[1024];
		DatagramPacket receivedPacket = new DatagramPacket(recv, recv.length);
		socket.receive(receivedPacket);
		CircuitDB circuitDB = CircuitDB.parseUDPdata(recv);
		logStr = "R" + RouterID + " receives a " + circuitDB.stringRepr();
		log.write(logStr.getBytes());
		log.write('\n');
		// Put the circuit_DB into the link state database
		linkStateDatabase[RouterID-1] = circuitDB;
		// Run the shortest path first algorithm
		spf();
		// Log the first-time updated LSDB
		logLSDB();
		// Send pkt_HELLO to each neighbour
		for (int i=0; i<circuitDB.numLink; i++) {
			PacketHELLO pHello = new PacketHELLO(RouterID, circuitDB.linkcost[i].link);
			byte[] helloData = pHello.getUDPdata();
			DatagramPacket packetHello = new DatagramPacket(helloData, helloData.length, nseAddress, nsePort);
			socket.send(packetHello);
			logStr = "R" + RouterID + " sends a " + pHello.stringRepr();
			log.write(logStr.getBytes());
			log.write('\n');
		}
	}

	// Send the initial LS PDU to all neighbours
	private static void sendLSPDU(DatagramSocket socket, int viaLinkID) throws Exception {
		for (int i=0; i<linkStateDatabase.length; i++) {
			if (linkStateDatabase[i] != null) {
				CircuitDB circuitDB = linkStateDatabase[i];
				for (int j=0; j<circuitDB.numLink; j++) {
					PacketLSPDU pLspdu = new PacketLSPDU(RouterID, RouterID, circuitDB.linkcost[j].link, circuitDB.linkcost[j].cost, viaLinkID);
					byte[] data = pLspdu.getUDPdata();
					DatagramPacket packet = new DatagramPacket(data, data.length, nseAddress, nsePort);
					socket.send(packet);
					logStr = "R" + RouterID + " sends a " + pLspdu.stringRepr();
					log.write(logStr.getBytes());
					log.write('\n');
				}
			}
		}
	}

	private static void forwardLSPDU(DatagramSocket socket, PacketLSPDU pLspdu) throws Exception {
		// Modify the 'sender' and 'via' field of the LS PDU and forward it to neighbours
		pLspdu.sender = RouterID;
		CircuitDB routerNeighboursInfo = linkStateDatabase[RouterID-1]; // Get router's CircuitDB
		for (int i=0; i<routerNeighboursInfo.numLink; i++) { // forward to neighbours
			pLspdu.via = routerNeighboursInfo.linkcost[i].link;
			byte[] data = pLspdu.getUDPdata();
			DatagramPacket packet = new DatagramPacket(data, data.length, nseAddress, nsePort);
			socket.send(packet);
			logStr = "R" + RouterID + " sends a " + pLspdu.stringRepr();
			log.write(logStr.getBytes());
			log.write('\n');
		}
	}

	// Update LSDB when receive a LS PDU
	private static boolean updateLSDB(PacketLSPDU packet) {
		if (linkStateDatabase[packet.routerID-1] == null) {
			linkStateDatabase[packet.routerID-1] = new CircuitDB();
		}
		CircuitDB targetRouterCircuitDB = linkStateDatabase[packet.routerID-1];
		for (int i=0; i<targetRouterCircuitDB.numLink; i++) {
			// The LS PDU is new
			if (targetRouterCircuitDB.linkcost[i].link == packet.linkID && targetRouterCircuitDB.linkcost[i].cost == packet.cost) return false;
		}
		LinkCost newLinkCost = new LinkCost(packet.linkID, packet.cost);
		linkStateDatabase[packet.routerID-1].addLinkCost(newLinkCost);
		return true;
	}

	private static void logLSDB() throws Exception {
		logStr = "# Topology database";
		log.write(logStr.getBytes());
		log.write('\n');
		for (int i=0; i<NUMBER_ROUTER; i++) {
			if (linkStateDatabase[i] != null) {
				logStr = "R" + RouterID + " -> R" + (i+1) + " numLink " + linkStateDatabase[i].numLink;
				log.write(logStr.getBytes());
				log.write('\n');
				LinkCost[] lc = linkStateDatabase[i].linkcost;
				for (int j=0; j<linkStateDatabase[i].numLink; j++) {
					logStr = "R" + RouterID + " -> R" + (i+1) + " link " + lc[j].link + " cost " + lc[j].cost;
					log.write(logStr.getBytes());
					log.write('\n');
				}
			}
		}
	}

	private static void logRIB() throws Exception {
		logStr = "# RIB";
		log.write(logStr.getBytes());
		log.write('\n');
		for (int i=0; i<NUMBER_ROUTER; i++) {
			logStr = "R" + RouterID + " -> " + RIB[i].stringRepr();
			log.write(logStr.getBytes());
			log.write('\n');
		}
	}

	// Initialize RIB
	private static void RIBInit() {
		for (int i=0; i<NUMBER_ROUTER; i++) {
			RIBEntry entry;
			if (i+1 == RouterID) {
				entry = new RIBEntry(RouterID, RouterID, 0); // Local with cost 0
			}
			else {
				entry = new RIBEntry(i+1);
			}
			RIB[i] = entry;
		}
	}

	// The shortest path first algorithm
	private static void spf() throws Exception {
		List<Integer> nodes = new ArrayList<>();
		nodes.add(RouterID);
		int numNodes = 1;
		boolean changed = false;
		while (numNodes < 5) {
			int minCost = Integer.MAX_VALUE;
			int minCostRouterID = 0;
			// Find the minimum cost and the corresponding destination router that is not in 'nodes'
			for (int i=0; i<NUMBER_ROUTER; i++) {
				if (nodes.contains(i+1) == false) {
					int cmpCost = RIB[i].cost;
					if (cmpCost <= minCost) {
						minCost = cmpCost;
						minCostRouterID = i+1;
					}
				}
			}
			// Add the node to 'nodes'
			nodes.add(minCostRouterID);
			numNodes++;
			if (minCost != Integer.MAX_VALUE) {
				// update neighbours costs
				for (int i=0; i<NUMBER_ROUTER; i++) {
					if (neighbours[minCostRouterID-1][i] != null) { // All nodes adjacent to the node with minimum cost
						if (nodes.contains(i+1) == false) { // And not in the set 'nodes'
							int previousCost = RIB[i].cost;
							int newCost = minCost + neighbours[minCostRouterID-1][i].cost;
							if (newCost < previousCost) { // Found a better path and need to update
								RIB[i].cost = newCost;
								RIB[i].thruRouter = RIB[minCostRouterID-1].thruRouter;
								changed = true;
							}
						}
					}
				}
			}		
		}
		if (changed) {
			logRIB();
		}
	}
}