package model;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * Please note that this class is not needed for Lab 1, but will later be
 * used in Lab 2. Hence, you do not have to implement it for the first
 * submission.
 */
public class ComputationRequestInfo implements Comparable<ComputationRequestInfo>, Serializable{
	
	private static final long serialVersionUID = -2297643822398302971L;
	
	private long timestamp; //in milliseconds using GregorianCalendar, used for sorting
	private String info; 	//formatted information
	
	public ComputationRequestInfo (File log) throws FileNotFoundException, IOException{
		String[]parts = log.getName().split("_|\\."); //20141203_104913.560_node1.log
		String date = parts[0];
		String time = parts[1];
		String millis = parts[2];
		
		String node = parts[3];
		
		String timestring = date + "_" + time + "." + millis;
		
		Calendar gc = new GregorianCalendar(
				Integer.parseInt(date.substring(0, 4)), 	//year
				Integer.parseInt(date.substring(4, 6)) - 1, //month, zero-based
				Integer.parseInt(date.substring(6, 8)), 
				Integer.parseInt(time.substring(0, 2)), 
				Integer.parseInt(time.substring(2, 4)), 
				Integer.parseInt(time.substring(4, 6)));
		gc.set(Calendar.MILLISECOND, Integer.parseInt(millis));
		timestamp = gc.getTimeInMillis();
		
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(log));
		} catch (FileNotFoundException ex) {
			throw ex;
		}

		String content = null;
		
		if(br != null){
			try {
				content = br.readLine();
				String result = br.readLine();

				if(result != null){
					content += " = " + result;
				}

				br.close();
				
			} catch (IOException ex) {
				throw ex;
			}
		}
		
		info = timestring + "[" + node + "]: " + content;
	}
	
	public String getInfo(){
		return info;
	}

	@Override
	public int compareTo(ComputationRequestInfo o) {
		return this.timestamp > o.timestamp ? 1 : this.timestamp == o.timestamp ? 0 : -1;
	}
}
