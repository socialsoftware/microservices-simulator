package pt.ulisboa.tecnico.socialsoftware.ms.versioning;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity
public class Version {
    @Id
    @GeneratedValue
    private Integer id;
    // represents the version of the last committed transaction in the system.
    private Long versionNumber;
    // used because of tests where the version number is temporarily decremented
    // to simulate concurrency in a deterministic test case
    private Long numberOfDecrements;

    public Version() {
        this.versionNumber = 0L;
        this.numberOfDecrements = 0L;
    }

    public Long getVersionNumber() {
        Long result = this.versionNumber;
        this.versionNumber = this.versionNumber + this.numberOfDecrements;
        this.numberOfDecrements = 0L;
        return result;
    }

    public void incrementVersion() {
        this.versionNumber = this.versionNumber + this.numberOfDecrements;
        this.numberOfDecrements = 0L;
        this.versionNumber++;
    }

    public void decrementVersion() {
        this.versionNumber--;
        this.numberOfDecrements++;
    }
}
