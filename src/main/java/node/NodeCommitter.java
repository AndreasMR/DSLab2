package node;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.security.Key;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import util.Config;
import util.Keys;
import cli.Base64Channel;
import cli.Channel;
import cli.HmacChannel;
import cli.TcpChannel;

/**
 * Created by Fabian on 20.12.2014.
 */
public class NodeCommitter {
    private String[] IPs;
    private int[] ports;
    private int nodesLeftToMessage;
    private int share;
    private boolean ok = true; //true if there are no nodes which have indicated that they need more than [share] resources
    private final Object messengerWaitLock = new Object(); //lock object for the NodeMessenger objects to wait for other NodeMessengers between the first and second stage of the protocol
    private final Object committerWaitLock = new Object(); //lock object for the node committer to wait while the NodeMessenger objects communicate with the other nodes
    private ExecutorService threadPool = Executors.newCachedThreadPool();
    private Key secret_key;

    public NodeCommitter(String[] IPs, int[] ports, int share, Config config){
        this.IPs = IPs;
        this.ports = ports;
        this.nodesLeftToMessage = IPs.length;
        this.share = share;
        try {
			this.secret_key = Keys.readSecretKey(new File(config.getString("hmac.key")));
		} catch (IOException e) {
			this.secret_key = null;
			e.printStackTrace();
		}
    }

    public boolean tryCommit(){
        int i = 0;
        while(i < IPs.length){
            NodeMessenger nodeMessenger = new NodeMessenger(IPs[i], ports[i]);
            threadPool.execute(nodeMessenger);
            i++;
        }
        synchronized (committerWaitLock){
            try {
                committerWaitLock.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        threadPool.shutdownNow();
        return ok;
    }

    public synchronized void decrementNodesLeftToMessage(){
        nodesLeftToMessage--;
    }

    private void wakeMessengers() {
        synchronized (messengerWaitLock){
            messengerWaitLock.notifyAll();
        }
    }

    private class NodeMessenger implements Runnable{
        String IP;

        int port;
        public NodeMessenger(String IP, int port){
            this.IP = IP;
            this.port = port;
        }
        @Override
        public void run() {
            Socket socket = null;
            try {
                socket = new Socket(IP, port);
//                PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
//                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                
                Channel channel = new HmacChannel(new Base64Channel(new TcpChannel(socket)), NodeCommitter.this.secret_key);
                channel.sendMessageLine("!share " + NodeCommitter.this.share);
                String response = channel.receiveMessageLine();
//                writer.println("!share " + NodeCommitter.this.share);
//                String response = reader.readLine();
                if(response.equals("!nok")){
                    ok = false;
                }
                if(nodesLeftToMessage > 0) {
                    decrementNodesLeftToMessage();
                }
                if(nodesLeftToMessage <= 0){
                    wakeMessengers();
                    synchronized (committerWaitLock){
                        committerWaitLock.notify();
                    }
                }
                else{
                    synchronized (messengerWaitLock){
                        try {
                            messengerWaitLock.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            return;
                        }
                    }
                }
                if(ok){
                	channel.sendMessageLine("!commit");
//                    writer.println("!commit");
                }
                else{
                	channel.sendMessageLine("!rollback");
//                    writer.println("!rollback");
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
            finally{
                if(socket != null){
                    try {
                        socket.close();
                    } catch (IOException e) {}
                }
            }
        }

    }
}
