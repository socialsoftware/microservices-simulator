package pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.book.aggregate;

import java.util.List;

public interface BookCustomRepository {
    List<Book> findByGenre(String genre);
    List<Book> findByAvailableTrue();
}