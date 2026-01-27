package pt.ulisboa.tecnico.socialsoftware.answers.coordination.webapi.requestDtos;

import jakarta.validation.constraints.*;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.ExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.UserDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuizDto;
import java.time.LocalDateTime;
import java.util.List;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuestionAnsweredDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.AnswerExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.AnswerUserDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.AnswerQuizDto;

public class UpdateAnswerRequestDto {
    @NotNull
    private LocalDateTime creationDate;
    @NotNull
    private LocalDateTime answerDate;
    @NotNull
    private Boolean completed;
    @NotNull
    private AnswerExecutionDto execution;
    @NotNull
    private AnswerUserDto user;
    @NotNull
    private AnswerQuizDto quiz;
    @NotNull
    private List<QuestionAnsweredDto> questions;

    public UpdateAnswerRequestDto() {}

    public UpdateAnswerRequestDto(LocalDateTime creationDate, LocalDateTime answerDate, Boolean completed, AnswerExecutionDto execution, AnswerUserDto user, AnswerQuizDto quiz, List<QuestionAnsweredDto> questions) {
        this.creationDate = creationDate;
        this.answerDate = answerDate;
        this.completed = completed;
        this.execution = execution;
        this.user = user;
        this.quiz = quiz;
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
    public AnswerExecutionDto getExecution() {
        return execution;
    }

    public void setExecution(AnswerExecutionDto execution) {
        this.execution = execution;
    }
    public AnswerUserDto getUser() {
        return user;
    }

    public void setUser(AnswerUserDto user) {
        this.user = user;
    }
    public AnswerQuizDto getQuiz() {
        return quiz;
    }

    public void setQuiz(AnswerQuizDto quiz) {
        this.quiz = quiz;
    }
    public List<QuestionAnsweredDto> getQuestions() {
        return questions;
    }

    public void setQuestions(List<QuestionAnsweredDto> questions) {
        this.questions = questions;
    }
}
