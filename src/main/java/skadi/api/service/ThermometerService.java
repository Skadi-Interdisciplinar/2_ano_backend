package skadi.api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import skadi.api.dto.ThermometerRequest;
import skadi.api.dto.ThermometerResponse;
import skadi.api.exceptions.ThermometerNotFoundException;
import skadi.api.exceptions.ThermometerRuleViolationException;
import skadi.api.mapper.ThermometerMapper;
import skadi.api.model.Thermometer;
import skadi.api.model.User;
import skadi.api.enums.NivelAcesso;
import skadi.api.repository.ThermometerRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ThermometerService {

    private final ThermometerRepository repository;
    private final DistributionCenterAccessService accessService;
    private final AuditUserContext auditUserContext;

    @Transactional
    public ThermometerResponse create(ThermometerRequest request, User user) {
        accessService.requireRole(user, NivelAcesso.ADMIN,
                "Somente ADMIN pode cadastrar termômetros.");
        auditUserContext.setCurrentUser(user.getId());
        Thermometer thermometer = ThermometerMapper.fromRequest(request);
        return ThermometerMapper.toResponse(repository.save(thermometer));
    }

    @Transactional(readOnly = true)
    public List<ThermometerResponse> findAll(User user) {
        Sort sort = Sort.by(Sort.Direction.ASC, "model")
                .and(Sort.by(Sort.Direction.ASC, "id"));
        List<Thermometer> thermometers = user != null
                && user.getNivelAcesso() == NivelAcesso.SUPER_ADMIN
                ? repository.findAll(sort)
                : repository.findAccessible(accessService.accessibleDistributionCenterIds(user));
        return thermometers.stream()
                .map(ThermometerMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ThermometerResponse findById(Long id, User user) {
        Thermometer thermometer = findEntityById(id);
        requireReadAccess(user, id);
        return ThermometerMapper.toResponse(thermometer);
    }

    @Transactional
    public ThermometerResponse update(Long id, ThermometerRequest request, User user) {
        Thermometer thermometer = findEntityById(id);
        requireAdminAccess(user, id);
        auditUserContext.setCurrentUser(user.getId());
        ThermometerMapper.update(thermometer, request);
        return ThermometerMapper.toResponse(repository.save(thermometer));
    }

    @Transactional
    public void delete(Long id, User user) {
        Thermometer thermometer = findEntityById(id);
        requireAdminAccess(user, id);

        if (repository.isAssignedToColdRoom(id)) {
            throw new ThermometerRuleViolationException(
                    "O termômetro não pode ser excluído porque está associado a uma câmara frigorífica");
        }
        if (repository.hasTemperatureReadings(id)) {
            throw new ThermometerRuleViolationException(
                    "O termômetro não pode ser excluído porque possui leituras de temperatura");
        }

        auditUserContext.setCurrentUser(user.getId());
        repository.delete(thermometer);
        repository.flush();
    }

    private Thermometer findEntityById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ThermometerNotFoundException(id));
    }

    private void requireReadAccess(User user, Long thermometerId) {
        repository.findDistributionCenterId(thermometerId)
                .ifPresentOrElse(
                        distributionCenterId -> accessService.requireRead(user, distributionCenterId),
                        () -> accessService.requireRole(user, NivelAcesso.ADMIN,
                                "Somente ADMIN pode consultar termômetros ainda não associados")
                );
    }

    private void requireAdminAccess(User user, Long thermometerId) {
        repository.findDistributionCenterId(thermometerId)
                .ifPresentOrElse(
                        distributionCenterId -> accessService.requireAdminOwnership(
                                user, distributionCenterId),
                        () -> accessService.requireRole(user, NivelAcesso.ADMIN,
                                "Somente ADMIN pode alterar termômetros ainda não associados")
                );
    }
}
