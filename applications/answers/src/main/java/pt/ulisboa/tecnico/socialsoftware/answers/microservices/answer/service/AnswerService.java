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

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.AggregateIdGeneratorService;
import pt.ulisboa.tecnico.socialsoftware.answers.events.AnswerDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.events.AnswerUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.events.AnswerQuestionRemovedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.events.AnswerQuestionUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.events.AnswerExecutionUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.exception.AnswersException;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.coordination.webapi.requestDtos.CreateAnswerRequestDto;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.user.aggregate.User;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.UserDto;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.aggregate.Execution;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.ExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.aggregate.Question;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.aggregate.Quiz;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuizDto;


@Service
@Transactional(noRollbackFor = AnswersException.class)
public class AnswerService {
    @Autowired
    private AggregateIdGeneratorService aggregateIdGeneratorService;

    @Autowired
    private UnitOfWorkService unitOfWorkService;

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
                Execution refSource = (Execution) unitOfWorkService.aggregateLoadAndRegisterRead(createRequest.getExecution().getAggregateId(), unitOfWork);
                ExecutionDto refSourceDto = new ExecutionDto(refSource);
                AnswerExecutionDto executionDto = new AnswerExecutionDto();
                executionDto.setAggregateId(refSourceDto.getAggregateId());
                executionDto.setVersion(refSourceDto.getVersion());
                executionDto.setState(refSourceDto.getState() != null ? refSourceDto.getState().name() : null);

                answerDto.setExecution(executionDto);
            }
            if (createRequest.getUser() != null) {
                User refSource = (User) unitOfWorkService.aggregateLoadAndRegisterRead(createRequest.getUser().getAggregateId(), unitOfWork);
                UserDto refSourceDto = new UserDto(refSource);
                AnswerUserDto userDto = new AnswerUserDto();
                userDto.setAggregateId(refSourceDto.getAggregateId());
                userDto.setVersion(refSourceDto.getVersion());
                userDto.setState(refSourceDto.getState() != null ? refSourceDto.getState().name() : null);
                userDto.setName(refSourceDto.getName());
                answerDto.setUser(userDto);
            }
            if (createRequest.getQuiz() != null) {
                Quiz refSource = (Quiz) unitOfWorkService.aggregateLoadAndRegisterRead(createRequest.getQuiz().getAggregateId(), unitOfWork);
                QuizDto refSourceDto = new QuizDto(refSource);
                AnswerQuizDto quizDto = new AnswerQuizDto();
                quizDto.setAggregateId(refSourceDto.getAggregateId());
                quizDto.setVersion(refSourceDto.getVersion());
                quizDto.setState(refSourceDto.getState() != null ? refSourceDto.getState().name() : null);

                answerDto.setQuiz(quizDto);
            }
            if (createRequest.getQuestions() != null) {
                answerDto.setQuestions(createRequest.getQuestions().stream().map(reqDto -> {
                    Question refItem = (Question) unitOfWorkService.aggregateLoadAndRegisterRead(reqDto.getAggregateId(), unitOfWork);
                    QuestionDto refItemDto = new QuestionDto(refItem);
                    AnswerQuestionDto projDto = new AnswerQuestionDto();
                    projDto.setAggregateId(refItemDto.getAggregateId());
                    projDto.setVersion(refItemDto.getVersion());
                    projDto.setState(refItemDto.getState() != null ? refItemDto.getState().name() : null);

                    return projDto;
                }).collect(Collectors.toList()));
            }

            Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
            Answer answer = answerFactory.createAnswer(aggregateId, answerDto);
            unitOfWorkService.registerChanged(answer, unitOfWork);
            return answerFactory.createAnswerDto(answer);
        } catch (AnswersException e) {
            throw e;
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
                .map(id -> {
                    try {
                        return (Answer) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(java.util.Objects::nonNull)
                .map(answerFactory::createAnswerDto)
                .collect(Collectors.toList());
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error retrieving answer: " + e.getMessage());
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

            unitOfWorkService.registerChanged(newAnswer, unitOfWork);            AnswerUpdatedEvent event = new AnswerUpdatedEvent(newAnswer.getAggregateId(), newAnswer.getCreationDate(), newAnswer.getAnswerDate(), newAnswer.getCompleted());
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
            unitOfWorkService.registerChanged(newAnswer, unitOfWork);            unitOfWorkService.registerEvent(new AnswerDeletedEvent(newAnswer.getAggregateId()), unitOfWork);
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


    public Answer handleExecutionUserUpdatedEvent(Integer aggregateId, Integer userAggregateId, Integer userVersion, UnitOfWork unitOfWork) {
        try {
            Answer oldAnswer = (Answer) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWork);
            Answer newAnswer = answerFactory.createAnswerFromExisting(oldAnswer);

        if (newAnswer.getUser() != null && 
            newAnswer.getUser().getUserAggregateId() != null &&
            newAnswer.getUser().getUserAggregateId().equals(userAggregateId)) {
            newAnswer.getUser().setUserVersion(userVersion);
        }

            unitOfWorkService.registerChanged(newAnswer, unitOfWork);

        unitOfWorkService.registerEvent(
            new AnswerExecutionUpdatedEvent(
                    newAnswer.getAggregateId(),
                    userAggregateId,
                    userVersion
            ),
            unitOfWork
        );

            return newAnswer;
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error handling ExecutionUserUpdatedEvent answer: " + e.getMessage());
        }
    }

    public Answer handleQuestionUpdatedEvent(Integer aggregateId, Integer questionAggregateId, Integer questionVersion, UnitOfWork unitOfWork) {
        try {
            Answer oldAnswer = (Answer) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWork);
            Answer newAnswer = answerFactory.createAnswerFromExisting(oldAnswer);



            unitOfWorkService.registerChanged(newAnswer, unitOfWork);


            return newAnswer;
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error handling QuestionUpdatedEvent answer: " + e.getMessage());
        }
    }




}