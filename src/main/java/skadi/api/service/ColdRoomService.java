package skadi.api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import skadi.api.dto.ColdRoomRequest;
import skadi.api.dto.ColdRoomResponse;
import skadi.api.exceptions.CdNotFoundException;
import skadi.api.exceptions.ColdRoomInvalidTemperatureRangeException;
import skadi.api.exceptions.ColdRoomNotFoundException;
import skadi.api.exceptions.ColdRoomRuleViolationException;
import skadi.api.exceptions.ThermometerNotFoundException;
import skadi.api.mapper.ColdRoomMapper;
import skadi.api.model.ColdRoom;
import skadi.api.model.DistributionCenter;
import skadi.api.model.Thermometer;
import skadi.api.model.User;
import skadi.api.repository.ColdRoomRepository;
import skadi.api.repository.DistributionCenterRepository;
import skadi.api.repository.ThermometerRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ColdRoomService {

    private final ColdRoomRepository repository;
    private final DistributionCenterRepository distributionCenterRepository;
    private final ThermometerRepository thermometerRepository;
    private final DistributionCenterAccessService accessService;
    private final AuditUserContext auditUserContext;

    @Transactional
    public ColdRoomResponse create(ColdRoomRequest request, User user) {
        accessService.requireAdminOwnership(user, request.distributionCenterId());
        validateTemperatureRange(request);
        DistributionCenter distributionCenter = findDistributionCenter(request.distributionCenterId());
        Thermometer thermometer = findThermometer(request.thermometerId());
        validateThermometerAvailability(request.thermometerId(), null);

        auditUserContext.setCurrentUser(user.getId());
        ColdRoom coldRoom = ColdRoomMapper.fromRequest(request, distributionCenter, thermometer);
        return ColdRoomMapper.toResponse(repository.save(coldRoom));
    }

    @Transactional(readOnly = true)
    public List<ColdRoomResponse> findAll(User user) {
        Sort sort = Sort.by(Sort.Direction.ASC, "location")
                .and(Sort.by(Sort.Direction.ASC, "model"))
                .and(Sort.by(Sort.Direction.ASC, "id"));
        List<ColdRoom> coldRooms = user != null
                && user.getNivelAcesso() == skadi.api.enums.NivelAcesso.SUPER_ADMIN
                ? repository.findAll(sort)
                : repository.findAllByDistributionCenter_IdInOrderByLocationAscModelAscIdAsc(
                        accessService.accessibleDistributionCenterIds(user));
        return coldRooms.stream()
                .map(ColdRoomMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ColdRoomResponse findById(Long id, User user) {
        ColdRoom coldRoom = findEntityById(id);
        accessService.requireRead(user, coldRoom.getDistributionCenter().getId());
        return ColdRoomMapper.toResponse(coldRoom);
    }

    @Transactional
    public ColdRoomResponse update(Long id, ColdRoomRequest request, User user) {
        ColdRoom coldRoom = findEntityById(id);
        Long currentDistributionCenterId = coldRoom.getDistributionCenter().getId();
        accessService.requireAdminOwnership(user, currentDistributionCenterId);
        if (!currentDistributionCenterId.equals(request.distributionCenterId())) {
            throw new ColdRoomRuleViolationException(
                    "Uma câmara frigorífica não pode ser transferida para outro centro de distribuição");
        }
        validateTemperatureRange(request);

        Long currentThermometerId = coldRoom.getThermometer().getId();
        boolean replacingThermometer = !currentThermometerId.equals(request.thermometerId());
        if (replacingThermometer && thermometerRepository.hasTemperatureReadings(currentThermometerId)) {
            throw new ColdRoomRuleViolationException(
                    "O termômetro da câmara não pode ser substituído porque possui leituras de temperatura");
        }

        DistributionCenter distributionCenter = findDistributionCenter(request.distributionCenterId());
        Thermometer thermometer = findThermometer(request.thermometerId());
        validateThermometerAvailability(request.thermometerId(), id);

        auditUserContext.setCurrentUser(user.getId());
        ColdRoomMapper.update(coldRoom, request, distributionCenter, thermometer);
        return ColdRoomMapper.toResponse(repository.save(coldRoom));
    }

    @Transactional
    public void delete(Long id, User user) {
        ColdRoom coldRoom = findEntityById(id);
        accessService.requireAdminOwnership(user, coldRoom.getDistributionCenter().getId());

        if (repository.hasBatchStorageHistory(id)) {
            throw new ColdRoomRuleViolationException(
                    "A câmara frigorífica não pode ser excluída porque possui histórico de armazenamento de lotes");
        }
        if (repository.hasAlerts(id)) {
            throw new ColdRoomRuleViolationException(
                    "A câmara frigorífica não pode ser excluída porque possui alertas relacionados");
        }
        if (thermometerRepository.hasTemperatureReadings(coldRoom.getThermometer().getId())) {
            throw new ColdRoomRuleViolationException(
                    "A câmara frigorífica não pode ser excluída porque possui histórico de temperatura");
        }

        auditUserContext.setCurrentUser(user.getId());
        repository.delete(coldRoom);
        repository.flush();
    }

    private void validateTemperatureRange(ColdRoomRequest request) {
        if (request.minimumTemperature().compareTo(request.maximumTemperature()) >= 0) {
            throw new ColdRoomInvalidTemperatureRangeException();
        }
    }

    private void validateThermometerAvailability(Long thermometerId, Long coldRoomId) {
        boolean alreadyAssigned = coldRoomId == null
                ? repository.existsByThermometer_Id(thermometerId)
                : repository.existsByThermometer_IdAndIdNot(thermometerId, coldRoomId);
        if (alreadyAssigned) {
            throw new ColdRoomRuleViolationException(
                    "O termômetro já está associado a outra câmara frigorífica");
        }
    }

    private ColdRoom findEntityById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ColdRoomNotFoundException(id));
    }

    private DistributionCenter findDistributionCenter(Long id) {
        return distributionCenterRepository.findById(id)
                .orElseThrow(() -> new CdNotFoundException(
                        "Centro de distribuição não encontrado com o id " + id));
    }

    private Thermometer findThermometer(Long id) {
        return thermometerRepository.findById(id)
                .orElseThrow(() -> new ThermometerNotFoundException(id));
    }
}
