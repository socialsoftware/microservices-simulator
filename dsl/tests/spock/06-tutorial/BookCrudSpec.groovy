package pt.ulisboa.tecnico.socialsoftware.tutorial

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.book.coordination.webapi.requestDtos.CreateBookRequestDto
import pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.book.service.BookService
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService
import spock.lang.Specification

@SpringBootTest
@ActiveProfiles(["test", "sagas", "local"])
class BookCrudSpec extends Specification {

    @Autowired
    BookService bookService

    @Autowired
    UnitOfWorkService unitOfWorkService

    def "createBook persists a row that getBookById can read back"() {
        given:
            def uow = unitOfWorkService.createUnitOfWork("tut-create-book")
            def req = new CreateBookRequestDto()
            req.title = "Refactoring"
            req.author = "Fowler"
            req.genre = "tech"
            req.available = true

        when:
            def created = bookService.createBook(req, uow)
            unitOfWorkService.commit(uow)

        then:
            created != null
            created.title == "Refactoring"

        and:
            def uowR = unitOfWorkService.createUnitOfWork("tut-read-book")
            def reloaded = bookService.getBookById(created.aggregateId, uowR)
            reloaded.title == "Refactoring"
            reloaded.author == "Fowler"
            reloaded.available == true
    }

    def "getAllBooks lists at least one row after a create"() {
        given:
            def uow = unitOfWorkService.createUnitOfWork("tut-list-create")
            def req = new CreateBookRequestDto()
            req.title = "Domain-Driven Design"
            req.author = "Evans"
            req.genre = "tech"
            req.available = true
            bookService.createBook(req, uow)
            unitOfWorkService.commit(uow)

        when:
            def uowL = unitOfWorkService.createUnitOfWork("tut-list-read")
            def all = bookService.getAllBooks(uowL)

        then:
            all.size() >= 1
            all.any { it.title == "Domain-Driven Design" }
    }
}
