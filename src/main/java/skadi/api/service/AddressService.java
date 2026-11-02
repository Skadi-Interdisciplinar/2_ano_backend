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
import skadi.api.model.User;
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
    private final DistributionCenterAccessService accessService;
    private final AuditUserContext auditUserContext;

    @Transactional
    public AddressResponse create(Long distributionCenterId, AddressRequest request, User user) {
        accessService.requireAdminOwnership(user, distributionCenterId);
        String zipCode = request.zipCode().trim();
        if (addressRepository.existsByZipCode(zipCode)) {
            throw new AddressAlreadyExistsException();
        }

        DistributionCenter distributionCenter = findDistributionCenter(distributionCenterId);
        State state = findState(request.stateId());
        auditUserContext.setCurrentUser(user.getId());
        Address address = AddressMapper.fromRequest(request, state, distributionCenter);
        return AddressMapper.toResponse(addressRepository.save(address));
    }

    @Transactional(readOnly = true)
    public List<AddressResponse> findAllByDistributionCenter(Long distributionCenterId, User user) {
        findDistributionCenter(distributionCenterId);
        accessService.requireRead(user, distributionCenterId);
        return addressRepository.findAllByDistributionCenterIdOrderByIdAsc(distributionCenterId)
                .stream()
                .map(AddressMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AddressResponse findById(Long id, User user) {
        Address address = findAddress(id);
        accessService.requireRead(user, address.getDistributionCenter().getId());
        return AddressMapper.toResponse(address);
    }

    @Transactional
    public AddressResponse update(Long id, AddressRequest request, User user) {
        Address address = findAddress(id);
        accessService.requireAdminOwnership(user, address.getDistributionCenter().getId());
        String zipCode = request.zipCode().trim();
        if (addressRepository.existsByZipCodeAndIdNot(zipCode, id)) {
            throw new AddressAlreadyExistsException();
        }

        auditUserContext.setCurrentUser(user.getId());
        State state = findState(request.stateId());
        AddressMapper.update(address, request, state);
        return AddressMapper.toResponse(addressRepository.save(address));
    }

    @Transactional
    public void delete(Long id, User user) {
        Address address = findAddress(id);
        accessService.requireAdminOwnership(user, address.getDistributionCenter().getId());
        auditUserContext.setCurrentUser(user.getId());
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
