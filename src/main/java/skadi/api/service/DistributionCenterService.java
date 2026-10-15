package skadi.api.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import skadi.api.dto.DistributionCenterRequest;
import skadi.api.dto.DistributionCenterRequestName;
import skadi.api.dto.DistributionCenterResponse;
import skadi.api.exceptions.CdNotFoundException;
import skadi.api.exceptions.DuplicatedCnpjException;
import skadi.api.mapper.DistributionCenterMapper;
import skadi.api.model.DistributionCenter;
import skadi.api.repository.DistributionCenterRepository;

import java.util.List;

@Service
@AllArgsConstructor
public class DistributionCenterService {
    public final DistributionCenterRepository repository;

    public DistributionCenterResponse insertCd(DistributionCenterRequest request) {
        if (repository.existsByCnpj(request.cnpj())) {
            throw new DuplicatedCnpjException();
        }

        DistributionCenter cd = repository.save(DistributionCenterMapper.fromDTO(request));
        return DistributionCenterMapper.toDTO(cd);
    }

    public List<DistributionCenterResponse> findAllCD() {
        return DistributionCenterMapper.toDTOList(repository.findAll());
    }

    public DistributionCenterResponse findCDById(Long id) {
        DistributionCenter cd = repository.findById(id).orElseThrow(CdNotFoundException::new);
        return DistributionCenterMapper.toDTO(cd);
    }

    public DistributionCenterResponse updateCD(DistributionCenterRequest request, Long id) {
        DistributionCenter cd = repository.findById(id).orElseThrow(CdNotFoundException::new);

        cd.setName(request.name());
        cd.setCnpj(request.cnpj());

        return DistributionCenterMapper.toDTO(repository.save(cd));
    }

    public DistributionCenterResponse updateNameCd(DistributionCenterRequestName request, Long id) {
        DistributionCenter cd = repository.findById(id).orElseThrow(CdNotFoundException::new);

        cd.setName(request.name());

        return DistributionCenterMapper.toDTO(repository.save(cd));
    }

    public void deleteCD(Long id) {
        if (!repository.existsById(id)) {
            throw new CdNotFoundException("Centro de distribuição não existe");
        }

        repository.deleteById(id);
    }
}
