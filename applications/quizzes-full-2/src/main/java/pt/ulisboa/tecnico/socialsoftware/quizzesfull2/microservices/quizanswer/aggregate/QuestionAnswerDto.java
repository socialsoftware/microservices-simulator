package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quizanswer.aggregate;

public class QuestionAnswerDto {
    private Integer questionAggregateId;
    private Long questionVersion;
    private Integer correctOptionKey;
    private Integer optionSequenceChoice;
    private Integer optionKey;
    private Boolean correct;
    private Integer timeTaken;

    public QuestionAnswerDto() {
    }

    public QuestionAnswerDto(Integer questionAggregateId, Long questionVersion, Integer correctOptionKey,
                             Integer optionSequenceChoice, Integer optionKey, Boolean correct, Integer timeTaken) {
        this.questionAggregateId = questionAggregateId;
        this.questionVersion = questionVersion;
        this.correctOptionKey = correctOptionKey;
        this.optionSequenceChoice = optionSequenceChoice;
        this.optionKey = optionKey;
        this.correct = correct;
        this.timeTaken = timeTaken;
    }

    public QuestionAnswerDto(QuestionAnswer questionAnswer) {
        this.questionAggregateId = questionAnswer.getQuestionAggregateId();
        this.questionVersion = questionAnswer.getQuestionVersion();
        this.correctOptionKey = questionAnswer.getCorrectOptionKey();
        this.optionSequenceChoice = questionAnswer.getOptionSequenceChoice();
        this.optionKey = questionAnswer.getOptionKey();
        this.correct = questionAnswer.getCorrect();
        this.timeTaken = questionAnswer.getTimeTaken();
    }

    public Integer getQuestionAggregateId() {
        return questionAggregateId;
    }

    public void setQuestionAggregateId(Integer questionAggregateId) {
        this.questionAggregateId = questionAggregateId;
    }

    public Long getQuestionVersion() {
        return questionVersion;
    }

    public void setQuestionVersion(Long questionVersion) {
        this.questionVersion = questionVersion;
    }

    public Integer getCorrectOptionKey() {
        return correctOptionKey;
    }

    public void setCorrectOptionKey(Integer correctOptionKey) {
        this.correctOptionKey = correctOptionKey;
    }

    public Integer getOptionSequenceChoice() {
        return optionSequenceChoice;
    }

    public void setOptionSequenceChoice(Integer optionSequenceChoice) {
        this.optionSequenceChoice = optionSequenceChoice;
    }

    public Integer getOptionKey() {
        return optionKey;
    }

    public void setOptionKey(Integer optionKey) {
        this.optionKey = optionKey;
    }

    public Boolean getCorrect() {
        return correct;
    }

    public void setCorrect(Boolean correct) {
        this.correct = correct;
    }

    public Integer getTimeTaken() {
        return timeTaken;
    }

    public void setTimeTaken(Integer timeTaken) {
        this.timeTaken = timeTaken;
    }
}
