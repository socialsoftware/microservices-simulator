package pt.ulisboa.tecnico.socialsoftware.quizzes.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/simulator/db")
@Profile({"train", "train-debug"})
public class DatabaseController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PostMapping("/execute")
    public void executeQuery(@RequestBody String query) {
        jdbcTemplate.execute(query);
    }

    @GetMapping("/tables")
    public List<String> getPublicTables() {
        return jdbcTemplate.queryForList(
            "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public' AND table_type = 'BASE TABLE'",
            String.class
        );
    }

    @PostMapping("/query-single")
    public Long querySingleValue(@RequestBody String query) {
        return jdbcTemplate.queryForObject(query, Long.class);
    }
}
