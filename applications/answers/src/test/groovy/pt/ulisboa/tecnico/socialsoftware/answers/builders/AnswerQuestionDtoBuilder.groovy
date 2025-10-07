package pt.ulisboa.tecnico.socialsoftware.answers.builders

import pt.ulisboa.tecnico.socialsoftware.answers.microservices.default.aggregate.AnswerQuestionDto
import java.time.LocalDateTime

class AnswerQuestionDtoBuilder extends SpockTest {
    private AnswerQuestionDto answerquestionDto

    static AnswerQuestionDtoBuilder aAnswerQuestionDto() {
        return new AnswerQuestionDtoBuilder()
    }

    AnswerQuestionDtoBuilder() {
        this.answerquestionDto = new AnswerQuestionDto()
        // Set default values
        this.answerquestionDto.setId(1L)
        this.answerquestionDto.setQuestionOptionSequenceChoice(1)
        this.answerquestionDto.setQuestionAggregateId(1)
        this.answerquestionDto.setQuestionVersion(1)
        this.answerquestionDto.setQuestionTimeTaken(1)
        this.answerquestionDto.setQuestionOptionKey(1)
        this.answerquestionDto.setQuestionCorrect(false)
    }

    AnswerQuestionDtoBuilder withId(Long id) {
        this.answerquestionDto.setId(id)
        return this
    }

    AnswerQuestionDtoBuilder withQuestionOptionSequenceChoice(Integer questionOptionSequenceChoice) {
        this.answerquestionDto.setQuestionOptionSequenceChoice(questionOptionSequenceChoice)
        return this
    }

    AnswerQuestionDtoBuilder withQuestionAggregateId(Integer questionAggregateId) {
        this.answerquestionDto.setQuestionAggregateId(questionAggregateId)
        return this
    }

    AnswerQuestionDtoBuilder withQuestionVersion(Integer questionVersion) {
        this.answerquestionDto.setQuestionVersion(questionVersion)
        return this
    }

    AnswerQuestionDtoBuilder withQuestionTimeTaken(Integer questionTimeTaken) {
        this.answerquestionDto.setQuestionTimeTaken(questionTimeTaken)
        return this
    }

    AnswerQuestionDtoBuilder withQuestionOptionKey(Integer questionOptionKey) {
        this.answerquestionDto.setQuestionOptionKey(questionOptionKey)
        return this
    }

    AnswerQuestionDtoBuilder withQuestionCorrect(Boolean questionCorrect) {
        this.answerquestionDto.setQuestionCorrect(questionCorrect)
        return this
    }

    AnswerQuestionDtoBuilder withQuestionState(AggregateState questionState) {
        this.answerquestionDto.setQuestionState(questionState)
        return this
    }

    AnswerQuestionDto build() {
        return this.answerquestionDto
    }
}
