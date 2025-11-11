package pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.OptionDto;

@Entity
public class Option {
    @Id
    @GeneratedValue
    private Long id;
    private Integer sequence;
    private Boolean correct;
    private String content;
    @OneToOne
    private Question question;

    public Option() {

    }

    public Option(OptionDto optionDto) {
        setSequence(optionDto.getSequence());
        setCorrect(optionDto.getCorrect());
        setContent(optionDto.getContent());
    }

    public Option(Option other) {
        setSequence(other.getSequence());
        setCorrect(other.getCorrect());
        setContent(other.getContent());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getSequence() {
        return sequence;
    }

    public void setSequence(Integer sequence) {
        this.sequence = sequence;
    }

    public Boolean getCorrect() {
        return correct;
    }

    public void setCorrect(Boolean correct) {
        this.correct = correct;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Question getQuestion() {
        return question;
    }

    public void setQuestion(Question question) {
        this.question = question;
    }


    public OptionDto buildDto() {
        OptionDto dto = new OptionDto();
        dto.setId(getId());
        dto.setSequence(getSequence());
        dto.setCorrect(getCorrect());
        dto.setContent(getContent());
        return dto;
    }
}