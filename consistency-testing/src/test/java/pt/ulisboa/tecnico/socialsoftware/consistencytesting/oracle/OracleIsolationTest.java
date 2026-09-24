package pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;

import pt.ulisboa.tecnico.socialsoftware.quizzes.QuizzesSimulator;

class OracleIsolationTest {

    @Test
    void rejectsSpringArgumentsThatWouldBreakRequiredIsolation() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> new Oracle(QuizzesSimulator.class, List.of("--server.port=8081")));

        // warn that such arguments are only valid when running with "consistency.isolation=unsupported"
        assertTrue(error.getMessage().contains("consistency.isolation=unsupported"));
    }

    @Test
    void allowsTheDisabledRSocketEndpointRequiredByTheTestJvm() {
        new Oracle(QuizzesSimulator.class, List.of("--spring.cloud.function.rsocket.enabled=false"));
    }

    @Test
    void isolatedOracleUsesH2WithoutWebServer() throws Exception {
        Oracle oracle = new Oracle(QuizzesSimulator.class, List.of());

        try {
            oracle.init();
            assertTrue(oracle.effectiveSpringAppArgs().contains("--spring.main.web-application-type=none"));
            assertTrue(databaseUrl(oracle).startsWith("jdbc:h2:mem:oracledb_"));
            assertEquals(1, queryOne(oracle));
        } finally {
            oracle.shutdown();
        }
    }

    private static String databaseUrl(Oracle oracle) throws SQLException {
        try (Connection connection = oracle.getBean(DataSource.class).getConnection()) {
            return connection.getMetaData().getURL();
        }
    }

    private static int queryOne(Oracle oracle) throws SQLException {
        try (Connection connection = oracle.getBean(DataSource.class).getConnection();
                var statement = connection.createStatement();
                var result = statement.executeQuery("SELECT 1")) {
            result.next();
            return result.getInt(1);
        }
    }
}
