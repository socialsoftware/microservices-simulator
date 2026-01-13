package pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.OptionDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuizOptionDto;

@Entity
public class QuizOption {
    @Id
    @GeneratedValue
    private Long id;
    private Integer optionSequence;
    private boolean optionCorrect;
    private String optionContent;
    @OneToOne
    private Quiz quiz;

    public QuizOption() {

    }

    public QuizOption(OptionDto optionDto) {
        setOptionSequence(optionDto.getSequence());
        setOptionCorrect(optionDto.getCorrect());
        setOptionContent(optionDto.getContent());
    }

    public QuizOption(QuizOption other) {
        setOptionCorrect(other.getOptionCorrect());
        setOptionContent(other.getOptionContent());
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
        return dto;
    }
}