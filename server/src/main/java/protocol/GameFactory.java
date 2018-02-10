package protocol;

import java.io.IOException;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;

import tokenizer.StringMessage;

public class GameFactory {

	public Game create(String gameType , ConcurrentHashMap<ProtocolCallback<StringMessage>, String> players) throws IOException
	{
		if(gameType.equals("BLUFFER"))
			return new BlufferGame(players);
		return null;
	}
}
