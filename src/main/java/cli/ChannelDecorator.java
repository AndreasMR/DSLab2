package cli;

public abstract class ChannelDecorator implements Channel {
	protected Channel channelToBeDecorated;
	
	public ChannelDecorator(Channel channelToBeDecorated) {
		this.channelToBeDecorated = channelToBeDecorated;
	}
	
	public String receiveMessageLine() {
		return channelToBeDecorated.receiveMessageLine();
	}
	
	public void sendMessageLine(String msg) {
		channelToBeDecorated.sendMessageLine(msg);
	}
}
