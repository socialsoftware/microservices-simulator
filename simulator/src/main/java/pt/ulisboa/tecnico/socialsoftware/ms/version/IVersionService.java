package pt.ulisboa.tecnico.socialsoftware.ms.version;

public interface IVersionService {

    Long getVersionNumber();

    Long getNextVersionNumber();

    Long incrementAndGetVersionNumber();

    void decrementVersionNumber();
}
