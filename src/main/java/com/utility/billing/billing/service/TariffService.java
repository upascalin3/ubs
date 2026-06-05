package com.utility.billing.billing.service;
import com.utility.billing.billing.dto.TariffRequest;
import com.utility.billing.billing.entity.Tariff;
import com.utility.billing.billing.repository.TariffRepository;
import com.utility.billing.common.exception.BusinessException;
import org.springframework.data.domain.Page; import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate; import java.util.UUID;
@Service
public class TariffService {
    private final TariffRepository repo;
    public TariffService(TariffRepository repo) { this.repo = repo; }
    @Transactional
    public Tariff create(TariffRequest req) {
        if (repo.existsByMeterTypeAndVersion(req.getMeterType(), req.getVersion()))
            throw new BusinessException("Tariff version must be unique per meter type");
        if (req.getEffectiveDate().isBefore(LocalDate.now().minusDays(1)))
            throw new BusinessException("Effective date must be current or future");
        Tariff t = Tariff.builder().meterType(req.getMeterType()).tariffName(req.getTariffName())
            .rate(req.getRate()).fixedCharge(req.getFixedCharge()).vat(req.getVat())
            .penaltyRate(req.getPenaltyRate()).version(req.getVersion())
            .effectiveDate(req.getEffectiveDate()).active(req.isActive()).build();
        return repo.save(t);
    }
    public Page<Tariff> list(Pageable p) { return repo.findAll(p); }
    public Tariff get(UUID id) { return repo.findById(id).orElseThrow(() -> new BusinessException("Tariff not found")); }
}
