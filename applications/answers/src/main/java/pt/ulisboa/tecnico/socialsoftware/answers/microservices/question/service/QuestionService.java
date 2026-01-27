package pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.aggregate.*;

import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.aggregate.QuestionCourse;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.aggregate.QuestionTopic;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.aggregate.Option;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.*;

import java.util.*;
import java.util.stream.Collectors;

import java.util.List;
import java.util.stream.Collectors;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.UserDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import java.time.LocalDateTime;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.AggregateIdGeneratorService;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.exception.AnswersException;
import pt.ulisboa.tecnico.socialsoftware.answers.coordination.webapi.requestDtos.CreateQuestionRequestDto;


@Service
@Transactional
public class QuestionService {
    private static final Logger logger = LoggerFactory.getLogger(QuestionService.class);

    @Autowired
    private AggregateIdGeneratorService aggregateIdGeneratorService;

    @Autowired
    private UnitOfWorkService<UnitOfWork> unitOfWorkService;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private QuestionFactory questionFactory;

    public QuestionService() {}

    // CRUD Operations
    public QuestionDto createQuestion(QuestionCourse course, CreateQuestionRequestDto createRequest, Set<QuestionTopic> topics, List<Option> options, UnitOfWork unitOfWork) {
        try {
            // Convert CreateRequestDto to regular DTO
            QuestionDto questionDto = new QuestionDto();
            questionDto.setTitle(createRequest.getTitle());
            questionDto.setContent(createRequest.getContent());
            questionDto.setCreationDate(createRequest.getCreationDate());
            
            Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
            Question question = questionFactory.createQuestion(aggregateId, course, questionDto, topics, options);
            unitOfWorkService.registerChanged(question, unitOfWork);
            return questionFactory.createQuestionDto(question);
        } catch (Exception e) {
            throw new AnswersException("Error creating question: " + e.getMessage());
        }
    }

    public QuestionDto getQuestionById(Integer id) {
        try {
            Question question = (Question) questionRepository.findById(id)
                .orElseThrow(() -> new AnswersException("Question not found with id: " + id));
            return new QuestionDto(question);
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error retrieving question: " + e.getMessage());
        }
    }

    public List<QuestionDto> getAllQuestions() {
        try {
            return questionRepository.findAll().stream()
                .map(entity -> new QuestionDto((Question) entity))
                .collect(Collectors.toList());
        } catch (Exception e) {
            throw new AnswersException("Error retrieving all questions: " + e.getMessage());
        }
    }

    public QuestionDto updateQuestion(QuestionDto questionDto) {
        try {
            Integer id = questionDto.getAggregateId();
            Question question = (Question) questionRepository.findById(id)
                .orElseThrow(() -> new AnswersException("Question not found with id: " + id));
            
                        if (questionDto.getTitle() != null) {
                question.setTitle(questionDto.getTitle());
            }
            if (questionDto.getContent() != null) {
                question.setContent(questionDto.getContent());
            }
            if (questionDto.getCreationDate() != null) {
                question.setCreationDate(questionDto.getCreationDate());
            }
            if (questionDto.getCourse() != null) {
                question.setCourse(questionDto.getCourse());
            }
            if (questionDto.getTopics() != null) {
                question.setTopics(questionDto.getTopics());
            }
            if (questionDto.getOptions() != null) {
                question.setOptions(questionDto.getOptions());
            }
            
            question = questionRepository.save(question);
            return new QuestionDto(question);
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error updating question: " + e.getMessage());
        }
    }

    public void deleteQuestion(Integer id) {
        try {
            if (!questionRepository.existsById(id)) {
                throw new AnswersException("Question not found with id: " + id);
            }
            questionRepository.deleteById(id);
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error deleting question: " + e.getMessage());
        }
    }

    // No business methods defined

    // No custom workflows defined

    // Query methods not implemented

    // Event Processing Methods
    private void publishQuestionCreatedEvent(Question question) {
        try {
            // TODO: Implement event publishing for QuestionCreated
            // eventPublisher.publishEvent(new QuestionCreatedEvent(question));
        } catch (Exception e) {
            // Log error but don't fail the transaction
            logger.error("Failed to publish QuestionCreatedEvent", e);
        }
    }

    private void publishQuestionUpdatedEvent(Question question) {
        try {
            // TODO: Implement event publishing for QuestionUpdated
            // eventPublisher.publishEvent(new QuestionUpdatedEvent(question));
        } catch (Exception e) {
            // Log error but don't fail the transaction
            logger.error("Failed to publish QuestionUpdatedEvent", e);
        }
    }

    private void publishQuestionDeletedEvent(Long questionId) {
        try {
            // TODO: Implement event publishing for QuestionDeleted
            // eventPublisher.publishEvent(new QuestionDeletedEvent(questionId));
        } catch (Exception e) {
            // Log error but don't fail the transaction
            logger.error("Failed to publish QuestionDeletedEvent", e);
        }
    }
}