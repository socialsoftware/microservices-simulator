package pt.ulisboa.tecnico.socialsoftware.ms.domain.version;

public interface IVersionService {

    Integer getVersionNumber();

    Integer incrementAndGetVersionNumber();

    void decrementVersionNumber();
}
