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
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import java.time.LocalDateTime;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.exception.AnswersException;


@Service
@Transactional
public class QuizService {
    private static final Logger logger = LoggerFactory.getLogger(QuizService.class);

    @Autowired
    private QuizRepository quizRepository;

    @Autowired
    private QuizFactory quizFactory;

    public QuizService() {}

    // CRUD Operations
    public QuizDto createQuiz(String title, QuizType quizType, LocalDateTime creationDate, LocalDateTime availableDate, LocalDateTime conclusionDate, LocalDateTime resultsDate, QuizExecution execution, Set<QuizQuestion> questions) {
        try {
            Quiz quiz = new Quiz(title, quizType, creationDate, availableDate, conclusionDate, resultsDate, execution, questions);
            quiz = quizRepository.save(quiz);
            return new QuizDto(quiz);
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

    public QuizDto updateQuiz(Integer id, QuizDto quizDto) {
        try {
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

    // Business Methods
    @Transactional
    public void getAvailableQuizzes(Integer id, UnitOfWork unitOfWork) {
        try {
            Quiz quiz = quizRepository.findById(id)
                .orElseThrow(() -> new AnswersException("Quiz not found with id: " + id));
            
            // Business logic for getAvailableQuizzes
            void result = quiz.getAvailableQuizzes();
            quizRepository.save(quiz);
            return result;
        } catch (Exception e) {
            throw new AnswersException("Error in getAvailableQuizzes: " + e.getMessage());
        }
    }

    @Transactional
    public void getCompletedQuizzes(Integer id, UnitOfWork unitOfWork) {
        try {
            Quiz quiz = quizRepository.findById(id)
                .orElseThrow(() -> new AnswersException("Quiz not found with id: " + id));
            
            // Business logic for getCompletedQuizzes
            void result = quiz.getCompletedQuizzes();
            quizRepository.save(quiz);
            return result;
        } catch (Exception e) {
            throw new AnswersException("Error in getCompletedQuizzes: " + e.getMessage());
        }
    }

    @Transactional
    public void searchQuizzesByTitle(Integer id, String title, UnitOfWork unitOfWork) {
        try {
            Quiz quiz = quizRepository.findById(id)
                .orElseThrow(() -> new AnswersException("Quiz not found with id: " + id));
            
            // Business logic for searchQuizzesByTitle
            void result = quiz.searchQuizzesByTitle();
            quizRepository.save(quiz);
            return result;
        } catch (Exception e) {
            throw new AnswersException("Error in searchQuizzesByTitle: " + e.getMessage());
        }
    }

    // Custom Workflow Methods
    @Transactional
    public void invalidateQuiz(Integer quizId, UnitOfWork unitOfWork) {
        try {
            // TODO: Implement workflow logic for invalidateQuiz
            throw new UnsupportedOperationException("Workflow invalidateQuiz not implemented");

        } catch (Exception e) {
            throw new AnswersException("Error in workflow invalidateQuiz: " + e.getMessage());
        }
    }

    @Transactional
    public void removeExecution(Integer executionId, Integer quizId, UnitOfWork unitOfWork) {
        try {
            // TODO: Implement workflow logic for removeExecution
            throw new UnsupportedOperationException("Workflow removeExecution not implemented");

        } catch (Exception e) {
            throw new AnswersException("Error in workflow removeExecution: " + e.getMessage());
        }
    }

    @Transactional
    public void removeQuestion(Integer questionId, Integer quizId, UnitOfWork unitOfWork) {
        try {
            // TODO: Implement workflow logic for removeQuestion
            throw new UnsupportedOperationException("Workflow removeQuestion not implemented");

        } catch (Exception e) {
            throw new AnswersException("Error in workflow removeQuestion: " + e.getMessage());
        }
    }

    @Transactional
    public void updateQuestion(Integer questionId, String questionTitle, String questionContent, Integer quizId, UnitOfWork unitOfWork) {
        try {
            // TODO: Implement workflow logic for updateQuestion
            throw new UnsupportedOperationException("Workflow updateQuestion not implemented");

        } catch (Exception e) {
            throw new AnswersException("Error in workflow updateQuestion: " + e.getMessage());
        }
    }

    @Transactional
    public void removeUser(Integer userAggregateId, Integer quizId, UnitOfWork unitOfWork) {
        try {
            // TODO: Implement workflow logic for removeUser
            throw new UnsupportedOperationException("Workflow removeUser not implemented");

        } catch (Exception e) {
            throw new AnswersException("Error in workflow removeUser: " + e.getMessage());
        }
    }

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