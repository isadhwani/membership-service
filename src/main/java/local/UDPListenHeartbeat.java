package local;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class UDPListenHeartbeat extends Thread {

    public StateValue state;


    long lastHeartbeatTimestamp = 0;

    // Heartbeats are sent every 5000 milliseconds, timeout is 2 heartbeats missed
    public final int HEARTBEAT_TIMEOUT = 10000;

    int listenPort = 0;

    String listenHostname = "";

    UDPListenHeartbeat(StateValue s, int listenPort, String listenHostname) {
        this.state = s;
        this.listenPort = listenPort;
        this.listenHostname = listenHostname;
    }

    @Override
    public void run() {
        System.out.println("running broadcast listener for peer " + listenHostname + " on port " + listenPort);
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

                //System.out.println("received:" + received);

                long currentTime = System.currentTimeMillis();

                if (currentTime - lastHeartbeatTimestamp > HEARTBEAT_TIMEOUT && lastHeartbeatTimestamp != 0) {
                    System.out.println("Peer " + listenHostname+ " not reachable");
                    //state.removePeer(address, port);

                } else {
                    System.out.println("Received heartbeat from " + listenHostname);
                    System.out.println("Time between heartbeats: " + (currentTime - lastHeartbeatTimestamp));
                    lastHeartbeatTimestamp = currentTime;
                }
            }
        } catch (SocketException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
