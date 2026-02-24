package pt.ulisboa.tecnico.socialsoftware.ms.domain.version;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class VersionController {
    @Autowired
    IVersionService versionService;

    @PostMapping(value = "/versions/decrement")
    public void decrementVersion() {
        LoggerFactory.getLogger(VersionController.class).info("Decrementing version number through API");
        versionService.decrementVersionNumber();
    }

}
