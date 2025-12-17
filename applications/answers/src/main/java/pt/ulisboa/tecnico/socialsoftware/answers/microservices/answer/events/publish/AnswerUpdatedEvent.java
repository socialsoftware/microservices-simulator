package pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.events.publish;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class AnswerUpdatedEvent extends Event {
    private LocalDateTime creationDate;
    private LocalDateTime answerDate;
    private Boolean completed;
    private AnswerExecution execution;
    private AnswerUser user;
    private AnswerQuiz quiz;
    private List<QuestionAnswered> questions;

    public AnswerUpdatedEvent() {
    }

    public AnswerUpdatedEvent(Integer aggregateId, LocalDateTime creationDate, LocalDateTime answerDate, Boolean completed, AnswerExecution execution, AnswerUser user, AnswerQuiz quiz, List<QuestionAnswered> questions) {
        super(aggregateId);
        setCreationDate(creationDate);
        setAnswerDate(answerDate);
        setCompleted(completed);
        setExecution(execution);
        setUser(user);
        setQuiz(quiz);
        setQuestions(questions);
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
    }

    public AnswerUser getUser() {
        return user;
    }

    public void setUser(AnswerUser user) {
        this.user = user;
    }

    public AnswerQuiz getQuiz() {
        return quiz;
    }

    public void setQuiz(AnswerQuiz quiz) {
        this.quiz = quiz;
    }

    public List<QuestionAnswered> getQuestions() {
        return questions;
    }

    public void setQuestions(List<QuestionAnswered> questions) {
        this.questions = questions;
    }

}