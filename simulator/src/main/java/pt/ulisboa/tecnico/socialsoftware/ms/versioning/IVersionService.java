package pt.ulisboa.tecnico.socialsoftware.ms.versioning;

public interface IVersionService {

    Long getVersionNumber();

    Long getNextVersionNumber();

    Long incrementAndGetVersionNumber();

    void decrementVersionNumber();
}
