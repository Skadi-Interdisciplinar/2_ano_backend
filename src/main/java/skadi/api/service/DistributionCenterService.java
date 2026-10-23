package skadi.api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import skadi.api.dto.DistributionCenterRequest;
import skadi.api.dto.DistributionCenterRequestName;
import skadi.api.dto.DistributionCenterResponse;
import skadi.api.exceptions.CdNotFoundException;
import skadi.api.exceptions.DuplicatedCnpjException;
import skadi.api.mapper.DistributionCenterMapper;
import skadi.api.model.AdminDistributionCenter;
import skadi.api.model.DistributionCenter;
import skadi.api.model.User;
import skadi.api.enums.NivelAcesso;
import skadi.api.repository.AdminDistributionCenterRepository;
import skadi.api.repository.DistributionCenterRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DistributionCenterService {
    private final DistributionCenterRepository repository;
    private final AdminDistributionCenterRepository adminDistributionCenterRepository;
    private final DistributionCenterAccessService accessService;
    private final AuditUserContext auditUserContext;

    @Transactional
    public DistributionCenterResponse insertCd(DistributionCenterRequest request, User user) {
        accessService.requireRole(user, NivelAcesso.ADMIN,
                "Somente ADMIN pode cadastrar centros de distribuição.");
        if (repository.existsByCnpj(request.cnpj())) {
            throw new DuplicatedCnpjException();
        }

        auditUserContext.setCurrentUser(user);
        DistributionCenter cd = repository.save(DistributionCenterMapper.fromDTO(request));
        adminDistributionCenterRepository.save(new AdminDistributionCenter(user, cd));
        return DistributionCenterMapper.toDTO(cd);
    }

    @Transactional(readOnly = true)
    public List<DistributionCenterResponse> findAllCD(User user) {
        if (user == null || user.getNivelAcesso() == null) {
            throw new AccessDeniedException("Usuário autenticado não identificado.");
        }
        if (user.getNivelAcesso() == NivelAcesso.SUPER_ADMIN) {
            return DistributionCenterMapper.toDTOList(
                    repository.findAll(Sort.by(Sort.Direction.ASC, "name")));
        }
        if (user.getNivelAcesso() == NivelAcesso.ADMIN) {
            return DistributionCenterMapper.toDTOList(
                    adminDistributionCenterRepository.findDistributionCentersByAdminId(user.getId()));
        }
        if (user.getCdId() == null) {
            throw new AccessDeniedException("Usuário não está vinculado a um centro de distribuição.");
        }
        return List.of(findCDById(user.getCdId().longValue(), user));
    }

    @Transactional(readOnly = true)
    public DistributionCenterResponse findCDById(Long id, User user) {
        DistributionCenter cd = repository.findById(id).orElseThrow(CdNotFoundException::new);
        accessService.requireRead(user, id);
        return DistributionCenterMapper.toDTO(cd);
    }

    @Transactional
    public DistributionCenterResponse updateCD(DistributionCenterRequest request, Long id, User user) {
        DistributionCenter cd = repository.findById(id).orElseThrow(CdNotFoundException::new);
        accessService.requireAdminOwnership(user, id);
        if (repository.existsByCnpjAndIdNot(request.cnpj(), id)) {
            throw new DuplicatedCnpjException();
        }

        auditUserContext.setCurrentUser(user);
        cd.setName(request.name());
        cd.setCnpj(request.cnpj());

        return DistributionCenterMapper.toDTO(repository.save(cd));
    }

    @Transactional
    public DistributionCenterResponse updateNameCd(
            DistributionCenterRequestName request,
            Long id,
            User user
    ) {
        DistributionCenter cd = repository.findById(id).orElseThrow(CdNotFoundException::new);
        accessService.requireAdminOwnership(user, id);

        auditUserContext.setCurrentUser(user);
        cd.setName(request.name());

        return DistributionCenterMapper.toDTO(repository.save(cd));
    }

    @Transactional
    public void deleteCD(Long id, User user) {
        DistributionCenter cd = repository.findById(id)
                .orElseThrow(() -> new CdNotFoundException("Centro de distribuição não existe"));
        accessService.requireAdminOwnership(user, id);
        auditUserContext.setCurrentUser(user);
        adminDistributionCenterRepository.deleteByDistributionCenter_Id(id);
        adminDistributionCenterRepository.flush();
        repository.delete(cd);
        repository.flush();
    }
}
