package pt.ulisboa.tecnico.socialsoftware.quizzes.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.events.publish.QuizAnswerQuestionAnswerEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.events.publish.AnonymizeStudentEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.events.publish.DeleteCourseExecutionEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.events.publish.DisenrollStudentFromCourseExecutionEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.events.publish.UpdateStudentNameEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.events.publish.DeleteQuestionEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.events.publish.UpdateQuestionEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.events.publish.InvalidateQuizEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.events.publish.DeleteTopicEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.events.publish.UpdateTopicEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.events.publish.DeleteUserEvent;

import java.util.HashMap;
import java.util.Map;

@Configuration
@Profile("stream")
public class EventClassMapConfig {

    @Bean
    public Map<String, Class<? extends Event>> eventClassMap() {
        Map<String, Class<? extends Event>> map = new HashMap<>();
        
        // Topic events
        map.put("UpdateTopicEvent", UpdateTopicEvent.class);
        map.put("DeleteTopicEvent", DeleteTopicEvent.class);
        
        // User events
        map.put("DeleteUserEvent", DeleteUserEvent.class);
        
        // CourseExecution events
        map.put("UpdateStudentNameEvent", UpdateStudentNameEvent.class);
        map.put("DisenrollStudentFromCourseExecutionEvent", DisenrollStudentFromCourseExecutionEvent.class);
        map.put("DeleteCourseExecutionEvent", DeleteCourseExecutionEvent.class);
        map.put("AnonymizeStudentEvent", AnonymizeStudentEvent.class);
        
        // Quiz events
        map.put("InvalidateQuizEvent", InvalidateQuizEvent.class);
        
        // Question events
        map.put("UpdateQuestionEvent", UpdateQuestionEvent.class);
        map.put("DeleteQuestionEvent", DeleteQuestionEvent.class);
        
        // Answer events
        map.put("QuizAnswerQuestionAnswerEvent", QuizAnswerQuestionAnswerEvent.class);
        
        return map;
    }
}
