package failureDetector;

public class TimeoutData {
long lastHeartbeatTimestamp;

String listenHostname;

boolean peerTimedout = false;

// Heartbeats are sent every 5000 milliseconds, timeout is 2 heartbeats missed
public final int HEARTBEAT_TIMEOUT = 2000;




    TimeoutData(long lastHeartbeatTimestamp, String listenHostname) {
        this.lastHeartbeatTimestamp = lastHeartbeatTimestamp;
        this.listenHostname = listenHostname;
    }

}
