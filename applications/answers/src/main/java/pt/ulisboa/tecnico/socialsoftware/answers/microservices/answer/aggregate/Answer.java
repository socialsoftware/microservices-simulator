package pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import java.time.LocalDateTime;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;

import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.AnswerDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.AnswerExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.AnswerQuizDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.AnswerUserDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuestionAnsweredDto;

@Entity
public abstract class Answer extends Aggregate {
    private LocalDateTime creationDate;
    private LocalDateTime answerDate;
    private boolean completed;
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

    public Answer(Integer aggregateId, AnswerExecution execution, AnswerUser user, AnswerQuiz quiz, AnswerDto answerDto, List<QuestionAnswered> questions) {
        super(aggregateId);
        setAggregateType(getClass().getSimpleName());
        setCreationDate(answerDto.getCreationDate());
        setAnswerDate(answerDto.getAnswerDate());
        setCompleted(answerDto.getCompleted());
        setExecution(execution);
        setUser(user);
        setQuiz(quiz);
        setQuestions(questions);
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

    public boolean getCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
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
    public Set<EventSubscription> getEventSubscriptions() {
        return new HashSet<>();
    }

    @Override
    public void verifyInvariants() {
        // No invariants defined
    }

    public AnswerDto buildDto() {
        AnswerDto dto = new AnswerDto();
        dto.setAggregateId(getAggregateId());
        dto.setVersion(getVersion());
        dto.setState(getState());
        dto.setCreationDate(getCreationDate());
        dto.setAnswerDate(getAnswerDate());
        dto.setCompleted(getCompleted());
        dto.setExecution(getExecution() != null ? new AnswerExecutionDto(getExecution()) : null);
        dto.setUser(getUser() != null ? new AnswerUserDto(getUser()) : null);
        dto.setQuiz(getQuiz() != null ? new AnswerQuizDto(getQuiz()) : null);
        dto.setQuestions(getQuestions() != null ? getQuestions().stream().map(QuestionAnsweredDto::new).collect(Collectors.toList()) : null);
        return dto;
    }
}