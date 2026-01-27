package pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;

import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuizQuestionDto;

@Entity
public class QuizQuestion {
    @Id
    @GeneratedValue
    private Long id;
    private AggregateState questionState;
    private Integer questionSequence;
    private String questionTitle;
    private String questionContent;
    private Integer questionAggregateId;
    private Integer questionVersion;
    @OneToOne
    private Quiz quiz;

    public QuizQuestion() {

    }

    public QuizQuestion(QuestionDto questionDto) {
        setQuestionAggregateId(questionDto.getAggregateId());
        setQuestionVersion(questionDto.getVersion());
        setQuestionState(questionDto.getState());
        setQuestionTitle(questionDto.getTitle());
        setQuestionContent(questionDto.getContent());
    }

    public QuizQuestion(QuizQuestionDto quizQuestionDto) {
        setQuestionState(quizQuestionDto.getState());
        setQuestionSequence(quizQuestionDto.getQuestionSequence());
        setQuestionTitle(quizQuestionDto.getTitle());
        setQuestionContent(quizQuestionDto.getContent());
        setQuestionAggregateId(quizQuestionDto.getAggregateId());
        setQuestionVersion(quizQuestionDto.getVersion());
    }

    public QuizQuestion(QuizQuestion other) {
        setQuestionSequence(other.getQuestionSequence());
        setQuestionTitle(other.getQuestionTitle());
        setQuestionContent(other.getQuestionContent());
        setQuestionAggregateId(other.getQuestionAggregateId());
        setQuestionVersion(other.getQuestionVersion());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public AggregateState getQuestionState() {
        return questionState;
    }

    public void setQuestionState(AggregateState questionState) {
        this.questionState = questionState;
    }

    public Integer getQuestionSequence() {
        return questionSequence;
    }

    public void setQuestionSequence(Integer questionSequence) {
        this.questionSequence = questionSequence;
    }

    public String getQuestionTitle() {
        return questionTitle;
    }

    public void setQuestionTitle(String questionTitle) {
        this.questionTitle = questionTitle;
    }

    public String getQuestionContent() {
        return questionContent;
    }

    public void setQuestionContent(String questionContent) {
        this.questionContent = questionContent;
    }

    public Integer getQuestionAggregateId() {
        return questionAggregateId;
    }

    public void setQuestionAggregateId(Integer questionAggregateId) {
        this.questionAggregateId = questionAggregateId;
    }

    public Integer getQuestionVersion() {
        return questionVersion;
    }

    public void setQuestionVersion(Integer questionVersion) {
        this.questionVersion = questionVersion;
    }

    public Quiz getQuiz() {
        return quiz;
    }

    public void setQuiz(Quiz quiz) {
        this.quiz = quiz;
    }



    public QuizQuestionDto buildDto() {
        QuizQuestionDto dto = new QuizQuestionDto();
        dto.setState(getQuestionState());
        dto.setQuestionSequence(getQuestionSequence());
        dto.setTitle(getQuestionTitle());
        dto.setContent(getQuestionContent());
        dto.setAggregateId(getQuestionAggregateId());
        dto.setVersion(getQuestionVersion());
        return dto;
    }
}