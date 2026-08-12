package pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;

import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuizOptionDto;

@Entity
public class QuizOption {
    @Id
    @GeneratedValue
    private Long id;
    private String optionContent;
    private Integer questionAggregateId;
    private Integer questionVersion;
    private AggregateState questionState;
    @OneToOne
    private Quiz quiz;

    public QuizOption() {

    }

    public QuizOption(QuestionDto questionDto) {
        setQuestionAggregateId(questionDto.getAggregateId());
        setQuestionVersion(questionDto.getVersion());
        setQuestionState(questionDto.getState());
    }

    public QuizOption(QuizOptionDto quizOptionDto) {
        setOptionContent(quizOptionDto.getContent());
        setQuestionAggregateId(quizOptionDto.getAggregateId());
        setQuestionVersion(quizOptionDto.getVersion());
        setQuestionState(quizOptionDto.getState() != null ? AggregateState.valueOf(quizOptionDto.getState()) : null);
    }

    public QuizOption(QuizOption other) {
        setOptionContent(other.getOptionContent());
        setQuestionAggregateId(other.getQuestionAggregateId());
        setQuestionVersion(other.getQuestionVersion());
        setQuestionState(other.getQuestionState());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOptionContent() {
        return optionContent;
    }

    public void setOptionContent(String optionContent) {
        this.optionContent = optionContent;
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

    public Quiz getQuiz() {
        return quiz;
    }

    public void setQuiz(Quiz quiz) {
        this.quiz = quiz;
    }




    public QuizOptionDto buildDto() {
        QuizOptionDto dto = new QuizOptionDto();
        dto.setContent(getOptionContent());
        dto.setAggregateId(getQuestionAggregateId());
        dto.setVersion(getQuestionVersion());
        dto.setState(getQuestionState() != null ? getQuestionState().name() : null);
        return dto;
    }
}