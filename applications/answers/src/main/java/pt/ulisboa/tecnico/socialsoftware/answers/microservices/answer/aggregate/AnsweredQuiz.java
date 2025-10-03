package pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import java.time.LocalDateTime;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.AnswerDto;

@Entity
public class AnsweredQuiz {
    @Id
    @GeneratedValue
    private Integer quizAggregateId;
    private String quizTitle;
    @Enumerated(EnumType.STRING)
    private QuizType quizType;
    private LocalDateTime availableDate;
    private LocalDateTime conclusionDate;
    private Integer numberOfQuestions;
    @OneToOne
    private Answer answer; 

    public AnsweredQuiz() {
    }

    public AnsweredQuiz(AnswerDto answerDto) {
        setQuizTitle(answerDto.getQuizTitle());
        setQuizType(answerDto.getQuizType());
        setAvailableDate(answerDto.getAvailableDate());
        setConclusionDate(answerDto.getConclusionDate());
        setNumberOfQuestions(answerDto.getNumberOfQuestions());
    }

    public AnsweredQuiz(AnsweredQuiz other) {
        setQuizTitle(other.getQuizTitle());
        setQuizType(other.getQuizType());
        setAvailableDate(other.getAvailableDate());
        setConclusionDate(other.getConclusionDate());
        setNumberOfQuestions(other.getNumberOfQuestions());
    }


    public Integer getQuizAggregateId() {
        return quizAggregateId;
    }

    public void setQuizAggregateId(Integer quizAggregateId) {
        this.quizAggregateId = quizAggregateId;
    }

    public String getQuizTitle() {
        return quizTitle;
    }

    public void setQuizTitle(String quizTitle) {
        this.quizTitle = quizTitle;
    }

    public QuizType getQuizType() {
        return quizType;
    }

    public void setQuizType(QuizType quizType) {
        this.quizType = quizType;
    }

    public LocalDateTime getAvailableDate() {
        return availableDate;
    }

    public void setAvailableDate(LocalDateTime availableDate) {
        this.availableDate = availableDate;
    }

    public LocalDateTime getConclusionDate() {
        return conclusionDate;
    }

    public void setConclusionDate(LocalDateTime conclusionDate) {
        this.conclusionDate = conclusionDate;
    }

    public Integer getNumberOfQuestions() {
        return numberOfQuestions;
    }

    public void setNumberOfQuestions(Integer numberOfQuestions) {
        this.numberOfQuestions = numberOfQuestions;
    }

    public Answer getAnswer() {
        return answer;
    }

    public void setAnswer(Answer answer) {
        this.answer = answer;
    }


}