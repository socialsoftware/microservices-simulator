package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.utils;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.core.io.Resource;
import org.springframework.util.StreamUtils;

import javax.sql.DataSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.internal.SessionFactoryImpl;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionRepository;

import java.util.Arrays;
import java.util.List;

import java.sql.*;
import org.hibernate.Session;



@Service
public class DatabaseService {

    @Autowired
    private DataSource dataSource;

    @PersistenceContext
    private EntityManager entityManager;


    public DatabaseService() {
    }

    public void resetAndRebuild() {
        String url = "jdbc:postgresql://localhost/postgres";  // Connect to 'postgres' db, not msdb
        String user = null;
        String password = null;

        try (Connection conn = DriverManager.getConnection(url, user, password);
            Statement stmt = conn.createStatement()) {

            System.out.println("Drop Started.");
            stmt.executeUpdate("DROP DATABASE IF EXISTS msdb");
            System.out.println("Drop completed.");
            stmt.executeUpdate("CREATE DATABASE msdb");
            
            System.out.println("Database reset via JDBC completed.");

        } catch (SQLException e) {
            throw new RuntimeException("Failed to reset database via JDBC", e);
        }
    }

    private void runSqlScript(Statement statement, String scriptPath) throws Exception {
        Resource resource = new ClassPathResource(scriptPath);

        if (!resource.exists()) {
            System.out.println("SQL script not found: " + scriptPath + " - skipping");
            return;
        }

        String sql = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);

        for (String query : sql.split(";")) {
            if (!query.trim().isEmpty()) {
                System.out.println("Executing SQL: " + query.trim());
                statement.execute(query.trim());
            }
        }
    }

    @Transactional
    public void showDatabaseInfo() {
        List<?> tables;
        if (tableExists("saga_tournament")) {
            entityManager.createNativeQuery("SELECT * FROM saga_tournament").getResultList();
            System.out.println("saga_tournament table exists, printing contents:");
            printFullTable("saga_tournament");
        }
        if (tableExists("saga_question")) {
            entityManager.createNativeQuery("SELECT * FROM saga_question").getResultList();
            System.out.println("saga_question table exists, printing contents:");
            printFullTable("saga_question");
        }
        if (tableExists("saga_topic")) {
            entityManager.createNativeQuery("SELECT * FROM saga_topic").getResultList();
            System.out.println("saga_topic table exists, printing contents:");
            printFullTable("saga_topic");
        }
        if (tableExists("saga_user")) {
            entityManager.createNativeQuery("SELECT * FROM saga_user").getResultList();
            System.out.println("saga_user table exists, printing contents:");
            printFullTable("saga_user");
            
        }  
        if (tableExists("saga_quiz")) {
            entityManager.createNativeQuery("SELECT * FROM saga_quiz").getResultList();
            System.out.println("saga_quiz table exists, printing contents:");
            printFullTable("saga_quiz");
        }
        if (tableExists("saga_course_execution")) {
            entityManager.createNativeQuery("SELECT * FROM saga_course_execution").getResultList();
            System.out.println("saga_course_execution table exists, printing contents:");
            printFullTable("saga_course_execution");
        }
    }

    private void printFullTable(String tableName){
        try {
            Session session = entityManager.unwrap(Session.class);
            
            session.doWork(connection -> {
                try (Statement stmt = connection.createStatement();
                    ResultSet rs = stmt.executeQuery("SELECT * FROM " + tableName + " LIMIT 10")) {

                    ResultSetMetaData meta = rs.getMetaData();
                    int columnCount = meta.getColumnCount();

                    // Print column headers
                    for (int i = 1; i <= columnCount; i++) {
                        System.out.print(meta.getColumnName(i));
                        if (i < columnCount) System.out.print(" | ");
                    }
                    System.out.println();

                    // Print row values
                    while (rs.next()) {
                        for (int i = 1; i <= columnCount; i++) {
                            System.out.print(rs.getObject(i));
                            if (i < columnCount) System.out.print(" | ");
                        }
                        System.out.println();
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean tableExists(String tableName) {
        try {
            entityManager.createNativeQuery("SELECT 1 FROM " + tableName + " LIMIT 1").getResultList();
            return true;
        } catch (Exception e) {
            System.out.println("Table " + tableName + " does not exist.");
            return false;
        }
    }
    public void getSchema() {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("SCRIPT TO 'schema.sql'");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to retrieve database schema", e);
        }
    }

    public void reset() {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("DROP ALL OBJECTS");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to reset database", e);
        }
    }

}