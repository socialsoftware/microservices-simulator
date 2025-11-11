package pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;

import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuestionDto;

@Entity
public class AnswerQuestion {
    @Id
    @GeneratedValue
    private Long id;
    private Integer questionAggregateId;
    private Integer questionVersion;
    private AggregateState questionState;
    private Integer questionOptionSequenceChoice;
    private Integer questionTimeTaken;
    private Integer questionOptionKey;
    private Boolean questionCorrect;
    @OneToOne
    private Answer answer;

    public AnswerQuestion() {

    }

    public AnswerQuestion(QuestionDto questionDto) {
        setQuestionAggregateId(questionDto.getAggregateId());
        setQuestionOptionSequenceChoice(questionDto.getSequence());
        setQuestionOptionKey(questionDto.getOptionKey());
    }

    public AnswerQuestion(AnswerQuestion other) {
        setQuestionAggregateId(other.getQuestionAggregateId());
        setQuestionVersion(other.getQuestionVersion());
        setQuestionState(other.getQuestionState());
        setQuestionOptionSequenceChoice(other.getQuestionOptionSequenceChoice());
        setQuestionTimeTaken(other.getQuestionTimeTaken());
        setQuestionOptionKey(other.getQuestionOptionKey());
        setQuestionCorrect(other.getQuestionCorrect());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public AggregateState getQuestionState() {
        return questionState;
    }

    public void setQuestionState(AggregateState questionState) {
        this.questionState = questionState;
    }

    public Integer getQuestionOptionSequenceChoice() {
        return questionOptionSequenceChoice;
    }

    public void setQuestionOptionSequenceChoice(Integer questionOptionSequenceChoice) {
        this.questionOptionSequenceChoice = questionOptionSequenceChoice;
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

    public Answer getAnswer() {
        return answer;
    }

    public void setAnswer(Answer answer) {
        this.answer = answer;
    }


    public QuestionDto buildDto() {
        QuestionDto dto = new QuestionDto();
        dto.setAggregateId(getQuestionAggregateId());
        dto.setSequence(getQuestionOptionSequenceChoice());
        dto.setOptionKey(getQuestionOptionKey());
        return dto;
    }
}