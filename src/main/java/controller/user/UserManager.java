package controller.user;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import util.Config;

public class UserManager {
	
	private Config userConfig = null;	
	
	//name-UserInfo mapping of all users registered in the property file
	private Map<String, UserInfo> registeredUsers = Collections.synchronizedMap(new HashMap<String, UserInfo>());
	
	//port-UserInfo mapping of users which are logged in
	private Map<Integer, UserInfo> activeUsers = Collections.synchronizedMap(new HashMap<Integer, UserInfo>());
	
	
	public UserManager(){
		
		//Read registered users from property file
		userConfig = new Config( "user" );
		
		Set<String> userKeys = userConfig.listKeys();
		for( String key : userKeys ) {
			String name = key.substring( 0, key.indexOf( '.' ) );
			if(!registeredUsers.containsKey(name)){
				registeredUsers.put(name, new UserInfo(name, userConfig.getString( name + ".password" ), userConfig.getInt( name + ".credits" )) );
			}
		}
		
	}
	
	public UserInfo getRegisteredUser(String name){
		return registeredUsers.get(name);
	}
	
	public UserInfo getActiveUser(int port){
		return activeUsers.get(port);
	}
	
	public boolean isActiveUser(UserInfo user){
		return activeUsers.containsValue(user);
	}
	
	public UserInfo[] getUsers(){
		UserInfo[] usersArray;
		synchronized(registeredUsers){
			usersArray = registeredUsers.values().toArray(new UserInfo[registeredUsers.values().size()]);
		}
		return usersArray;
	}
	
	public void activate(int port, UserInfo user){
		user.setPort(port);
		activeUsers.put(port, user);
	}
	
	public void deactivate(int port){
		activeUsers.remove(port);
	}
	
}
