package local;

import java.util.ArrayList;

/**
 * Object for sharing state among listener, talker and main threads
 */
public class StateValue {

    boolean amLeader = false;

    // If this state is the leader and it should have all it's talkers send a REQ message

    public ArrayList<String> members = new ArrayList<String>();

    public int viewId = 0;
    int requestId = 0;

    String operationType;


    boolean sentJoinRequest = false;

    // Sending fields:
    boolean sendAddReq = false;
    boolean sendOkay = false;

    // Stores the next peer to be added, either initiated by leader or received from a ADD message
    String peerToAdd;

    // INVARIANT: if amLeader = false, leaderValues = null
    LeaderValues leaderValues;




}







