package controller.user;

public class UserInfo {
	
	private String name = null;
	private Integer passwordHash = null;
	private Integer credits = 0;
	private Integer port = 0;
	
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
		}
	}
	
	public void setCredits(int credits){
		synchronized(this){
			this.credits = credits;
		}
	}
	
	public int getCredits(){
		return credits;
	}
	
	public String getName(){
		return this.name;
	}
}
