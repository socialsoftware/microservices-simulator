package pt.ulisboa.tecnico.socialsoftware.ms.domain.version;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
@Transactional
public interface VersionRepository extends JpaRepository<Version, Integer> {
}
