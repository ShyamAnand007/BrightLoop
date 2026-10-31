package com.brightloop.repository;
import com.brightloop.model.*;import org.springframework.data.jpa.repository.*;import java.util.*;
public interface BillingFailureRepository extends JpaRepository<BillingFailure,Long>{ List<BillingFailure> findTop50ByOrderByIdDesc(); }
