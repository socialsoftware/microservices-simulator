package pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.OptionDto;

@Entity
public class QuizOption {
    @Id
    @GeneratedValue
    private Integer optionSequence;
    private Boolean optionCorrect;
    private String optionContent;
    @OneToOne
    private Quiz quiz;

    public QuizOption() {

    }

    public QuizOption(OptionDto optionDto) {
        setOptionCorrect(optionDto.getCorrect());
        setOptionContent(optionDto.getContent());
    }

    public QuizOption(QuizOption other) {
        setOptionCorrect(other.getOptionCorrect());
        setOptionContent(other.getOptionContent());
    }

    public Integer getOptionSequence() {
        return optionSequence;
    }

    public void setOptionSequence(Integer optionSequence) {
        this.optionSequence = optionSequence;
    }

    public Boolean getOptionCorrect() {
        return optionCorrect;
    }

    public void setOptionCorrect(Boolean optionCorrect) {
        this.optionCorrect = optionCorrect;
    }

    public String getOptionContent() {
        return optionContent;
    }

    public void setOptionContent(String optionContent) {
        this.optionContent = optionContent;
    }

    public Quiz getQuiz() {
        return quiz;
    }

    public void setQuiz(Quiz quiz) {
        this.quiz = quiz;
    }


    public OptionDto buildDto() {
        OptionDto dto = new OptionDto();
        dto.setSequence(getOptionSequence());
        dto.setCorrect(getOptionCorrect());
        dto.setContent(getOptionContent());
        return dto;
    }
}