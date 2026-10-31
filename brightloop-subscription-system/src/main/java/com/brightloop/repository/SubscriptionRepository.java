package com.brightloop.repository;
import com.brightloop.model.Subscription;import org.springframework.data.jpa.repository.JpaRepository;
public interface SubscriptionRepository extends JpaRepository<Subscription,Long>{ java.util.List<Subscription> findByStatus(com.brightloop.model.Subscription.Status status); boolean existsByNumber(String number); java.util.List<Subscription> findByCustomerEmail(String email); }
