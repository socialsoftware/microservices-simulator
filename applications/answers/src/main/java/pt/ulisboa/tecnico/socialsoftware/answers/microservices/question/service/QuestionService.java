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
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.user.aggregate.UserDto;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.exception.AnswersException;


@Service
@Transactional
public class QuestionService {
    private static final Logger logger = LoggerFactory.getLogger(QuestionService.class);

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private QuestionFactory questionFactory;

    public QuestionService() {}

    // CRUD Operations
    public QuestionDto createQuestion(String title, String content, Integer numberOfOptions, Integer correctOption, Integer order, QuestionCourse course, Set<QuestionTopic> topics, Set<Option> options) {
        try {
            Question question = new Question(title, content, numberOfOptions, correctOption, order, course, topics, options);
            question = questionRepository.save(question);
            return new QuestionDto(question);
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

    public QuestionDto updateQuestion(Integer id, QuestionDto questionDto) {
        try {
            Question question = (Question) questionRepository.findById(id)
                .orElseThrow(() -> new AnswersException("Question not found with id: " + id));
            
                        if (questionDto.getTitle() != null) {
                question.setTitle(questionDto.getTitle());
            }
            if (questionDto.getContent() != null) {
                question.setContent(questionDto.getContent());
            }
            if (questionDto.getNumberOfOptions() != null) {
                question.setNumberOfOptions(questionDto.getNumberOfOptions());
            }
            if (questionDto.getCorrectOption() != null) {
                question.setCorrectOption(questionDto.getCorrectOption());
            }
            if (questionDto.getOrder() != null) {
                question.setOrder(questionDto.getOrder());
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

    // Business Methods
    @Transactional
    public void searchQuestionsByTitle(Integer id, String title, UnitOfWork unitOfWork) {
        try {
            Question question = questionRepository.findById(id)
                .orElseThrow(() -> new AnswersException("Question not found with id: " + id));
            
            // Business logic for searchQuestionsByTitle
            void result = question.searchQuestionsByTitle();
            questionRepository.save(question);
            return result;
        } catch (Exception e) {
            throw new AnswersException("Error in searchQuestionsByTitle: " + e.getMessage());
        }
    }

    // Custom Workflow Methods
    @Transactional
    public void deleteQuestion(Integer questionId, UnitOfWork unitOfWork) {
        try {
            // TODO: Implement workflow logic for deleteQuestion
            throw new UnsupportedOperationException("Workflow deleteQuestion not implemented");

        } catch (Exception e) {
            throw new AnswersException("Error in workflow deleteQuestion: " + e.getMessage());
        }
    }

    @Transactional
    public void updateQuestion(Integer questionId, String title, String content, UnitOfWork unitOfWork) {
        try {
            // TODO: Implement workflow logic for updateQuestion
            throw new UnsupportedOperationException("Workflow updateQuestion not implemented");

        } catch (Exception e) {
            throw new AnswersException("Error in workflow updateQuestion: " + e.getMessage());
        }
    }

    @Transactional
    public void removeCourse(Integer courseId, Integer questionId, UnitOfWork unitOfWork) {
        try {
            // TODO: Implement workflow logic for removeCourse
            throw new UnsupportedOperationException("Workflow removeCourse not implemented");

        } catch (Exception e) {
            throw new AnswersException("Error in workflow removeCourse: " + e.getMessage());
        }
    }

    @Transactional
    public void removeTopic(Integer topicId, Integer questionId, UnitOfWork unitOfWork) {
        try {
            // TODO: Implement workflow logic for removeTopic
            throw new UnsupportedOperationException("Workflow removeTopic not implemented");

        } catch (Exception e) {
            throw new AnswersException("Error in workflow removeTopic: " + e.getMessage());
        }
    }

    // Query methods disabled - repository methods not implemented

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