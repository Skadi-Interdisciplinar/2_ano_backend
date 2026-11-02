package skadi.api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import skadi.api.repository.AccessLogRepository;

@Service
@RequiredArgsConstructor
public class AccessLogService {
    private final AccessLogRepository repository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registerAttempt(
            Long userId,
            boolean successful,
            String ipAddress,
            String userAgent,
            String failureReason
    ) {
        String sourceIp = ipAddress == null || ipAddress.isBlank() ? "127.0.0.1" : ipAddress;
        String client = userAgent == null || userAgent.isBlank() ? "Não informado" : userAgent;

        repository.insertAttempt(userId, successful, sourceIp, client, failureReason);
    }
}
