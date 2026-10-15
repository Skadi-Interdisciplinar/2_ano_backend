package skadi.api.dto;

public record AddressResponse(
        Long id,
        String zipCode,
        String street,
        Integer number,
        String city,
        String neighborhood,
        String complement,
        Long stateId,
        String stateAbbreviation,
        Long distributionCenterId,
        String distributionCenterName
) {
}
