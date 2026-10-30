package skadi.api.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import skadi.api.controller.BatchController;
import skadi.api.controller.ReportController;
import skadi.api.controller.TemperatureReadingController;
import skadi.api.service.BatchService;
import skadi.api.service.ReportService;
import skadi.api.service.TemperatureReadingService;
import skadi.api.service.UserDetailsServiceImpl;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

class BatchSecurityConfigurationTest {

    private AnnotationConfigWebApplicationContext context;
    private BatchService batchService;
    private ReportService reportService;
    private TemperatureReadingService temperatureReadingService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        context.register(TestConfiguration.class);
        context.refresh();

        batchService = context.getBean(BatchService.class);
        reportService = context.getBean(ReportService.class);
        temperatureReadingService = context.getBean(TemperatureReadingService.class);
        mockMvc = webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    @Test
    void findByIdRejectsRequestWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/batches/13"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(batchService);
    }

    @Test
    void updateRejectsRequestWithoutAuthentication() throws Exception {
        mockMvc.perform(put("/batches/13")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "batchCode": "LOTE-2026-0015",
                                  "categoryId": 1,
                                  "manufacturingDate": "2026-10-01",
                                  "expirationDate": "2026-10-30"
                                }
                                """))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(batchService);
    }

    @Test
    void operatorCannotWriteBatches() throws Exception {
        mockMvc.perform(put("/batches/13")
                        .with(user("operator").roles("OPERADOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "batchCode": "LOTE-2026-0015",
                                  "categoryId": 1,
                                  "manufacturingDate": "2026-10-01",
                                  "expirationDate": "2026-10-30",
                                  "coldRoomId": 2
                                }
                                """))
                .andExpect(status().isForbidden());

        verifyNoInteractions(batchService);
    }

    @Test
    void operatorCannotReadReports() throws Exception {
        mockMvc.perform(get("/reports")
                        .with(user("operator").roles("OPERADOR")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(batchService, reportService);
    }

    @Test
    void operatorCannotCreateReports() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/reports")
                        .with(user("operator").roles("OPERADOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"Relatório","periodStart":"2026-10-01",
                                "periodEnd":"2026-10-31","distributionCenterId":1}
                                """))
                .andExpect(status().isForbidden());

        verifyNoInteractions(reportService);
    }

    @Test
    void superAdminCannotCreateReports() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/reports")
                        .with(user("superadmin").roles("SUPER_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(reportService);
    }

    @Test
    void operatorCannotReadSignatures() throws Exception {
        mockMvc.perform(get("/signatures")
                        .with(user("operator").roles("OPERADOR")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(reportService);
    }

    @Test
    void operatorCannotPublishTemperatureReadings() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/readings")
                        .with(user("operator").roles("OPERADOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"thermometerId":1,"temperature":-12.50,
                                "measuredAt":"2026-10-03T18:00:00-03:00"}
                                """))
                .andExpect(status().isForbidden());

        verifyNoInteractions(temperatureReadingService);
    }

    @Configuration(proxyBeanMethods = false)
    @EnableWebMvc
    @Import(SecurityConfiguration.class)
    static class TestConfiguration {

        @Bean
        BatchService batchService() {
            return mock(BatchService.class);
        }

        @Bean
        BatchController batchController(BatchService batchService) {
            return new BatchController(batchService);
        }

        @Bean
        ReportService reportService() {
            return mock(ReportService.class);
        }

        @Bean
        ReportController reportController(ReportService reportService) {
            return new ReportController(reportService);
        }

        @Bean
        TemperatureReadingService temperatureReadingService() {
            return mock(TemperatureReadingService.class);
        }

        @Bean
        TemperatureReadingController temperatureReadingController(
                TemperatureReadingService temperatureReadingService
        ) {
            return new TemperatureReadingController(temperatureReadingService);
        }

        @Bean
        UserDetailsServiceImpl userDetailsService() {
            return mock(UserDetailsServiceImpl.class);
        }

        @Bean
        JWTConfigurationFilter jwtConfigurationFilter(UserDetailsServiceImpl userDetailsService) {
            return new JWTConfigurationFilter(mock(TokenProvider.class), userDetailsService);
        }
    }
}
