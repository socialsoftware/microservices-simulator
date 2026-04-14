package pt.ulisboa.tecnico.socialsoftware.answers.microservices.user.aggregate;

import java.util.Set;

public interface UserCustomRepository {
    Set<Integer> findStudentIds();
    Set<Integer> findTeacherIds();
}