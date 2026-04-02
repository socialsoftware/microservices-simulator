package pt.ulisboa.tecnico.socialsoftware.ms.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@Profile("!gateway")
public class RoutesController {

    private static final Logger logger = LoggerFactory.getLogger(RoutesController.class);

    private final Environment environment;
    private final ResourceLoader resourceLoader;

    public RoutesController(Environment environment, ResourceLoader resourceLoader) {
        this.environment = environment;
        this.resourceLoader = resourceLoader;
    }

    @GetMapping("/routes")
    public List<Map<String, Object>> getRoutes() {
        List<Map<String, Object>> allRoutes = new ArrayList<>();
        for (String profile : environment.getActiveProfiles()) {
            String filename = "application-" + profile + ".yaml";
            Resource resource = resourceLoader.getResource("classpath:" + filename);
            if (!resource.exists()) {
                continue;
            }
            Yaml yaml = new Yaml();
            try {
                for (Object doc : yaml.loadAll(resource.getInputStream())) {
                    if (!(doc instanceof Map<?, ?> data)) {
                        continue;
                    }
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> routes = (List<Map<String, Object>>) data.get("routes");
                    if (routes != null) {
                        allRoutes.addAll(routes);
                    }
                }
            } catch (IOException e) {
                logger.warn("Failed to read routes from classpath:{}", filename, e);
            }
        }
        return allRoutes;
    }
}
