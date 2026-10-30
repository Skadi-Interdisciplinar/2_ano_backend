package skadi.api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import skadi.api.dto.BatchStorageRequest;
import skadi.api.dto.BatchStorageResponse;
import skadi.api.exceptions.BatchNotFoundException;
import skadi.api.exceptions.BatchStorageNotFoundException;
import skadi.api.exceptions.BatchStorageRuleViolationException;
import skadi.api.exceptions.ColdRoomNotFoundException;
import skadi.api.mapper.BatchStorageMapper;
import skadi.api.model.Batch;
import skadi.api.model.BatchStorage;
import skadi.api.model.ColdRoom;
import skadi.api.model.User;
import skadi.api.repository.BatchRepository;
import skadi.api.repository.BatchStorageRepository;
import skadi.api.repository.ColdRoomRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BatchStorageService {

    private static final String INCOMPATIBLE_TEMPERATURE_MESSAGE =
            "A faixa de temperatura da categoria do lote é incompatível com a câmara frigorífica";

    private final BatchStorageRepository storageRepository;
    private final BatchRepository batchRepository;
    private final ColdRoomRepository coldRoomRepository;
    private final DistributionCenterAccessService accessService;
    private final AuditUserContext auditUserContext;

    @Transactional
    public BatchStorageResponse enter(Long batchId, BatchStorageRequest request, User user) {
        Batch batch = lockBatch(batchId);
        ensureBatchHasNoActiveStorage(batchId);
        ColdRoom coldRoom = findColdRoom(request.coldRoomId());
        accessService.requireAdminOwnership(user, coldRoom.getDistributionCenter().getId());
        storageRepository.findFirstByBatch_IdOrderByEntryDateAsc(batchId)
                .ifPresent(firstStorage -> ensureSameDistributionCenter(
                        firstStorage.getColdRoom(), coldRoom));

        auditUserContext.setCurrentUser(user);
        BatchStorage storage = new BatchStorage(
                null,
                batch,
                coldRoom,
                LocalDateTime.now(),
                null
        );
        return BatchStorageMapper.toResponse(saveAndFlush(storage));
    }

    @Transactional(readOnly = true)
    public List<BatchStorageResponse> findHistory(Long batchId, User user) {
        if (!batchRepository.existsById(batchId)) {
            throw new BatchNotFoundException(batchId);
        }
        BatchStorage firstStorage = storageRepository.findFirstByBatch_IdOrderByEntryDateAsc(batchId)
                .orElseThrow(() -> new BatchStorageRuleViolationException(
                        "O lote não possui um centro de distribuição definido"));
        accessService.requireRead(user, firstStorage.getColdRoom().getDistributionCenter().getId());

        return storageRepository.findByBatch_IdOrderByEntryDateDesc(batchId).stream()
                .map(BatchStorageMapper::toResponse)
                .toList();
    }

    @Transactional
    public BatchStorageResponse move(
            Long batchId,
            Long storageId,
            BatchStorageRequest request,
            User user
    ) {
        lockBatch(batchId);
        BatchStorage currentStorage = findStorage(batchId, storageId);
        ensureStorageIsActive(currentStorage);

        if (currentStorage.getColdRoom().getId().equals(request.coldRoomId())) {
            throw new BatchStorageRuleViolationException(
                    "A câmara frigorífica de destino deve ser diferente da câmara atual"
            );
        }

        ColdRoom destination = findColdRoom(request.coldRoomId());
        accessService.requireAdminOwnership(
                user, currentStorage.getColdRoom().getDistributionCenter().getId());
        ensureSameDistributionCenter(currentStorage.getColdRoom(), destination);
        auditUserContext.setCurrentUser(user);
        LocalDateTime movementDate = exitDateFor(currentStorage);
        currentStorage.setExitDate(movementDate);
        saveAndFlush(currentStorage);

        BatchStorage destinationStorage = new BatchStorage(
                null,
                currentStorage.getBatch(),
                destination,
                movementDate,
                null
        );
        return BatchStorageMapper.toResponse(saveAndFlush(destinationStorage));
    }

    @Transactional
    public BatchStorageResponse exit(Long batchId, Long storageId, User user) {
        lockBatch(batchId);
        BatchStorage storage = findStorage(batchId, storageId);
        ensureStorageIsActive(storage);
        accessService.requireAdminOwnership(
                user, storage.getColdRoom().getDistributionCenter().getId());

        auditUserContext.setCurrentUser(user);
        storage.setExitDate(exitDateFor(storage));
        return BatchStorageMapper.toResponse(saveAndFlush(storage));
    }

    private Batch lockBatch(Long batchId) {
        return batchRepository.findByIdForUpdate(batchId)
                .orElseThrow(() -> new BatchNotFoundException(batchId));
    }

    private ColdRoom findColdRoom(Long coldRoomId) {
        return coldRoomRepository.findById(coldRoomId)
                .orElseThrow(() -> new ColdRoomNotFoundException(coldRoomId));
    }

    private BatchStorage findStorage(Long batchId, Long storageId) {
        return storageRepository.findByIdAndBatch_Id(storageId, batchId)
                .orElseThrow(() -> new BatchStorageNotFoundException(storageId, batchId));
    }

    private void ensureBatchHasNoActiveStorage(Long batchId) {
        storageRepository.findByBatch_IdAndExitDateIsNull(batchId)
                .ifPresent(storage -> {
                    throw new BatchStorageRuleViolationException(
                            "O lote já está armazenado na câmara frigorífica "
                                    + storage.getColdRoom().getId()
                    );
                });
    }

    private void ensureStorageIsActive(BatchStorage storage) {
        if (storage.getExitDate() != null) {
            throw new BatchStorageRuleViolationException("O armazenamento informado já foi encerrado");
        }
    }

    private void ensureSameDistributionCenter(ColdRoom origin, ColdRoom destination) {
        if (!origin.getDistributionCenter().getId()
                .equals(destination.getDistributionCenter().getId())) {
            throw new BatchStorageRuleViolationException(
                    "O lote não pode ser movimentado entre centros de distribuição diferentes");
        }
    }

    private LocalDateTime exitDateFor(BatchStorage storage) {
        LocalDateTime now = LocalDateTime.now();
        return now.isAfter(storage.getEntryDate())
                ? now
                : storage.getEntryDate().plusNanos(1_000);
    }

    private BatchStorage saveAndFlush(BatchStorage storage) {
        try {
            return storageRepository.saveAndFlush(storage);
        } catch (DataAccessException ex) {
            throw new BatchStorageRuleViolationException(INCOMPATIBLE_TEMPERATURE_MESSAGE, ex);
        }
    }
}
