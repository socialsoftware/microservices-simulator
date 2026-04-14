package pt.ulisboa.tecnico.socialsoftware.tutorial.command.book;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.tutorial.shared.dtos.BookDto;

public class UpdateBookCommand extends Command {
    private final BookDto bookDto;

    public UpdateBookCommand(UnitOfWork unitOfWork, String serviceName, BookDto bookDto) {
        super(unitOfWork, serviceName, null);
        this.bookDto = bookDto;
    }

    public BookDto getBookDto() { return bookDto; }
}
