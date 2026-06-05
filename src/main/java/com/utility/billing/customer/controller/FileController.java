package com.utility.billing.customer.controller;
import com.utility.billing.common.dto.ApiResponse;
import com.utility.billing.customer.entity.StoredFile;
import com.utility.billing.customer.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation; import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.Resource; import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders; import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity; import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*; import org.springframework.web.multipart.MultipartFile;
import java.nio.file.Path; import java.util.UUID;
@RestController @RequestMapping("/api/files")
@Tag(name = "Files", description = "File upload and retrieval (PDF, PNG, JPG max 5MB)")
public class FileController {
    private final FileStorageService service;
    public FileController(FileStorageService s) { service=s; }
    @PostMapping("/upload") @PreAuthorize("hasAnyRole('ADMIN','CUSTOMER')")
    @Operation(summary = "Upload file", description = "PDF, PNG, JPG only. Max 5MB.")
    public ApiResponse<StoredFile> upload(@RequestParam MultipartFile file,
            @RequestParam(required=false) String entityType, @RequestParam(required=false) UUID entityId) throws Exception {
        return ApiResponse.success(service.store(file, entityType, entityId));
    }
    @GetMapping("/{id}") @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Download file by ID")
    public ResponseEntity<Resource> download(@PathVariable UUID id) throws Exception {
        StoredFile meta = service.getMeta(id);
        Path path = service.load(id);
        Resource resource = new UrlResource(path.toUri());
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(meta.getContentType()))
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + meta.getOriginalName() + "\"")
            .body(resource);
    }
}
