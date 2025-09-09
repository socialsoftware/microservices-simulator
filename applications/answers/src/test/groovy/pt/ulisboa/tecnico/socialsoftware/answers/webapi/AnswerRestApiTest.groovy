package pt.ulisboa.tecnico.socialsoftware.answers.webapi

import java.time.LocalDateTime
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import com.fasterxml.jackson.databind.ObjectMapper
import spock.lang.Specification
import pt.ulisboa.tecnico.socialsoftware.SpockTest
import pt.ulisboa.tecnico.socialsoftware.answers.coordination.functionalities.AnswerFunctionalities
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate.QuizAnswerDto
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.webapi.AnswerController
import pt.ulisboa.tecnico.socialsoftware.ms.utils.BehaviourService
import pt.ulisboa.tecnico.socialsoftware.ms.utils.DateHandler

class AnswerRestApiTest extends SpockTest {
    @Autowired
    MockMvc mockMvc

    @Autowired
    ObjectMapper objectMapper

    @Autowired
    AnswerFunctionalities answerFunctionalities

    def setup() {
        BehaviourService.clearBehaviour()
    }

    def "should create QuizAnswer via REST API"() {
        when:
        def answerDto = new QuizAnswerDto()
        def jsonContent = objectMapper.writeValueAsString(answerDto)
        
        mockMvc.perform(MockMvcRequestBuilders.post("/api/answer")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonContent))
                .andExpect(MockMvcResultMatchers.status().isCreated())
        then:
        noExceptionThrown()
    }

    def "should get QuizAnswer via REST API"() {
        when:
        def answerDto = new QuizAnswerDto()
        def createdAnswer = answerFunctionalities.createAnswer(answerDto)
        
        mockMvc.perform(MockMvcRequestBuilders.get("/api/answer/" + createdAnswer.getId()))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpected(MockMvcResultMatchers.jsonPath("$.id").value(createdAnswer.getId()))
        then:
        noExceptionThrown()
    }

    def "should update QuizAnswer via REST API"() {
        when:
        def answerDto = new QuizAnswerDto()
        def createdAnswer = answerFunctionalities.createAnswer(answerDto)
        
        // Update the DTO
        createdAnswer.setName("Updated Name")
        def jsonContent = objectMapper.writeValueAsString(createdAnswer)
        
        mockMvc.perform(MockMvcRequestBuilders.put("/api/answer/" + createdAnswer.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonContent))
                .andExpected(MockMvcResultMatchers.status().isOk())
        then:
        noExceptionThrown()
    }

    def "should delete QuizAnswer via REST API"() {
        when:
        def answerDto = new QuizAnswerDto()
        def createdAnswer = answerFunctionalities.createAnswer(answerDto)
        
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/answer/" + createdAnswer.getId()))
                .andExpected(MockMvcResultMatchers.status().isNoContent())
        then:
        noExceptionThrown()
    }

    def "should list all QuizAnswers via REST API"() {
        when:
        // Create some test data
        def answerDto1 = new QuizAnswerDto()
        def answerDto2 = new QuizAnswerDto()
        answerFunctionalities.createAnswer(answerDto1)
        answerFunctionalities.createAnswer(answerDto2)
        
        mockMvc.perform(MockMvcRequestBuilders.get("/api/answer"))
                .andExpected(MockMvcResultMatchers.status().isOk())
                .andExpected(MockMvcResultMatchers.jsonPath("$.length()").value(2))
        then:
        noExceptionThrown()
    }
}
