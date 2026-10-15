package skadi.api.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import skadi.api.dto.AddressRequest;
import skadi.api.dto.AddressResponse;
import skadi.api.exceptions.AddressAlreadyExistsException;
import skadi.api.exceptions.AddressNotFoundException;
import skadi.api.exceptions.CdNotFoundException;
import skadi.api.exceptions.StateNotFoundException;
import skadi.api.model.Address;
import skadi.api.model.DistributionCenter;
import skadi.api.model.State;
import skadi.api.repository.AddressRepository;
import skadi.api.repository.DistributionCenterRepository;
import skadi.api.repository.StateRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddressServiceTest {

    @Mock
    private AddressRepository addressRepository;

    @Mock
    private StateRepository stateRepository;

    @Mock
    private DistributionCenterRepository distributionCenterRepository;

    private AddressService service;

    @BeforeEach
    void setUp() {
        service = new AddressService(addressRepository, stateRepository, distributionCenterRepository);
    }

    @Test
    void createSavesAddressWithExistingStateAndDistributionCenter() {
        when(addressRepository.existsByZipCode("01001000")).thenReturn(false);
        when(distributionCenterRepository.findById(1L)).thenReturn(Optional.of(distributionCenter()));
        when(stateRepository.findById(25L)).thenReturn(Optional.of(state()));
        when(addressRepository.save(any(Address.class))).thenAnswer(invocation -> {
            Address address = invocation.getArgument(0);
            address.setId(10L);
            return address;
        });

        AddressResponse response = service.create(1L, request());

        ArgumentCaptor<Address> captor = ArgumentCaptor.forClass(Address.class);
        verify(addressRepository).save(captor.capture());
        assertEquals(10L, response.id());
        assertEquals("SP", response.stateAbbreviation());
        assertEquals(1L, response.distributionCenterId());
        assertEquals("Rua das Flores", captor.getValue().getStreet());
        assertNull(captor.getValue().getComplement());
    }

    @Test
    void createRejectsDuplicatedZipCode() {
        when(addressRepository.existsByZipCode("01001000")).thenReturn(true);

        assertThrows(AddressAlreadyExistsException.class, () -> service.create(1L, request()));

        verify(distributionCenterRepository, never()).findById(any());
        verify(addressRepository, never()).save(any());
    }

    @Test
    void createRejectsUnknownDistributionCenter() {
        when(addressRepository.existsByZipCode("01001000")).thenReturn(false);
        when(distributionCenterRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(CdNotFoundException.class, () -> service.create(99L, request()));

        verify(stateRepository, never()).findById(any());
    }

    @Test
    void createRejectsUnknownState() {
        when(addressRepository.existsByZipCode("01001000")).thenReturn(false);
        when(distributionCenterRepository.findById(1L)).thenReturn(Optional.of(distributionCenter()));
        when(stateRepository.findById(25L)).thenReturn(Optional.empty());

        assertThrows(StateNotFoundException.class, () -> service.create(1L, request()));
    }

    @Test
    void findAllByDistributionCenterReturnsOnlyItsAddresses() {
        when(distributionCenterRepository.findById(1L)).thenReturn(Optional.of(distributionCenter()));
        when(addressRepository.findAllByDistributionCenterIdOrderByIdAsc(1L))
                .thenReturn(List.of(address()));

        List<AddressResponse> response = service.findAllByDistributionCenter(1L);

        assertEquals(1, response.size());
        assertEquals(10L, response.getFirst().id());
    }

    @Test
    void findByIdRejectsUnknownAddress() {
        when(addressRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(AddressNotFoundException.class, () -> service.findById(99L));
    }

    @Test
    void updateChangesAddressButKeepsDistributionCenter() {
        Address address = address();
        AddressRequest request = new AddressRequest(
                "01310930", "Avenida Paulista", 1000, "São Paulo",
                "Bela Vista", "Conjunto 10", 25L
        );
        when(addressRepository.findById(10L)).thenReturn(Optional.of(address));
        when(addressRepository.existsByZipCodeAndIdNot("01310930", 10L)).thenReturn(false);
        when(stateRepository.findById(25L)).thenReturn(Optional.of(state()));
        when(addressRepository.save(address)).thenReturn(address);

        AddressResponse response = service.update(10L, request);

        assertEquals("01310930", response.zipCode());
        assertEquals("Avenida Paulista", response.street());
        assertEquals(1L, response.distributionCenterId());
    }

    @Test
    void updateRejectsZipCodeUsedByAnotherAddress() {
        when(addressRepository.findById(10L)).thenReturn(Optional.of(address()));
        when(addressRepository.existsByZipCodeAndIdNot("01001000", 10L)).thenReturn(true);

        assertThrows(AddressAlreadyExistsException.class, () -> service.update(10L, request()));

        verify(stateRepository, never()).findById(any());
    }

    @Test
    void deleteRemovesExistingAddress() {
        Address address = address();
        when(addressRepository.findById(10L)).thenReturn(Optional.of(address));

        service.delete(10L);

        verify(addressRepository).delete(address);
        verify(addressRepository).flush();
    }

    private AddressRequest request() {
        return new AddressRequest(
                "01001000", " Rua das Flores ", 100, "São Paulo",
                "Centro", "   ", 25L
        );
    }

    private Address address() {
        return new Address(
                10L, "01001000", "Rua das Flores", 100, "São Paulo",
                "Centro", null, state(), distributionCenter()
        );
    }

    private State state() {
        return new State(25L, "SP");
    }

    private DistributionCenter distributionCenter() {
        return new DistributionCenter(1L, "CD São Paulo", "48372615000194");
    }
}
