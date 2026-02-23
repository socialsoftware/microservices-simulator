package pt.ulisboa.tecnico.socialsoftware.advanced.sagas.aggregates.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pt.ulisboa.tecnico.socialsoftware.advanced.sagas.aggregates.SagaInvoice;

@Repository
public interface InvoiceCustomRepositorySagas extends JpaRepository<SagaInvoice, Integer> {
}