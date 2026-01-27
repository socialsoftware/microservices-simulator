package pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate.*;

import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate.AnswerUser;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate.AnswerExecution;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate.QuestionAnswered;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate.AnswerQuiz;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.*;

import java.util.*;
import java.util.stream.Collectors;

import java.util.List;
import java.util.stream.Collectors;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.UserDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.AnswerDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.AnswerUserDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.AnswerExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuestionAnsweredDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.AnswerQuizDto;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import java.time.LocalDateTime;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.AggregateIdGeneratorService;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.exception.AnswersException;
import pt.ulisboa.tecnico.socialsoftware.answers.coordination.webapi.requestDtos.CreateAnswerRequestDto;


@Service
@Transactional
public class AnswerService {
    private static final Logger logger = LoggerFactory.getLogger(AnswerService.class);

    @Autowired
    private AggregateIdGeneratorService aggregateIdGeneratorService;

    @Autowired
    private UnitOfWorkService<UnitOfWork> unitOfWorkService;

    @Autowired
    private AnswerRepository answerRepository;

    @Autowired
    private AnswerFactory answerFactory;

    public AnswerService() {}

    public AnswerDto createAnswer(CreateAnswerRequestDto createRequest, UnitOfWork unitOfWork) {
        try {
            AnswerDto answerDto = new AnswerDto();
            answerDto.setCreationDate(createRequest.getCreationDate());
            answerDto.setAnswerDate(createRequest.getAnswerDate());
            answerDto.setCompleted(createRequest.getCompleted());
            if (createRequest.getExecution() != null) {
                AnswerExecutionDto executionDto = new AnswerExecutionDto();
                executionDto.setAggregateId(createRequest.getExecution().getAggregateId());
                executionDto.setVersion(createRequest.getExecution().getVersion());
                executionDto.setState(createRequest.getExecution().getState());
                answerDto.setExecution(executionDto);
            }
            if (createRequest.getUser() != null) {
                AnswerUserDto userDto = new AnswerUserDto();
                userDto.setAggregateId(createRequest.getUser().getAggregateId());
                userDto.setVersion(createRequest.getUser().getVersion());
                userDto.setState(createRequest.getUser().getState());
                answerDto.setUser(userDto);
            }
            if (createRequest.getQuiz() != null) {
                AnswerQuizDto quizDto = new AnswerQuizDto();
                quizDto.setAggregateId(createRequest.getQuiz().getAggregateId());
                quizDto.setVersion(createRequest.getQuiz().getVersion());
                quizDto.setState(createRequest.getQuiz().getState());
                answerDto.setQuiz(quizDto);
            }
            answerDto.setQuestions(createRequest.getQuestions());
            
            Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
            Answer answer = answerFactory.createAnswer(aggregateId, answerDto);
            unitOfWorkService.registerChanged(answer, unitOfWork);
            return answerFactory.createAnswerDto(answer);
        } catch (Exception e) {
            throw new AnswersException("Error creating answer: " + e.getMessage());
        }
    }

    public AnswerDto getAnswerById(Integer id) {
        try {
            Answer answer = (Answer) answerRepository.findById(id)
                .orElseThrow(() -> new AnswersException("Answer not found with id: " + id));
            return new AnswerDto(answer);
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error retrieving answer: " + e.getMessage());
        }
    }

    public List<AnswerDto> getAllAnswers() {
        try {
            return answerRepository.findAll().stream()
                .map(entity -> new AnswerDto((Answer) entity))
                .collect(Collectors.toList());
        } catch (Exception e) {
            throw new AnswersException("Error retrieving all answers: " + e.getMessage());
        }
    }

    public AnswerDto updateAnswer(AnswerDto answerDto) {
        try {
            Integer id = answerDto.getAggregateId();
            Answer answer = (Answer) answerRepository.findById(id)
                .orElseThrow(() -> new AnswersException("Answer not found with id: " + id));
            if (answerDto.getCreationDate() != null) {
                answer.setCreationDate(answerDto.getCreationDate());
            }
            if (answerDto.getAnswerDate() != null) {
                answer.setAnswerDate(answerDto.getAnswerDate());
            }
            answer.setCompleted(answerDto.getCompleted());

            answer = answerRepository.save(answer);
            return new AnswerDto(answer);
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error updating answer: " + e.getMessage());
        }
    }

    public void deleteAnswer(Integer id) {
        try {
            if (!answerRepository.existsById(id)) {
                throw new AnswersException("Answer not found with id: " + id);
            }
            answerRepository.deleteById(id);
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error deleting answer: " + e.getMessage());
        }
    }

    // No business methods defined

    // No custom workflows defined

    // Query methods not implemented

    // Event Processing Methods
    private void publishAnswerCreatedEvent(Answer answer) {
        try {
            // TODO: Implement event publishing for AnswerCreated
            // eventPublisher.publishEvent(new AnswerCreatedEvent(answer));
        } catch (Exception e) {
            // Log error but don't fail the transaction
            logger.error("Failed to publish AnswerCreatedEvent", e);
        }
    }

    private void publishAnswerUpdatedEvent(Answer answer) {
        try {
            // TODO: Implement event publishing for AnswerUpdated
            // eventPublisher.publishEvent(new AnswerUpdatedEvent(answer));
        } catch (Exception e) {
            // Log error but don't fail the transaction
            logger.error("Failed to publish AnswerUpdatedEvent", e);
        }
    }

    private void publishAnswerDeletedEvent(Long answerId) {
        try {
            // TODO: Implement event publishing for AnswerDeleted
            // eventPublisher.publishEvent(new AnswerDeletedEvent(answerId));
        } catch (Exception e) {
            // Log error but don't fail the transaction
            logger.error("Failed to publish AnswerDeletedEvent", e);
        }
    }
}