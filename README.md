My implementation of a membership service consists of a failure detector, a leader, and up to 9 other peers.

The leader is always the first peer listed in the hostsfile given with the `-h` command. All other peers will no who the leader is upon starting.

The `-d` command line input specifies the join delay of this peer. Upon starting, the designated peer will slepp for `d` seconds before sending a join message. 

The `-c` command line input specifies the crash delay of this peer. After joining, the specified peer will sleep for `c` seconds before crashing. This is for testing how the algorithm handles failures





