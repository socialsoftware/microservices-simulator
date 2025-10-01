package pt.ulisboa.tecnico.socialsoftware.answers

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.SagaManager

class AnswersBeanConfiguration extends SpockTest {
    @Configuration
    static class TestConfiguration {
        
        @Bean
        @Primary
        SagaManager sagaManager() {
            return Mock(SagaManager)
        }
        
        @Bean
        @Primary
        RestTemplate restTemplate() {
            return Mock(RestTemplate)
        }
    }
}
