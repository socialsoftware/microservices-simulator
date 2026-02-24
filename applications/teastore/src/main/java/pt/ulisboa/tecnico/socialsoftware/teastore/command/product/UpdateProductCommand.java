package pt.ulisboa.tecnico.socialsoftware.teastore.command.product;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;
import pt.ulisboa.tecnico.socialsoftware.teastore.shared.dtos.ProductDto;

public class UpdateProductCommand extends Command {
    private final ProductDto productDto;

    public UpdateProductCommand(UnitOfWork unitOfWork, String serviceName, ProductDto productDto) {
        super(unitOfWork, serviceName, null);
        this.productDto = productDto;
    }

    public ProductDto getProductDto() { return productDto; }
}
