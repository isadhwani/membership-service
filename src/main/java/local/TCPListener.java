package local;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class TCPListener extends Thread {


    StateValue state;
    int port;
    String myHostname;
    String connectedHost;

    String leader;

    TCPListener(StateValue s, String myHostname, String connectedHost, int port, String leader) {
        this.port = port;
        this.myHostname = myHostname;
        this.state = s;
        this.connectedHost = connectedHost;
        this.leader = leader;
    }


    @Override
    public void run() {
        //System.out.println("Running listener thread!");
        try {
            ServerSocket serverSocket = new ServerSocket(port);
            //System.out.println("Server is listening on port " + port);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                //System.out.println("Client connected: " + clientSocket.getInetAddress().getHostName());

                // Create a new thread to handle the client communication
                handleClient(clientSocket);
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }


    private void handleClient(Socket clientSocket) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            String receivedMessages;
            while ((receivedMessages = reader.readLine()) != null) {
                //System.out.println("Received from " + clientSocket.getInetAddress().getHostName() + ": " + receivedMessages);
                System.out.println("Received: " + receivedMessages);

                String[] messages = receivedMessages.split("\\}\\{");

                // if multiple messages are received at once, iterate through them
                for (String message : messages) {

                    Map<String, String> decoded = decodeJSON(message);

                    String msgType = decoded.get("message");

                    if (msgType.equals("JOIN")) {
                        if (state.amLeader) {
                            //System.out.println("Processed JOIN message and telling main to send ADD");
                            state.sendAddReq = true;
                            state.peerToAdd = decoded.get("id");
                        }

                    } else if (msgType.equals("REQ")) {
                        state.requestId = Integer.parseInt(decoded.get("requestId"));
                        state.viewId = Integer.parseInt(decoded.get("viewId"));
                        state.operationType = decoded.get("operation");
                        state.sendOkay = true;

                        if (decoded.get("operation").equals("ADD")) {
                           // System.out.println("Received ADD request from " + decoded.get("id"));
                            state.peerToAdd = decoded.get("peerToAdd");
                        } else if (decoded.get("operation").equals("DEL")) {
                           // System.out.println("Received DEL request from " + decoded.get("id"));
                            state.peerToDel = decoded.get("peerToDel");
                            //System.out.println("Preparing to remove: " + state.peerToDel);
                        }

                    } else if (state.amLeader && msgType.equals("OK")) {
                        state.leaderValues.okayCount++;
                        //System.out.println("Received OK from " + decoded.get("id") + " and okayCount is now " + state.leaderValues.okayCount);

                    } else if (msgType.equals("NEWVIEW")) {
                        // Becasue ADD and DEL operations cannot happen concurrently, we can assume that after receiving
                        //  a new view that all current operations are over
                        state.peerToAdd = null;
                        state.peerToDel = null;

                        // not sure if i should do this here
                        state.requestId++;
                        state.viewId++;

                        List<String> list = Arrays.asList(decoded.get("members").split(";"));
                        state.members = new TreeSet<>(list);
                        System.out.println("Membership: " + state.members);
                    } else if (msgType.equals("NEWLEADER")) {
                        System.out.println("Received new leader, peerToDel: " + state.peerToDel);
                        state.sendStatus = true;
                        leader = decoded.get("id");

                    } else if (state.amLeader && msgType.equals("STATUS")) {
                        state.viewId = Integer.parseInt(decoded.get("viewId"));
                        state.operationType = decoded.get("operation");

                        if (decoded.get("operation").equals("ADD")) {
                            System.out.println("Received ADD request from " + decoded.get("id"));
                            state.peerToAdd = decoded.get("peerToAdd");
                            state.sendAddReq = true;
                        } else if (decoded.get("operation").equals("DEL")) {
                            System.out.println("Received DEL request from " + decoded.get("id"));
                            state.peerToDel = decoded.get("peerToDel");
                            state.sendDelReq = true;
                        }
                    }
                }
            }

        } catch (IOException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }


    public static Map<String, String> decodeJSON(String jsonString) {
        Map<String, String> resultMap = new HashMap<>();

        // Remove curly braces from the JSON string
        jsonString = jsonString.substring(1, jsonString.length() - 1);

        // Split the string into key-value pairs
        String[] keyValuePairs = jsonString.split(",");

        for (String pair : keyValuePairs) {
            // Split each pair into key and value
            String[] entry = pair.split(":");

            // Trim whitespace from key and value
            String key = entry[0].trim();
            String value = entry[1].trim();

            String stringWithoutSpaces = value.replaceAll("\\s", "");

            resultMap.put(key, stringWithoutSpaces);
        }

        return resultMap;
    }
}


