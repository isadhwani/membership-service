package local;

/**
 * Represents a Listener and talker for one process to connect to another
 */
public class TCPConnection {
    TCPListener listener;
    TCPTalker talker;



    TCPConnection(TCPTalker talk, TCPListener listen) {
        this.talker = talk;
        this.listener = listen;
    }

}
