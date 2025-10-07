package pt.ulisboa.tecnico.socialsoftware.answers.builders

import pt.ulisboa.tecnico.socialsoftware.answers.microservices.default.aggregate.AnswerUserDto
import java.time.LocalDateTime

class AnswerUserDtoBuilder extends SpockTest {
    private AnswerUserDto answeruserDto

    static AnswerUserDtoBuilder aAnswerUserDto() {
        return new AnswerUserDtoBuilder()
    }

    AnswerUserDtoBuilder() {
        this.answeruserDto = new AnswerUserDto()
        // Set default values
        this.answeruserDto.setId(1L)
        this.answeruserDto.setUserAggregateId(1)
        this.answeruserDto.setUserName("Default userName")
    }

    AnswerUserDtoBuilder withId(Long id) {
        this.answeruserDto.setId(id)
        return this
    }

    AnswerUserDtoBuilder withUserAggregateId(Integer userAggregateId) {
        this.answeruserDto.setUserAggregateId(userAggregateId)
        return this
    }

    AnswerUserDtoBuilder withUserName(String userName) {
        this.answeruserDto.setUserName(userName)
        return this
    }

    AnswerUserDtoBuilder withUserState(AggregateState userState) {
        this.answeruserDto.setUserState(userState)
        return this
    }

    AnswerUserDto build() {
        return this.answeruserDto
    }
}
