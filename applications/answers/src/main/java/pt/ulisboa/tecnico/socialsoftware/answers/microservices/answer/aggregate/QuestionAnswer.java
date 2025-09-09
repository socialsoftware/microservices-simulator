package pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate;

import jakarta.persistence.Embeddable;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import java.time.LocalDateTime;

@Embeddable
public class QuestionAnswer {
    private Integer questionId;
    private String answer;
    private String option;
    private LocalDateTime answerDate; 

    public QuestionAnswer(Integer questionId, String answer, String option, LocalDateTime answerDate) {
        this.questionId = questionId;
        this.answer = answer;
        this.option = option;
        this.answerDate = answerDate;
    }

    public QuestionAnswer(QuestionAnswer other) {
        // Copy constructor
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