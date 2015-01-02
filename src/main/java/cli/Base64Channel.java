package cli;
import java.io.IOException;

import org.bouncycastle.util.encoders.Base64;

public class Base64Channel extends ChannelDecorator{
	public Base64Channel(Channel channelToBeD) {
		super(channelToBeD);
	}
	
	@Override
	public String receiveMessageLine() throws IOException{
		String msg = super.receiveMessageLine();
		if (msg != null)
			return new String(Base64.decode(msg));
		else
			return null;
	}
	
	public byte[] receiveMessageLineInBytes() throws IOException{
		String msg = super.receiveMessageLine();
		if (msg != null)
			return Base64.decode(msg);
		else
			return null;
	}
	
	@Override
	public void sendMessageLine(String msg) throws IOException{
		super.sendMessageLine(new String(Base64.encode(msg.getBytes())));
	}
	
	public void sendMessageLineInBytes(byte[] msg) throws IOException{
		super.sendMessageLine(new String(Base64.encode(msg)));
	}
}
