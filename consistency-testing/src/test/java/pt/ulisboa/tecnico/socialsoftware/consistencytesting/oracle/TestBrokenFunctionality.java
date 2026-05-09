package pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle;

import java.util.ArrayList;
import java.util.List;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaWorkflow;

public class TestBrokenFunctionality extends WorkflowFunctionality {
    public static final String FIRST_STEP_NAME = "firstStepCorrect";
    public static final String SECOND_STEP_NAME = "secondStepCorrect";
    public static final String THIRD_STEP_NAME = "thirdStepBreaks";

    private final SagaUnitOfWorkService unitOfWorkService;
    private final SagaUnitOfWork unitOfWork;
    private final RuntimeException expectedException;

    private boolean firstStepExecuted = false;
    private boolean secondStepExecuted = false;
    private boolean thirdStepBroke = false;

    private boolean firstStepCompensated = false;
    private boolean secondStepCompensated = false;
    private boolean thirdStepCompensated = false;

    public TestBrokenFunctionality(
            SagaUnitOfWorkService unitOfWorkService,
            SagaUnitOfWork unitOfWork,
            RuntimeException expectedException) {

        this.unitOfWorkService = unitOfWorkService;
        this.unitOfWork = unitOfWork;
        this.expectedException = expectedException;
        buildWorkflow();
    }

    public void buildWorkflow() {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep firstStepCorrect = new SagaStep(FIRST_STEP_NAME, () -> {
            firstStepExecuted = true;
        }, new ArrayList<>());

        firstStepCorrect.registerCompensation(() -> {
            firstStepCompensated = true;
        }, unitOfWork);

        SagaStep secondStepCorrect = new SagaStep(SECOND_STEP_NAME, () -> {
            secondStepExecuted = true;
        }, new ArrayList<>(List.of(firstStepCorrect)));

        secondStepCorrect.registerCompensation(() -> {
            secondStepCompensated = true;
        }, unitOfWork);

        SagaStep thirdStepBreaks = new SagaStep(THIRD_STEP_NAME, () -> {
            thirdStepBroke = true;
            throw expectedException;
        }, new ArrayList<>(List.of(firstStepCorrect, secondStepCorrect)));

        thirdStepBreaks.registerCompensation(() -> {
            thirdStepCompensated = true;
        }, unitOfWork);

        this.workflow.addStep(firstStepCorrect);
        this.workflow.addStep(secondStepCorrect);
        this.workflow.addStep(thirdStepBreaks);
    }

    public boolean hasFirstStepExecuted() {
        return firstStepExecuted;
    }

    public boolean hasSecondStepExecuted() {
        return secondStepExecuted;
    }

    public boolean hasThirdStepFailed() {
        return thirdStepBroke;
    }

    public boolean hasFirstStepCompensated() {
        return firstStepCompensated;
    }

    public boolean hasSecondStepCompensated() {
        return secondStepCompensated;
    }

    public boolean hasThirdStepCompensated() {
        return thirdStepCompensated;
    }
}
