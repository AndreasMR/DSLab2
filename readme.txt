Reflect about your solution!

Stage 1:
A two-stage commit protocol was implemented for new nodes signing in with the cloud controller. When a node comes online, it first requests data about the active nodes and available resources
from the cloud controller. It then decodes the answer and calculates the resource share for every node if it were to sign on.
If its own resource requirements exceed this share, it shuts down immediately. Otherwise it creates a NodeCommitter object
which inquires of the other nodes if the calculated share is sufficient. It does this by spawning a separate thread
running a NodeMessenger Runnable for each other node and waiting for the result. The NodeMessengers first send their
nodes the share of resources. Upon receiving the answer (accept/reject) they set a boolean variable to false if the
answer is negative and in either case wait until their brethren are finished with the first round. The last to finish
wakes the others (and the NodeMessenger) and they send either commit or rollback messages. The NodeMessenger returns true
if the node can come online and false otherwise. Dependent on this the node either comes online or shuts down.

Stage 2:

Stage 3:
In order to provide an integrity check of TCP communications a HmacChannel was implemented to provide a channel-decoration with HMAC check. It is initialized with a given hmac-key and prepends a HMAC and evaluates respectively outgoing and incoming messages. If the evaluation of the HMAC fails a TamperedException is thrown. The Node reacts to this exception by sending a !tampered message back and the CloudController prints an appropriate message.

Stage 4:
The CloudController creates a registry on the RMI-port and exports the AdminService Object, which implements IAdminConsole on the server-side and binds it to the registry.

The AdminConsole locates the registry on the server and gets the remote AdminService Object in order to execute the admin-commands remotely and print the return-values to the console. For the subscribe-command a NotificationCallback Object is made available for remote access via exporting. Its notify method prints an appropriate message to the AdminConsoles stream.

In AdminService the admin-commands are implemented on the server-side. subscribe() adds the NotificationCallback object and credits-limit to the respective user and gets checked whenever the credits-amount changes. getLogs() sends a request via TCP to all active nodes which read the log-files and return a list of objects containing the information via an ObjectOutputStream over TCP. statistics() sorts and returns a Map of operators and their usage, which gets increased by each !compute command.

