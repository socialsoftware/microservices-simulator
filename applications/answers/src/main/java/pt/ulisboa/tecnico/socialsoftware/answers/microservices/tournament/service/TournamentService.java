package pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.TournamentDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.TournamentExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.TournamentCreatorDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.TournamentParticipantDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.TournamentTopicDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.TournamentQuizDto;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.AggregateIdGeneratorService;
import pt.ulisboa.tecnico.socialsoftware.answers.events.TournamentDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.events.TournamentUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.events.TournamentParticipantRemovedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.events.TournamentParticipantUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.events.TournamentTopicRemovedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.events.TournamentTopicUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.exception.AnswersException;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.coordination.webapi.requestDtos.CreateTournamentRequestDto;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.aggregate.Execution;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.ExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.user.aggregate.User;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.UserDto;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.aggregate.Quiz;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuizDto;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.aggregate.Topic;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.TopicDto;


@Service
@Transactional(noRollbackFor = AnswersException.class)
public class TournamentService {
    @Autowired
    private AggregateIdGeneratorService aggregateIdGeneratorService;

    @Autowired
    private UnitOfWorkService unitOfWorkService;

    @Autowired
    private TournamentRepository tournamentRepository;

    @Autowired
    private TournamentFactory tournamentFactory;

    public TournamentService() {}

    public TournamentDto createTournament(CreateTournamentRequestDto createRequest, UnitOfWork unitOfWork) {
        try {
            TournamentDto tournamentDto = new TournamentDto();
            tournamentDto.setStartTime(createRequest.getStartTime());
            tournamentDto.setEndTime(createRequest.getEndTime());
            tournamentDto.setNumberOfQuestions(createRequest.getNumberOfQuestions());
            tournamentDto.setCancelled(createRequest.getCancelled());
            if (createRequest.getCreator() != null) {
                User refSource = (User) unitOfWorkService.aggregateLoadAndRegisterRead(createRequest.getCreator().getAggregateId(), unitOfWork);
                UserDto refSourceDto = new UserDto(refSource);
                TournamentCreatorDto creatorDto = new TournamentCreatorDto();
                creatorDto.setAggregateId(refSourceDto.getAggregateId());
                creatorDto.setVersion(refSourceDto.getVersion());
                creatorDto.setState(refSourceDto.getState() != null ? refSourceDto.getState().name() : null);
                creatorDto.setName(refSourceDto.getName());
                creatorDto.setUsername(refSourceDto.getUsername());
                tournamentDto.setCreator(creatorDto);
            }
            if (createRequest.getParticipants() != null) {
                tournamentDto.setParticipants(createRequest.getParticipants().stream().map(reqDto -> {
                    User refItem = (User) unitOfWorkService.aggregateLoadAndRegisterRead(reqDto.getAggregateId(), unitOfWork);
                    UserDto refItemDto = new UserDto(refItem);
                    TournamentParticipantDto projDto = new TournamentParticipantDto();
                    projDto.setAggregateId(refItemDto.getAggregateId());
                    projDto.setVersion(refItemDto.getVersion());
                    projDto.setState(refItemDto.getState() != null ? refItemDto.getState().name() : null);
                    projDto.setName(refItemDto.getName());
                    projDto.setUsername(refItemDto.getUsername());
                    return projDto;
                }).collect(Collectors.toSet()));
            }
            if (createRequest.getExecution() != null) {
                Execution refSource = (Execution) unitOfWorkService.aggregateLoadAndRegisterRead(createRequest.getExecution().getAggregateId(), unitOfWork);
                ExecutionDto refSourceDto = new ExecutionDto(refSource);
                TournamentExecutionDto executionDto = new TournamentExecutionDto();
                executionDto.setAggregateId(refSourceDto.getAggregateId());
                executionDto.setVersion(refSourceDto.getVersion());
                executionDto.setState(refSourceDto.getState() != null ? refSourceDto.getState().name() : null);
                executionDto.setCourseAggregateId((refSourceDto.getCourse() != null ? refSourceDto.getCourse().getAggregateId() : null));
                executionDto.setAcronym(refSourceDto.getAcronym());
                tournamentDto.setExecution(executionDto);
            }
            if (createRequest.getTopics() != null) {
                tournamentDto.setTopics(createRequest.getTopics().stream().map(reqDto -> {
                    Topic refItem = (Topic) unitOfWorkService.aggregateLoadAndRegisterRead(reqDto.getAggregateId(), unitOfWork);
                    TopicDto refItemDto = new TopicDto(refItem);
                    TournamentTopicDto projDto = new TournamentTopicDto();
                    projDto.setAggregateId(refItemDto.getAggregateId());
                    projDto.setVersion(refItemDto.getVersion());
                    projDto.setState(refItemDto.getState() != null ? refItemDto.getState().name() : null);
                    projDto.setName(refItemDto.getName());
                    projDto.setCourseAggregateId((refItemDto.getCourse() != null ? refItemDto.getCourse().getAggregateId() : null));
                    return projDto;
                }).collect(Collectors.toSet()));
            }
            if (createRequest.getQuiz() != null) {
                Quiz refSource = (Quiz) unitOfWorkService.aggregateLoadAndRegisterRead(createRequest.getQuiz().getAggregateId(), unitOfWork);
                QuizDto refSourceDto = new QuizDto(refSource);
                TournamentQuizDto quizDto = new TournamentQuizDto();
                quizDto.setAggregateId(refSourceDto.getAggregateId());
                quizDto.setVersion(refSourceDto.getVersion());
                quizDto.setState(refSourceDto.getState() != null ? refSourceDto.getState().name() : null);

                tournamentDto.setQuiz(quizDto);
            }

            Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
            Tournament tournament = tournamentFactory.createTournament(aggregateId, tournamentDto);
            unitOfWorkService.registerChanged(tournament, unitOfWork);
            return tournamentFactory.createTournamentDto(tournament);
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error creating tournament: " + e.getMessage());
        }
    }

    public TournamentDto getTournamentById(Integer id, UnitOfWork unitOfWork) {
        try {
            Tournament tournament = (Tournament) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            return tournamentFactory.createTournamentDto(tournament);
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error retrieving tournament: " + e.getMessage());
        }
    }

    public List<TournamentDto> getAllTournaments(UnitOfWork unitOfWork) {
        try {
            Set<Integer> aggregateIds = tournamentRepository.findAll().stream()
                .map(Tournament::getAggregateId)
                .collect(Collectors.toSet());

            return aggregateIds.stream()
                .map(id -> {
                    try {
                        return (Tournament) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(java.util.Objects::nonNull)
                .map(tournamentFactory::createTournamentDto)
                .collect(Collectors.toList());
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error retrieving tournament: " + e.getMessage());
        }
    }

    public TournamentDto updateTournament(TournamentDto tournamentDto, UnitOfWork unitOfWork) {
        try {
            Integer id = tournamentDto.getAggregateId();
            Tournament oldTournament = (Tournament) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            Tournament newTournament = tournamentFactory.createTournamentFromExisting(oldTournament);
            if (tournamentDto.getStartTime() != null) {
                newTournament.setStartTime(tournamentDto.getStartTime());
            }
            if (tournamentDto.getEndTime() != null) {
                newTournament.setEndTime(tournamentDto.getEndTime());
            }
            if (tournamentDto.getNumberOfQuestions() != null) {
                newTournament.setNumberOfQuestions(tournamentDto.getNumberOfQuestions());
            }
            newTournament.setCancelled(tournamentDto.getCancelled());

            unitOfWorkService.registerChanged(newTournament, unitOfWork);            TournamentUpdatedEvent event = new TournamentUpdatedEvent(newTournament.getAggregateId(), newTournament.getStartTime(), newTournament.getEndTime(), newTournament.getNumberOfQuestions(), newTournament.getCancelled());
            event.setPublisherAggregateVersion(newTournament.getVersion());
            unitOfWorkService.registerEvent(event, unitOfWork);
            return tournamentFactory.createTournamentDto(newTournament);
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error updating tournament: " + e.getMessage());
        }
    }

    public void deleteTournament(Integer id, UnitOfWork unitOfWork) {
        try {
            Tournament oldTournament = (Tournament) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            Tournament newTournament = tournamentFactory.createTournamentFromExisting(oldTournament);
            newTournament.remove();
            unitOfWorkService.registerChanged(newTournament, unitOfWork);            unitOfWorkService.registerEvent(new TournamentDeletedEvent(newTournament.getAggregateId()), unitOfWork);
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error deleting tournament: " + e.getMessage());
        }
    }

    public TournamentParticipantDto addTournamentParticipant(Integer tournamentId, Integer participantAggregateId, TournamentParticipantDto TournamentParticipantDto, UnitOfWork unitOfWork) {
        try {
            Tournament oldTournament = (Tournament) unitOfWorkService.aggregateLoadAndRegisterRead(tournamentId, unitOfWork);
            Tournament newTournament = tournamentFactory.createTournamentFromExisting(oldTournament);
            TournamentParticipant element = new TournamentParticipant(TournamentParticipantDto);
            newTournament.getParticipants().add(element);
            unitOfWorkService.registerChanged(newTournament, unitOfWork);
            return TournamentParticipantDto;
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error adding TournamentParticipant: " + e.getMessage());
        }
    }

    public List<TournamentParticipantDto> addTournamentParticipants(Integer tournamentId, List<TournamentParticipantDto> TournamentParticipantDtos, UnitOfWork unitOfWork) {
        try {
            Tournament oldTournament = (Tournament) unitOfWorkService.aggregateLoadAndRegisterRead(tournamentId, unitOfWork);
            Tournament newTournament = tournamentFactory.createTournamentFromExisting(oldTournament);
            TournamentParticipantDtos.forEach(dto -> {
                TournamentParticipant element = new TournamentParticipant(dto);
                newTournament.getParticipants().add(element);
            });
            unitOfWorkService.registerChanged(newTournament, unitOfWork);
            return TournamentParticipantDtos;
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error adding TournamentParticipants: " + e.getMessage());
        }
    }

    public TournamentParticipantDto getTournamentParticipant(Integer tournamentId, Integer participantAggregateId, UnitOfWork unitOfWork) {
        try {
            Tournament tournament = (Tournament) unitOfWorkService.aggregateLoadAndRegisterRead(tournamentId, unitOfWork);
            TournamentParticipant element = tournament.getParticipants().stream()
                .filter(item -> item.getParticipantAggregateId() != null &&
                               item.getParticipantAggregateId().equals(participantAggregateId))
                .findFirst()
                .orElseThrow(() -> new AnswersException("TournamentParticipant not found"));
            return element.buildDto();
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error retrieving TournamentParticipant: " + e.getMessage());
        }
    }

    public void removeTournamentParticipant(Integer tournamentId, Integer participantAggregateId, UnitOfWork unitOfWork) {
        try {
            Tournament oldTournament = (Tournament) unitOfWorkService.aggregateLoadAndRegisterRead(tournamentId, unitOfWork);
            Tournament newTournament = tournamentFactory.createTournamentFromExisting(oldTournament);
            newTournament.getParticipants().removeIf(item ->
                item.getParticipantAggregateId() != null &&
                item.getParticipantAggregateId().equals(participantAggregateId)
            );
            unitOfWorkService.registerChanged(newTournament, unitOfWork);
            TournamentParticipantRemovedEvent event = new TournamentParticipantRemovedEvent(tournamentId, participantAggregateId);
            event.setPublisherAggregateVersion(newTournament.getVersion());
            unitOfWorkService.registerEvent(event, unitOfWork);
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error removing TournamentParticipant: " + e.getMessage());
        }
    }

    public TournamentParticipantDto updateTournamentParticipant(Integer tournamentId, Integer participantAggregateId, TournamentParticipantDto TournamentParticipantDto, UnitOfWork unitOfWork) {
        try {
            Tournament oldTournament = (Tournament) unitOfWorkService.aggregateLoadAndRegisterRead(tournamentId, unitOfWork);
            Tournament newTournament = tournamentFactory.createTournamentFromExisting(oldTournament);
            TournamentParticipant element = newTournament.getParticipants().stream()
                .filter(item -> item.getParticipantAggregateId() != null &&
                               item.getParticipantAggregateId().equals(participantAggregateId))
                .findFirst()
                .orElseThrow(() -> new AnswersException("TournamentParticipant not found"));

            unitOfWorkService.registerChanged(newTournament, unitOfWork);
            TournamentParticipantUpdatedEvent event = new TournamentParticipantUpdatedEvent(tournamentId, element.getParticipantAggregateId(), element.getParticipantVersion(), element.getParticipantName(), element.getParticipantUsername(), element.getParticipantEnrollTime());
            event.setPublisherAggregateVersion(newTournament.getVersion());
            unitOfWorkService.registerEvent(event, unitOfWork);
            return element.buildDto();
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error updating TournamentParticipant: " + e.getMessage());
        }
    }

    public TournamentTopicDto addTournamentTopic(Integer tournamentId, Integer topicAggregateId, TournamentTopicDto TournamentTopicDto, UnitOfWork unitOfWork) {
        try {
            Tournament oldTournament = (Tournament) unitOfWorkService.aggregateLoadAndRegisterRead(tournamentId, unitOfWork);
            Tournament newTournament = tournamentFactory.createTournamentFromExisting(oldTournament);
            TournamentTopic element = new TournamentTopic(TournamentTopicDto);
            newTournament.getTopics().add(element);
            unitOfWorkService.registerChanged(newTournament, unitOfWork);
            return TournamentTopicDto;
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error adding TournamentTopic: " + e.getMessage());
        }
    }

    public List<TournamentTopicDto> addTournamentTopics(Integer tournamentId, List<TournamentTopicDto> TournamentTopicDtos, UnitOfWork unitOfWork) {
        try {
            Tournament oldTournament = (Tournament) unitOfWorkService.aggregateLoadAndRegisterRead(tournamentId, unitOfWork);
            Tournament newTournament = tournamentFactory.createTournamentFromExisting(oldTournament);
            TournamentTopicDtos.forEach(dto -> {
                TournamentTopic element = new TournamentTopic(dto);
                newTournament.getTopics().add(element);
            });
            unitOfWorkService.registerChanged(newTournament, unitOfWork);
            return TournamentTopicDtos;
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error adding TournamentTopics: " + e.getMessage());
        }
    }

    public TournamentTopicDto getTournamentTopic(Integer tournamentId, Integer topicAggregateId, UnitOfWork unitOfWork) {
        try {
            Tournament tournament = (Tournament) unitOfWorkService.aggregateLoadAndRegisterRead(tournamentId, unitOfWork);
            TournamentTopic element = tournament.getTopics().stream()
                .filter(item -> item.getTopicAggregateId() != null &&
                               item.getTopicAggregateId().equals(topicAggregateId))
                .findFirst()
                .orElseThrow(() -> new AnswersException("TournamentTopic not found"));
            return element.buildDto();
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error retrieving TournamentTopic: " + e.getMessage());
        }
    }

    public void removeTournamentTopic(Integer tournamentId, Integer topicAggregateId, UnitOfWork unitOfWork) {
        try {
            Tournament oldTournament = (Tournament) unitOfWorkService.aggregateLoadAndRegisterRead(tournamentId, unitOfWork);
            Tournament newTournament = tournamentFactory.createTournamentFromExisting(oldTournament);
            newTournament.getTopics().removeIf(item ->
                item.getTopicAggregateId() != null &&
                item.getTopicAggregateId().equals(topicAggregateId)
            );
            unitOfWorkService.registerChanged(newTournament, unitOfWork);
            TournamentTopicRemovedEvent event = new TournamentTopicRemovedEvent(tournamentId, topicAggregateId);
            event.setPublisherAggregateVersion(newTournament.getVersion());
            unitOfWorkService.registerEvent(event, unitOfWork);
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error removing TournamentTopic: " + e.getMessage());
        }
    }

    public TournamentTopicDto updateTournamentTopic(Integer tournamentId, Integer topicAggregateId, TournamentTopicDto TournamentTopicDto, UnitOfWork unitOfWork) {
        try {
            Tournament oldTournament = (Tournament) unitOfWorkService.aggregateLoadAndRegisterRead(tournamentId, unitOfWork);
            Tournament newTournament = tournamentFactory.createTournamentFromExisting(oldTournament);
            TournamentTopic element = newTournament.getTopics().stream()
                .filter(item -> item.getTopicAggregateId() != null &&
                               item.getTopicAggregateId().equals(topicAggregateId))
                .findFirst()
                .orElseThrow(() -> new AnswersException("TournamentTopic not found"));

            unitOfWorkService.registerChanged(newTournament, unitOfWork);
            TournamentTopicUpdatedEvent event = new TournamentTopicUpdatedEvent(tournamentId, element.getTopicAggregateId(), element.getTopicVersion(), element.getTopicName());
            event.setPublisherAggregateVersion(newTournament.getVersion());
            unitOfWorkService.registerEvent(event, unitOfWork);
            return element.buildDto();
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error updating TournamentTopic: " + e.getMessage());
        }
    }


    public Tournament handleExecutionUpdatedEvent(Integer aggregateId, Integer executionAggregateId, Integer executionVersion, String executionAcronym, UnitOfWork unitOfWork) {
        try {
            Tournament oldTournament = (Tournament) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWork);
            Tournament newTournament = tournamentFactory.createTournamentFromExisting(oldTournament);



            unitOfWorkService.registerChanged(newTournament, unitOfWork);


            return newTournament;
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error handling ExecutionUpdatedEvent tournament: " + e.getMessage());
        }
    }

    public Tournament handleExecutionUserUpdatedEvent(Integer aggregateId, Integer executionuserAggregateId, Integer executionuserVersion, String creatorName, String creatorUsername, UnitOfWork unitOfWork) {
        try {
            Tournament oldTournament = (Tournament) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWork);
            Tournament newTournament = tournamentFactory.createTournamentFromExisting(oldTournament);



            unitOfWorkService.registerChanged(newTournament, unitOfWork);


            return newTournament;
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error handling ExecutionUserUpdatedEvent tournament: " + e.getMessage());
        }
    }

    public Tournament handleTopicUpdatedEvent(Integer aggregateId, Integer topicAggregateId, Integer topicVersion, String topicName, UnitOfWork unitOfWork) {
        try {
            Tournament oldTournament = (Tournament) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWork);
            Tournament newTournament = tournamentFactory.createTournamentFromExisting(oldTournament);

        if (newTournament.getTopics() != null) {
            newTournament.getTopics().stream()
                .filter(item -> item.getTopicAggregateId() != null && 
                               item.getTopicAggregateId().equals(topicAggregateId))
                .forEach(item -> item.setTopicVersion(topicVersion));
        }

            unitOfWorkService.registerChanged(newTournament, unitOfWork);


            return newTournament;
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error handling TopicUpdatedEvent tournament: " + e.getMessage());
        }
    }

    public Tournament handleQuizUpdatedEvent(Integer aggregateId, Integer quizAggregateId, Integer quizVersion, UnitOfWork unitOfWork) {
        try {
            Tournament oldTournament = (Tournament) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWork);
            Tournament newTournament = tournamentFactory.createTournamentFromExisting(oldTournament);

        if (newTournament.getQuiz() != null && 
            newTournament.getQuiz().getQuizAggregateId() != null &&
            newTournament.getQuiz().getQuizAggregateId().equals(quizAggregateId)) {
            newTournament.getQuiz().setQuizVersion(quizVersion);
        }

            unitOfWorkService.registerChanged(newTournament, unitOfWork);


            return newTournament;
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error handling QuizUpdatedEvent tournament: " + e.getMessage());
        }
    }




}