package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.question.aggregate.sagas.repositories;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.question.aggregate.Question;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.question.aggregate.QuestionCustomRepository;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.question.aggregate.QuestionRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Profile("sagas")
public class QuestionCustomRepositorySagas implements QuestionCustomRepository {

    @Autowired
    private QuestionRepository questionRepository;

    @Override
    public List<Integer> findQuestionIdsByCourseId(Integer courseAggregateId) {
        return questionRepository.findAll().stream()
                .filter(q -> courseAggregateId.equals(q.getQuestionCourse().getCourseAggregateId()))
                .map(Question::getAggregateId)
                .distinct()
                .collect(Collectors.toList());
    }
}
