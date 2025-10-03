package pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


public class AnswerDto implements Serializable {
    
    // Standard aggregate fields
    private Integer aggregateId;
    private Integer version;
    private String state;

    // Fields from AnswerDetails
    private Integer questionId;
    private String answer;
    private String option;
    private LocalDateTime answerDate;
    
    public AnswerDto() {
    }
    
    public AnswerDto(pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate.Answer answer) {
        // Standard aggregate fields
        setAggregateId(answer.getAggregateId());
        setVersion(answer.getVersion());
        setState(answer.getState().toString());

        // Fields from AnswerDetails
        setQuestionId(answer.getAnswerDetails().getQuestionId());
        setAnswer(answer.getAnswerDetails().getAnswer());
        setOption(answer.getAnswerDetails().getOption());
        setAnswerDate(answer.getAnswerDetails().getAnswerDate());

    }
    
    public Integer getAggregateId() {
        return aggregateId;
    }
    
    public void setAggregateId(Integer aggregateId) {
        this.aggregateId = aggregateId;
    }

    public Integer getVersion() {
        return version;
    }
    
    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getState() {
        return state;
    }
    
    public void setState(String state) {
        this.state = state;
    }

    public Integer getQuestionId() {
        return questionId;
    }
    
    public void setQuestionId(Integer questionId) {
        this.questionId = questionId;
    }

    public String getAnswer() {
        return answer;
    }
    
    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public String getOption() {
        return option;
    }
    
    public void setOption(String option) {
        this.option = option;
    }

    public LocalDateTime getAnswerDate() {
        return answerDate;
    }
    
    public void setAnswerDate(LocalDateTime answerDate) {
        this.answerDate = answerDate;
    }
}