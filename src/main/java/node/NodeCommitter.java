package node;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by Fabian on 20.12.2014.
 */
public class NodeCommitter {
    private String[] IPs;
    private int[] ports;
    private int nodesLeftToMessage;
    private int share;
    private boolean ok = true; //true if there are no nodes which have indicated that they need more than [share] resources
    private final Object messengerWaitLock = new Object();
    private final Object committerWaitLock = new Object();
    private ExecutorService threadPool = Executors.newCachedThreadPool();

    public NodeCommitter(String[] IPs, int[] ports, int share){
        this.IPs = IPs;
        this.ports = ports;
        this.nodesLeftToMessage = IPs.length;
        this.share = share;
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
                PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                writer.println("!share " + NodeCommitter.this.share);
                String response = reader.readLine();
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
                        }
                    }
                }
                if(ok){
                    writer.println("!commit");
                }
                else{
                    writer.println("!rollback");
                }

            } catch (IOException e) {
                e.printStackTrace(); //TODO: handle unreachable nodes etc
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
