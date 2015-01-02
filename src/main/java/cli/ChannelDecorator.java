package cli;

import java.io.IOException;

public abstract class ChannelDecorator implements Channel {
	protected Channel channelToBeDecorated;
	
	public ChannelDecorator(Channel channelToBeDecorated) {
		this.channelToBeDecorated = channelToBeDecorated;
	}
	
	public String receiveMessageLine() throws IOException{
		return channelToBeDecorated.receiveMessageLine();
	}
	
	public void sendMessageLine(String msg) throws IOException {
		channelToBeDecorated.sendMessageLine(msg);
	}
}
