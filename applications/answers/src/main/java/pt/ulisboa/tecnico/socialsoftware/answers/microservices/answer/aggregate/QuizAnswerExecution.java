package pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.OneToOne;
import java.util.stream.Collectors;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.AnswerDto;

@Entity
public class QuizAnswerExecution {
    @Id
    @GeneratedValue
    private Integer executionAggregateId;
    private String executionName;
    private String executionAcronym;
    private String executionAcademicTerm;
    @OneToOne
    private Answer answer; 

    public QuizAnswerExecution() {
    }

    public QuizAnswerExecution(AnswerDto answerDto) {
        setExecutionName(answerDto.getExecutionName());
        setExecutionAcronym(answerDto.getExecutionAcronym());
        setExecutionAcademicTerm(answerDto.getExecutionAcademicTerm());
    }

    public QuizAnswerExecution(QuizAnswerExecution other) {
        setExecutionName(other.getExecutionName());
        setExecutionAcronym(other.getExecutionAcronym());
        setExecutionAcademicTerm(other.getExecutionAcademicTerm());
    }


    public Integer getExecutionAggregateId() {
        return executionAggregateId;
    }

    public void setExecutionAggregateId(Integer executionAggregateId) {
        this.executionAggregateId = executionAggregateId;
    }

    public String getExecutionName() {
        return executionName;
    }

    public void setExecutionName(String executionName) {
        this.executionName = executionName;
    }

    public String getExecutionAcronym() {
        return executionAcronym;
    }

    public void setExecutionAcronym(String executionAcronym) {
        this.executionAcronym = executionAcronym;
    }

    public String getExecutionAcademicTerm() {
        return executionAcademicTerm;
    }

    public void setExecutionAcademicTerm(String executionAcademicTerm) {
        this.executionAcademicTerm = executionAcademicTerm;
    }

    public Answer getAnswer() {
        return answer;
    }

    public void setAnswer(Answer answer) {
        this.answer = answer;
    }


}