package cli;

import java.io.IOException;

public interface Channel {
	public String receiveMessageLine() throws IOException;
	public void sendMessageLine(String msg) throws IOException;
}
