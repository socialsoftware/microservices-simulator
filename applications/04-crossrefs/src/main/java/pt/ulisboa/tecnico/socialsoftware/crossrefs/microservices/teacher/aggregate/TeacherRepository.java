package pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.teacher.aggregate;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import jakarta.transaction.Transactional;

@Repository
@Transactional
public interface TeacherRepository extends JpaRepository<Teacher, Integer> {

}