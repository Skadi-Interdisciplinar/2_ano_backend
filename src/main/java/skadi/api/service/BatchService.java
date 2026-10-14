package skadi.api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import skadi.api.dto.BatchRequest;
import skadi.api.dto.BatchResponse;
import skadi.api.enums.BatchStatus;
import skadi.api.exceptions.BatchAlreadyExistsException;
import skadi.api.exceptions.BatchInvalidDatesException;
import skadi.api.exceptions.BatchNotFoundException;
import skadi.api.exceptions.BatchRuleViolationException;
import skadi.api.exceptions.CategoryNotFoundException;
import skadi.api.mapper.BatchMapper;
import skadi.api.model.Batch;
import skadi.api.model.Category;
import skadi.api.repository.BatchRepository;
import skadi.api.repository.CategoryRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class BatchService {

    private final BatchRepository batchRepository;
    private final CategoryRepository categoryRepository;

    @Transactional
    public BatchResponse create(BatchRequest request) {
        validateDates(request.manufacturingDate(), request.expirationDate());

        String batchCode = request.batchCode().trim();
        if (batchRepository.existsByBatchCodeIgnoreCase(batchCode)) {
            throw new BatchAlreadyExistsException("Já existe um lote com esse código");
        }

        Category category = findCategory(request.categoryId());
        Batch batch = BatchMapper.fromRequest(request, category);
        return BatchMapper.toResponse(batchRepository.save(batch));
    }

    @Transactional(readOnly = true)
    public List<BatchResponse> findAll() {
        return batchRepository.findAll(Sort.by(Sort.Direction.ASC, "batchCode"))
                .stream()
                .map(BatchMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public BatchResponse findById(Long id) {
        return BatchMapper.toResponse(findEntityById(id));
    }

    @Transactional
    public BatchResponse update(Long id, BatchRequest request) {
        validateDates(request.manufacturingDate(), request.expirationDate());

        Batch batch = findEntityById(id);
        String batchCode = request.batchCode().trim();
        if (batchRepository.existsByBatchCodeIgnoreCaseAndIdNot(batchCode, id)) {
            throw new BatchAlreadyExistsException("Já existe outro lote com esse código");
        }

        if (!Objects.equals(batch.getCategory().getId(), request.categoryId())
                && batchRepository.hasActiveStorage(id)) {
            throw new BatchRuleViolationException(
                    "A categoria não pode ser alterada enquanto o lote estiver armazenado em uma câmara"
            );
        }

        Category category = findCategory(request.categoryId());
        BatchMapper.update(batch, request, category);
        return BatchMapper.toResponse(batchRepository.save(batch));
    }

    @Transactional
    public BatchResponse updateStatus(Long id, BatchStatus status) {
        Batch batch = findEntityById(id);
        batch.setStatus(status);
        return BatchMapper.toResponse(batchRepository.save(batch));
    }

    @Transactional
    public void delete(Long id) {
        Batch batch = findEntityById(id);
        if (batchRepository.hasStorageHistory(id)) {
            throw new BatchRuleViolationException(
                    "O lote não pode ser excluído porque possui histórico de armazenamento"
            );
        }

        batchRepository.delete(batch);
        batchRepository.flush();
    }

    private Batch findEntityById(Long id) {
        return batchRepository.findById(id)
                .orElseThrow(() -> new BatchNotFoundException(id));
    }

    private Category findCategory(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new CategoryNotFoundException(
                        "Categoria não encontrada com o id " + categoryId));
    }

    private void validateDates(LocalDate manufacturingDate, LocalDate expirationDate) {
        if (expirationDate.isBefore(manufacturingDate)) {
            throw new BatchInvalidDatesException();
        }
    }
}
