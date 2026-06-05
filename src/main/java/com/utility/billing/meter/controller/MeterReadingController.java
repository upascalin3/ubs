package com.utility.billing.meter.controller;
import com.utility.billing.common.dto.ApiResponse;
import com.utility.billing.meter.dto.*;
import com.utility.billing.meter.service.MeterReadingService;
import io.swagger.v3.oas.annotations.Operation; import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page; import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus; import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;
@RestController @RequestMapping("/api/readings")
@Tag(name = "Meter Readings", description = "Capture and view meter readings")
public class MeterReadingController {
    private final MeterReadingService service;
    public MeterReadingController(MeterReadingService s) { service=s; }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasRole('OPERATOR')")
    @Operation(summary = "Capture Reading", description = "Accessible only by ROLE_OPERATOR. Meter must be active.")
    public ApiResponse<ReadingResponse> capture(@Valid @RequestBody ReadingRequest req) { return ApiResponse.success(service.capture(req)); }
    @GetMapping("/meter/{meterId}") @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','FINANCE')")
    @Operation(summary = "Reading History", description = "Required roles: ROLE_ADMIN, ROLE_OPERATOR, ROLE_FINANCE")
    public ApiResponse<Page<ReadingResponse>> history(@PathVariable UUID meterId, @ParameterObject @PageableDefault(size = 20) Pageable pageable) { return ApiResponse.success(service.history(meterId, pageable)); }
    @GetMapping("/search") @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','FINANCE')")
    @Operation(summary = "Search readings by meter", description = "Required roles: ROLE_OPERATOR. Query param: meterId")
    public ApiResponse<Page<ReadingResponse>> search(@RequestParam UUID meterId, @ParameterObject @PageableDefault(size = 20) Pageable pageable) { return ApiResponse.success(service.history(meterId, pageable)); }
    @GetMapping @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','FINANCE')")
    @Operation(summary = "List all readings")
    public ApiResponse<Page<ReadingResponse>> list(@ParameterObject @PageableDefault(size = 20) Pageable pageable) { return ApiResponse.success(service.list(pageable)); }
}
