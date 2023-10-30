package failureDetector;

import local.StateValue;

public class HandleTimeout extends Thread {
    TimeoutData timeoutData;

    // Handle timeout must have access to the state to remove peers when they crash
    StateValue state;

    HandleTimeout(TimeoutData timeoutData, StateValue state) {
        this.timeoutData = timeoutData;
        this.state = state;
    }

    @Override
    public void run() {
        while (true) {
            long currentTime = System.currentTimeMillis();

            if (currentTime - timeoutData.lastHeartbeatTimestamp > timeoutData.HEARTBEAT_TIMEOUT &&
                    timeoutData.lastHeartbeatTimestamp != 0) {
                System.out.println("Peer " + timeoutData.listenHostname + " not reachable");

                if (timeoutData.isLeader) {
                    //System.out.println("Leader " + timeoutData.listenHostname + " not reachable");
                    state.leaderTimedout = true;
                } else {
                    // If the failed peer is not the leader, delete it from the membership service
                    state.peerToDel = timeoutData.listenHostname;
                    state.sendDelReq = true;
                }

                break;
            }

            try {
                sleep(1);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

    }
}
