package marcos.games.hexes.swing;

import java.util.ArrayList;

public class MultiPlayerClient {

	public enum ResultStatus {
		STATUS_OK,
		STATUS_FAILED,
		STATUS_CANCELED,
	}
	
	public static class User {
		public User(String name) {
			this.name = name;
		}
		public final String name;
		boolean waitingForGame;
	}
	
	public static interface Listener {
		/**
		 * 
		 * @param cmd
		 * @param params
		 * @return
		 */
		boolean onCommandReceived(String cmd, Object ... params);
	}
	
	public MultiPlayerClient(Listener listener) {
		
	}
	
	public void disconnect() {
		// TODO Auto-generated method stub
		
	}

	/**
	 * Generalized callback interface
	 * @author chriscaron
	 *
	 * @param <T>
	 */
	public static interface Callback<T> {
		void complete(ResultStatus status, String statusMsg, T...params);
	}

	int tries = 0;
	
	public void connect(final String userName, String passWord, final Callback<Void> cb) {
		new Thread() {
			public void run() {
				try {
					Thread.sleep(2000);
					switch (tries++ % 3) {
						case 0: 
							cb.complete(ResultStatus.STATUS_FAILED, "No connection");
							break;
							
						case 1:
							cb.complete(ResultStatus.STATUS_FAILED, "Unknown user " + userName);
							break;
							
						case 2:
							cb.complete(ResultStatus.STATUS_OK, "");
							break;
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}.start();
	}

	public User chooseRandomUserForNewGame() {
		switch (tries++ % 3) {
			case 0:
				return null;
				
			default:
				return new User("simon");
		}
	}
	
	public void sendCommand(String cmd, Object ... params) {
		
	}

	private ArrayList<Listener> listeners = new ArrayList<MultiPlayerClient.Listener>();
	
	/**
	 * Add a listener.  Most recent listeners get first chance to respond to events.  Returning true from
	 * a callback will consume the event and prevent other listeners from handling.
	 * @param listener
	 */
	public void addListener(Listener listener) {
		if (!listeners.contains(listener))
			listeners.add(listener);
	}
	
	public final void removeListener(Listener listener) {
		listeners.remove(listener);
	}
}
