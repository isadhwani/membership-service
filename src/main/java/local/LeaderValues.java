package local;

public class LeaderValues {
    // Invariant: if amLeader = false, sendNewView = false
    boolean sendNewView = false;

    // field only used by leader, counts the number of okay's received
    // INVARIANT: if amLeader = false, okayCount = 0
    int okayCount = 0;

    String hostToAdd;
}
