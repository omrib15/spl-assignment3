package protocol;

import tokenizer.StringMessage;

/**
 * an interface all text based games should implement in order to use the TBGP protocol
 * @author omribas
 *
 */
public interface Game {
	public String getName();
	public void process(StringMessage messge,ProtocolCallback<StringMessage> sender);
	public void startGame();
	public boolean shouldClose();

}
