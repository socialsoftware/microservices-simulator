package pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;

import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.AnswerQuestionDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuestionDto;

@Entity
public class AnswerQuestion {
    @Id
    @GeneratedValue
    private Long id;
    private Integer sequence;
    private Integer key;
    private Integer timeTaken;
    private Boolean correct;
    private Integer questionAggregateId;
    private Integer questionVersion;
    private AggregateState questionState;
    @OneToOne
    private Answer answer;

    public AnswerQuestion() {

    }

    public AnswerQuestion(QuestionDto questionDto) {
        setQuestionAggregateId(questionDto.getAggregateId());
        setQuestionVersion(questionDto.getVersion());
        setQuestionState(questionDto.getState());
    }

    public AnswerQuestion(AnswerQuestionDto answerQuestionDto) {
        setSequence(answerQuestionDto.getSequence());
        setKey(answerQuestionDto.getKey());
        setTimeTaken(answerQuestionDto.getTimeTaken());
        setCorrect(answerQuestionDto.getCorrect());
        setQuestionAggregateId(answerQuestionDto.getAggregateId());
        setQuestionVersion(answerQuestionDto.getVersion());
        setQuestionState(answerQuestionDto.getState());
    }

    public AnswerQuestion(AnswerQuestion other) {
        setSequence(other.getSequence());
        setKey(other.getKey());
        setTimeTaken(other.getTimeTaken());
        setCorrect(other.getCorrect());
        setQuestionAggregateId(other.getQuestionAggregateId());
        setQuestionVersion(other.getQuestionVersion());
        setQuestionState(other.getQuestionState());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getSequence() {
        return sequence;
    }

    public void setSequence(Integer sequence) {
        this.sequence = sequence;
    }

    public Integer getKey() {
        return key;
    }

    public void setKey(Integer key) {
        this.key = key;
    }

    public Integer getTimeTaken() {
        return timeTaken;
    }

    public void setTimeTaken(Integer timeTaken) {
        this.timeTaken = timeTaken;
    }

    public Boolean getCorrect() {
        return correct;
    }

    public void setCorrect(Boolean correct) {
        this.correct = correct;
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

    public Answer getAnswer() {
        return answer;
    }

    public void setAnswer(Answer answer) {
        this.answer = answer;
    }




    public AnswerQuestionDto buildDto() {
        AnswerQuestionDto dto = new AnswerQuestionDto();
        dto.setSequence(getSequence());
        dto.setKey(getKey());
        dto.setTimeTaken(getTimeTaken());
        dto.setCorrect(getCorrect());
        dto.setAggregateId(getQuestionAggregateId());
        dto.setVersion(getQuestionVersion());
        dto.setState(getQuestionState());
        return dto;
    }
}