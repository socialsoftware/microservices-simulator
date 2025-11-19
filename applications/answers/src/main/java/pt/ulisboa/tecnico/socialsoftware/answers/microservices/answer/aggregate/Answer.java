package pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import java.time.LocalDateTime;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;

import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.AnswerDto;

@Entity
public abstract class Answer extends Aggregate {
    private LocalDateTime creationDate;
    private LocalDateTime answerDate;
    private Boolean completed;
    @OneToOne(cascade = CascadeType.ALL, mappedBy = "answer")
    private AnswerExecution execution;
    @OneToOne(cascade = CascadeType.ALL, mappedBy = "answer")
    private AnswerUser user;
    @OneToOne(cascade = CascadeType.ALL, mappedBy = "answer")
    private AnswerQuiz quiz;
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "answer")
    private List<QuestionAnswered> questions = new ArrayList<>();

    public Answer() {

    }

    public Answer(Integer aggregateId, AnswerDto answerDto, AnswerExecution execution, AnswerUser user, AnswerQuiz quiz) {
        super(aggregateId);
        setAggregateType(getClass().getSimpleName());
        setCreationDate(answerDto.getCreationDate());
        setAnswerDate(answerDto.getAnswerDate());
        setCompleted(answerDto.getCompleted());
        setExecution(execution);
        setUser(user);
        setQuiz(quiz);
    }

    public Answer(Answer other) {
        super(other);
        setCreationDate(other.getCreationDate());
        setAnswerDate(other.getAnswerDate());
        setCompleted(other.getCompleted());
        setExecution(new AnswerExecution(other.getExecution()));
        setUser(new AnswerUser(other.getUser()));
        setQuiz(new AnswerQuiz(other.getQuiz()));
        setQuestions(other.getQuestions().stream().map(QuestionAnswered::new).collect(Collectors.toList()));
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

    public AnswerExecution getExecution() {
        return execution;
    }

    public void setExecution(AnswerExecution execution) {
        this.execution = execution;
        if (this.execution != null) {
            this.execution.setAnswer(this);
        }
    }

    public AnswerUser getUser() {
        return user;
    }

    public void setUser(AnswerUser user) {
        this.user = user;
        if (this.user != null) {
            this.user.setAnswer(this);
        }
    }

    public AnswerQuiz getQuiz() {
        return quiz;
    }

    public void setQuiz(AnswerQuiz quiz) {
        this.quiz = quiz;
        if (this.quiz != null) {
            this.quiz.setAnswer(this);
        }
    }

    public List<QuestionAnswered> getQuestions() {
        return questions;
    }

    public void setQuestions(List<QuestionAnswered> questions) {
        this.questions = questions;
        if (this.questions != null) {
            this.questions.forEach(item -> item.setAnswer(this));
        }
    }

    public void addQuestionAnswered(QuestionAnswered questionAnswered) {
        if (this.questions == null) {
            this.questions = new ArrayList<>();
        }
        this.questions.add(questionAnswered);
        if (questionAnswered != null) {
            questionAnswered.setAnswer(this);
        }
    }

    public void removeQuestionAnswered(Integer id) {
        if (this.questions != null) {
            this.questions.removeIf(item -> 
                item.getSequence() != null && item.getSequence().equals(id));
        }
    }

    public boolean containsQuestionAnswered(Integer id) {
        if (this.questions == null) {
            return false;
        }
        return this.questions.stream().anyMatch(item -> 
            item.getSequence() != null && item.getSequence().equals(id));
    }

    public QuestionAnswered findQuestionAnsweredById(Integer id) {
        if (this.questions == null) {
            return null;
        }
        return this.questions.stream()
            .filter(item -> item.getSequence() != null && item.getSequence().equals(id))
            .findFirst()
            .orElse(null);
    }


    @Override
    public void verifyInvariants() {
        // No invariants defined
    }

}