package pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos;

import java.io.Serializable;
import java.time.LocalDateTime;

public class AnswerDto implements Serializable {
    
    private Integer questionId;
    private String answer;
    private String option;
    private LocalDateTime answerDate;
    
    public AnswerDto() {
    }
    
    public AnswerDto(Integer questionId, String answer, String option, LocalDateTime answerDate) {
        setQuestionId(questionId);
        setAnswer(answer);
        setOption(option);
        setAnswerDate(answerDate);
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