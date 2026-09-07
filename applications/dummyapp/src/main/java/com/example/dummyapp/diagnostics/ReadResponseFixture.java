package com.example.dummyapp.diagnostics;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.sagaread.ReadResponseAdapter;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;

/** Source-only generic provenance fixture; not an application service or generated workload. */
public final class ReadResponseFixture {
    private ReadResponseFixture() { }

    public static final class Inspect extends Command {
        public Inspect(UnitOfWork unitOfWork, Integer requested) { super(unitOfWork, "item", requested); }
    }

    public record Revision(Integer token, Long stamp) { }

    /** Negative control: identity alone does not establish a returned revision. */
    public record Unversioned(Integer token) { }

    public static final class RevisionAdapter implements ReadResponseAdapter<Inspect, Revision> {
        @Override public Class<Inspect> commandType() { return Inspect.class; }
        @Override public Class<Revision> responseType() { return Revision.class; }
        @Override public String contractId() { return "dummyapp.item.outer-revision"; }
        @Override public String contractVersion() { return "1"; }
        @Override public String aggregateType() { return "com.example.dummyapp.item.aggregate.Item"; }
        @Override public String runtimeType() { return "com.example.dummyapp.item.aggregate.Item"; }
        @Override public Integer aggregateId(Revision response) { return response.token(); }
        @Override public Long version(Revision response) { return response.stamp(); }
    }

    public static final class UnversionedAdapter implements ReadResponseAdapter<Inspect, Unversioned> {
        @Override public Class<Inspect> commandType() { return Inspect.class; }
        @Override public Class<Unversioned> responseType() { return Unversioned.class; }
        @Override public String contractId() { return "dummyapp.item.unversioned"; }
        @Override public String contractVersion() { return "1"; }
        @Override public String aggregateType() { return "com.example.dummyapp.item.aggregate.Item"; }
        @Override public String runtimeType() { return "com.example.dummyapp.item.aggregate.Item"; }
        @Override public Integer aggregateId(Unversioned response) { return response.token(); }
        @Override public Long version(Unversioned response) { return null; }
    }
}
