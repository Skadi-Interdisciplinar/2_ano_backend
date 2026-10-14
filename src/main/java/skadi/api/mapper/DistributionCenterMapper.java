package skadi.api.mapper;

import skadi.api.dto.DistributionCenterRequest;
import skadi.api.model.DistributionCenter;
import skadi.api.repository.DistributionCenterRepository;

public class DistributionCenterMapper {


    public static DistributionCenter fromDTO(DistributionCenterRequest cd){
        return new DistributionCenter(cd.name(), cd.cnpj());
    }
}
