package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.aggregates.repositories;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.aggregate.QuestionCustomRepository;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.aggregate.QuestionRepository;


@Service
@Profile("tcc")
public class QuestionCustomRepositoryTCC implements QuestionCustomRepository {

    @Autowired
    private QuestionRepository questionRepository;
}
