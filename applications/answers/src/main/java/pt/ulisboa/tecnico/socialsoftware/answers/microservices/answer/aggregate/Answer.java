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
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;

import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.events.subscribe.AnswerSubscribesExecutionDeletedExecutionRef;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.events.subscribe.AnswerSubscribesExecutionUserUpdated;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.events.subscribe.AnswerSubscribesQuestionDeletedQuestionsRef;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.events.subscribe.AnswerSubscribesQuestionUpdated;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.events.subscribe.AnswerSubscribesQuizDeletedQuizRef;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.events.subscribe.AnswerSubscribesUserDeletedUserRef;

import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.AnswerDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.AnswerExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.AnswerQuizDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.AnswerUserDto;

import static pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorErrorMessage.INVARIANT_BREAK;

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
    private List<AnswerQuestion> questions = new ArrayList<>();

    public Answer() {

    }

    public Answer(Integer aggregateId, AnswerDto answerDto) {
        super(aggregateId);
        setAggregateType(getClass().getSimpleName());
        setCreationDate(answerDto.getCreationDate());
        setAnswerDate(answerDto.getAnswerDate());
        setCompleted(answerDto.getCompleted());
        setExecution(answerDto.getExecution() != null ? new AnswerExecution(answerDto.getExecution()) : null);
        setUser(answerDto.getUser() != null ? new AnswerUser(answerDto.getUser()) : null);
        setQuiz(answerDto.getQuiz() != null ? new AnswerQuiz(answerDto.getQuiz()) : null);
        setQuestions(answerDto.getQuestions() != null ? answerDto.getQuestions().stream().map(AnswerQuestion::new).collect(Collectors.toList()) : null);
    }


    public Answer(Answer other) {
        super(other);
        setCreationDate(other.getCreationDate());
        setAnswerDate(other.getAnswerDate());
        setCompleted(other.getCompleted());
        setExecution(other.getExecution() != null ? new AnswerExecution(other.getExecution()) : null);
        setUser(other.getUser() != null ? new AnswerUser(other.getUser()) : null);
        setQuiz(other.getQuiz() != null ? new AnswerQuiz(other.getQuiz()) : null);
        setQuestions(other.getQuestions() != null ? other.getQuestions().stream().map(AnswerQuestion::new).collect(Collectors.toList()) : null);
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

    public List<AnswerQuestion> getQuestions() {
        return questions;
    }

    public void setQuestions(List<AnswerQuestion> questions) {
        this.questions = questions;
        if (this.questions != null) {
            this.questions.forEach(item -> item.setAnswer(this));
        }
    }

    public void addAnswerQuestion(AnswerQuestion answerQuestion) {
        if (this.questions == null) {
            this.questions = new ArrayList<>();
        }
        this.questions.add(answerQuestion);
        if (answerQuestion != null) {
            answerQuestion.setAnswer(this);
        }
    }

    public void removeAnswerQuestion(Integer id) {
        if (this.questions != null) {
            this.questions.removeIf(item -> 
                item.getSequence() != null && item.getSequence().equals(id));
        }
    }

    public boolean containsAnswerQuestion(Integer id) {
        if (this.questions == null) {
            return false;
        }
        return this.questions.stream().anyMatch(item -> 
            item.getSequence() != null && item.getSequence().equals(id));
    }

    public AnswerQuestion findAnswerQuestionById(Integer id) {
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
        Set<EventSubscription> eventSubscriptions = new HashSet<>();
        if (this.getState() == AggregateState.ACTIVE) {
            interInvariantExecutionRef(eventSubscriptions);
            interInvariantUserRef(eventSubscriptions);
            interInvariantQuizRef(eventSubscriptions);
            interInvariantQuestionsRef(eventSubscriptions);
            eventSubscriptions.add(new AnswerSubscribesExecutionUserUpdated());
            eventSubscriptions.add(new AnswerSubscribesQuestionUpdated());
        }
        return eventSubscriptions;
    }
    private void interInvariantExecutionRef(Set<EventSubscription> eventSubscriptions) {
        eventSubscriptions.add(new AnswerSubscribesExecutionDeletedExecutionRef(this.getExecution()));
    }

    private void interInvariantUserRef(Set<EventSubscription> eventSubscriptions) {
        eventSubscriptions.add(new AnswerSubscribesUserDeletedUserRef(this.getUser()));
    }

    private void interInvariantQuizRef(Set<EventSubscription> eventSubscriptions) {
        eventSubscriptions.add(new AnswerSubscribesQuizDeletedQuizRef(this.getQuiz()));
    }

    private void interInvariantQuestionsRef(Set<EventSubscription> eventSubscriptions) {
        for (AnswerQuestion item : this.questions) {
            eventSubscriptions.add(new AnswerSubscribesQuestionDeletedQuestionsRef(item));
        }
    }


    private boolean invariantRule0() {
        return this.execution != null;
    }

    private boolean invariantRule1() {
        return this.user != null;
    }

    private boolean invariantRule2() {
        return this.quiz != null;
    }

    private boolean invariantRule3() {
        return this.questions != null;
    }
    @Override
    public void verifyInvariants() {
        if (!invariantRule0()) {
            throw new SimulatorException(INVARIANT_BREAK, "Answer must be associated with an execution");
        }
        if (!invariantRule1()) {
            throw new SimulatorException(INVARIANT_BREAK, "Answer must be associated with a user");
        }
        if (!invariantRule2()) {
            throw new SimulatorException(INVARIANT_BREAK, "Answer must be associated with a quiz");
        }
        if (!invariantRule3()) {
            throw new SimulatorException(INVARIANT_BREAK, "Answer must have a questions collection");
        }
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
        dto.setQuestions(getQuestions() != null ? getQuestions().stream().map(AnswerQuestion::buildDto).collect(Collectors.toList()) : null);
        return dto;
    }
}