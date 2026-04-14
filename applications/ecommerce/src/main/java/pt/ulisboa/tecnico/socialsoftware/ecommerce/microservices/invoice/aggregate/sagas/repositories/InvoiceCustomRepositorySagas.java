package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.invoice.aggregate.sagas.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.invoice.aggregate.sagas.SagaInvoice;

@Repository
public interface InvoiceCustomRepositorySagas extends JpaRepository<SagaInvoice, Integer> {
}