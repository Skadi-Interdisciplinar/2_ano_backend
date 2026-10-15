package skadi.api.mapper;

import skadi.api.dto.DistributionCenterRequest;
import skadi.api.dto.DistributionCenterResponse;
import skadi.api.model.DistributionCenter;

import java.util.List;

public class DistributionCenterMapper {

    public static DistributionCenter fromDTO(DistributionCenterRequest cd) {
        return new DistributionCenter(cd.name(), cd.cnpj());
    }

    public static DistributionCenterResponse toDTO(DistributionCenter cd) {
        return new DistributionCenterResponse(cd.getId(), cd.getName(), cd.getCnpj());
    }

    public static List<DistributionCenterResponse> toDTOList(List<DistributionCenter> cds) {
        return cds.stream().map(DistributionCenterMapper::toDTO).toList();
    }
}
