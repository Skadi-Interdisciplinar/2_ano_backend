package skadi.api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import skadi.api.enums.NivelAcesso;
import skadi.api.model.User;
import skadi.api.repository.AdminDistributionCenterRepository;
import skadi.api.exceptions.ResourceOutsideScopeException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DistributionCenterAccessService {

    private final AdminDistributionCenterRepository adminDistributionCenterRepository;

    @Transactional(readOnly = true)
    public boolean canRead(User user, Long distributionCenterId) {
        if (user == null || user.getNivelAcesso() == null || distributionCenterId == null) {
            return false;
        }
        if (user.getNivelAcesso() == NivelAcesso.SUPER_ADMIN) {
            return true;
        }
        if (user.getNivelAcesso() == NivelAcesso.ADMIN) {
            return adminDistributionCenterRepository
                    .existsByAdmin_IdAndDistributionCenter_Id(user.getId(), distributionCenterId);
        }
        return user.getCdId() != null && distributionCenterId.equals(user.getCdId().longValue());
    }

    @Transactional(readOnly = true)
    public void requireRead(User user, Long distributionCenterId) {
        if (!canRead(user, distributionCenterId)) {
            throw new ResourceOutsideScopeException();
        }
    }

    @Transactional(readOnly = true)
    public void requireAdminOwnership(User user, Long distributionCenterId) {
        requireRole(user, NivelAcesso.ADMIN,
                "Somente o administrador responsável pelo CD pode realizar esta operação.");
        if (!adminDistributionCenterRepository
                .existsByAdmin_IdAndDistributionCenter_Id(user.getId(), distributionCenterId)) {
            throw new ResourceOutsideScopeException();
        }
    }

    public void requireRole(User user, NivelAcesso role, String message) {
        if (user == null || user.getNivelAcesso() != role) {
            throw new AccessDeniedException(message);
        }
    }

    @Transactional(readOnly = true)
    public void requireOperationalWrite(User user, Long distributionCenterId) {
        if (user == null || user.getNivelAcesso() == null
                || user.getNivelAcesso() == NivelAcesso.SUPER_ADMIN) {
            throw new AccessDeniedException(
                    "SUPER_ADMIN possui acesso somente para consulta dos dados operacionais.");
        }
        if (user.getNivelAcesso() != NivelAcesso.ADMIN
                && user.getNivelAcesso() != NivelAcesso.GESTOR
                && user.getNivelAcesso() != NivelAcesso.OPERADOR) {
            throw new AccessDeniedException("Perfil sem permissão para esta operação.");
        }
        requireRead(user, distributionCenterId);
    }

    @Transactional(readOnly = true)
    public List<Long> accessibleDistributionCenterIds(User user) {
        if (user == null || user.getNivelAcesso() == null) {
            throw new AccessDeniedException("Usuário autenticado não identificado.");
        }
        if (user.getNivelAcesso() == NivelAcesso.SUPER_ADMIN) {
            return List.of();
        }
        if (user.getNivelAcesso() == NivelAcesso.ADMIN) {
            return adminDistributionCenterRepository.findDistributionCenterIdsByAdminId(user.getId());
        }
        if (user.getCdId() == null) {
            throw new AccessDeniedException("Usuário não está vinculado a um centro de distribuição.");
        }
        return List.of(user.getCdId().longValue());
    }
}
