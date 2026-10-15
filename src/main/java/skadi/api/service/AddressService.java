package skadi.api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import skadi.api.dto.AddressRequest;
import skadi.api.dto.AddressResponse;
import skadi.api.exceptions.AddressAlreadyExistsException;
import skadi.api.exceptions.AddressNotFoundException;
import skadi.api.exceptions.CdNotFoundException;
import skadi.api.exceptions.StateNotFoundException;
import skadi.api.mapper.AddressMapper;
import skadi.api.model.Address;
import skadi.api.model.DistributionCenter;
import skadi.api.model.State;
import skadi.api.repository.AddressRepository;
import skadi.api.repository.DistributionCenterRepository;
import skadi.api.repository.StateRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AddressService {

    private final AddressRepository addressRepository;
    private final StateRepository stateRepository;
    private final DistributionCenterRepository distributionCenterRepository;

    @Transactional
    public AddressResponse create(Long distributionCenterId, AddressRequest request) {
        String zipCode = request.zipCode().trim();
        if (addressRepository.existsByZipCode(zipCode)) {
            throw new AddressAlreadyExistsException();
        }

        DistributionCenter distributionCenter = findDistributionCenter(distributionCenterId);
        State state = findState(request.stateId());
        Address address = AddressMapper.fromRequest(request, state, distributionCenter);
        return AddressMapper.toResponse(addressRepository.save(address));
    }

    @Transactional(readOnly = true)
    public List<AddressResponse> findAllByDistributionCenter(Long distributionCenterId) {
        findDistributionCenter(distributionCenterId);
        return addressRepository.findAllByDistributionCenterIdOrderByIdAsc(distributionCenterId)
                .stream()
                .map(AddressMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AddressResponse findById(Long id) {
        return AddressMapper.toResponse(findAddress(id));
    }

    @Transactional
    public AddressResponse update(Long id, AddressRequest request) {
        Address address = findAddress(id);
        String zipCode = request.zipCode().trim();
        if (addressRepository.existsByZipCodeAndIdNot(zipCode, id)) {
            throw new AddressAlreadyExistsException();
        }

        State state = findState(request.stateId());
        AddressMapper.update(address, request, state);
        return AddressMapper.toResponse(addressRepository.save(address));
    }

    @Transactional
    public void delete(Long id) {
        Address address = findAddress(id);
        addressRepository.delete(address);
        addressRepository.flush();
    }

    private Address findAddress(Long id) {
        return addressRepository.findById(id)
                .orElseThrow(() -> new AddressNotFoundException(id));
    }

    private State findState(Long id) {
        return stateRepository.findById(id)
                .orElseThrow(() -> new StateNotFoundException(id));
    }

    private DistributionCenter findDistributionCenter(Long id) {
        return distributionCenterRepository.findById(id)
                .orElseThrow(CdNotFoundException::new);
    }
}
