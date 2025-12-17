package pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.service;

import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.dao.CannotAcquireLockException;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.AggregateIdGeneratorService;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.aggregate.*;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.*;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.events.publish.QuestionUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.events.publish.QuestionDeletedEvent;

@Service
public class QuestionService {
    @Autowired
    private AggregateIdGeneratorService aggregateIdGeneratorService;

    private final UnitOfWorkService<UnitOfWork> unitOfWorkService;

    private final QuestionRepository questionRepository;

    @Autowired
    private QuestionFactory questionFactory;

    public QuestionService(UnitOfWorkService unitOfWorkService, QuestionRepository questionRepository) {
        this.unitOfWorkService = unitOfWorkService;
        this.questionRepository = questionRepository;
    }

    @Retryable(
            value = { SQLException.class, CannotAcquireLockException.class },
            maxAttemptsExpression = "${retry.db.maxAttempts}",
            backoff = @Backoff(
                delayExpression = "${retry.db.delay}",
                multiplierExpression = "${retry.db.multiplier}"
            ))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public QuestionDto createQuestion(QuestionDto questionDto, UnitOfWork unitOfWork) {
        Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
        Question question = questionFactory.createQuestion(aggregateId, questionDto);
        unitOfWorkService.registerChanged(question, unitOfWork);
        return questionFactory.createQuestionDto(question);
    }

    @Retryable(
            value = { SQLException.class, CannotAcquireLockException.class },
            maxAttemptsExpression = "${retry.db.maxAttempts}",
            backoff = @Backoff(
                delayExpression = "${retry.db.delay}",
                multiplierExpression = "${retry.db.multiplier}"
            ))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public QuestionDto getQuestionById(Integer aggregateId, UnitOfWork unitOfWork) {
        return questionFactory.createQuestionDto((Question) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWork));
    }

    @Retryable(
            value = { SQLException.class, CannotAcquireLockException.class },
            maxAttemptsExpression = "${retry.db.maxAttempts}",
            backoff = @Backoff(
                delayExpression = "${retry.db.delay}",
                multiplierExpression = "${retry.db.multiplier}"
            ))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public QuestionDto updateQuestion(Integer aggregateId, QuestionDto questionDto, UnitOfWork unitOfWork) {
        Question oldQuestion = (Question) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWork);
        Question newQuestion = questionFactory.createQuestionFromExisting(oldQuestion);
        newQuestion.setTitle(questionDto.getTitle());
        newQuestion.setContent(questionDto.getContent());
        newQuestion.setCreationDate(questionDto.getCreationDate());
        unitOfWorkService.registerChanged(newQuestion, unitOfWork);
        unitOfWorkService.registerEvent(new QuestionUpdatedEvent(newQuestion.getAggregateId(), newQuestion.getTitle(), newQuestion.getContent(), newQuestion.getCreationDate()), unitOfWork);
        return questionFactory.createQuestionDto(newQuestion);
    }

    @Retryable(
            value = { SQLException.class, CannotAcquireLockException.class },
            maxAttemptsExpression = "${retry.db.maxAttempts}",
            backoff = @Backoff(
                delayExpression = "${retry.db.delay}",
                multiplierExpression = "${retry.db.multiplier}"
            ))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void deleteQuestion(Integer aggregateId, UnitOfWork unitOfWork) {
        Question oldQuestion = (Question) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWork);
        Question newQuestion = questionFactory.createQuestionFromExisting(oldQuestion);
        newQuestion.remove();
        unitOfWorkService.registerChanged(newQuestion, unitOfWork);
        unitOfWorkService.registerEvent(new QuestionDeletedEvent(newQuestion.getAggregateId()), unitOfWork);
    }

    @Retryable(
            value = { SQLException.class, CannotAcquireLockException.class },
            maxAttemptsExpression = "${retry.db.maxAttempts}",
            backoff = @Backoff(
                delayExpression = "${retry.db.delay}",
                multiplierExpression = "${retry.db.multiplier}"
            ))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public List<QuestionDto> searchQuestions(String title, String content, Integer courseAggregateId, UnitOfWork unitOfWork) {
        Set<Integer> aggregateIds = questionRepository.findAll().stream()
                .filter(entity -> {
                    if (title != null) {
                        if (!entity.getTitle().equals(title)) {
                            return false;
                        }
                    }
                    if (content != null) {
                        if (!entity.getContent().equals(content)) {
                            return false;
                        }
                    }
                    if (courseAggregateId != null) {
                        if (!entity.getCourse().getCourseAggregateId().equals(courseAggregateId)) {
                            return false;
                        }
                                            }
                    return true;
                })
                .map(Question::getAggregateId)
                .collect(Collectors.toSet());
        return aggregateIds.stream()
                .map(id -> (Question) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork))
                .map(questionFactory::createQuestionDto)
                .collect(Collectors.toList());
    }

}
