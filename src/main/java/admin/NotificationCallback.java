package admin;

import java.io.PrintStream;
import java.rmi.RemoteException;

public class NotificationCallback implements INotificationCallback{

	private PrintStream stream;
	
	public NotificationCallback(PrintStream stream){
		this.stream = stream;
	}
	
	@Override
	public void notify(String username, int credits) throws RemoteException {
		synchronized(stream){
			stream.println("Notification: " + username + " has less than " + credits + " credits.");
		}
	}

}
