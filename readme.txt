Reflect about your solution!

Stage 1:

Stage 2:

Stage 3:
In order to provide an integrity check of TCP communications a HmacChannel was implemented to provide a channel-decoration with HMAC check. It is initialized with a given hmac-key and prepends a HMAC and evaluates respectively outgoing and incoming messages. If the evaluation of the HMAC fails a TamperedException is thrown. The Node reacts to this exception by sending a !tampered message back and the CloudController prints an appropriate message.

Stage 4:
The CloudController creates a registry on the RMI-port and exports the AdminService Object, which implements IAdminConsole on the server-side and binds it to the registry.

The AdminConsole locates the registry on the server and gets the remote AdminService Object in order to execute the admin-commands remotely and print the return-values to the console. For the subscribe-command a NotificationCallback Object is made available for remote access via exporting. Its notify method prints an appropriate message to the AdminConsoles stream.

In AdminService the admin-commands are implemented on the server-side. subscribe() adds the NotificationCallback object and credits-limit to the respective user and gets checked whenever the credits-amount changes. getLogs() sends a request via TCP to all active nodes which read the log-files and return a list of objects containing the information via an ObjectOutputStream over TCP. statistics() sorts and returns a Map of operators and their usage, which gets increased by each !compute command.

