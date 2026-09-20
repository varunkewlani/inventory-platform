package com.inventoryplatform.users;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // Email is global (see V1 migration comment), so login can resolve a
    // user from email alone.
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    // Tenant-scoped accessors: every other lookup must go through these, not
    // the raw findById/findAll above, so a query can never cross tenants.
    Optional<User> findByIdAndOrganizationId(Long id, Long organizationId);

    Page<User> findAllByOrganizationId(Long organizationId, Pageable pageable);
}
