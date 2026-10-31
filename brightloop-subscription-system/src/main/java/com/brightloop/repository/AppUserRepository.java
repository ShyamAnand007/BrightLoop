package com.brightloop.repository;
import com.brightloop.model.AppUser;import org.springframework.data.jpa.repository.JpaRepository;
public interface AppUserRepository extends JpaRepository<AppUser,Long>{ java.util.Optional<AppUser> findByEmail(String email); boolean existsByEmail(String email); }
