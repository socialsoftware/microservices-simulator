package pt.ulisboa.tecnico.socialsoftware.teastore.command.category;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.teastore.shared.dtos.CategoryDto;

public class UpdateCategoryCommand extends Command {
    private final CategoryDto categoryDto;

    public UpdateCategoryCommand(UnitOfWork unitOfWork, String serviceName, CategoryDto categoryDto) {
        super(unitOfWork, serviceName, null);
        this.categoryDto = categoryDto;
    }

    public CategoryDto getCategoryDto() { return categoryDto; }
}
