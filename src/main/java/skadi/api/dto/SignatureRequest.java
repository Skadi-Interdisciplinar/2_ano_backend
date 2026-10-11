package skadi.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignatureRequest(
        @NotBlank @Size(max = 255) String certificateHolder,
        @NotBlank @Size(max = 64) String serialNumber,
        @NotBlank @Size(max = 255) String certifyingAuthority,
        @NotBlank @Size(max = 50) String signatureAlgorithm,
        @NotBlank String certificatePublicKey,
        @NotBlank String signature,
        @NotBlank String timestampToken
) {
}
