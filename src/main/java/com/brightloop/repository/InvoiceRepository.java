package com.brightloop.repository;
import com.brightloop.model.Invoice;import org.springframework.data.jpa.repository.JpaRepository;
public interface InvoiceRepository extends JpaRepository<Invoice,Long>{ boolean existsBySubscriptionIdAndInvoiceDate(Long id,java.time.LocalDate date); java.util.List<Invoice> findByStatus(com.brightloop.model.Invoice.Status status); }
