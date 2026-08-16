package pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle;

import java.util.ArrayList;
import java.util.List;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaWorkflow;

class TestBrokenFunctionality extends WorkflowFunctionality {
    static final String FIRST_STEP_NAME = "firstStepCorrect";
    static final String SECOND_STEP_NAME = "secondStepCorrect";
    static final String THIRD_STEP_NAME = "thirdStepBreaks";

    private boolean firstStepExecuted = false;
    private boolean secondStepExecuted = false;
    private boolean thirdStepBroke = false;

    // ints so multiple runs of the same compensation are detectable
    private int firstStepCompensations = 0;
    private int secondStepCompensations = 0;
    private int thirdStepCompensations = 0;

    TestBrokenFunctionality(
            SagaUnitOfWorkService unitOfWorkService,
            SagaUnitOfWork unitOfWork,
            RuntimeException expectedException) {

        buildWorkflow(unitOfWorkService, unitOfWork, expectedException);
    }

    void buildWorkflow(
            SagaUnitOfWorkService unitOfWorkService,
            SagaUnitOfWork unitOfWork,
            RuntimeException expectedException) {

        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep firstStepCorrect = new SagaStep(FIRST_STEP_NAME, () -> {
            firstStepExecuted = true;
        }, new ArrayList<>());

        firstStepCorrect.registerCompensation(() -> {
            firstStepCompensations++;
        }, unitOfWork);

        SagaStep secondStepCorrect = new SagaStep(SECOND_STEP_NAME, () -> {
            secondStepExecuted = true;
        }, new ArrayList<>(List.of(firstStepCorrect)));

        secondStepCorrect.registerCompensation(() -> {
            secondStepCompensations++;
        }, unitOfWork);

        SagaStep thirdStepBreaks = new SagaStep(THIRD_STEP_NAME, () -> {
            thirdStepBroke = true;
            throw expectedException;
        }, new ArrayList<>(List.of(firstStepCorrect, secondStepCorrect)));

        thirdStepBreaks.registerCompensation(() -> {
            thirdStepCompensations++;
        }, unitOfWork);

        this.workflow.addStep(firstStepCorrect);
        this.workflow.addStep(secondStepCorrect);
        this.workflow.addStep(thirdStepBreaks);
    }

    boolean hasFirstStepExecuted() {
        return firstStepExecuted;
    }

    boolean hasSecondStepExecuted() {
        return secondStepExecuted;
    }

    boolean hasThirdStepFailed() {
        return thirdStepBroke;
    }

    boolean hasFirstStepCompensated() {
        return firstStepCompensations > 0;
    }

    boolean hasSecondStepCompensated() {
        return secondStepCompensations > 0;
    }

    boolean hasThirdStepCompensated() {
        return thirdStepCompensations > 0;
    }

    int getFirstStepCompensations() {
        return firstStepCompensations;
    }

    int getSecondStepCompensations() {
        return secondStepCompensations;
    }

    int getThirdStepCompensations() {
        return thirdStepCompensations;
    }
}
