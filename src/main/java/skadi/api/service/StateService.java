package skadi.api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import skadi.api.dto.StateResponse;
import skadi.api.exceptions.StateNotFoundException;
import skadi.api.mapper.StateMapper;
import skadi.api.repository.StateRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StateService {

    private final StateRepository repository;

    @Transactional(readOnly = true)
    public List<StateResponse> findAll() {
        return repository.findAll(Sort.by(Sort.Direction.ASC, "abbreviation"))
                .stream()
                .map(StateMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public StateResponse findById(Long id) {
        return repository.findById(id)
                .map(StateMapper::toResponse)
                .orElseThrow(() -> new StateNotFoundException(id));
    }
}
