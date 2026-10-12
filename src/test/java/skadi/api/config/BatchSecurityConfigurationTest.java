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
import skadi.api.dto.BatchResponse;
import skadi.api.enums.BatchStatus;
import skadi.api.service.BatchService;
import skadi.api.service.UserDetailsServiceImpl;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

class BatchSecurityConfigurationTest {

    private AnnotationConfigWebApplicationContext context;
    private BatchService batchService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        context.register(TestConfiguration.class);
        context.refresh();

        batchService = context.getBean(BatchService.class);
        mockMvc = webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    @Test
    void findById_allowsRequestWithoutAuthentication() throws Exception {
        when(batchService.findById(13L)).thenReturn(batch());

        mockMvc.perform(get("/batches/13"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(13));

        verify(batchService).findById(13L);
    }

    @Test
    void update_allowsRequestWithoutAuthentication() throws Exception {
        when(batchService.update(eq(13L), any())).thenReturn(batch());

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
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.batchCode").value("LOTE-2026-0015"));

        verify(batchService).update(eq(13L), any());
    }

    private BatchResponse batch() {
        return new BatchResponse(
                13L,
                "LOTE-2026-0015",
                1L,
                "Alcatra bovina resfriada",
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 30),
                BatchStatus.ACTIVE
        );
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
        UserDetailsServiceImpl userDetailsService() {
            return mock(UserDetailsServiceImpl.class);
        }

        @Bean
        JWTConfigurationFilter jwtConfigurationFilter(UserDetailsServiceImpl userDetailsService) {
            return new JWTConfigurationFilter(mock(TokenProvider.class), userDetailsService);
        }
    }
}
