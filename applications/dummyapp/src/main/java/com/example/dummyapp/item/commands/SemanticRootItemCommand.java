package com.example.dummyapp.item.commands;

import com.example.dummyapp.item.aggregate.ItemDto;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;

/** Command-constructor shapes used to verify semantic root-key tracing. */
public class SemanticRootItemCommand extends Command {

    public SemanticRootItemCommand(UnitOfWork unitOfWork, Integer relatedAggregateId,
                                   String serviceName, Integer itemAggregateId) {
        super(unitOfWork, serviceName, itemAggregateId);
    }

    public SemanticRootItemCommand(UnitOfWork unitOfWork, String serviceName, ItemDto itemDto) {
        super(unitOfWork, serviceName, itemDto.getAggregateId());
    }

    public SemanticRootItemCommand(UnitOfWork unitOfWork, String serviceName,
                                   Integer itemAggregateId, boolean delegated) {
        this(unitOfWork, serviceName, itemAggregateId, (byte) 1);
    }

    private SemanticRootItemCommand(UnitOfWork unitOfWork, String serviceName,
                                    Integer itemAggregateId, byte delegated) {
        super(unitOfWork, serviceName, itemAggregateId);
    }

    public SemanticRootItemCommand(UnitOfWork unitOfWork, String serviceName, long marker) {
        super(unitOfWork, serviceName, 41);
    }

    public SemanticRootItemCommand(UnitOfWork unitOfWork, String serviceName, float marker) {
        super(unitOfWork, serviceName, -41);
    }

    public SemanticRootItemCommand(UnitOfWork unitOfWork, String serviceName, double marker) {
        super(unitOfWork, serviceName, +42);
    }

    public SemanticRootItemCommand(UnitOfWork unitOfWork, String serviceName) {
        super(unitOfWork, serviceName, null);
    }

    public SemanticRootItemCommand(UnitOfWork unitOfWork, String serviceName,
                                   Integer itemAggregateId, String unsupportedMarker) {
        super(unitOfWork, serviceName, normalize(itemAggregateId));
    }

    public SemanticRootItemCommand(UnitOfWork unitOfWork, String serviceName,
                                   Integer itemAggregateId, char ambiguousMarker) {
        // Intentionally ambiguous in this source-only verifier fixture.
        this(unitOfWork, serviceName, itemAggregateId, null, (short) 1);
    }

    private SemanticRootItemCommand(UnitOfWork unitOfWork, String serviceName,
                                    Integer itemAggregateId, String selector, short marker) {
        super(unitOfWork, serviceName, itemAggregateId);
    }

    private SemanticRootItemCommand(UnitOfWork unitOfWork, String serviceName,
                                    Integer itemAggregateId, ItemDto selector, short marker) {
        super(unitOfWork, serviceName, itemAggregateId);
    }

    private static Integer normalize(Integer aggregateId) {
        return aggregateId;
    }
}
