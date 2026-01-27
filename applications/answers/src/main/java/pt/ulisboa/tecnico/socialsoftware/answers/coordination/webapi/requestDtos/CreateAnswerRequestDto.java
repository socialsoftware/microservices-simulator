package pt.ulisboa.tecnico.socialsoftware.answers.coordination.webapi.requestDtos;

import jakarta.validation.constraints.*;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.ExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.UserDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuizDto;
import java.time.LocalDateTime;
import java.util.List;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuestionAnsweredDto;

public class CreateAnswerRequestDto {
    @NotNull
    private ExecutionDto execution;
    @NotNull
    private UserDto user;
    @NotNull
    private QuizDto quiz;
    @NotNull
    private LocalDateTime creationDate;
    @NotNull
    private LocalDateTime answerDate;
    @NotNull
    private Boolean completed;
    private List<QuestionAnsweredDto> questions;

    public CreateAnswerRequestDto() {}

    public CreateAnswerRequestDto(ExecutionDto execution, UserDto user, QuizDto quiz, LocalDateTime creationDate, LocalDateTime answerDate, Boolean completed, List<QuestionAnsweredDto> questions) {
        this.execution = execution;
        this.user = user;
        this.quiz = quiz;
        this.creationDate = creationDate;
        this.answerDate = answerDate;
        this.completed = completed;
        this.questions = questions;
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
    public List<QuestionAnsweredDto> getQuestions() {
        return questions;
    }

    public void setQuestions(List<QuestionAnsweredDto> questions) {
        this.questions = questions;
    }
}
