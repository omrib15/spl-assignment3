package input;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import com.google.gson.stream.JsonReader;
/**
 * this class reads the json file input into one object
 */
public class JsonData
	{
		private LinkedList<Question> questions;
			
		public LinkedList<Question> getQuestions() {
			return questions;
		}
		public JsonData(LinkedList<Question> questions) {
		 this.questions = questions;	
		}
	public JsonData(){}	
		
	/**
     * @return jsonData , that contains the input
     */
		public JsonData readInput(InputStream in) throws IOException {
	        JsonReader reader = new JsonReader(new InputStreamReader(in, "UTF-8"));
	        try {
	        	reader.beginObject();
	            while (reader.hasNext()) {
	              String name = reader.nextName();
	              if (name.equals("questions")) {
	            	  questions = readQuestions(reader);
	            
	              } else {
	            	  
				         reader.skipValue();  
				  }
	              
	            }
	            return this;
	              
	        } finally {
	          reader.close();
	        }
	    }
	    
	    

		public Question readQuestion(JsonReader reader) throws IOException {
	    	String questionText=null;;
	    	String realAnswer = null;
	    	
	        reader.beginObject();
	        while (reader.hasNext()) {
	          String name = reader.nextName();
	          if (name.equals("questionText")) {
	        	  questionText = reader.nextString();
	          } else if (name.equals("realAnswer")) {
	        	  realAnswer = reader.nextString();
	          }  else {
	        	  reader.skipValue();
	          }
	        }
	        reader.endObject();
	        return new Question(questionText,realAnswer);
	    }
	    
	    public LinkedList<Question> readQuestions(JsonReader reader) throws IOException {
	    	LinkedList<Question> questions = new LinkedList<Question>();

	        reader.beginArray();
	        while (reader.hasNext()) {
	        	questions.add(readQuestion(reader));
	        }
	        reader.endArray();
	        return questions;
	    }
	    
	    public JsonData readInput() throws IOException{
			File init = new File("src/main/java/json.json");
			InputStream target = new FileInputStream(init);
			JsonData input = new JsonData();
			input = input.readInput(target);
			return input;
	    }
	   public LinkedList<Question> getAllQuestions() {
		   return questions;
		   
		   
	   }
	}		