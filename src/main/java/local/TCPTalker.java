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
    // Invariant: if amLeader = false, sendNewView = false
    boolean sendNewView = false;






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

                    String message = "{id:" + myHostname +
                            ", message:JOIN}";

                    System.out.println("Sending JOIN to server: " + message);
                    byte[] messageBytes = message.getBytes();

                    outputStream.write(messageBytes);

                    outputStream.flush();
                    sendJoin = false;

                    //Thread.sleep(2000);
                    //System.out.println("Sent message to server: " + message);
                } else if (sendAddReq) {
                  String message = "{id: " + myHostname + ", requestId:" + state.requestId +
                          ", viewId:" + state.viewId + ", hostToAdd:" + state.leaderValues.hostToAdd +
                          ", message:ADD}";
                  // type: "JOIN"
                  //state.requestId++;
                  sendAddReq = false;


                    byte[] messageBytes = message.getBytes();

                    outputStream.write(messageBytes);
                    outputStream.flush();
                } else if (sendOkay) {
                    String message = "{id: " + myHostname + ", requestId:" + state.requestId +
                            ", viewId:" + state.viewId + ", message:OK}";


                    byte[] messageBytes = message.getBytes();

                    outputStream.write(messageBytes);
                    outputStream.flush();
                } else if(sendNewView) {
                    state.viewId++;
                    String message = "{id: " + myHostname +
                            ", viewId:" + state.viewId + ", message:NEWVIEW, members:" + state.members + "}";

                    byte[] messageBytes = message.getBytes();

                    outputStream.write(messageBytes);
                    outputStream.flush();
                }


                // Close the socket
                socket.close();
            } catch (UnknownHostException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
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


