package skadi.api.service;

import org.junit.jupiter.api.Test;
import skadi.api.exceptions.ReportRuleViolationException;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.Signature;
import java.util.Base64;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Testa a validação criptográfica usada pelo fluxo de criação de assinatura.
 * A chave é gerada para o teste e não representa um certificado ICP-Brasil.
 */
class SignatureCryptographyTest {

    private static final String ALGORITHM = "SHA256withRSA";
    private final SignatureVerificationService verificationService = new SignatureVerificationService();

    @Test
    void signatureVerifiesForTheOriginalReport() throws GeneralSecurityException {
        KeyPair keyPair = generateKeyPair();
        String contentHash = hash("Relatório de demonstração");
        String signature = sign(keyPair, payload(42, contentHash));

        verificationService.verify(42, contentHash, ALGORITHM,
                publicKey(keyPair), signature);
    }

    @Test
    void signatureDoesNotVerifyAfterReportContentChanges() throws GeneralSecurityException {
        KeyPair keyPair = generateKeyPair();
        String originalHash = hash("Relatório original");
        String signature = sign(keyPair, payload(42, originalHash));

        assertThrows(ReportRuleViolationException.class,
                () -> verificationService.verify(42, hash("Relatório alterado"), ALGORITHM,
                        publicKey(keyPair), signature));
    }

    @Test
    void verificationFailsWhenSignatureBytesAreChanged() throws GeneralSecurityException {
        KeyPair keyPair = generateKeyPair();
        String contentHash = hash("Relatório de demonstração");
        byte[] signatureBytes = Base64.getDecoder().decode(
                sign(keyPair, payload(42, contentHash)));
        signatureBytes[0] ^= 1;

        assertThrows(ReportRuleViolationException.class,
                () -> verificationService.verify(42, contentHash, ALGORITHM,
                        publicKey(keyPair), Base64.getEncoder().encodeToString(signatureBytes)));
    }

    @Test
    void verificationRejectsUnsupportedAlgorithm() throws GeneralSecurityException {
        KeyPair keyPair = generateKeyPair();
        String contentHash = hash("Relatório de demonstração");
        String signature = sign(keyPair, payload(42, contentHash));

        assertThrows(ReportRuleViolationException.class,
                () -> verificationService.verify(42, contentHash, "SHA1withRSA",
                        publicKey(keyPair), signature));
    }

    @Test
    void verificationRejectsInvalidBase64PublicKey() throws GeneralSecurityException {
        KeyPair keyPair = generateKeyPair();
        String contentHash = hash("Relatório de demonstração");
        String signature = sign(keyPair, payload(42, contentHash));

        assertThrows(ReportRuleViolationException.class,
                () -> verificationService.verify(42, contentHash, ALGORITHM,
                        "%%%chave-invalida%%%", signature));
    }

    @Test
    void verificationRejectsInvalidBase64Signature() throws GeneralSecurityException {
        KeyPair keyPair = generateKeyPair();
        String contentHash = hash("Relatório de demonstração");

        assertThrows(ReportRuleViolationException.class,
                () -> verificationService.verify(42, contentHash, ALGORITHM,
                        publicKey(keyPair), "%%%assinatura-invalida%%%"));
    }

    @Test
    void verificationRejectsSignatureWhenDifferentPublicKeyIsProvided()
            throws GeneralSecurityException {
        KeyPair signerKeyPair = generateKeyPair();
        KeyPair otherKeyPair = generateKeyPair();
        String contentHash = hash("Relatório de demonstração");
        String signature = sign(signerKeyPair, payload(42, contentHash));

        assertThrows(ReportRuleViolationException.class,
                () -> verificationService.verify(42, contentHash, ALGORITHM,
                        publicKey(otherKeyPair), signature));
    }

    @Test
    void verificationRejectsSignatureForDifferentReportId() throws GeneralSecurityException {
        KeyPair keyPair = generateKeyPair();
        String contentHash = hash("Relatório de demonstração");
        String signature = sign(keyPair, payload(42, contentHash));

        assertThrows(ReportRuleViolationException.class,
                () -> verificationService.verify(43, contentHash, ALGORITHM,
                        publicKey(keyPair), signature));
    }

    private KeyPair generateKeyPair() throws GeneralSecurityException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    private String hash(String content) throws GeneralSecurityException {
        byte[] contentHash = MessageDigest.getInstance("SHA-256")
                .digest(content.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(contentHash);
    }

    private byte[] payload(Integer reportId, String contentHash) {
        return SignatureVerificationService.payload(reportId, contentHash);
    }

    private String sign(KeyPair keyPair, byte[] payload) throws GeneralSecurityException {
        Signature signer = Signature.getInstance(ALGORITHM);
        signer.initSign(keyPair.getPrivate());
        signer.update(payload);
        return Base64.getEncoder().encodeToString(signer.sign());
    }

    private String publicKey(KeyPair keyPair) {
        return Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
    }
}
