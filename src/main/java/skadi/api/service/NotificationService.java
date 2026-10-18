package skadi.api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import skadi.api.dto.NotificationResponse;
import skadi.api.exceptions.NotificationNotFoundException;
import skadi.api.mapper.NotificationMapper;
import skadi.api.model.AlertNotification;
import skadi.api.repository.NotificationRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository repository;
    private final AuditUserContext auditUserContext;

    public List<NotificationResponse> findAllByUserId(Long userId) {
        return repository.findAllByUserIdOrderBySentAtDesc(userId).stream()
                .map(NotificationMapper::toResponse)
                .toList();
    }

    public NotificationResponse findById(Integer id, Long userId) {
        return NotificationMapper.toResponse(findNotification(id, userId));
    }

    @Transactional
    public NotificationResponse markAsRead(Integer id, Long userId) {
        AlertNotification notification = findNotification(id, userId);
        if (notification.getReadAt() == null) {
            auditUserContext.setCurrentUser(userId);
            notification.setReadAt(LocalDateTime.now());
            notification = repository.save(notification);
        }
        return NotificationMapper.toResponse(notification);
    }

    private AlertNotification findNotification(Integer id, Long userId) {
        return repository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new NotificationNotFoundException(id));
    }
}
