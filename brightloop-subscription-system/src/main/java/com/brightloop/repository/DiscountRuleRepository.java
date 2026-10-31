package com.brightloop.repository;
import com.brightloop.model.DiscountRule;import org.springframework.data.jpa.repository.*;import org.springframework.data.repository.query.Param;
public interface DiscountRuleRepository extends JpaRepository<DiscountRule,Long>{ @Modifying @Query("update DiscountRule d set d.usedCount = d.usedCount + 1 where d.id = :id and (d.usageLimit is null or d.usedCount < d.usageLimit)") int consume(@Param("id") Long id); }
