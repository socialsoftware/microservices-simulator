package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.aggregate;

import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.aggregate.OptionDto;

import jakarta.persistence.Embeddable;

@Embeddable
public class QuizOption {
    private Integer sequence;
    private boolean correct;
    private String content;

    public QuizOption(OptionDto optionDto) {
        setSequence(optionDto.getSequence());
        setCorrect(optionDto.isCorrect());
        setContent(optionDto.getContent());
    }

    public QuizOption() {

    }

    public Integer getSequence() {
        return sequence;
    }

    public void setSequence(Integer sequence) {
        this.sequence = sequence;
    }

    public boolean isCorrect() {
        return correct;
    }

    public void setCorrect(boolean correct) {
        this.correct = correct;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public OptionDto buildDto() {
        OptionDto optionDto = new OptionDto();
        optionDto.setSequence(getSequence());
        optionDto.setCorrect(isCorrect());
        optionDto.setContent(getContent());

        return optionDto;
    }
}
