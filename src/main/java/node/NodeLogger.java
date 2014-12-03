package node;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.LinkedBlockingQueue;

public abstract class NodeLogger {
	
	private static String logdir;
	private static String nodeID;
	private static LogThread logThread;
	private static LinkedBlockingQueue <LogEntry> jobList = new LinkedBlockingQueue<LogEntry>();
	
	public static void setLogDir(String logdir){
		NodeLogger.logdir = logdir;
		NodeLogger.nodeID = logdir.substring(logdir.lastIndexOf("/") + 1);
	}
	
	public static void createLog(long timestamp, String content){
		
		synchronized(NodeLogger.class){
			if(logThread == null){
				logThread = new LogThread(logdir, nodeID);
				logThread.start();
			}
		}
		
		try {
			jobList.put(new LogEntry(timestamp, content));
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
	}
	
	public static void cleanup(){
		if(logThread != null)
			logThread.exit();
	}
	
	private static class LogEntry{
		private long timestamp;
		private String content;
		
		public LogEntry(long timestamp, String content){
			this.timestamp = timestamp;
			this.content = content;
		}
		
		public long getTimestamp(){
			return timestamp;
		}
		
		public String getContent(){
			return content;
		}
	}
	
	private static class LogThread extends Thread{
		
		private boolean exit = false;
		
		private String logdir;
		private String nodeID;
		private DateFormat df;

		public LogThread(String logdir, String nodeID){
			this.logdir = logdir;
			this.nodeID = nodeID;
			this.df = new SimpleDateFormat("yyyyMMdd_HHmmss.SSS");
		}
		
		@Override
		public void run(){
			
			try {
				while(!exit){

					LogEntry logEntry;
					logEntry = jobList.take();

					File f = new File(logdir + "/" + df.format(new Date(logEntry.getTimestamp())) + "_" + nodeID + ".log");

					try{

						f.createNewFile();

						BufferedWriter bw = new BufferedWriter(new FileWriter(f));
						bw.write(logEntry.getContent());
						bw.close();

					} catch (IOException e) {
						e.printStackTrace();
					}

				}
			} catch (InterruptedException e1) {
				//proceed with the shutdown of the thread
			} finally {
				logThread = null;
			}
		}
		
		public void exit(){
			this.exit = true;
			this.interrupt();
		}
	}
	
}
