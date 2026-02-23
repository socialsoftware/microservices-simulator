package pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.book.aggregate;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import jakarta.transaction.Transactional;

@Repository
@Transactional
public interface BookRepository extends JpaRepository<Book, Integer> {
    @Query(value = "SELECT e FROM Book e WHERE e.genre = :genre")
    List<Book> findByGenre(String genre);
    @Query(value = "SELECT e FROM Book e WHERE e.available = true")
    List<Book> findByAvailableTrue();
}