package pt.ulisboa.tecnico.socialsoftware.advanced.microservices.invoice.aggregate;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import jakarta.transaction.Transactional;

@Repository
@Transactional
public interface InvoiceRepository extends JpaRepository<Invoice, Integer> {

}