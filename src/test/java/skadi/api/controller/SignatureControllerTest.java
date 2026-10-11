package skadi.api.controller;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import skadi.api.config.GlobalExceptionHandler;
import skadi.api.dto.SignatureRequest;
import skadi.api.dto.SignatureResponse;
import skadi.api.enums.NivelAcesso;
import skadi.api.exceptions.ReportRuleViolationException;
import skadi.api.exceptions.SignatureAlreadyExistsException;
import skadi.api.exceptions.SignatureNotFoundException;
import skadi.api.model.User;
import skadi.api.service.SignatureService;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SignatureControllerTest {

    @Mock private SignatureService service;

    private MockMvc mockMvc;
    private User user;

    @BeforeEach
    void setUp() {
        user = new User(7L, "Test User", "testuser", "12345678901",
                "test@example.com", "password", NivelAcesso.OPERADOR, 1, null);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new SignatureController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
                    @Override
                    public boolean supportsParameter(MethodParameter parameter) {
                        return parameter.hasParameterAnnotation(AuthenticationPrincipal.class);
                    }

                    @Override
                    public Object resolveArgument(MethodParameter parameter,
                                                  ModelAndViewContainer mavContainer,
                                                  NativeWebRequest webRequest,
                                                  WebDataBinderFactory binderFactory) {
                        return user;
                    }
                })
                .build();
    }

    @AfterEach
    void tearDown() {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    @Test
    void create_returnsCreatedSignature() throws Exception {
        when(service.create(eq(4), any(SignatureRequest.class), eq(7)))
                .thenReturn(response());

        mockMvc.perform(post("/reports/4/signatures")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(12))
                .andExpect(jsonPath("$.reportId").value(4))
                .andExpect(jsonPath("$.certificateHolder").value("Assinante de teste"))
                .andExpect(jsonPath("$.signature").value("assinatura-demonstrativa"));

        verify(service).create(eq(4), any(SignatureRequest.class), eq(7));
    }

    @Test
    void create_returnsBadRequestWhenRequiredFieldIsBlank() throws Exception {
        mockMvc.perform(post("/reports/4/signatures")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "certificateHolder": "",
                                  "serialNumber": "SERIE-TESTE-1",
                                  "certifyingAuthority": "Autoridade de teste",
                                  "signatureAlgorithm": "SHA256withRSA",
                                  "certificatePublicKey": "dGVzdC1wdWJsaWMta2V5",
                                  "signature": "assinatura-demonstrativa",
                                  "timestampToken": "carimbo-demonstrativo"
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void create_returnsBadRequestWhenPublicKeyIsBlank() throws Exception {
        mockMvc.perform(post("/reports/4/signatures")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "certificateHolder": "Assinante de teste",
                                  "serialNumber": "SERIE-TESTE-1",
                                  "certifyingAuthority": "Autoridade de teste",
                                  "signatureAlgorithm": "SHA256withRSA",
                                  "certificatePublicKey": "",
                                  "signature": "assinatura-demonstrativa",
                                  "timestampToken": "carimbo-demonstrativo"
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void create_returnsConflictWhenReportCannotBeSigned() throws Exception {
        when(service.create(eq(4), any(SignatureRequest.class), eq(7)))
                .thenThrow(new ReportRuleViolationException("Relatório já assinado"));

        mockMvc.perform(post("/reports/4/signatures")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson()))
                .andExpect(status().isConflict());
    }

    @Test
    void create_returnsConflictWhenReportAlreadyHasSignature() throws Exception {
        when(service.create(eq(4), any(SignatureRequest.class), eq(7)))
                .thenThrow(new SignatureAlreadyExistsException(4));

        mockMvc.perform(post("/reports/4/signatures")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("O relatório 4 já possui uma assinatura."));
    }

    @Test
    void findByReportId_returnsSignatures() throws Exception {
        when(service.findByReportId(4, 7)).thenReturn(List.of(response()));

        mockMvc.perform(get("/reports/4/signatures"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(12))
                .andExpect(jsonPath("$[0].reportId").value(4));

        verify(service).findByReportId(4, 7);
    }

    @Test
    void findByReportId_returnsEmptyListWhenNoneExist() throws Exception {
        when(service.findByReportId(4, 7)).thenReturn(List.of());

        mockMvc.perform(get("/reports/4/signatures"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void findById_returnsSignature() throws Exception {
        when(service.findById(12, 7)).thenReturn(response());

        mockMvc.perform(get("/signatures/12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(12))
                .andExpect(jsonPath("$.signedAt").value("2026-10-10"));
    }

    @Test
    void findById_returnsNotFoundWhenSignatureDoesNotExist() throws Exception {
        when(service.findById(99, 7)).thenThrow(new SignatureNotFoundException(99));

        mockMvc.perform(get("/signatures/99"))
                .andExpect(status().isNotFound());
    }

    private String requestJson() {
        return """
                {
                  "certificateHolder": "Assinante de teste",
                  "serialNumber": "SERIE-TESTE-1",
                  "certifyingAuthority": "Autoridade de teste",
                  "signatureAlgorithm": "SHA256withRSA",
                  "certificatePublicKey": "dGVzdC1wdWJsaWMta2V5",
                  "signature": "assinatura-demonstrativa",
                  "timestampToken": "carimbo-demonstrativo"
                }
                """;
    }

    private SignatureResponse response() {
        return new SignatureResponse(12, 4, "Assinante de teste", "SERIE-TESTE-1",
                "Autoridade de teste", "SHA256withRSA", "dGVzdC1wdWJsaWMta2V5",
                "assinatura-demonstrativa",
                LocalDate.of(2026, 10, 10), "carimbo-demonstrativo");
    }
}
