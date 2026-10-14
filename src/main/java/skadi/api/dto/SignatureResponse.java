package skadi.api.dto;

import java.time.LocalDate;

public record SignatureResponse(
        Integer id,
        Integer reportId,
        String certificateHolder,
        String serialNumber,
        String certifyingAuthority,
        String signatureAlgorithm,
        String certificatePublicKey,
        String signature,
        LocalDate signedAt,
        String timestampToken
) {
}
