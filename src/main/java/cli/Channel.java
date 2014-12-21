package cli;

import java.io.IOException;

public interface Channel {
	public String receiveMessageLine();
	public void sendMessageLine(String msg);
}
