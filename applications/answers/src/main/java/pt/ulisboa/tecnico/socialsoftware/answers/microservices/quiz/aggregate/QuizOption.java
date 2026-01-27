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
    private Integer optionSequence;
    private boolean optionCorrect;
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
        setOptionContent(questionDto.getContent());
    }

    public QuizOption(QuizOption other) {
        setOptionCorrect(other.getOptionCorrect());
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

    public Integer getOptionSequence() {
        return optionSequence;
    }

    public void setOptionSequence(Integer optionSequence) {
        this.optionSequence = optionSequence;
    }

    public boolean getOptionCorrect() {
        return optionCorrect;
    }

    public void setOptionCorrect(boolean optionCorrect) {
        this.optionCorrect = optionCorrect;
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
        dto.setSequence(getOptionSequence());
        dto.setCorrect(getOptionCorrect());
        dto.setContent(getOptionContent());
        dto.setAggregateId(getQuestionAggregateId());
        dto.setVersion(getQuestionVersion());
        dto.setState(getQuestionState());
        return dto;
    }
}