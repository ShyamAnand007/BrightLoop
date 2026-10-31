package com.brightloop.repository;
import com.brightloop.model.*;import org.springframework.data.jpa.repository.*;import java.util.*;
public interface ResetTokenRepository extends JpaRepository<ResetToken,Long>{ Optional<ResetToken> findByToken(String token); }
