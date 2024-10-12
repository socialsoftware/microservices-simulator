package pt.ulisboa.tecnico.socialsoftware.ms.domain.version;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity
public class Version {
    @Id
    @GeneratedValue
    private Integer id;
    // represents the version of the last committed transaction in the system.
    private Integer versionNumber;
    // used because of tests where the version number is temporarily decremented
    // to simulate concurrency in a deterministic test case
    private Integer numberOfDecrements;

    public Version() {
        this.versionNumber = 0;
        this.numberOfDecrements = 0;
    }

    public Integer getVersionNumber() {
        Integer result = this.versionNumber;
        this.versionNumber = this.versionNumber + this.numberOfDecrements;
        this.numberOfDecrements = 0;
        return result;
    }

    public void incrementVersion() {
        this.versionNumber = this.versionNumber + this.numberOfDecrements;
        this.numberOfDecrements = 0;
        this.versionNumber++;
    }

    public void decrementVersion() {
        this.versionNumber--;
        this.numberOfDecrements++;
    }
}
