package protocol;

import java.io.IOException;
import java.util.LinkedList;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import tokenizer.StringMessage;

/**
 * a simple implementation of the server protocol interface
 */
public class TBGPProtocol implements AsyncServerProtocol<StringMessage> {

	private boolean _shouldClose = false;
	private boolean _connectionTerminated = false;
	private ConcurrentHashMap<ProtocolCallback<StringMessage>, String> Callbacks_And_Nicks;
	private ConcurrentHashMap<String, LinkedBlockingQueue<ProtocolCallback<StringMessage>>> Rooms_And_Callbacks;
	private ConcurrentHashMap<String, Game> Rooms_And_Games;
	private LinkedList<String> Available_Games;
	private GameFactory gameFactory = new GameFactory();
	
	
	
	private  TBGPProtocol() {
		Callbacks_And_Nicks = new ConcurrentHashMap<ProtocolCallback<StringMessage>, String>();
		Rooms_And_Callbacks = new ConcurrentHashMap<String, LinkedBlockingQueue<ProtocolCallback<StringMessage>>>();
		Rooms_And_Games = new ConcurrentHashMap<String, Game>();
		Available_Games = new LinkedList<String>();
		Available_Games.add("BLUFFER");
		
	}
	
	private static class TBGPProtocolHolder
	{
		private static TBGPProtocol instance = new TBGPProtocol();
	}
	
	public static TBGPProtocol getInstance()
	{
		return TBGPProtocolHolder.instance;
	}
	/**
	 * processes a message<BR>
	 * this simple interface prints the message to the screen, then composes a simple
	 * reply and sends it back to the client
	 *
	 * @param msg the message to process
	 * @return the reply that should be sent to the client, or null if no reply needed
	 * @throws IOException 
	 */
	@Override
	public void processMessage(StringMessage msg, ProtocolCallback<StringMessage> callback) throws IOException {        
		String[] split_msg = msg.getMessage().split(" ", 2);
		String command = split_msg[0];
		String argument = split_msg[split_msg.length-1];
		boolean exists =false;

		//NICK MESSAGE
		if(command.equals("NICK"))
		{
			exists= NickExists(argument);
					
			if(!exists)
			{	
				Callbacks_And_Nicks.put(callback, argument);
			
					callback.sendMessage(new StringMessage("SYSMSG NICK ACCEPTED"));
			}
			//the nick is already in use
			else
			{
					callback.sendMessage(new StringMessage("SYSMSG REJECTED nick already taken, try again"));
			}
		}
		
		
		
		//JOIN MESSAGE
		else if(command.equals("JOIN"))
		{
			if (!registered(callback))
				callback.sendMessage(new StringMessage("SYSMSG REJECTED you have to sign in with NICK command first"));
			else if (Rooms_And_Games.containsKey(argument))
				callback.sendMessage(new StringMessage("SYSMSG REJECTED this room is in a middle of a game try later.."));
			else if (gameStarted(callback))
				callback.sendMessage(new StringMessage("SYSMSG REJECTED not in the middle of the game dude..."));
			else {
				if (getRoom(callback) != null)
					Rooms_And_Callbacks.get(getRoom(callback)).remove(callback);
				joinRoom(argument, callback);
				callback.sendMessage(new StringMessage("SYSMSG JOIN ACCEPTED"));
			}
		}
		
		
		//MSG MESSAGE
		else if(command.equals("MSG"))
		{
			//Can't send message to others if you are not registered yet
			if (!(registered(callback)))
				callback.sendMessage(new StringMessage("SYSMSG REJECTED you have to sign in with NICK command first"));
			//Can't send messages to others if you are not in a certain room
			else if (getRoom(callback) == null) 
				callback.sendMessage(new StringMessage("SYSMSG REJECTED you are not in a room"));
			//your are registered and in a room, ok
			else{
				callback.sendMessage(new StringMessage("SYSMSG ACCEPTED"));
				sendUSRMSG(argument, callback);
			}
		}
		
		
		//LISTGAMES MESSAGE
		else if(command.equals("LISTGAMES"))
		{
			//go over the available games and send the options to the requester
				String games="";
				for(String g : Available_Games)
					games = games + " " + g; 
				
				callback.sendMessage(new StringMessage("SYSMSG LISTGAMES ACCEPTED "+games));
				
			
		}
		
		//STARTGAME MESSAGE
		else if(command.equals("STARTGAME"))
		{
			if (argument.equals("STARTGAME"))
				callback.sendMessage(new StringMessage("SYSMSG REJECTED you have to enter what game do you want to start"));
			else if(!Available_Games.contains(argument))
				callback.sendMessage(new StringMessage("SYSMSG REJECTED "+argument+ "is not available this moment"));
			//Can't send message to others if you are not registered yet
			else if (!(registered(callback)))
				callback.sendMessage(new StringMessage("SYSMSG REJECTED you have to sign in with NICK command first"));
			
			//Can't send messages to others if you are not in a certain room
			else if (getRoom(callback) == null) 
				callback.sendMessage(new StringMessage("SYSMSG REJECTED you are not in a room"));
			
			else if(gameStarted(callback))
				callback.sendMessage(new StringMessage("SYSMSG REJECTED the game is already in process.."));
			//your are registered and in a room, ok
			else {
				String room = getRoom(callback);
				ConcurrentHashMap<ProtocolCallback<StringMessage> , String> players = new ConcurrentHashMap<ProtocolCallback<StringMessage> , String>();
				for(ProtocolCallback<StringMessage> p : Rooms_And_Callbacks.get(room))
				{
					players.put(p, Callbacks_And_Nicks.get(p));
				} 
				
				Game game = gameFactory.create(argument, players);
				Rooms_And_Games.put(room, game);
				
				callback.sendMessage(new StringMessage("SYSMSG STARTGAME ACCEPTED, starting game: "+game.getName()));
				game.startGame();
			}
			
		}
		
		
		//TXTRESP OR SELECTRESP MESSAGES
		else if(command.equals("TXTRESP") ||command.equals("SELECTRESP"))
		{
			if (!gameStarted( callback))
				callback.sendMessage(new StringMessage("SYSMSG REJECTED you have to sign in with NICK,JOIN and STARTGAME  commands first "));
			else {
			Rooms_And_Games.get(getRoom(callback)).process(msg, callback);
			}
		}
		
		
		
		/*else if (this._connectionTerminated) {
			return null;
		}*/
		
		//QUIT MESSAGE
		else if (this.isEnd(msg)) {
			if (!gameStarted(callback)){
				callback.sendMessage(new StringMessage("SYSMSG QUIT ACCEPTED"));
				if(getRoom(callback) != null)
					Rooms_And_Callbacks.get(getRoom(callback)).remove(callback);
				if (Callbacks_And_Nicks.containsKey(callback)) 	
					Callbacks_And_Nicks.remove(callback);
			}
			else
				callback.sendMessage(new StringMessage("SYSMSG QUIT REJECTED not in the middle of the game dude..."));

		}
		
		//UNKNOWN COMMANDS
		else
		{
			callback.sendMessage(new StringMessage("SYSMSG UNIDENTIFIED bad command, try again"));
		}
		
		//check if any of the games is over
		for (String room : Rooms_And_Games.keySet())
		{
			if(Rooms_And_Games.get(room).shouldClose())
				Rooms_And_Games.remove(room);
		}
		
		
	}
	
	
	
	
	/**
	 * this method sends USRMSG to everyone in the sender's room
	 * @param argument the message the user wants to send
	 * @param callback the callback of the sender of the message
	 */
	private void sendUSRMSG(String argument, ProtocolCallback<StringMessage> callback) {
		String room = getRoom(callback);
		String name = Callbacks_And_Nicks.get(callback);
		for(ProtocolCallback<StringMessage> c : (Rooms_And_Callbacks.get(room)) ){
			//dont send the message to yourself
			if(c != callback){
				try {
					c.sendMessage(new StringMessage("USRMSG "+name+": "+argument));
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
	}
	
	/**
	 * checks if the player who sent the msg is in an active game
	 * @param callback callback the player's callback
	 * @return true if the player is in an active game, false otherwise
	 */
	private boolean gameStarted( ProtocolCallback<StringMessage> callback) {
		if (Rooms_And_Games.isEmpty()) {
			return false;
		}
		else
			return Rooms_And_Games.containsKey(getRoom(callback));
	}
	
	
	
	/**
	 * 
	 * @param callback  the player's callback
	 * @return
	 */
	private String getRoom(ProtocolCallback<StringMessage> callback)
	{
		for(LinkedBlockingQueue<ProtocolCallback<StringMessage>> queue : Rooms_And_Callbacks.values()){
			if(queue.contains(callback))
			{
				for(String room : Rooms_And_Callbacks.keySet())
				{
					if(Rooms_And_Callbacks.get(room) == queue)
						return room;
				}
			}	
		}
		return null;
	}

	
	/**
	 * detetmine whether the given message is the termination message
	 *
	 * @param msg the message to examine
	 * @return false - this simple protocol doesn't allow termination...
	 */
	@Override
	public boolean isEnd(StringMessage msg) {
		return msg.equals("QUIT");
	}

	/**
	 * Is the protocol in a closing state?.
	 * When a protocol is in a closing state, it's handler should write out all pending data, 
	 * and close the connection.
	 * @return true if the protocol is in closing state.
	 */
	@Override
	public boolean shouldClose() {
		return this._shouldClose;
	}

	/**
	 * Indicate to the protocol that the client disconnected.
	 */
	@Override
	public void connectionTerminated() {
		this._connectionTerminated = true;
	}
	
	/**
	 * this method checks if the specified nickname is taken
	 * @param argument the nickname we want to check exists
	 * @return true if there is already a registered player with the specified nickname
	 */
	public boolean NickExists(String argument) {
		boolean exists = false;
		for(String s : Callbacks_And_Nicks.values())
		{
			if(argument.equals(s)){
				exists=true;
				break;
			}
		}
		return exists;
		
	}
	
	/**
	 * this method check if the player had successfully registered with a unique nickname
	 * @param callback the player's callback
	 * @return true if the player is registered
	 */
	public boolean registered(ProtocolCallback<StringMessage> callback)
	{
		return Callbacks_And_Nicks.containsKey(callback);
	}
	
	
	/**
	 * this method adds the given {@code callback} to the specified room {@code argument}
	 * @param argument the room to join
	 * @param callback the player's callback
	 */
	public void joinRoom(String argument,ProtocolCallback<StringMessage> callback )
	{
		boolean joinedRoom=false;
		if(!Rooms_And_Callbacks.isEmpty()){
			for(String room : Rooms_And_Callbacks.keySet())
			{
				if(argument.equals(room)){
					try {
						Rooms_And_Callbacks.get(room).put(callback);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					joinedRoom = true;
				}
				if(joinedRoom)
					break;

			}
		}
		
		
		//if no room exists with the specified game, create one
		if(!joinedRoom){
			Rooms_And_Callbacks.put(argument, new LinkedBlockingQueue<ProtocolCallback<StringMessage>>());
			try {
				Rooms_And_Callbacks.get(argument).put(callback);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
			
	}
	
	

}
