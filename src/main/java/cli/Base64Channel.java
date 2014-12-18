package cli;
import org.bouncycastle.util.encoders.Base64;

public class Base64Channel extends ChannelDecorator{
	public Base64Channel(Channel channelToBeD) {
		super(channelToBeD);
	}
	
	@Override
	public String receiveMessageLine() {
		String msg = super.receiveMessageLine();
		return Base64.decode(msg).toString();
	}
	
	@Override
	public void sendMessageLine(String msg) {
		super.sendMessageLine(Base64.encode(msg.getBytes()).toString());
	}
}
