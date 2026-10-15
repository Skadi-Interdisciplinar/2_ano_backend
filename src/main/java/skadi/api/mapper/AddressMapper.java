package skadi.api.mapper;

import skadi.api.dto.AddressRequest;
import skadi.api.dto.AddressResponse;
import skadi.api.model.Address;
import skadi.api.model.DistributionCenter;
import skadi.api.model.State;

public final class AddressMapper {

    private AddressMapper() {
    }

    public static Address fromRequest(
            AddressRequest request,
            State state,
            DistributionCenter distributionCenter
    ) {
        return new Address(
                null,
                request.zipCode().trim(),
                request.street().trim(),
                request.number(),
                request.city().trim(),
                request.neighborhood().trim(),
                normalizeComplement(request.complement()),
                state,
                distributionCenter
        );
    }

    public static void update(Address address, AddressRequest request, State state) {
        address.setZipCode(request.zipCode().trim());
        address.setStreet(request.street().trim());
        address.setNumber(request.number());
        address.setCity(request.city().trim());
        address.setNeighborhood(request.neighborhood().trim());
        address.setComplement(normalizeComplement(request.complement()));
        address.setState(state);
    }

    public static AddressResponse toResponse(Address address) {
        return new AddressResponse(
                address.getId(),
                address.getZipCode(),
                address.getStreet(),
                address.getNumber(),
                address.getCity(),
                address.getNeighborhood(),
                address.getComplement(),
                address.getState().getId(),
                address.getState().getAbbreviation().trim(),
                address.getDistributionCenter().getId(),
                address.getDistributionCenter().getName()
        );
    }

    private static String normalizeComplement(String complement) {
        if (complement == null || complement.isBlank()) {
            return null;
        }
        return complement.trim();
    }
}
