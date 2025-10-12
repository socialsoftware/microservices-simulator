package pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.OneToOne;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;

@Entity
public class AnswerQuestion {
    @Id
    @GeneratedValue
    private Long id;
    private Integer questionOptionSequenceChoice;
    private Integer questionAggregateId;
    private Integer questionVersion;
    private Integer questionTimeTaken;
    private Integer questionOptionKey;
    private Boolean questionCorrect;
    private AggregateState questionState;
    @OneToOne
    private Answer answer;

    public AnswerQuestion() {
    }

    public AnswerQuestion(QuestionDto questionDto) {
        setQuestionOptionSequenceChoice(questionDto.getQuestionOptionSequenceChoice());
        setQuestionAggregateId(questionDto.getQuestionAggregateId());
        setQuestionVersion(questionDto.getQuestionVersion());
        setQuestionTimeTaken(questionDto.getQuestionTimeTaken());
        setQuestionOptionKey(questionDto.getQuestionOptionKey());
        setQuestionCorrect(questionDto.getQuestionCorrect());
        setQuestionState(questionDto.getQuestionState());
    }

    public AnswerQuestion(AnswerQuestion other) {
        setQuestionOptionSequenceChoice(other.getQuestionOptionSequenceChoice());
        setQuestionAggregateId(other.getQuestionAggregateId());
        setQuestionVersion(other.getQuestionVersion());
        setQuestionTimeTaken(other.getQuestionTimeTaken());
        setQuestionOptionKey(other.getQuestionOptionKey());
        setQuestionCorrect(other.getQuestionCorrect());
        setQuestionState(other.getQuestionState());
    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getQuestionOptionSequenceChoice() {
        return questionOptionSequenceChoice;
    }

    public void setQuestionOptionSequenceChoice(Integer questionOptionSequenceChoice) {
        this.questionOptionSequenceChoice = questionOptionSequenceChoice;
    }

    public Integer getQuestionAggregateId() {
        return questionAggregateId;
    }

    public void setQuestionAggregateId(Integer questionAggregateId) {
        this.questionAggregateId = questionAggregateId;
    }

    public Integer getQuestionVersion() {
        return questionVersion;
    }

    public void setQuestionVersion(Integer questionVersion) {
        this.questionVersion = questionVersion;
    }

    public Integer getQuestionTimeTaken() {
        return questionTimeTaken;
    }

    public void setQuestionTimeTaken(Integer questionTimeTaken) {
        this.questionTimeTaken = questionTimeTaken;
    }

    public Integer getQuestionOptionKey() {
        return questionOptionKey;
    }

    public void setQuestionOptionKey(Integer questionOptionKey) {
        this.questionOptionKey = questionOptionKey;
    }

    public Boolean getQuestionCorrect() {
        return questionCorrect;
    }

    public void setQuestionCorrect(Boolean questionCorrect) {
        this.questionCorrect = questionCorrect;
    }

    public AggregateState getQuestionState() {
        return questionState;
    }

    public void setQuestionState(AggregateState questionState) {
        this.questionState = questionState;
    }

    public Answer getAnswer() {
        return answer;
    }

    public void setAnswer(Answer answer) {
        this.answer = answer;
    }


}