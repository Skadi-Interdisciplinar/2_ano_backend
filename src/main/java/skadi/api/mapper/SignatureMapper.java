package skadi.api.mapper;

import skadi.api.dto.SignatureRequest;
import skadi.api.dto.SignatureResponse;
import skadi.api.model.Signature;

import java.time.LocalDate;

public class SignatureMapper {

    public static Signature fromDTO(Integer reportId, SignatureRequest request) {
        Signature signature = new Signature();
        signature.setReportId(reportId);
        signature.setCertificateHolder(request.certificateHolder());
        signature.setSerialNumber(request.serialNumber());
        signature.setCertifyingAuthority(request.certifyingAuthority());
        signature.setSignatureAlgorithm(request.signatureAlgorithm());
        signature.setCertificatePublicKey(request.certificatePublicKey());
        signature.setSignature(request.signature());
        signature.setTimestampToken(request.timestampToken());
        signature.setSignedAt(LocalDate.now());
        return signature;
    }

    public static SignatureResponse toDTO(Signature signature) {
        return new SignatureResponse(
                signature.getId(),
                signature.getReportId(),
                signature.getCertificateHolder(),
                signature.getSerialNumber(),
                signature.getCertifyingAuthority(),
                signature.getSignatureAlgorithm(),
                signature.getCertificatePublicKey(),
                signature.getSignature(),
                signature.getSignedAt(),
                signature.getTimestampToken()
        );
    }
}
