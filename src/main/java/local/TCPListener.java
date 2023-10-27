package local;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TCPListener extends Thread {


    StateValue state;
    int port;
    String myHostname;

    TCPListener(StateValue s, String myHostname, String connectedHost, int port) {
        this.port = port;
        this.myHostname = myHostname;
        this.state = s;
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
                System.out.println("Received: " + receivedMessages + " from " + clientSocket.getInetAddress().getHostName());

                String[] messages = receivedMessages.split("\\}\\{");

                // if multiple messages are received at once, iterate through them
                for (String message : messages) {
                    // Add back the missing curly braces for each message




                    Map<String, String> decoded = decodeJSON(message);
                    // Print out each element of map decoded:
//                    for (Map.Entry<String, String> entry : decoded.entrySet()) {
//                        System.out.println("Key = " + entry.getKey() +
//                                ", Value = " + entry.getValue());
//                    }


                    String msgType = decoded.get("message");
                    System.out.println("Message type received: " + msgType);

                    //System.out.println(message);
                    //System.out.println("extracted msg: " + msg);

                    if(msgType.equals("JOIN")) {
                        System.out.println("Processed JOIN message, and amLeader = " + state.amLeader);
                        if(state.amLeader) {
                            state.sendAddReq = true;
                            System.out.println("TELLING MAIN TO SEND ADD REQ");
                            System.out.println("StateID: " + this.state);
                        }

                    } else if(msgType.equals("REQ")) {
                        if(decoded.get("operation").equals("ADD")) {
                            state.requestId = Integer.parseInt(decoded.get("requestId"));
                            state.viewId = Integer.parseInt(decoded.get("viewId"));
                            state.operationType = decoded.get("operationType");
                            state.sendOkay = true;
                            state.peerToAdd = decoded.get("peerToAdd");
                        }


                    } else if (state.amLeader && msgType.equals("OK")) {
                        state.leaderValues.okayCount++;
                        if(state.leaderValues.okayCount == state.members.size()) {
                            state.leaderValues.sendNewView = true;
                        }
                    } else if (msgType.equals("NEWVIEW")) {
                        state.leaderValues.okayCount++;
                        if(state.leaderValues.okayCount == state.members.size()) {
                            state.leaderValues.sendNewView = true;
                        }
                    }

                }
            }

        } catch (IOException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }


    private static int extractMarkerNumber(String marker) {
        // Define a pattern to match "peer" followed by one or more digits
        Pattern pattern = Pattern.compile("marker(\\d+)");

        // Create a matcher with the input hostName
        Matcher matcher = pattern.matcher(marker);

        // Check if the pattern is found
        if (matcher.find()) {
            // Extract and parse the matched digits
            String numberString = matcher.group(1);
            return Integer.parseInt(numberString);
        } else {
            // Return a default value or throw an exception, depending on your requirements
            throw new IllegalArgumentException("Invalid host name format: " + marker);
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

            // Remove quotes if the value is a string
//            if (value.startsWith("'") && value.endsWith("'")) {
//                value = value.substring(1, value.length() - 1);
//            }

            // Add the key-value pair to the result map

            String stringWithoutSpaces = value.replaceAll("\\s", "");

            resultMap.put(key, stringWithoutSpaces);
        }

        return resultMap;
    }

    private static Object parseValue(String value) {
        // Try to parse the value as an integer
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            // If parsing as an integer fails, return the value as a string
            return value;
        }
    }


}


