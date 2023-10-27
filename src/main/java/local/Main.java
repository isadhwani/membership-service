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
        int crashDelay = 0;
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
        System.out.println("Hostname: " + hostsfile);
        System.out.println("Start Delay: " + joinDelay);
        System.out.println("Crash Delay: " + crashDelay);


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
        TCPConnection[] tcpConnections = new TCPConnection[numHosts];

        // Array to keep track of ports to connect each peer to all others.
        int[][] outGoingTCPPortTable = new int[numHosts][numHosts];

        int port = 4950;
        for (int r = 0; r < 5; r++) {
            for (int c = 0; c < 5; c++) {
                outGoingTCPPortTable[r][c] = port;
                port++;
            }
        }

        for (int i = 0; i < peers.size(); i++) {
            String connectedPeer = peers.get(i);

            if(connectedPeer.equals(myHostname)) {
                continue;
            }

            TCPListener listen = new TCPListener(state, myHostname, connectedPeer,  outGoingTCPPortTable[i][myPeerIndex]);
            TCPTalker talk = new TCPTalker(state, connectedPeer, myHostname, outGoingTCPPortTable[myPeerIndex][i]);

            TCPConnection connection = new TCPConnection(talk, listen);
            tcpConnections[i] = connection;
        }

        TCPConnection leaderConnection = tcpConnections[0];

        if(state.amLeader) {
            state.sentJoinRequest = true;
            state.leaderValues = new LeaderValues();

            // Establish TCP listeners to all peers
            for(int i = 0; i < tcpConnections.length; i++) {
                if(i == myPeerIndex) {
                    continue;
                }
                tcpConnections[i].listener.start();
            }

            sleep(1);

            // Establish TCP talkers to all peers
            for(int i = 0; i < tcpConnections.length; i++) {
                if(i == myPeerIndex) {
                    continue;
                }
                tcpConnections[i].talker.start();
            }

            // Establish UDP listeners to all peers
            // Establish UDP talkers to all peers

        } else {

            // Connection to leader should always be the first index of connections array, as nothing
            // at this point has crashed and the leader is the lowest index.

            leaderConnection.listener.start();

            sleep(1);
            leaderConnection.talker.start();

            // Establish TCP talker to leader
            // Establish TCP listener to leader

            // Establish UDP talker to leader
            // Establish UDP listener to leader
        }

        // Main program loop:

        while(true) {
            if(!state.sentJoinRequest) {
                final int finalJoinDelay = joinDelay;
                joinGroup(finalJoinDelay, leaderConnection);
               // new Thread(() -> joinGroup(finalJoinDelay, leaderConnection)).start();
                state.sentJoinRequest = true;
            }

            if(state.amLeader){
                sleep(1);
                System.out.println("Send add req: " + state.sendAddReq);
            }
            if(state.amLeader && state.sendAddReq) {
                System.out.println("telling all my talkers to send ADDs");
                for(TCPConnection conn : tcpConnections) {
                    conn.talker.sendAddReq = true;
                }
                state.sendAddReq = false;
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
        }

    }

    public static void joinGroup(int joinDelay, TCPConnection leaderConnection) {
        sleep(joinDelay);
        System.out.println("Sending join request to leader: " + leaderConnection.talker.targetHostname);
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
