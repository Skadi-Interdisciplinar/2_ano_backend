package skadi.api.service;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import skadi.api.model.User;

@Component
@RequiredArgsConstructor
public class AuditUserContext {

    private final EntityManager entityManager;

    public void setCurrentUser(User user) {
      if (user == null || user.getId() == null) {
        throw new AccessDeniedException("Usuário autenticado não identificado para auditoria.");
      }
      
      Number userId = user.getId();
      entityManager.createNativeQuery("SELECT set_config('app.usuario_atual', :userId, true)")
        .setParameter("userId", userId.toString())
        .getSingleResult();
    }
}
