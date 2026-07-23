package pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.course;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;

public class CreateCourseCommand extends Command {
    private final String name;
    private final String type;

    public CreateCourseCommand(UnitOfWork unitOfWork, String serviceName, String name, String type) {
        super(unitOfWork, serviceName, null);
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }
}
