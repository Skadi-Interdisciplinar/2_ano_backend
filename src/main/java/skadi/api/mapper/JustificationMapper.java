package skadi.api.mapper;

import skadi.api.dto.JustificationPatchRequest;
import skadi.api.dto.JustificationRequest;
import skadi.api.dto.JustificationResponse;
import skadi.api.model.Justification;

public class JustificationMapper {

    public static Justification fromDTO(JustificationRequest request, Integer alertHandlingId) {
        Justification justification = new Justification();
        justification.setAlertHandlingId(alertHandlingId);
        justification.setReason(request.reason());
        justification.setDescription(request.description());
        return justification;
    }

    public static void updateFromDTO(Justification justification, JustificationPatchRequest request) {
        if (request.reason() != null) {
            justification.setReason(request.reason());
        }
        if (request.description() != null) {
            justification.setDescription(request.description());
        }
    }

    public static JustificationResponse toDTO(Justification justification) {
        return new JustificationResponse(
                justification.getId(),
                justification.getAlertHandlingId(),
                justification.getReason(),
                justification.getDescription(),
                justification.getCreatedAt()
        );
    }
}
