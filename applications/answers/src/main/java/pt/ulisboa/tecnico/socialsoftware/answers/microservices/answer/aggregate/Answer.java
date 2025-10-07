package pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.CascadeType;
import jakarta.persistence.FetchType;
import java.util.stream.Collectors;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.HashSet;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.AnswerDto;

@Entity
public abstract class Answer extends Aggregate {
    @Id
    private LocalDateTime creationDate;
    private LocalDateTime answerDate;
    private Boolean completed;
    @OneToOne(cascade = CascadeType.ALL, mappedBy = "answer")
    private AnswerExecution answerExecution;
    @OneToOne(cascade = CascadeType.ALL, mappedBy = "answer")
    private AnswerUser answerUser;
    @OneToOne(cascade = CascadeType.ALL, mappedBy = "answer")
    private AnswerQuiz answerQuiz;
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "answer")
    private Set<AnswerQuestion> answerQuestion = new HashSet<>(); 

    public Answer() {
    }

    public Answer(Integer aggregateId, AnswerDto answerDto, AnswerExecution answerExecution, AnswerUser answerUser, AnswerQuiz answerQuiz) {
        super(aggregateId);
        setAggregateType(getClass().getSimpleName());
        setCreationDate(answerDto.getCreationDate());
        setAnswerDate(answerDto.getAnswerDate());
        setCompleted(answerDto.getCompleted());
        setAnswerExecution(answerExecution);
        setAnswerUser(answerUser);
        setAnswerQuiz(answerQuiz);
    }

    public Answer(Answer other) {
        super(other);
        setCreationDate(other.getCreationDate());
        setAnswerDate(other.getAnswerDate());
        setCompleted(other.getCompleted());
        setAnswerExecution(new AnswerExecution(other.getAnswerExecution()));
        setAnswerUser(new AnswerUser(other.getAnswerUser()));
        setAnswerQuiz(new AnswerQuiz(other.getAnswerQuiz()));
        setAnswerQuestion(other.getAnswerQuestion().stream().map(AnswerQuestion::new).collect(Collectors.toSet()));
    }


    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(LocalDateTime creationDate) {
        this.creationDate = creationDate;
    }

    public LocalDateTime getAnswerDate() {
        return answerDate;
    }

    public void setAnswerDate(LocalDateTime answerDate) {
        this.answerDate = answerDate;
    }

    public Boolean getCompleted() {
        return completed;
    }

    public void setCompleted(Boolean completed) {
        this.completed = completed;
    }

    public AnswerExecution getAnswerExecution() {
        return answerExecution;
    }

    public void setAnswerExecution(AnswerExecution answerExecution) {
        this.answerExecution = answerExecution;
        if (this.answerExecution != null) {
            this.answerExecution.setAnswer(this);
        }
    }

    public AnswerUser getAnswerUser() {
        return answerUser;
    }

    public void setAnswerUser(AnswerUser answerUser) {
        this.answerUser = answerUser;
        if (this.answerUser != null) {
            this.answerUser.setAnswer(this);
        }
    }

    public AnswerQuiz getAnswerQuiz() {
        return answerQuiz;
    }

    public void setAnswerQuiz(AnswerQuiz answerQuiz) {
        this.answerQuiz = answerQuiz;
        if (this.answerQuiz != null) {
            this.answerQuiz.setAnswer(this);
        }
    }

    public Set<AnswerQuestion> getAnswerQuestion() {
        return answerQuestion;
    }

    public void setAnswerQuestion(Set<AnswerQuestion> answerQuestion) {
        this.answerQuestion = answerQuestion;
        if (this.answerQuestion != null) {
            this.answerQuestion.forEach(answerquestion -> answerquestion.setAnswer(this));
        }
    }



}