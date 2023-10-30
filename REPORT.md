## Setup 
After all peers decide on the initial leader, they each establish TCP talk and TCP listen channels to the leader. 
The leader will establish connections TCP talkers and TCP listeners to all other peers.

Each talker and listener will operate on a separate thread. Each thread has access to a `StateValue` object.   

This object contains all information that needs to be shared between threads, such as when certain messages are revived and when messages need to be sent.  

Each `StateValue` object also has a `LeaderValues` object. This object contains information only the leader needs to keep track of and will be `null` if not the leader.  

## Algorithm
The algorithm begins when one peer decides to join the membership service. This happens when it's specified `d` delay expires  

After sending a `JOIN` message to the leader, the leader will send an `ADD` operation to all peers in the service with the peer requesting to join's ID.  

Once the leader receives unanimous `OK` messages from all peers, it will send a `NEWVIEW` message to all peers in the membership service including the one that requested to join. This message will always contain a list of all the peers in the current view.  

Upon receiving a `NEWVIEW` message, each peer will update its list of peers to the list in the message.

When a non leader peer fails, every peer will detect this failure due to how the failure detector is set up(described below).   

When the leader detects that a peer has failed, it will begin a `DEL` operation as follows.

The leader will get the ID of the peer that failed and sends a `DEL` operation including this peer to all peers in the current view. 

Upon receiving a `DEL` message, all peers will respond with an `OK` message. 

When the leader receives `OK` messages from all alive peers in the membership, it will send a `NEWVIEW` message with the peer who failed removed.


When the leader detects a failure, it will begin a `DEL` operation as described above.

When the leader fails, all peers will notice by the nature of the failure detector. Upon detecting a leader failure, the next peer in the specied hostsfile will become the leader. All peers will immediately know who the new leader is.

The new will leader will start TCP connections to all other peers. Each peer will close its connection to the previous leader and start a new connection to the new leader.

Next, the new leader will query all peer in the current view for any outstanding operations with a `NEWLEADER` message.

When a peer receives a `NEWLEADER` message, it will check if it has any pending operations. If so, it will share them wiht the new leader in a `STATUS` message.

When the leader receives `STATUS` messages back with any pending operations, it will restart and execute the pending message as described abo

## Failure Detector
My failure detector uses a heartbeat protocol to detect failures. Each peer has a listener and a broadcast thread. The listener listens for incoming messages on the broadcast channel and the talker sends messages to other peers on the network.  

The talker sends a heartbeat message to each peer every 2 seconds. The listener receives messages and updates a timestamp for the peer that sent the message. If a peer has not sent a message in 4 seconds, it is considered failed.  
ve. 


## Testcases
### Testcase 1
Tests that each peer can join the membership service one after the other. Each peer joins sequentially, and the test ends with `one` as the leader and all others in view `4`

### Testcase 2
Tests that after all peers have joined the membership service, peer `five` will crash and will be properly removed in view `5`. The test ends with `one` as the leader and view `5` containing `[one, two, three, four]` not necessarily in order

### Testcase 3
Tests that after all peers have joined, each other than the leader `one` will crash and each will be properly removed. The test ends with `one` as the leader and `[one]` as the only peer in view `8`

### Testcase 4
Tests a leader failure with an operation pending after all other peers have joined the membership service 
 
Before leader `one` crashes, it will send a `DEL` operation of peer `five` to all peers other than the next leader, `two`. After all peers detect `one`'s failure, they will decide on `two` as the new leader. `two` will ask for any pending operations, and it will hear about the `DEL` proposed by `one` before it crashed. `two` will restart this operation and send a `NEWVIEW` message with `five` removed. The test ends with `two` as the leader and `[two, three, four]` as the view