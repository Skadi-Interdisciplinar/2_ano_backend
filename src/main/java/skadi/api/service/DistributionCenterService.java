package skadi.api.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import skadi.api.dto.DistributionCenterRequest;
import skadi.api.exceptions.CdNotFoundException;
import skadi.api.mapper.DistributionCenterMapper;
import skadi.api.model.DistributionCenter;
import skadi.api.repository.DistributionCenterRepository;

import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class DistributionCenterService {
    public final DistributionCenterRepository repository;


    public DistributionCenter insertCd(DistributionCenterRequest request){
        DistributionCenter distributionCenter = DistributionCenterMapper.fromDTO(request);
        DistributionCenter cd = repository.save(distributionCenter);
        return cd;
    }


    public List<DistributionCenter> findAllCD(){
        return repository.findAll();
    }

    public Optional<DistributionCenter> findCDById(Long id){
        return repository.findById(id);
    }

    public DistributionCenter updateCD(DistributionCenterRequest request, Long id){

        DistributionCenter cd = repository.findById(id).orElseThrow(CdNotFoundException::new);

        cd.setName(request.name());
        cd.setCnpj(request.cnpj());

        return repository.save(cd);


    }

    public DistributionCenter updateNameCd(String name,Long id){
        DistributionCenter cd = repository.findById(id).orElseThrow(CdNotFoundException::new);

        cd.setName(name);

        return repository.save(cd);
    }




}
