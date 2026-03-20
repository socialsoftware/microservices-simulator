package pt.ulisboa.tecnico.socialsoftware.ms.domain.version;

public interface IVersionService {

    Long getVersionNumber();

    Long getNextVersionNumber();

    Long incrementAndGetVersionNumber();

    void decrementVersionNumber();
}
