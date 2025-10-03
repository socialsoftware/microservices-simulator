package pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.OneToOne;
import java.util.stream.Collectors;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import java.time.LocalDateTime;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.AnswerDto;

@Entity
public class QuestionAnswer {
    @Id
    @GeneratedValue
    private Integer questionId;
    private String answer;
    private String option;
    private LocalDateTime answerDate;
    @OneToOne
    private Answer answer; 

    public QuestionAnswer() {
    }

    public QuestionAnswer(AnswerDto answerDto) {
        setAnswer(answerDto.getAnswer());
        setOption(answerDto.getOption());
        setAnswerDate(answerDto.getAnswerDate());
    }

    public QuestionAnswer(QuestionAnswer other) {
        setAnswer(other.getAnswer());
        setOption(other.getOption());
        setAnswerDate(other.getAnswerDate());
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

    public Answer getAnswer() {
        return answer;
    }

    public void setAnswer(Answer answer) {
        this.answer = answer;
    }


}