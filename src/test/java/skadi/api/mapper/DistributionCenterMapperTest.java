package skadi.api.mapper;

import org.junit.jupiter.api.Test;
import skadi.api.dto.DistributionCenterRequest;
import skadi.api.dto.DistributionCenterResponse;
import skadi.api.model.DistributionCenter;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class DistributionCenterMapperTest {

    @Test
    void fromDTO_copiesRequestFields() {
        DistributionCenterRequest request = new DistributionCenterRequest("CD Osasco", "12345678901234");

        DistributionCenter cd = DistributionCenterMapper.fromDTO(request);

        assertNull(cd.getId());
        assertEquals("CD Osasco", cd.getName());
        assertEquals("12345678901234", cd.getCnpj());
    }

    @Test
    void toDTO_copiesEntityFields() {
        DistributionCenter cd = new DistributionCenter(1L, "CD Osasco", "12345678901234", null);

        DistributionCenterResponse response = DistributionCenterMapper.toDTO(cd);

        assertEquals(1L, response.id());
        assertEquals("CD Osasco", response.name());
        assertEquals("12345678901234", response.cnpj());
    }

    @Test
    void toDTOList_mapsAllCenters() {
        List<DistributionCenterResponse> responses = DistributionCenterMapper.toDTOList(List.of(
                new DistributionCenter(1L, "CD Osasco", "12345678901234", null)
        ));

        assertEquals(1, responses.size());
        assertEquals("CD Osasco", responses.getFirst().name());
    }
}
