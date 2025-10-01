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
import pt.ulisboa.tecnico.socialsoftware.answers.coordination.functionalities.ExecutionFunctionalities
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.aggregate.ExecutionDto
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.webapi.ExecutionController
import pt.ulisboa.tecnico.socialsoftware.ms.utils.BehaviourService
import pt.ulisboa.tecnico.socialsoftware.ms.utils.DateHandler

class ExecutionRestApiTest extends SpockTest {
    @Autowired
    MockMvc mockMvc

    @Autowired
    ObjectMapper objectMapper

    @Autowired
    ExecutionFunctionalities executionFunctionalities

    def setup() {
        BehaviourService.clearBehaviour()
    }

    def "should create Execution via REST API"() {
        when:
        def executionDto = new ExecutionDto()
        def jsonContent = objectMapper.writeValueAsString(executionDto)
        
        mockMvc.perform(MockMvcRequestBuilders.post("/api/execution")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonContent))
                .andExpect(MockMvcResultMatchers.status().isCreated())
        then:
        noExceptionThrown()
    }

    def "should get Execution via REST API"() {
        when:
        def executionDto = new ExecutionDto()
        def createdExecution = executionFunctionalities.createExecution(executionDto)
        
        mockMvc.perform(MockMvcRequestBuilders.get("/api/execution/" + createdExecution.getId()))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpected(MockMvcResultMatchers.jsonPath("$.id").value(createdExecution.getId()))
        then:
        noExceptionThrown()
    }

    def "should update Execution via REST API"() {
        when:
        def executionDto = new ExecutionDto()
        def createdExecution = executionFunctionalities.createExecution(executionDto)
        
        // Update the DTO
        createdExecution.setName("Updated Name")
        def jsonContent = objectMapper.writeValueAsString(createdExecution)
        
        mockMvc.perform(MockMvcRequestBuilders.put("/api/execution/" + createdExecution.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonContent))
                .andExpected(MockMvcResultMatchers.status().isOk())
        then:
        noExceptionThrown()
    }

    def "should delete Execution via REST API"() {
        when:
        def executionDto = new ExecutionDto()
        def createdExecution = executionFunctionalities.createExecution(executionDto)
        
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/execution/" + createdExecution.getId()))
                .andExpected(MockMvcResultMatchers.status().isNoContent())
        then:
        noExceptionThrown()
    }

    def "should list all Executions via REST API"() {
        when:
        // Create some test data
        def executionDto1 = new ExecutionDto()
        def executionDto2 = new ExecutionDto()
        executionFunctionalities.createExecution(executionDto1)
        executionFunctionalities.createExecution(executionDto2)
        
        mockMvc.perform(MockMvcRequestBuilders.get("/api/execution"))
                .andExpected(MockMvcResultMatchers.status().isOk())
                .andExpected(MockMvcResultMatchers.jsonPath("$.length()").value(2))
        then:
        noExceptionThrown()
    }
}
