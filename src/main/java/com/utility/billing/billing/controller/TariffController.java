package com.utility.billing.billing.controller;
import com.utility.billing.billing.dto.TariffRequest;
import com.utility.billing.billing.entity.Tariff;
import com.utility.billing.billing.service.TariffService;
import com.utility.billing.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation; import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page; import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus; import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;
@RestController @RequestMapping("/api/tariffs")
@Tag(name = "Tariffs", description = "Tariff management - ROLE_ADMIN")
@PreAuthorize("hasRole('ADMIN')")
public class TariffController {
    private final TariffService service;
    public TariffController(TariffService s) { service=s; }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Configure Tariff", description = "Accessible only by ROLE_ADMIN")
    public ApiResponse<Tariff> create(@Valid @RequestBody TariffRequest req) { return ApiResponse.success(service.create(req)); }
    @GetMapping @Operation(summary = "List tariffs")
    public ApiResponse<Page<Tariff>> list(@ParameterObject @PageableDefault(size = 20) Pageable pageable) { return ApiResponse.success(service.list(pageable)); }
    @GetMapping("/{id}") @Operation(summary = "Get tariff")
    public ApiResponse<Tariff> get(@PathVariable UUID id) { return ApiResponse.success(service.get(id)); }
}
