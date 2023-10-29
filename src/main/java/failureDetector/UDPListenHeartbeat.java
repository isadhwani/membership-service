package failureDetector;

import local.StateValue;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class UDPListenHeartbeat extends Thread {

    public StateValue state;


    int listenPort = 0;

    String listenHostname = "";

    // Is this heartbeat listener listening to the leader?
    boolean isLeader = false;
    public UDPListenHeartbeat(StateValue s, int listenPort, String listenHostname, boolean isLeader) {
        this.state = s;
        this.listenPort = listenPort;
        this.listenHostname = listenHostname;
        this.isLeader = isLeader;
    }

    @Override
    public void run() {
        TimeoutData timeoutData = new TimeoutData(0, listenHostname, isLeader);
        HandleTimeout handleTimeout = new HandleTimeout(timeoutData, state);
        handleTimeout.start();

        //System.out.println("running broadcast listener for peer " + listenHostname + " on port " + listenPort);
        DatagramSocket socket = null;
        try {
            socket = new DatagramSocket(listenPort);
            byte[] buf = new byte[256];


            while (true) {
                DatagramPacket packet = new DatagramPacket(buf, buf.length);

                socket.receive(packet);

                InetAddress address = packet.getAddress();
                int port = packet.getPort();
                packet = new DatagramPacket(buf, buf.length, address, port);
                String received = new String(packet.getData(), 0, packet.getLength());

                long currentTime = System.currentTimeMillis();
                //System.out.println("Received heartbeat from " + listenHostname);
                //System.out.println("Time between heartbeats: " + (currentTime - timeoutData.lastHeartbeatTimestamp));
                timeoutData.lastHeartbeatTimestamp = currentTime;
            }
        } catch (SocketException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


}
