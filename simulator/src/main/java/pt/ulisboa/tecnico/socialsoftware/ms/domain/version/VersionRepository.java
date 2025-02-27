package pt.ulisboa.tecnico.socialsoftware.ms.domain.version;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import jakarta.transaction.Transactional;

@Repository
@Transactional
public interface VersionRepository extends JpaRepository<Version, Integer> {
}
