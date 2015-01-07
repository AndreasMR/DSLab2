package cli;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;

import org.bouncycastle.util.encoders.Base64;

public class HmacChannel extends ChannelDecorator{
	
	private Mac hMac;
	
	public HmacChannel(Channel channelToBeD, Key secret_hmac_key) {
		super(channelToBeD);

		try{
			hMac = Mac.getInstance("HmacSHA256");
			hMac.init(secret_hmac_key);

		} catch (NoSuchAlgorithmException e1) {
			e1.printStackTrace();
		}  catch (InvalidKeyException e1) {
			e1.printStackTrace();
		}

	}
	
	@Override
	public String receiveMessageLine() throws IOException{
				
		String msg = super.receiveMessageLine();
		
		if (msg != null){
			String[]parts = msg.split("\\s+");
			
			byte[] receivedHash = Base64.decode(parts[0].getBytes());
			String msg_term = msg.substring( parts[0].length() + 1, msg.length() );
			
			// MESSAGE is the message to sign in bytes
			hMac.update(msg_term.getBytes());
			byte[] computedHash = hMac.doFinal();
			if(!MessageDigest.isEqual(computedHash, receivedHash)){
				throw new TamperedException(msg_term);
			}

			return msg_term;

		} else {
			return null;
		}
	}
	
	public byte[] receiveMessageLineInBytes() throws IOException{
		
		String msg = this.receiveMessageLine();

		if (msg != null)
			return msg.getBytes();
		else
			return null;
	}
	
	@Override
	public void sendMessageLine(String msg) throws IOException{
		
		this.sendMessageLineInBytes(msg.getBytes());
		
	}
	
	public void sendMessageLineInBytes(byte[] msg) throws IOException{
		
		hMac.update(msg);
		byte[] computedHash = Base64.encode(hMac.doFinal());
		byte[] hmac_term = new byte[computedHash.length + msg.length+1];
		int q;
		for (q = 0; q < computedHash.length; q++){
			hmac_term[q] = computedHash[q];
		}
		hmac_term[q++] = ' ';
		for (int g = 0; g < msg.length; g++){
			hmac_term[q++] = msg[g];
		}
		
		super.sendMessageLine(new String(hmac_term));
	}
}
