package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Service
public class DatabaseService {

    @PersistenceContext
    private EntityManager entityManager;

    public DatabaseService() {
    }

    @Transactional
    public void cleanDatabase() {
        if (tableExists("execution_event")) {
            entityManager.createNativeQuery("DELETE FROM execution_event").executeUpdate();
        }
        if (tableExists("course_execution")) {
            entityManager.createNativeQuery("DELETE FROM course_execution").executeUpdate();
        }
        if (tableExists("course")) {
            entityManager.createNativeQuery("DELETE FROM course").executeUpdate();
        }
        if (tableExists("user")) {
            entityManager.createNativeQuery("DELETE FROM user").executeUpdate();
        }  
        if (tableExists("quiz")) {
            entityManager.createNativeQuery("DELETE FROM quiz").executeUpdate();
        }
        if (tableExists("question")) {
            entityManager.createNativeQuery("DELETE FROM question").executeUpdate();
        }
        if (tableExists("Tournament")) {
            entityManager.createNativeQuery("DELETE FROM Tournament").executeUpdate();
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
