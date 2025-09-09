package com.generated.microservices.answers.coordination.functionalities;

import static com.generated.microservices.ms.TransactionalModel.SAGAS;
import static com.generated.microservices.answers.microservices.exception.AnswersErrorMessage.*;

import static com.generated.microservices.ms.TransactionalModel.SAGAS;
import static com.generated.microservices.answers.microservices.exception.AnswersErrorMessage.*;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import com.generated.microservices.answers.microservices.exception.AnswersException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import com.generated.microservices.ms.TransactionalModel;
import com.generated.microservices.ms.coordination.unitOfWork.UnitOfWork;
import com.generated.microservices.answers.microservices.user.aggregate.UserDto;
import com.generated.microservices.ms.domain.aggregate.Aggregate.AggregateState;
import com.generated.microservices.answers.microservices.tournament.service.TournamentService;
import com.generated.microservices.answers.microservices.tournament.aggregate.TournamentDto;
import com.generated.microservices.answers.microservices.tournament.aggregate.TournamentCourseExecutionDto;
import com.generated.microservices.answers.microservices.tournament.aggregate.TournamentCreatorDto;
import com.generated.microservices.answers.microservices.tournament.aggregate.TournamentParticipantDto;
import com.generated.microservices.answers.microservices.tournament.aggregate.TournamentParticipantQuizAnswerDto;
import com.generated.microservices.answers.microservices.tournament.aggregate.TournamentTopicDto;
import com.generated.microservices.answers.microservices.tournament.aggregate.TournamentQuizDto;

@Service
public class TournamentFunctionalities {

private TournamentService tournamentService;

@Autowired
private Environment env;

private TransactionalModel workflowType;

@PostConstruct
public void init() {
String[] activeProfiles = env.getActiveProfiles();
if (Arrays.asList(activeProfiles).contains(SAGAS.getValue())) {
workflowType = SAGAS;
} else {
throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
}
}

[object Object],[object Object],[object Object],[object Object],[object Object],[object Object],[object Object],[object Object],[object Object],[object Object],[object Object],[object Object],[object Object]
}