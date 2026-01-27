package pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.aggregate.*;

import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.aggregate.QuizExecution;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.aggregate.QuizQuestion;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.aggregate.QuizOption;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.*;

import java.util.*;
import java.util.stream.Collectors;

import java.util.List;
import java.util.stream.Collectors;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.UserDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuizDto;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import java.time.LocalDateTime;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.AggregateIdGeneratorService;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.exception.AnswersException;
import pt.ulisboa.tecnico.socialsoftware.answers.coordination.webapi.requestDtos.CreateQuizRequestDto;


@Service
@Transactional
public class QuizService {
    private static final Logger logger = LoggerFactory.getLogger(QuizService.class);

    @Autowired
    private AggregateIdGeneratorService aggregateIdGeneratorService;

    @Autowired
    private UnitOfWorkService<UnitOfWork> unitOfWorkService;

    @Autowired
    private QuizRepository quizRepository;

    @Autowired
    private QuizFactory quizFactory;

    public QuizService() {}

    // CRUD Operations
    public QuizDto createQuiz(QuizExecution execution, CreateQuizRequestDto createRequest, Set<QuizQuestion> questions, UnitOfWork unitOfWork) {
        try {
            // Convert CreateRequestDto to regular DTO
            QuizDto quizDto = new QuizDto();
            quizDto.setTitle(createRequest.getTitle());
            quizDto.setQuizType(createRequest.getQuizType() != null ? createRequest.getQuizType().name() : null);
            quizDto.setCreationDate(createRequest.getCreationDate());
            quizDto.setAvailableDate(createRequest.getAvailableDate());
            quizDto.setConclusionDate(createRequest.getConclusionDate());
            quizDto.setResultsDate(createRequest.getResultsDate());
            
            Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
            Quiz quiz = quizFactory.createQuiz(aggregateId, execution, quizDto, questions);
            unitOfWorkService.registerChanged(quiz, unitOfWork);
            return quizFactory.createQuizDto(quiz);
        } catch (Exception e) {
            throw new AnswersException("Error creating quiz: " + e.getMessage());
        }
    }

    public QuizDto getQuizById(Integer id) {
        try {
            Quiz quiz = (Quiz) quizRepository.findById(id)
                .orElseThrow(() -> new AnswersException("Quiz not found with id: " + id));
            return new QuizDto(quiz);
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error retrieving quiz: " + e.getMessage());
        }
    }

    public List<QuizDto> getAllQuizs() {
        try {
            return quizRepository.findAll().stream()
                .map(entity -> new QuizDto((Quiz) entity))
                .collect(Collectors.toList());
        } catch (Exception e) {
            throw new AnswersException("Error retrieving all quizs: " + e.getMessage());
        }
    }

    public QuizDto updateQuiz(QuizDto quizDto) {
        try {
            Integer id = quizDto.getAggregateId();
            Quiz quiz = (Quiz) quizRepository.findById(id)
                .orElseThrow(() -> new AnswersException("Quiz not found with id: " + id));
            
                        if (quizDto.getTitle() != null) {
                quiz.setTitle(quizDto.getTitle());
            }
            if (quizDto.getQuizType() != null) {
                quiz.setQuizType(quizDto.getQuizType());
            }
            if (quizDto.getCreationDate() != null) {
                quiz.setCreationDate(quizDto.getCreationDate());
            }
            if (quizDto.getAvailableDate() != null) {
                quiz.setAvailableDate(quizDto.getAvailableDate());
            }
            if (quizDto.getConclusionDate() != null) {
                quiz.setConclusionDate(quizDto.getConclusionDate());
            }
            if (quizDto.getResultsDate() != null) {
                quiz.setResultsDate(quizDto.getResultsDate());
            }
            if (quizDto.getExecution() != null) {
                quiz.setExecution(quizDto.getExecution());
            }
            if (quizDto.getQuestions() != null) {
                quiz.setQuestions(quizDto.getQuestions());
            }
            
            quiz = quizRepository.save(quiz);
            return new QuizDto(quiz);
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error updating quiz: " + e.getMessage());
        }
    }

    public void deleteQuiz(Integer id) {
        try {
            if (!quizRepository.existsById(id)) {
                throw new AnswersException("Quiz not found with id: " + id);
            }
            quizRepository.deleteById(id);
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error deleting quiz: " + e.getMessage());
        }
    }

    // No business methods defined

    // No custom workflows defined

    // Query methods not implemented

    // Event Processing Methods
    private void publishQuizCreatedEvent(Quiz quiz) {
        try {
            // TODO: Implement event publishing for QuizCreated
            // eventPublisher.publishEvent(new QuizCreatedEvent(quiz));
        } catch (Exception e) {
            // Log error but don't fail the transaction
            logger.error("Failed to publish QuizCreatedEvent", e);
        }
    }

    private void publishQuizUpdatedEvent(Quiz quiz) {
        try {
            // TODO: Implement event publishing for QuizUpdated
            // eventPublisher.publishEvent(new QuizUpdatedEvent(quiz));
        } catch (Exception e) {
            // Log error but don't fail the transaction
            logger.error("Failed to publish QuizUpdatedEvent", e);
        }
    }

    private void publishQuizDeletedEvent(Long quizId) {
        try {
            // TODO: Implement event publishing for QuizDeleted
            // eventPublisher.publishEvent(new QuizDeletedEvent(quizId));
        } catch (Exception e) {
            // Log error but don't fail the transaction
            logger.error("Failed to publish QuizDeletedEvent", e);
        }
    }
}