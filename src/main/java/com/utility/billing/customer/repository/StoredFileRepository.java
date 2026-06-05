package com.utility.billing.customer.repository;
import com.utility.billing.customer.entity.StoredFile;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
public interface StoredFileRepository extends JpaRepository<StoredFile, UUID> {}
