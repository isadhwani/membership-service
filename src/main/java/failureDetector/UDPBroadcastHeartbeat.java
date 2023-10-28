package failureDetector;

import local.StateValue;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class UDPBroadcastHeartbeat extends Thread {
    public final int HEARTBEAT_INTERVAL = 1000;

    StateValue state;
    int port;
    String myHostname;

    String broadcastMessage = "heartbeat";

    public UDPBroadcastHeartbeat(StateValue s, String myHostname, int port) {
        this.state = s;
        this.port = port;
        this.myHostname = myHostname;
    }

    private static DatagramSocket socket = null;

    @Override
    public void run() {

        try {
            // Continuously broadcast the heartbeat message every 5 seconds
            while (true) {

                // Network broadcast address
                InetAddress address = InetAddress.getByName("255.255.255.255");

                socket = new DatagramSocket();
                socket.setBroadcast(true);

                byte[] buffer = broadcastMessage.getBytes();

                DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, port);

                //System.out.println("Sending heartbeat from " + myHostname + " on port " + port);
                socket.send(packet);

                // Wait HEARTBEAT_INTERVAL mili seconds before sending next heartbeat
                sleep(HEARTBEAT_INTERVAL);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        socket.close();
    }
}

