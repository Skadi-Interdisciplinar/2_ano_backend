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
import skadi.api.repository.ThermometerRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ThermometerService {

    private final ThermometerRepository repository;

    @Transactional
    public ThermometerResponse create(ThermometerRequest request) {
        Thermometer thermometer = ThermometerMapper.fromRequest(request);
        return ThermometerMapper.toResponse(repository.save(thermometer));
    }

    @Transactional(readOnly = true)
    public List<ThermometerResponse> findAll() {
        Sort sort = Sort.by(Sort.Direction.ASC, "model")
                .and(Sort.by(Sort.Direction.ASC, "id"));
        return repository.findAll(sort).stream()
                .map(ThermometerMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ThermometerResponse findById(Long id) {
        return ThermometerMapper.toResponse(findEntityById(id));
    }

    @Transactional
    public ThermometerResponse update(Long id, ThermometerRequest request) {
        Thermometer thermometer = findEntityById(id);
        ThermometerMapper.update(thermometer, request);
        return ThermometerMapper.toResponse(repository.save(thermometer));
    }

    @Transactional
    public void delete(Long id) {
        Thermometer thermometer = findEntityById(id);

        if (repository.isAssignedToColdRoom(id)) {
            throw new ThermometerRuleViolationException(
                    "O termômetro não pode ser excluído porque está associado a uma câmara frigorífica");
        }
        if (repository.hasTemperatureReadings(id)) {
            throw new ThermometerRuleViolationException(
                    "O termômetro não pode ser excluído porque possui leituras de temperatura");
        }

        repository.delete(thermometer);
        repository.flush();
    }

    private Thermometer findEntityById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ThermometerNotFoundException(id));
    }
}
