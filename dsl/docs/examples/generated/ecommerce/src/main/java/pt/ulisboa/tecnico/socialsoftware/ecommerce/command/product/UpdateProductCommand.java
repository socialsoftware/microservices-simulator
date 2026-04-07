package pt.ulisboa.tecnico.socialsoftware.ecommerce.command.product;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.ProductDto;

public class UpdateProductCommand extends Command {
    private final ProductDto productDto;

    public UpdateProductCommand(UnitOfWork unitOfWork, String serviceName, ProductDto productDto) {
        super(unitOfWork, serviceName, null);
        this.productDto = productDto;
    }

    public ProductDto getProductDto() { return productDto; }
}
