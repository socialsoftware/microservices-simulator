package pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.aggregate;

import java.util.stream.Collectors;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;

import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.aggregate.QuestionDto;

@Entity
public class QuizQuestion {
    @Id
    @GeneratedValue
    private Long id;
    private Integer questionAggregateId;
    private Integer questionVersion;
    private AggregateState questionState;
    private String questionTitle;
    private String questionContent;
    private Integer questionSequence;
    @OneToOne
    private Quiz quiz;

    public QuizQuestion() {

    }

    public QuizQuestion(QuestionDto questionDto) {
        setTitle(questionDto.getTitle());
        setContent(questionDto.getContent());
        setCreationDate(questionDto.getCreationDate());
        setTopics(questionDto.getTopics());
        setOptions(questionDto.getOptions() != null ? questionDto.getOptions().stream().map(dto -> new Option(dto)).collect(Collectors.toList()) : null);
    }

    public QuizQuestion(QuizQuestion other) {
        setQuestionAggregateId(other.getQuestionAggregateId());
        setQuestionVersion(other.getQuestionVersion());
        setQuestionState(other.getQuestionState());
        setQuestionTitle(other.getQuestionTitle());
        setQuestionContent(other.getQuestionContent());
        setQuestionSequence(other.getQuestionSequence());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public AggregateState getQuestionState() {
        return questionState;
    }

    public void setQuestionState(AggregateState questionState) {
        this.questionState = questionState;
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

    public Integer getQuestionSequence() {
        return questionSequence;
    }

    public void setQuestionSequence(Integer questionSequence) {
        this.questionSequence = questionSequence;
    }

    public Quiz getQuiz() {
        return quiz;
    }

    public void setQuiz(Quiz quiz) {
        this.quiz = quiz;
    }


    public QuestionDto buildDto() {
        QuestionDto dto = new QuestionDto();
        dto.setAggregateId(getAggregateId());
        dto.setVersion(getVersion());
        dto.setState(getState());
        return dto;
    }
}