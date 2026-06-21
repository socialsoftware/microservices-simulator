package pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle;

/**
 * A single aggregate-level read or write observed while a step executed,
 * captured at the unit-of-work chokepoints by
 * {@link TracingSagaUnitOfWorkService}.
 * <p>
 * It carries both the {@code aggregateId} — needed to match a read to the write
 * it observed (same aggregate instance) while computing reads-from relations —
 * and the {@code aggregateType}, which is the stable identifier reported to the
 * developer (aggregate ids are reassigned every run, so they are not comparable
 * across runs).
 */
sealed interface Effect permits Effect.Read, Effect.Write {

    Integer aggregateId();

    String aggregateType();

    record Read(Integer aggregateId, String aggregateType) implements Effect {
    }

    record Write(Integer aggregateId, String aggregateType) implements Effect {
    }
}
