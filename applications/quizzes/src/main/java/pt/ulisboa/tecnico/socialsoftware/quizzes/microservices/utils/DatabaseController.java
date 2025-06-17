package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class DatabaseController {

    @Autowired
    private DatabaseService databaseService;

   @PostMapping("/database/clean")
    public void clean() {
        databaseService.reset();
    }


}