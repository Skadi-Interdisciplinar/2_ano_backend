package skadi.api.service;

import org.springframework.stereotype.Service;
import skadi.api.exceptions.ReportRuleViolationException;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Service
public class SignatureVerificationService {

    private static final String SUPPORTED_ALGORITHM = "SHA256withRSA";

    public void verify(
            Integer reportId,
            String contentHash,
            String algorithm,
            String encodedPublicKey,
            String encodedSignature
    ) {
        if (!SUPPORTED_ALGORITHM.equals(algorithm)) {
            throw new ReportRuleViolationException("Algoritmo de assinatura não suportado.");
        }

        try {
            byte[] publicKeyBytes = Base64.getDecoder().decode(encodedPublicKey);
            byte[] signatureBytes = Base64.getDecoder().decode(encodedSignature);
            PublicKey publicKey = KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(publicKeyBytes));

            Signature verifier = Signature.getInstance(SUPPORTED_ALGORITHM);
            verifier.initVerify(publicKey);
            verifier.update(payload(reportId, contentHash));

            if (!verifier.verify(signatureBytes)) {
                throw new ReportRuleViolationException("A assinatura não corresponde ao relatório.");
            }
        } catch (IllegalArgumentException | GeneralSecurityException exception) {
            throw new ReportRuleViolationException("Assinatura ou chave pública inválida.");
        }
    }

    static byte[] payload(Integer reportId, String contentHash) {
        return (reportId + ":" + contentHash).getBytes(StandardCharsets.UTF_8);
    }
}
