package skadi.api.mapper;

import skadi.api.dto.NotificationResponse;
import skadi.api.model.AlertNotification;

public final class NotificationMapper {
    private NotificationMapper() {}

    public static NotificationResponse toResponse(AlertNotification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getAlertId(),
                notification.getUserId(),
                notification.getSentAt(),
                notification.getReadAt()
        );
    }
}
