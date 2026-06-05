package com.utility.billing.customer.service;
import com.utility.billing.customer.config.FileProperties;
import com.utility.billing.customer.entity.StoredFile;
import com.utility.billing.customer.repository.StoredFileRepository;
import com.utility.billing.common.exception.BusinessException;
import com.utility.billing.common.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException; import java.nio.file.*; import java.time.LocalDateTime;
import java.util.Set; import java.util.UUID;
@Service
public class FileStorageService {
    private static final Set<String> ALLOWED = Set.of("application/pdf","image/png","image/jpeg");
    private static final long MAX = 5 * 1024 * 1024;
    private final StoredFileRepository repo; private final Path uploadDir;
    public FileStorageService(StoredFileRepository repo, FileProperties props) throws IOException {
        this.repo = repo; this.uploadDir = Paths.get(props.getUploadDir()).toAbsolutePath().normalize();
        Files.createDirectories(uploadDir);
    }
    public StoredFile store(MultipartFile file, String entityType, UUID entityId) throws IOException {
        if (file.getSize() > MAX) throw new BusinessException("File size exceeds 5MB limit");
        if (!ALLOWED.contains(file.getContentType())) throw new BusinessException("Only PDF, PNG, JPG files are allowed");
        String stored = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Files.copy(file.getInputStream(), uploadDir.resolve(stored), StandardCopyOption.REPLACE_EXISTING);
        StoredFile sf = StoredFile.builder().originalName(file.getOriginalFilename()).storedName(stored)
            .contentType(file.getContentType()).sizeBytes(file.getSize()).entityType(entityType)
            .entityId(entityId).createdAt(LocalDateTime.now()).createdBy(SecurityUtils.getCurrentUserId()).build();
        return repo.save(sf);
    }
    public Path load(UUID id) {
        StoredFile sf = repo.findById(id).orElseThrow(() -> new BusinessException("File not found"));
        return uploadDir.resolve(sf.getStoredName());
    }
    public StoredFile getMeta(UUID id) { return repo.findById(id).orElseThrow(() -> new BusinessException("File not found")); }
}
