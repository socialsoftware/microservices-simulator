package pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuestionAnsweredDto;

@Entity
public class QuestionAnswered {
    @Id
    @GeneratedValue
    private Long id;
    private Integer sequence;
    private Integer key;
    private Integer timeTaken;
    private Boolean correct;
    private Integer questionAggregateId;
    @OneToOne
    private Answer answer;

    public QuestionAnswered() {

    }

    public QuestionAnswered(QuestionAnsweredDto questionansweredDto) {
        setSequence(questionansweredDto.getSequence());
        setKey(questionansweredDto.getKey());
        setTimeTaken(questionansweredDto.getTimeTaken());
        setCorrect(questionansweredDto.getCorrect());
        setQuestionAggregateId(questionansweredDto.getQuestionAggregateId());
    }

    public QuestionAnswered(QuestionAnswered other) {
        setKey(other.getKey());
        setTimeTaken(other.getTimeTaken());
        setCorrect(other.getCorrect());
        setQuestionAggregateId(other.getQuestionAggregateId());
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

    public Answer getAnswer() {
        return answer;
    }

    public void setAnswer(Answer answer) {
        this.answer = answer;
    }


}