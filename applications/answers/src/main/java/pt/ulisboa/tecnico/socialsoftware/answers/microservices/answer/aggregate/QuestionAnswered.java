package pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;

import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuestionAnsweredDto;

@Entity
public class QuestionAnswered {
    @Id
    @GeneratedValue
    private Long id;
    private Integer sequence;
    private Integer key;
    private Integer timeTaken;
    private boolean correct;
    private Integer questionAggregateId;
    private Integer questionVersion;
    private AggregateState state;
    @OneToOne
    private Answer answer;

    public QuestionAnswered() {
        this.state = AggregateState.ACTIVE;
    }

    public QuestionAnswered(QuestionAnsweredDto questionAnsweredDto) {
        setSequence(questionAnsweredDto.getSequence());
        setKey(questionAnsweredDto.getKey());
        setTimeTaken(questionAnsweredDto.getTimeTaken());
        setCorrect(questionAnsweredDto.getCorrect());
        setQuestionAggregateId(questionAnsweredDto.getQuestionAggregateId());
        setQuestionVersion(questionAnsweredDto.getQuestionVersion());
        setState(questionAnsweredDto.getState());
    }

    public QuestionAnswered(QuestionAnswered other) {
        setKey(other.getKey());
        setTimeTaken(other.getTimeTaken());
        setCorrect(other.getCorrect());
        setQuestionAggregateId(other.getQuestionAggregateId());
        setQuestionVersion(other.getQuestionVersion());
        setState(other.getState());
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

    public boolean getCorrect() {
        return correct;
    }

    public void setCorrect(boolean correct) {
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

    public AggregateState getState() {
        return state;
    }

    public void setState(AggregateState state) {
        this.state = state;
    }

    public Answer getAnswer() {
        return answer;
    }

    public void setAnswer(Answer answer) {
        this.answer = answer;
    }


    public QuestionAnsweredDto buildDto() {
        QuestionAnsweredDto dto = new QuestionAnsweredDto();
        dto.setSequence(getSequence());
        dto.setKey(getKey());
        dto.setTimeTaken(getTimeTaken());
        dto.setCorrect(getCorrect());
        dto.setQuestionAggregateId(getQuestionAggregateId());
        dto.setQuestionVersion(getQuestionVersion());
        dto.setState(getState());
        return dto;
    }
}