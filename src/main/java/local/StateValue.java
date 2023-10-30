package local;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

/**
 * Object for sharing state among listener, talker and main threads
 */
public class StateValue {

    public boolean leaderTimedout = false;
    boolean amLeader = false;

    // If this state is the leader and it should have all it's talkers send a REQ message

    public Set<String> members = new TreeSet<String>();

    public int viewId = 0;
    int requestId = 0;

    String operationType;


    boolean sentJoinRequest = false;

    // Sending fields:
    boolean sendAddReq = false;
    boolean sendOkay = false;

    public boolean sendDelReq = false;

    // Used when responding to a NEWLEADEr message, tells this peer to send whatever current operation it has stored
    boolean sendStatus = false;

    // Stores the next peer to be added, either initiated by leader or received from a ADD message
    String peerToAdd = null;

    public String peerToDel = null;

    // INVARIANT: if amLeader = false, leaderValues = null
    LeaderValues leaderValues;




}







