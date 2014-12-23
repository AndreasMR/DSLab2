package controller.user;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import admin.INotificationCallback;

public class UserInfo implements Serializable{
	
	private static final long serialVersionUID = -8726975885204304346L;
	
	private String name = null;
	private Integer passwordHash = null;
	private Integer credits = 0;
	private Integer port = 0;
	
	private Map<INotificationCallback, Integer> creditNotifications = Collections.synchronizedMap(new HashMap<INotificationCallback, Integer>());
	
	public UserInfo( String name, String password, Integer credits ){
		this.name = name;
		this.passwordHash = password.hashCode();
		this.credits = credits;
	}
	
	public void setPort(int port){
		synchronized(this){
			this.port = port;
		}
	}
	
	public int getPort(){
		return this.port;
	}
	
	public boolean checkPassword(String password){
		return this.passwordHash == password.hashCode();
	}
	
	public void addCredits(int additionalCredits){
		synchronized(this){
			credits += additionalCredits;
			checkCreditNotifications();
		}
	}
	
	public void setCredits(int credits){
		synchronized(this){
			this.credits = credits;
			checkCreditNotifications();
		}
	}
	
	public int getCredits(){
		return credits;
	}
	
	public String getName(){
		return this.name;
	}
	
	public void addCreditsNotification(INotificationCallback callback, int credits){
		synchronized(this){
			creditNotifications.put(callback, credits);
		}
	}
	
	private void checkCreditNotifications(){
		synchronized(this){
			Iterator<INotificationCallback> it = creditNotifications.keySet().iterator();
			while(it.hasNext()){
				INotificationCallback c = it.next();
				if(this.credits < creditNotifications.get(c)){
					try {
						c.notify(this.name, creditNotifications.get(c));
					} catch (RemoteException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					it.remove();
				}
			}
		}
	}
}
