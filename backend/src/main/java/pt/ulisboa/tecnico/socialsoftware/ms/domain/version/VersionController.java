package pt.ulisboa.tecnico.socialsoftware.ms.domain.version;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class VersionController {
    @Autowired
    VersionService versionService;

    @PostMapping(value = "/versions/decrement")
    public void decrementVersion() {
        versionService.decrementVersionNumber();
    }

}
