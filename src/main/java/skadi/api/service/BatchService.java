package skadi.api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
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
import skadi.api.exceptions.BatchStorageRuleViolationException;
import skadi.api.exceptions.CategoryNotFoundException;
import skadi.api.mapper.BatchMapper;
import skadi.api.model.Batch;
import skadi.api.model.Category;
import skadi.api.model.BatchStorage;
import skadi.api.model.ColdRoom;
import skadi.api.model.User;
import skadi.api.repository.BatchRepository;
import skadi.api.repository.BatchStorageRepository;
import skadi.api.repository.CategoryRepository;
import skadi.api.repository.ColdRoomRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class BatchService {

    private final BatchRepository batchRepository;
    private final CategoryRepository categoryRepository;
    private final BatchStorageRepository storageRepository;
    private final ColdRoomRepository coldRoomRepository;
    private final DistributionCenterAccessService accessService;
    private final AuditUserContext auditUserContext;

    @Transactional
    public BatchResponse create(BatchRequest request, User user) {
        validateDates(request.manufacturingDate(), request.expirationDate());

        String batchCode = request.batchCode().trim();
        if (batchRepository.existsByBatchCodeIgnoreCase(batchCode)) {
            throw new BatchAlreadyExistsException("Já existe um lote com esse código");
        }

        Category category = findCategory(request.categoryId());
        ColdRoom coldRoom = coldRoomRepository.findById(request.coldRoomId())
                .orElseThrow(() -> new skadi.api.exceptions.ColdRoomNotFoundException(request.coldRoomId()));
        accessService.requireAdminOwnership(user, coldRoom.getDistributionCenter().getId());

        auditUserContext.setCurrentUser(user.getId());
        Batch batch = BatchMapper.fromRequest(request, category);
        batch = batchRepository.save(batch);
        try {
            storageRepository.saveAndFlush(new BatchStorage(
                    null, batch, coldRoom, LocalDateTime.now(), null));
        } catch (DataAccessException ex) {
            throw new BatchStorageRuleViolationException(
                    "A faixa de temperatura da categoria do lote é incompatível com a câmara frigorífica",
                    ex);
        }
        return BatchMapper.toResponse(batch);
    }

    @Transactional(readOnly = true)
    public List<BatchResponse> findAll(User user) {
        List<Batch> batches = user != null
                && user.getNivelAcesso() == skadi.api.enums.NivelAcesso.SUPER_ADMIN
                ? batchRepository.findAll(Sort.by(Sort.Direction.ASC, "batchCode"))
                : storageRepository.findAccessibleBatches(
                        accessService.accessibleDistributionCenterIds(user));
        return batches
                .stream()
                .map(BatchMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public BatchResponse findById(Long id, User user) {
        Batch batch = findEntityById(id);
        accessService.requireRead(user, findDistributionCenterId(id));
        return BatchMapper.toResponse(batch);
    }

    @Transactional
    public BatchResponse update(Long id, BatchRequest request, User user) {
        validateDates(request.manufacturingDate(), request.expirationDate());

        Batch batch = findEntityById(id);
        BatchStorage firstStorage = findFirstStorage(id);
        accessService.requireAdminOwnership(
                user, firstStorage.getColdRoom().getDistributionCenter().getId());
        if (!firstStorage.getColdRoom().getId().equals(request.coldRoomId())) {
            throw new BatchRuleViolationException(
                    "A movimentação do lote deve ser feita pelas rotas de armazenamento");
        }
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

        auditUserContext.setCurrentUser(user.getId());
        Category category = findCategory(request.categoryId());
        BatchMapper.update(batch, request, category);
        return BatchMapper.toResponse(batchRepository.save(batch));
    }

    @Transactional
    public BatchResponse updateStatus(Long id, BatchStatus status, User user) {
        Batch batch = findEntityById(id);
        accessService.requireAdminOwnership(user, findDistributionCenterId(id));
        auditUserContext.setCurrentUser(user);
        batch.setStatus(status);
        return BatchMapper.toResponse(batchRepository.save(batch));
    }

    @Transactional
    public void delete(Long id, User user) {
        Batch batch = findEntityById(id);
        accessService.requireAdminOwnership(user, findDistributionCenterId(id));
        if (batchRepository.hasStorageHistory(id)) {
            throw new BatchRuleViolationException(
                    "O lote não pode ser excluído porque possui histórico de armazenamento"
            );
        }

        auditUserContext.setCurrentUser(user.getId());
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

    private BatchStorage findFirstStorage(Long batchId) {
        return storageRepository.findFirstByBatch_IdOrderByEntryDateAsc(batchId)
                .orElseThrow(() -> new BatchRuleViolationException(
                        "O lote não possui um centro de distribuição definido"));
    }

    private Long findDistributionCenterId(Long batchId) {
        return findFirstStorage(batchId).getColdRoom().getDistributionCenter().getId();
    }

    private void validateDates(LocalDate manufacturingDate, LocalDate expirationDate) {
        if (expirationDate.isBefore(manufacturingDate)) {
            throw new BatchInvalidDatesException();
        }
    }
}
