package pt.ulisboa.tecnico.socialsoftware.answers

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

class AnswersBeanConfiguration extends SpockTest {
    @Configuration
    static class TestConfiguration {
        
        @Bean
        @Primary
        RestTemplate restTemplate() {
            return Mock(RestTemplate)
        }
    }
}
