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
        setQuestionAggregateId(questionDto.getAggregateId());
        setQuestionVersion(questionDto.getVersion());
        setQuestionState(questionDto.getState());
        setQuestionTitle(questionDto.getTitle());
        setQuestionContent(questionDto.getContent());
    }

    public QuizQuestion(QuizQuestion other) {
        setQuestionVersion(other.getQuestionVersion());
        setQuestionState(other.getQuestionState());
        setQuestionTitle(other.getQuestionTitle());
        setQuestionContent(other.getQuestionContent());
        setQuestionSequence(other.getQuestionSequence());
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


    public QuizQuestionDto buildDto() {
        QuizQuestionDto dto = new QuizQuestionDto();
        dto.setAggregateId(getQuestionAggregateId());
        dto.setVersion(getQuestionVersion());
        dto.setState(getQuestionState());
        dto.setTitle(getQuestionTitle());
        dto.setContent(getQuestionContent());
        dto.setQuestionSequence(getQuestionSequence());
        return dto;
    }
}