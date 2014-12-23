package controller;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.LinkedHashMap;
import java.util.List;

import model.ComputationRequestInfo;
import admin.INotificationCallback;

/**
 * Please note that this interface is not needed for Lab 1, but will
 * later be used in Lab 2. Hence, you do not have to implement it for the
 * first submission.
 */
public interface IAdminConsole extends Remote {
	/**
	 * Registers the given {@link INotificationCallback} to be invoked if the
	 * account of a certain user has less than the given amount of credits.
	 * <p/>
	 * If the account has already less credits than the given threshold the
	 * callback is registered for, it is invoked the next time when the users
	 * credits are decreased.
	 *
	 * @param username
	 *            the name of user
	 * @param credits
	 *            the amount of credits
	 * @param callback
	 *            the callback to invoke
	 * @return {@code true} if the callback was registered successfully,
	 *         {@code false} otherwise
	 * @throws RemoteException
	 *             if a remote error occurs
	 */
	boolean subscribe(String username, int credits,
			INotificationCallback callback) throws RemoteException;

	/**
	 * Requests information about each log-file. E.g.:
	 * 
	 * </pre>
	 * 20141001_161542.169 [node1]: 5 + 20 = 25<br>
 	 * 20141001_161542.203 [node2]: 25 / 2 = 12
	 * </pre>
	 * 
	 * @return a list of {@link ComputationRequestInfo} of all requests handled
	 *         so far
	 * @throws RemoteException
	 *             if a remote error occurs
	 */
	List<ComputationRequestInfo> getLogs() throws RemoteException;

	/**
	 * Returns a statistics of the operator's frequency in mathematical terms in
	 * descending order.
	 * <p/>
	 * Descending order means that the first entry is the most used operator.
	 *
	 * @return the statistics
	 * @throws RemoteException
	 *             if a remote error occurs
	 */
	LinkedHashMap<Character, Long> statistics() throws RemoteException;

	/**
	 * Returns the public key used by the cloud controller for encrypted
	 * connections.
	 *
	 * @return the public key
	 * @throws RemoteException
	 *             if a remote error occurs
	 */
//	Key getControllerPublicKey() throws RemoteException;

	/**
	 * Sets or replaces the public key ({@code &lt;username&gt;.pub.pem}) of the
	 * given user.
	 *
	 * @param username
	 *            the name of the user
	 * @param key
	 *            the content of the public key file
	 * @throws RemoteException
	 *             if a remote error occurs
	 */
//	void setUserPublicKey(String username, byte[] key) throws RemoteException;
}
