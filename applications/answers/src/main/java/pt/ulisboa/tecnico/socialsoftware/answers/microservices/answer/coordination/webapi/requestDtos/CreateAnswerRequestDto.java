package pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.coordination.webapi.requestDtos;

import jakarta.validation.constraints.*;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.ExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.UserDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuizDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.AnswerQuestionDto;
import java.util.Set;
import java.time.LocalDateTime;

public class CreateAnswerRequestDto {
    @NotNull
    private ExecutionDto execution;
    @NotNull
    private UserDto user;
    @NotNull
    private QuizDto quiz;
    @NotNull
    private Set<AnswerQuestionDto> questions;
    @NotNull
    private LocalDateTime creationDate;
    @NotNull
    private LocalDateTime answerDate;
    @NotNull
    private Boolean completed;

    public CreateAnswerRequestDto() {}

    public CreateAnswerRequestDto(ExecutionDto execution, UserDto user, QuizDto quiz, Set<AnswerQuestionDto> questions, LocalDateTime creationDate, LocalDateTime answerDate, Boolean completed) {
        this.execution = execution;
        this.user = user;
        this.quiz = quiz;
        this.questions = questions;
        this.creationDate = creationDate;
        this.answerDate = answerDate;
        this.completed = completed;
    }

    public ExecutionDto getExecution() {
        return execution;
    }

    public void setExecution(ExecutionDto execution) {
        this.execution = execution;
    }
    public UserDto getUser() {
        return user;
    }

    public void setUser(UserDto user) {
        this.user = user;
    }
    public QuizDto getQuiz() {
        return quiz;
    }

    public void setQuiz(QuizDto quiz) {
        this.quiz = quiz;
    }
    public Set<AnswerQuestionDto> getQuestions() {
        return questions;
    }

    public void setQuestions(Set<AnswerQuestionDto> questions) {
        this.questions = questions;
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
}
