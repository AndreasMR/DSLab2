Reflect about your solution!

Summary:

The CloudController creates 3 primary threads: One listening for TCP-connections, one for UDP-packets and one periodically checking if nodes have timed-out.
The TCP-thread executes a new thread for each new connection, which handles incoming commands from the client until it disconnects. The thread communicating with a client creates connections to nodes if needed and holds them open until the client or respective node closes.
The UDP-thread executes a new thread for each received package just to process its content in regard of isAlive-messages.
Separate classes are used to manage user related data and node related data which have their respective data objects. 
The UserManager holds a map with registered users identified by name and a map of connected users identified by port, which intentionally allows for users to connect from multiple clients at once.
The NodeManager holds all via isAlive-messages identified nodes in a map identified by port and also provides functionality to get the right node for a requested operation.

The Node creates 3 primary threads: One listening for TCP-connections, one to periodically broadcast isAlive-messages via UDP and the static-accessible NodeLogger uses one thread to process Log-Jobs stored in a LinkedBlockingQueue.
The TCP-thread executes a new thread for each new connection, which handles incoming computation commands from the CloudController and commissions Log-Jobs until it disconnects.

The Client tries to establish a connection to the CloudController, if unsuccessful (offline) the user can let it retry. As long as a connection is available, inputs from the user are relayed to the CloudController and response-messages written to the clients stream. The verification, classification and processing of user-commands happens, for this version, on the CloudControllers side.
