package skadi.api.service;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuditUserContext {

    private final EntityManager entityManager;

    public void setCurrentUser(Number userId) {
      if (userId == null) {
        throw new AccessDeniedException("Usuário autenticado não identificado para auditoria.");
      }

      entityManager.createNativeQuery("SELECT set_config('app.usuario_atual', :userId, true)")
        .setParameter("userId", userId.toString())
        .getSingleResult();
    }
}
