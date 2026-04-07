package pt.ulisboa.tecnico.socialsoftware.ms.version;

import jakarta.transaction.Transactional;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
@Transactional
@Profile({"!remote & !distributed-version", "version-service"})
public interface VersionRepository extends JpaRepository<Version, Integer> {
}
