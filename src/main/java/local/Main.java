package local;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

/**
 * Hello world!
 *
 */
public class Main {
    public static void main( String[] args ) {
        int joinDelay = 0;
        int crashDelay = -1;
        String hostsfile = "";

        for (int i = 0; i < args.length; i++) {
            if ("-h".equals(args[i]) && i + 1 < args.length) {
                hostsfile = args[i + 1];
            } else if ("-d".equals(args[i]) && i + 1 < args.length) {
                try {
                    joinDelay = Integer.parseInt(args[i + 1]);
                } catch (NumberFormatException e) {
                    // Handle invalid startDelay input
                    System.err.println("Invalid startDelay value");
                }
            } else if ("-c".equals(args[i]) && i + 1 < args.length) {
                try {
                    crashDelay = Integer.parseInt(args[i + 1]);
                } catch (NumberFormatException e) {
                    // Handle invalid crashDelay input
                    System.err.println("Invalid crashDelay value");
                }
            }
        }

        // Print the extracted values
        //System.out.println("Hostname: " + hostsfile);
        //System.out.println("Start Delay: " + joinDelay);
        //System.out.println("Crash Delay: " + crashDelay);


        // Change to "hostsname.txt" when running on Docker
        //String fileName = "docker-compose-testcases-and-hostsfile-lab3/hostsfile.txt";
        String fileName = "hostsfile.txt";

        String myHostname = getMyHostname();
        StateValue state = new StateValue();


        List<String> peers = getPeerList(fileName);

        // determine what my peer index is
        int myPeerIndex = -1;
        for(int i = 0; i < peers.size(); i++) {
            if(peers.get(i).equals(myHostname)) {
                myPeerIndex = i;
            }
        }

        final String leader = peers.get(0);


        if(myHostname.equals(leader)) {
            state.amLeader = true;
            System.out.println("I am the leader!");
        }

        int numHosts = peers.size();

        // Each peer has a tcp connection to all other peers, ordered by the order in hostsfile.txt
        TCPConnection[] tcpConnections = new TCPConnection[numHosts - 1];

        // Array to keep track of ports to connect each peer to all others.
        int[][] outGoingTCPPortTable = new int[numHosts][numHosts];

        int port = 4900;
        for (int r = 0; r < 5; r++) {
            for (int c = 0; c < 5; c++) {
                outGoingTCPPortTable[r][c] = port;
                port++;
            }
        }

        int failureDetectorStartPort = 5000;
        int myBroadcastPort = failureDetectorStartPort + myPeerIndex + 1;
        UDPBroadcastHeartbeat broadcastHeartbeat = new UDPBroadcastHeartbeat(state, myHostname, myBroadcastPort);
        UDPListenHeartbeat[] heartbeatListeners = new UDPListenHeartbeat[numHosts - 1];




        int index = 0;
        int tcpIndex = 0;
        // Start a broadcast listener for each peer (not including myself)
        // Start TCP connections for each to the leader, leader needs to connect to everything
        while (index < peers.size()) {
            String connectedPeer = peers.get(index);

            if (connectedPeer.equals(myHostname)) {
                index++;
                continue;
            }

            TCPListener listen = new TCPListener(state, myHostname, connectedPeer, outGoingTCPPortTable[index][myPeerIndex]);
            TCPTalker talk = new TCPTalker(state, connectedPeer, myHostname, outGoingTCPPortTable[myPeerIndex][index]);

            TCPConnection connection = new TCPConnection(talk, listen);
            tcpConnections[tcpIndex] = connection;

            UDPListenHeartbeat udpListenHeartbeat= new UDPListenHeartbeat(state, failureDetectorStartPort + index + 1, connectedPeer);
            heartbeatListeners[tcpIndex] = udpListenHeartbeat;

            tcpIndex++;
            index++;
        }

        // Start TCP connections to leader
        TCPConnection leaderConnection = tcpConnections[0];

        if(state.amLeader) {
            state.sentJoinRequest = true;
            state.leaderValues = new LeaderValues();
            state.members.add(myHostname);

            // Establish TCP listeners to all peers
            for(int i = 0; i < tcpConnections.length; i++) {
                tcpConnections[i].listener.start();
            }

            sleep(1);

            // Establish TCP talkers to all peers
            for(int i = 0; i < tcpConnections.length; i++) {
                tcpConnections[i].talker.start();
            }

            // Establish UDP listeners to all peers
            // Establish UDP talkers to all peers

        } else {
            state.members.add(leader);

            // Connection to leader should always be the first index of connections array, as nothing
            // at this point has crashed and the leader is the lowest index.

            leaderConnection.listener.start();

            sleep(1);
            leaderConnection.talker.start();
        }

        // Start failure detectors:
        broadcastHeartbeat.start();
        for(UDPListenHeartbeat listenHeartbeat : heartbeatListeners) {
            listenHeartbeat.start();
        }



        // Main program loop:

        while(true) {
            if(!state.sentJoinRequest) {
                final int finalJoinDelay = joinDelay;
                joinGroup(finalJoinDelay, leaderConnection);

                if(crashDelay > 0) {
                    final int finalCrashDelay = crashDelay;
                    new Thread(() -> crash(finalCrashDelay)).start();
                }

                state.members.add(myHostname);
                state.sentJoinRequest = true;
            }




            if(state.amLeader && state.sendAddReq) {
                //System.out.println("telling all my talkers to send ADDs");
                for(TCPConnection conn : tcpConnections) {
                    // If this talker goes to a member in the current view
                    if(state.members.contains(conn.talker.targetHostname)) {
                        //System.out.println(conn.talker.targetHostname + " is in the current view");
                        conn.talker.sendAddReq = true;

                    }
                }
                state.sendAddReq = false;
            }

            if(state.amLeader && state.peerToAdd != null) {
                //System.out.println("Checking if we can send NEWVIEW...");
                // If the process of adding a peer has begun, check the number of OK's received"

                // If all members have sent OK, send NEWVIEW to all members. -1 because I include myself in the membership list
                if(state.leaderValues.okayCount == state.members.size() - 1) {
                    //System.out.println("All members have sent OK, sending NEWVIEW to all members");

                    // If all members have sent OK, send NEWVIEW to all members
                    state.leaderValues.okayCount = 0;
                    state.members.add(state.peerToAdd);
                    state.peerToAdd = null;

                    for (TCPConnection conn : tcpConnections)
                        if (state.members.contains(conn.talker.targetHostname))
                            conn.talker.sendNewView = true;

                    state.viewId++;
                    state.requestId++;
                    System.out.println("New member list: " + state.members);
                }
            }

            if(state.amLeader && state.leaderValues.sendNewView) {
                for(TCPConnection conn : tcpConnections) {
                    conn.talker.sendNewView = true;
                }
                state.leaderValues.sendNewView = false;
            }

            if(state.sendOkay) {
                leaderConnection.talker.sendOkay = true;
                state.sendOkay = false;
            }
            sleep(0.01F);

        }


    }

    public static void joinGroup(int joinDelay, TCPConnection leaderConnection) {
        sleep(joinDelay);
        leaderConnection.talker.sendJoin = true;
    }

    public static String getMyHostname() {
        String ret;
        try {
            InetAddress localHost = InetAddress.getLocalHost();
            ret = localHost.getHostName();
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }

        return ret;
    }

    public static ArrayList<String> getPeerList(String fileName) {
        ArrayList<String> peers = new ArrayList<String>();
        try  {
            BufferedReader br = new BufferedReader(new FileReader(fileName));
            String line;
            while ((line = br.readLine()) != null) {
                peers.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return peers;

    }

    public static void sleep(float seconds) {
        try {
            Thread.sleep((long) (seconds * 1000));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static void crash(float seconds) {
        sleep(seconds);
        System.out.println("Crashing...");
        System.exit(0);
    }

    public static int getNextValue(int current, int peerCount) {
        // Assuming the input is between 1 and 5 (inclusive)
        if (current >= 1 && current <= peerCount) {
            // If the current value is 5, the next value is 1
            if (current == peerCount) {
                return 1;
            }
            // For any other value, the next value is the current value + 1
            else {
                return current + 1;
            }
        } else {
            // Handle invalid input (you can throw an exception or return a special value)
            System.out.println("Invalid input. Please provide a number between 1 and 5 (inclusive).");
            return -1; // You can choose a different special value if needed
        }
    }


}
