package input;
                
public class Question
{
    private String realAnswer;

    private String questionText;
    

	public Question( String questionText,String realAnswer) {
		super();
		this.realAnswer = realAnswer;
		this.questionText = questionText;
	}
	
    public String getRealAnswer ()
    {
        return realAnswer;
    }

    public void setRealAnswer (String realAnswer)
    {
        this.realAnswer = realAnswer;
    }

    public String getQuestionText ()
    {
        return questionText;
    }

    public void setQuestionText (String questionText)
    {
        this.questionText = questionText;
    }


}