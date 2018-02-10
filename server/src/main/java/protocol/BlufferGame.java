package protocol;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;

import input.JsonData;
import input.Question;
import tokenizer.StringMessage;

public class BlufferGame implements Game{
	private ConcurrentHashMap<ProtocolCallback<StringMessage>,Integer> score_Table;
	private ConcurrentHashMap<ProtocolCallback<StringMessage>, Integer> current_Round_Score;
	private JsonData input =new JsonData();
	private ConcurrentHashMap<ProtocolCallback<StringMessage>, String> Players_And_Nicks;
	private LinkedList<Question> All_Questions;
	private Question[] questions;
	private int question_Counter;
	private int players_Count;
	private String[] possible_Answers;
	private ConcurrentHashMap<Integer, ProtocolCallback<StringMessage>> bluffs_And_Players;
	private ConcurrentHashMap<ProtocolCallback<StringMessage>, Integer> players_And_Answers; 
	private int current_Answer_Index;
	private boolean close;
	private boolean txtPhase;
	String summary ;

	public BlufferGame(ConcurrentHashMap<ProtocolCallback<StringMessage>, String> Players_And_Nicks) throws IOException
	{
		players_And_Answers = new ConcurrentHashMap<ProtocolCallback<StringMessage>, Integer>();
		current_Round_Score = new ConcurrentHashMap<ProtocolCallback<StringMessage>, Integer>();
		score_Table = new ConcurrentHashMap<ProtocolCallback<StringMessage>, Integer>(); 
		this.Players_And_Nicks = Players_And_Nicks;
		this.input = input.readInput();
		this.All_Questions = input.getAllQuestions();
		questions = new Question[3];
		question_Counter =0;
		players_Count= Players_And_Nicks.size();
		summary = "GAMEMSG Summary: ";
		possible_Answers = new String[players_Count+1];
		close = false;
		txtPhase = false;

		for(int i =0; i < players_Count+1 ; i++)
			possible_Answers[i] = null;

		bluffs_And_Players = new ConcurrentHashMap<Integer, ProtocolCallback<StringMessage>>();
		current_Answer_Index=0;

		for(ProtocolCallback<StringMessage> p: Players_And_Nicks.keySet()){
			score_Table.put(p, 0);
			current_Round_Score.put(p, 0);

		}
	}




	@Override
	public String getName() {
		return "bluffer";
	}
	
	/**
	 * this is the bluffer game message processing ,
	 * either TXTRESP messages or SELECTRESP messages
	 */
	public synchronized void process(StringMessage msg, ProtocolCallback<StringMessage> sender)
	{

		String[] split_msg = msg.getMessage().split(" ", 2);
		String command = split_msg[0];
		String argument = split_msg[split_msg.length-1];


		if(command.equals("TXTRESP"))
			processTXTRESP(msg, sender);

		if(command.equals("SELECTRESP"))
			processSELECTRESP(msg, sender);
		
	}

	
	/**
	 * processes TXTRESP messages
	 * @param msg
	 * @param sender
	 */
	public void processTXTRESP(StringMessage msg, ProtocolCallback<StringMessage> sender)
	{
		String[] split_msg = msg.getMessage().split(" ", 2);
		String command = split_msg[0];
		String argument = split_msg[split_msg.length-1];
		if (!txtPhase){
			try {
				sender.sendMessage(new StringMessage("SYSMSG "+command+ " REJECTED select your answer first"));
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
		else{

			try {
				sender.sendMessage(new StringMessage("SYSMSG "+command+ " ACCEPTED"));
			} catch (IOException e) {
				e.printStackTrace();
			}

			System.out.println("num of players is "+Players_And_Nicks.size());
			System.out.println("player count "+players_Count);
			players_Count--;
			System.out.println("player count "+players_Count);
			boolean inserted = false;

			while(!inserted){
				int random = (int)(Math.random()*(possible_Answers.length));
				System.out.println(possible_Answers[random]);
				if(possible_Answers[random] == null)
				{
					bluffs_And_Players.put(random, sender);
					possible_Answers[random] = argument;
					inserted = true;
				}
			}

			if(players_Count == 0)
			{
				System.out.println("about to send askchices");
				players_Count = Players_And_Nicks.size();
				sendASKCHOICES();

			}
		}
	}

	
	/**
	 * precesses SELECTRESP messages
	 * @param msg
	 * @param sender
	 */
	public void processSELECTRESP(StringMessage msg, ProtocolCallback<StringMessage> sender)
	{
		String[] split_msg = msg.getMessage().split(" ", 2);
		String command = split_msg[0];
		String argument = split_msg[split_msg.length-1];

		if (txtPhase){
			try {
				sender.sendMessage(new StringMessage("SYSMSG "+command+ " REJECTED answer your bluff first"));
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
		else if (Integer.parseInt(argument)>possible_Answers.length-1) {
			try {
				System.out.println();
				sender.sendMessage(new StringMessage("SYSMSG "+command+ " REJECTED your choice has to be betwin 0 to "+(possible_Answers.length-1)));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		else{ 
			try {
				sender.sendMessage(new StringMessage("SYSMSG "+command+ " ACCEPTED"));
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			players_Count--;
			int answer = Integer.parseInt(argument);
			players_And_Answers.put(sender, answer);
			if(answer == current_Answer_Index)
			{
				int previous_score = current_Round_Score.get(sender);
				//int previous_score_table = score_Table.get(sender);
				current_Round_Score.put(sender, previous_score+10);
				//score_Table.put(sender, previous_score_table+10);
			}

			else
			{
				ProtocolCallback<StringMessage> lier = bluffs_And_Players.get(answer);
				int previous_score = current_Round_Score.get(lier);
				current_Round_Score.put(lier, previous_score+5);
			}

			//everybody had sent theyre selected answers
			if(players_Count == 0 )
			{
				question_Counter++;
				players_Count = Players_And_Nicks.size();
				
				for(ProtocolCallback<StringMessage> player : Players_And_Nicks.keySet())
				{
					int previous_score = score_Table.get(player);
					int round_score = current_Round_Score.get(player);
					score_Table.put(player, previous_score + round_score);

					if(players_And_Answers.get(player) == current_Answer_Index){
						try {
							player.sendMessage(new StringMessage("GAMEMSG correct! +"+round_score+" pts"));
						} catch (IOException e) {
							e.printStackTrace();
						}
					}

					else
					{
						try {
							player.sendMessage(new StringMessage("GAMEMSG wrong! +"+round_score+" pts"));
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
					//reset the round score for the next question
					current_Round_Score.put(player, 0);	
				}
				//reset the possible answers for the next round
				for(int i =0; i < players_Count+1 ; i++)
					possible_Answers[i] = null;

				if(question_Counter < 3)
					sendASKTXT(question_Counter);
			}

			if(question_Counter == 3){
				for(ProtocolCallback<StringMessage> p :Players_And_Nicks.keySet())
					summary = summary + Players_And_Nicks.get(p)+": "+score_Table.get(p)+",";

				for(ProtocolCallback<StringMessage> p :Players_And_Nicks.keySet())
					try {
						p.sendMessage(new StringMessage(summary));
					} catch (IOException e) {
						e.printStackTrace();
					}
				close = true;
			}
		}
	}

	
	/**
	 * this method initializes the game, and sends the first question
	 */
	public void startGame() {
		System.out.println("starting game");

		Collections.shuffle(All_Questions);

		for(int i = 0 ; i < 3 ; i++)
		{
			questions[i] = All_Questions.get(i);
		}
		sendASKTXT(question_Counter);



	}

	/**
	 * this method send the current question to all the players in the game
	 * @param index the number of question to send
	 */
	private void sendASKTXT(int index)
	{
		txtPhase=true;
		int real_index = (int)(Math.random()*(possible_Answers.length));
		possible_Answers[real_index] = questions[index].getRealAnswer();
		current_Answer_Index = real_index;
		System.out.println("sending ASKTXT");
		for(ProtocolCallback<StringMessage> p : Players_And_Nicks.keySet())
		{
			try {
				p.sendMessage(new StringMessage("ASKTXT "+questions[index].getQuestionText()));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * this method sends the choices, the player has to choose his answer from it
	 */
	private void sendASKCHOICES()
	{
		txtPhase = false;
		String choices = "";
		for(int i =0; i < possible_Answers.length ; i++)
		{
			choices = choices + " "+ i + ". "+possible_Answers[i];
		}

		for(ProtocolCallback<StringMessage> p : Players_And_Nicks.keySet()){
			try {
				p.sendMessage(new StringMessage("ASKCHOICES "+choices));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}
	
	
	public boolean shouldClose() {
		return close;
	}






}
