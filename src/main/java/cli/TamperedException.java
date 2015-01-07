package cli;

import java.io.IOException;

public class TamperedException extends IOException{

	private static final long serialVersionUID = 1243171850874632131L;

	private String tamperedMsg;
	
	public TamperedException(String tamperedMsg){
		this.tamperedMsg = tamperedMsg;
	}
	
	public String getTamperedMsg(){
		return tamperedMsg;
	}
	
}
