package pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.member.aggregate;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import jakarta.transaction.Transactional;

@Repository
@Transactional
public interface MemberRepository extends JpaRepository<Member, Integer> {

}