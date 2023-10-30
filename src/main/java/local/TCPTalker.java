package local;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

public class TCPTalker extends Thread {

    public StateValue state;
    String targetHostname;
    int port;
    String myHostname;

    // Should this talker send a join message?
    boolean sendJoin = false;

    // Each talker will have a sendReq and a state will. Each sendReq on a talker will tell that talker
    // to send a single request message, where the sendReq in state will tell all the leader's talkers to sendReq
    boolean sendAddReq = false;

    // Should this talker send an OKAY message to the leader connected on this port?
    boolean sendOkay = false;

    // Should this leader send a new view message?
    // Invariant: if amLeader = false, sendNewView = false and sendNewLeader = false
    boolean sendNewView = false;

    // send a response to a NEWLEADER message
    public boolean sendStatus;

    boolean sendDelReq = false;

    boolean sendNewLeader = false;


    public TCPTalker(StateValue s, String targetHostname, String myHostname, int port) {
        this.state = s;
        this.targetHostname = targetHostname;
        this.port = port;
        this.myHostname = myHostname;
    }


    @Override
    public void run() {

        while (true) {

            //printMessagesToSend();

            //System.out.println("Should this talker on port " + this.port + " send a token? " + this.sendToken);

            try {
                //System.out.println("Send marker? : " + this.sendMarker);
                Socket socket = new Socket(targetHostname, port);

                OutputStream outputStream = socket.getOutputStream();

                if (sendJoin) {

                    String message = "{id:" + myHostname + ", message:JOIN}";

                    System.out.println("Sending JOIN to server: " + message);
                    byte[] messageBytes = message.getBytes();
                    outputStream.write(messageBytes);
                    outputStream.flush();
                    sendJoin = false;

                } else if (sendAddReq) {
                    String message = "{id: " + myHostname + ", requestId:" + state.requestId +
                            ", viewId:" + state.viewId + ", peerToAdd:" + state.peerToAdd +
                            ", message:REQ, operation:ADD}";

                    System.out.println("Sending ADD to " + targetHostname + ": " + message);

                    sendAddReq = false;

                    byte[] messageBytes = message.getBytes();
                    outputStream.write(messageBytes);
                    outputStream.flush();
                } else if (sendOkay) {
                    String message = "{id: " + myHostname + ", requestId:" + state.requestId +
                            ", viewId:" + state.viewId + ", message:OK}";

                    System.out.println("Sending message: " + message);

                    byte[] messageBytes = message.getBytes();

                    outputStream.write(messageBytes);
                    outputStream.flush();
                    this.sendOkay = false;

                } else if (sendNewView) {
                    String members = "";
                    for(String m : state.members) {
                        members += m + ";";
                    }
                    String message = "{id: " + myHostname +
                            ", viewId:" + state.viewId + ", message:NEWVIEW, members:" + members + "}";

                    byte[] messageBytes = message.getBytes();

                    outputStream.write(messageBytes);
                    outputStream.flush();
                    sendNewView = false;
                } else if (sendDelReq) {

                    String message = "{id: " + myHostname + ", requestId:" + state.requestId +
                            ", viewId:" + state.viewId + ", peerToDel:" + state.peerToDel +
                            ", message:REQ, operation:DEL}";

                    System.out.println("Sending DEL to " + targetHostname + ": " + message);

                    sendDelReq = false;

                    byte[] messageBytes = message.getBytes();
                    outputStream.write(messageBytes);
                    outputStream.flush();
                } else if(sendNewLeader) {
                    String message = "{id: " + myHostname + ", viewId:" + state.viewId +
                            ", message:NEWLEADER, operation:PENDING}";

                    System.out.println("Sending NEWLEADER to " + targetHostname + ": " + message);

                    sendNewLeader = false;

                    byte[] messageBytes = message.getBytes();
                    outputStream.write(messageBytes);
                    outputStream.flush();
                } else if(sendStatus) {

                    String operation = "NOTHING";
                    String targetPeer = "";
                    if(state.peerToDel != null) {
                        operation = "DEL";
                        targetPeer = ", peerToDel:" + state.peerToDel;

                    } else if(state.peerToAdd != null) {
                        operation = "ADD";
                        targetPeer = ", peerToAdd:" + state.peerToAdd;
                    }

                    String message = "{id: " + myHostname + ", viewId:" + state.viewId +
                            ", message:STATUS" + targetPeer + ", operation:" + operation + "}";

                    System.out.println("Sending STATUS to " + targetHostname + ": " + message);

                    sendStatus = false;

                    byte[] messageBytes = message.getBytes();
                    outputStream.write(messageBytes);
                    outputStream.flush();
                }

                sleep(1);


                // Close the socket
                socket.close();
            } catch (UnknownHostException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void printMessagesToSend() {
        String ret = "";
        ret += "sendJoin: " + sendJoin + ", ";
        ret += "sendAddReq: " + sendAddReq + ", ";
        ret += "sendOkay: " + sendOkay + ", ";
        ret += "sendNewView: " + sendNewView;
        try {
            sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        System.out.println(ret);
    }

}


