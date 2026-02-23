package pt.ulisboa.tecnico.socialsoftware.teastore.sagas.coordination.cart;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.cart.service.CartService;
import pt.ulisboa.tecnico.socialsoftware.teastore.shared.dtos.CartDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.teastore.coordination.webapi.requestDtos.CreateCartRequestDto;

public class CreateCartFunctionalitySagas extends WorkflowFunctionality {
    private CartDto createdCartDto;
    private final CartService cartService;
    private final SagaUnitOfWorkService unitOfWorkService;


    public CreateCartFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, CartService cartService, CreateCartRequestDto createRequest) {
        this.cartService = cartService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(createRequest, unitOfWork);
    }

    public void buildWorkflow(CreateCartRequestDto createRequest, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep createCartStep = new SagaSyncStep("createCartStep", () -> {
            CartDto createdCartDto = cartService.createCart(createRequest, unitOfWork);
            setCreatedCartDto(createdCartDto);
        });

        workflow.addStep(createCartStep);
    }
    public CartDto getCreatedCartDto() {
        return createdCartDto;
    }

    public void setCreatedCartDto(CartDto createdCartDto) {
        this.createdCartDto = createdCartDto;
    }
}
