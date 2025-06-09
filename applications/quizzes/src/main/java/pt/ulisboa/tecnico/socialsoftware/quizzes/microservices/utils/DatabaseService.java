package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.utils;

import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.internal.SessionFactoryImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.Arrays;
import java.util.List;

import java.sql.*;
import org.hibernate.Session;

@Service
public class DatabaseService {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private CourseExecutionRepository courseExecutionRepository;

    public DatabaseService() {
    }

    @Transactional
    public void cleanDatabase() {
        if (tableExists("saga_course_execution")) {
            //entityManager.createNativeQuery("DELETE FROM saga_course_execution").executeUpdate();
        }
        if (tableExists("saga_user")) {
            entityManager.createNativeQuery("DELETE FROM saga_user").executeUpdate();
        }  
        if (tableExists("saga_quiz")) {
            entityManager.createNativeQuery("DELETE FROM saga_quiz").executeUpdate();
        }
        if (tableExists("saga_question")) {
            entityManager.createNativeQuery("DELETE FROM saga_question").executeUpdate();
        }
        if (tableExists("saga_tournament")) {
            entityManager.createNativeQuery("DELETE FROM saga_tournament").executeUpdate();
        }
        if (tableExists("saga_topic")) {
            entityManager.createNativeQuery("DELETE FROM saga_topic").executeUpdate();
        }
        entityManager.clear();
    }

    @Transactional
    public void showDatabaseInfo() {
        List<?> tables;
        if (tableExists("saga_course_execution")) {
            // tables = entityManager.createNativeQuery("SELECT * FROM saga_course_execution").getResultList();
            // for (Object table : tables) {
            //     Object[] row = (Object[]) table;
            //     System.out.println(Arrays.toString(row)); // Prints each row's values clearly
            // }
            
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
        if (tableExists("saga_question")) {
            entityManager.createNativeQuery("SELECT * FROM saga_question").getResultList();
            System.out.println("saga_question table exists, printing contents:");
            printFullTable("saga_question");
        }
        if (tableExists("saga_tournament")) {
            entityManager.createNativeQuery("SELECT * FROM saga_tournament").getResultList();
            System.out.println("saga_tournament table exists, printing contents:");
            printFullTable("saga_tournament");
        }
        if (tableExists("saga_topic")) {
            entityManager.createNativeQuery("SELECT * FROM saga_topic").getResultList();
            System.out.println("saga_topic table exists, printing contents:");
            printFullTable("saga_topic");
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

    boolean tableExists(String tableName) {
    try {
        entityManager.createNativeQuery("SELECT 1 FROM " + tableName + " LIMIT 1").getResultList();
        return true;
    } catch (Exception e) {
        return false;
    }
}
}
