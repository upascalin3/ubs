package com.utility.billing.auth.repository;

import com.utility.billing.auth.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByPhoneNumber(String phoneNumber);

    boolean existsByNationalId(String nationalId);

    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.roleName = 'ROLE_CUSTOMER'")
    Page<User> findCustomers(Pageable pageable);

    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.roleName = 'ROLE_CUSTOMER' AND " +
           "(LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "u.nationalId LIKE CONCAT('%', :keyword, '%'))")
    Page<User> searchCustomers(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT u FROM User u WHERE LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<User> search(@Param("keyword") String keyword, Pageable pageable);
}
