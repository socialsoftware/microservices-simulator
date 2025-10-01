package pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.aggregate;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import jakarta.transaction.Transactional;

@Repository
@Transactional
public interface ExecutionRepository extends JpaRepository<Execution, Integer> {
        @Query(value = "select execution.id from Execution execution where execution.name = :name AND execution.state = 'ACTIVE' AND execution.sagaState = 'NOT_IN_SAGA'")
    Optional<Integer> findExecutionIdByNameForSaga(String name);

    @Query(value = "select execution.id from Execution execution where execution.acronym = :acronym AND execution.state = 'ACTIVE' AND execution.sagaState = 'NOT_IN_SAGA'")
    Optional<Integer> findExecutionIdByAcronymForSaga(String acronym);

    @Query(value = "select execution.id from Execution execution where execution.academicTerm = :academicTerm AND execution.state = 'ACTIVE' AND execution.sagaState = 'NOT_IN_SAGA'")
    Optional<Integer> findExecutionIdByAcademicTermForSaga(String academicTerm);


    }