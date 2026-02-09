package pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.AnswerDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.AnswerUserDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.AnswerExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.AnswerQuestionDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.AnswerQuizDto;
import java.time.LocalDateTime;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.AggregateIdGeneratorService;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.events.publish.AnswerDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.events.publish.AnswerUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.events.publish.AnswerUserDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.events.publish.AnswerUserUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.events.publish.AnswerExecutionDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.events.publish.AnswerExecutionUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.events.publish.AnswerQuestionDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.events.publish.AnswerQuestionUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.events.publish.AnswerQuizDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.events.publish.AnswerQuizUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.events.publish.AnswerQuestionRemovedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.events.publish.AnswerQuestionUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.exception.AnswersException;
import pt.ulisboa.tecnico.socialsoftware.answers.coordination.webapi.requestDtos.CreateAnswerRequestDto;


@Service
@Transactional
public class AnswerService {
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
            if (createRequest.getQuestions() != null) {
                answerDto.setQuestions(createRequest.getQuestions().stream().map(srcDto -> {
                    AnswerQuestionDto projDto = new AnswerQuestionDto();
                    projDto.setAggregateId(srcDto.getAggregateId());
                    projDto.setVersion(srcDto.getVersion());
                    projDto.setState(srcDto.getState());
                    return projDto;
                }).collect(Collectors.toList()));
            }
            
            Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
            Answer answer = answerFactory.createAnswer(aggregateId, answerDto);
            unitOfWorkService.registerChanged(answer, unitOfWork);
            return answerFactory.createAnswerDto(answer);
        } catch (Exception e) {
            throw new AnswersException("Error creating answer: " + e.getMessage());
        }
    }

    public AnswerDto getAnswerById(Integer id, UnitOfWork unitOfWork) {
        try {
            Answer answer = (Answer) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            return answerFactory.createAnswerDto(answer);
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error retrieving answer: " + e.getMessage());
        }
    }

    public List<AnswerDto> getAllAnswers(UnitOfWork unitOfWork) {
        try {
            Set<Integer> aggregateIds = answerRepository.findAll().stream()
                .map(Answer::getAggregateId)
                .collect(Collectors.toSet());

            return aggregateIds.stream()
                .map(id -> (Answer) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork))
                .map(answerFactory::createAnswerDto)
                .collect(Collectors.toList());
        } catch (Exception e) {
            throw new AnswersException("Error retrieving all answers: " + e.getMessage());
        }
    }

    public AnswerDto updateAnswer(AnswerDto answerDto, UnitOfWork unitOfWork) {
        try {
            Integer id = answerDto.getAggregateId();
            Answer oldAnswer = (Answer) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            Answer newAnswer = answerFactory.createAnswerFromExisting(oldAnswer);
            if (answerDto.getCreationDate() != null) {
                newAnswer.setCreationDate(answerDto.getCreationDate());
            }
            if (answerDto.getAnswerDate() != null) {
                newAnswer.setAnswerDate(answerDto.getAnswerDate());
            }
            newAnswer.setCompleted(answerDto.getCompleted());

            unitOfWorkService.registerChanged(newAnswer, unitOfWork);
            AnswerUpdatedEvent event = new AnswerUpdatedEvent(newAnswer.getAggregateId(), newAnswer.getCreationDate(), newAnswer.getAnswerDate(), newAnswer.getCompleted());
            event.setPublisherAggregateVersion(newAnswer.getVersion());
            unitOfWorkService.registerEvent(event, unitOfWork);
            return answerFactory.createAnswerDto(newAnswer);
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error updating answer: " + e.getMessage());
        }
    }

    public void deleteAnswer(Integer id, UnitOfWork unitOfWork) {
        try {
            Answer oldAnswer = (Answer) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            Answer newAnswer = answerFactory.createAnswerFromExisting(oldAnswer);
            newAnswer.remove();
            unitOfWorkService.registerChanged(newAnswer, unitOfWork);
            unitOfWorkService.registerEvent(new AnswerDeletedEvent(newAnswer.getAggregateId()), unitOfWork);
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error deleting answer: " + e.getMessage());
        }
    }

    public AnswerQuestionDto addAnswerQuestion(Integer answerId, Integer questionAggregateId, AnswerQuestionDto AnswerQuestionDto, UnitOfWork unitOfWork) {
        try {
            Answer oldAnswer = (Answer) unitOfWorkService.aggregateLoadAndRegisterRead(answerId, unitOfWork);
            Answer newAnswer = answerFactory.createAnswerFromExisting(oldAnswer);
            AnswerQuestion element = new AnswerQuestion(AnswerQuestionDto);
            newAnswer.getQuestions().add(element);
            unitOfWorkService.registerChanged(newAnswer, unitOfWork);
            return AnswerQuestionDto;
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error adding AnswerQuestion: " + e.getMessage());
        }
    }

    public List<AnswerQuestionDto> addAnswerQuestions(Integer answerId, List<AnswerQuestionDto> AnswerQuestionDtos, UnitOfWork unitOfWork) {
        try {
            Answer oldAnswer = (Answer) unitOfWorkService.aggregateLoadAndRegisterRead(answerId, unitOfWork);
            Answer newAnswer = answerFactory.createAnswerFromExisting(oldAnswer);
            AnswerQuestionDtos.forEach(dto -> {
                AnswerQuestion element = new AnswerQuestion(dto);
                newAnswer.getQuestions().add(element);
            });
            unitOfWorkService.registerChanged(newAnswer, unitOfWork);
            return AnswerQuestionDtos;
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error adding AnswerQuestions: " + e.getMessage());
        }
    }

    public AnswerQuestionDto getAnswerQuestion(Integer answerId, Integer questionAggregateId, UnitOfWork unitOfWork) {
        try {
            Answer answer = (Answer) unitOfWorkService.aggregateLoadAndRegisterRead(answerId, unitOfWork);
            AnswerQuestion element = answer.getQuestions().stream()
                .filter(item -> item.getQuestionAggregateId() != null &&
                               item.getQuestionAggregateId().equals(questionAggregateId))
                .findFirst()
                .orElseThrow(() -> new AnswersException("AnswerQuestion not found"));
            return element.buildDto();
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error retrieving AnswerQuestion: " + e.getMessage());
        }
    }

    public void removeAnswerQuestion(Integer answerId, Integer questionAggregateId, UnitOfWork unitOfWork) {
        try {
            Answer oldAnswer = (Answer) unitOfWorkService.aggregateLoadAndRegisterRead(answerId, unitOfWork);
            Answer newAnswer = answerFactory.createAnswerFromExisting(oldAnswer);
            newAnswer.getQuestions().removeIf(item ->
                item.getQuestionAggregateId() != null &&
                item.getQuestionAggregateId().equals(questionAggregateId)
            );
            unitOfWorkService.registerChanged(newAnswer, unitOfWork);
            AnswerQuestionRemovedEvent event = new AnswerQuestionRemovedEvent(answerId, questionAggregateId);
            event.setPublisherAggregateVersion(newAnswer.getVersion());
            unitOfWorkService.registerEvent(event, unitOfWork);
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error removing AnswerQuestion: " + e.getMessage());
        }
    }

    public AnswerQuestionDto updateAnswerQuestion(Integer answerId, Integer questionAggregateId, AnswerQuestionDto AnswerQuestionDto, UnitOfWork unitOfWork) {
        try {
            Answer oldAnswer = (Answer) unitOfWorkService.aggregateLoadAndRegisterRead(answerId, unitOfWork);
            Answer newAnswer = answerFactory.createAnswerFromExisting(oldAnswer);
            AnswerQuestion element = newAnswer.getQuestions().stream()
                .filter(item -> item.getQuestionAggregateId() != null &&
                               item.getQuestionAggregateId().equals(questionAggregateId))
                .findFirst()
                .orElseThrow(() -> new AnswersException("AnswerQuestion not found"));

            unitOfWorkService.registerChanged(newAnswer, unitOfWork);
            AnswerQuestionUpdatedEvent event = new AnswerQuestionUpdatedEvent(answerId, element.getQuestionAggregateId(), element.getQuestionVersion(), element.getSequence(), element.getKey(), element.getTimeTaken(), element.getCorrect());
            event.setPublisherAggregateVersion(newAnswer.getVersion());
            unitOfWorkService.registerEvent(event, unitOfWork);
            return element.buildDto();
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error updating AnswerQuestion: " + e.getMessage());
        }
    }


    public Answer handleExecutionUserUpdatedEvent(Integer aggregateId, Integer executionuserAggregateId, Integer executionuserVersion, UnitOfWork unitOfWork) {
        try {
            Answer oldAnswer = (Answer) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWork);
            Answer newAnswer = answerFactory.createAnswerFromExisting(oldAnswer);



            unitOfWorkService.registerChanged(newAnswer, unitOfWork);


            return newAnswer;
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error handling ExecutionUserUpdatedEvent: " + e.getMessage());
        }
    }

    public Answer handleExecutionUserDeletedEvent(Integer aggregateId, Integer executionuserAggregateId, Integer executionuserVersion, UnitOfWork unitOfWork) {
        try {
            Answer oldAnswer = (Answer) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWork);
            Answer newAnswer = answerFactory.createAnswerFromExisting(oldAnswer);



            unitOfWorkService.registerChanged(newAnswer, unitOfWork);


            return newAnswer;
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error handling ExecutionUserDeletedEvent: " + e.getMessage());
        }
    }

    public Answer handleQuestionUpdatedEvent(Integer aggregateId, Integer questionAggregateId, Integer questionVersion, UnitOfWork unitOfWork) {
        try {
            Answer oldAnswer = (Answer) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWork);
            Answer newAnswer = answerFactory.createAnswerFromExisting(oldAnswer);

        // Handle questions collection
        if (newAnswer.getQuestions() != null) {
            newAnswer.getQuestions().stream()
                .filter(item -> item.getQuestionAggregateId() != null && 
                               item.getQuestionAggregateId().equals(questionAggregateId))
                .forEach(item -> item.setQuestionVersion(questionVersion));
        }

            unitOfWorkService.registerChanged(newAnswer, unitOfWork);


            return newAnswer;
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error handling QuestionUpdatedEvent: " + e.getMessage());
        }
    }

    public Answer handleQuestionDeletedEvent(Integer aggregateId, Integer questionAggregateId, Integer questionVersion, UnitOfWork unitOfWork) {
        try {
            Answer oldAnswer = (Answer) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWork);
            Answer newAnswer = answerFactory.createAnswerFromExisting(oldAnswer);

        // Handle questions collection
        if (newAnswer.getQuestions() != null) {
            newAnswer.getQuestions().stream()
                .filter(item -> item.getQuestionAggregateId() != null && 
                               item.getQuestionAggregateId().equals(questionAggregateId))
                .forEach(item -> item.setQuestionState(Aggregate.AggregateState.INACTIVE));
        }

            unitOfWorkService.registerChanged(newAnswer, unitOfWork);

        unitOfWorkService.registerEvent(
            new AnswerQuestionDeletedEvent(
                newAnswer.getAggregateId(),
                questionAggregateId
            ),
            unitOfWork
        );

            return newAnswer;
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error handling QuestionDeletedEvent: " + e.getMessage());
        }
    }




}