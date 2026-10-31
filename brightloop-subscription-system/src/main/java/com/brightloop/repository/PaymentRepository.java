package com.brightloop.repository;
import com.brightloop.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface PaymentRepository extends JpaRepository<Payment,Long>{
 List<Payment> findByInvoiceCustomerEmail(String email);
}
