package com.brightloop.repository;
import com.brightloop.model.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface InvoiceRepository extends JpaRepository<Invoice,Long>{
 boolean existsBySubscriptionIdAndInvoiceDate(Long id,java.time.LocalDate date);
 boolean existsBySubscriptionIdAndPeriodStart(Long id,java.time.LocalDate date);
 List<Invoice> findByStatus(Invoice.Status status);
 List<Invoice> findByCustomerEmail(String email);
}
