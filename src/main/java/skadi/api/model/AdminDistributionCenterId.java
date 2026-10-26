package skadi.api.model;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class AdminDistributionCenterId implements Serializable {
    private Long admin;
    private Long distributionCenter;
}
