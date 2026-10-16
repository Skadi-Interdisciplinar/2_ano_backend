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
import skadi.api.controller.BatchStorageController;
import skadi.api.service.BatchStorageService;
import skadi.api.service.UserDetailsServiceImpl;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

class BatchStorageSecurityConfigurationTest {

    private AnnotationConfigWebApplicationContext context;
    private BatchStorageService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        context.register(TestConfiguration.class);
        context.refresh();

        service = context.getBean(BatchStorageService.class);
        mockMvc = webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    @Test
    void enterRejectsRequestWithoutAuthentication() throws Exception {
        mockMvc.perform(post("/batches/1/storages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"coldRoomId\": 3 }"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(service);
    }

    @Test
    void findHistoryRejectsRequestWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/batches/1/storages"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(service);
    }

    @Test
    void moveRejectsRequestWithoutAuthentication() throws Exception {
        mockMvc.perform(put("/batches/1/storages/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"coldRoomId\": 4 }"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(service);
    }

    @Test
    void exitRejectsRequestWithoutAuthentication() throws Exception {
        mockMvc.perform(patch("/batches/1/storages/10/exit"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(service);
    }

    @Configuration(proxyBeanMethods = false)
    @EnableWebMvc
    @Import(SecurityConfiguration.class)
    static class TestConfiguration {

        @Bean
        BatchStorageService batchStorageService() {
            return mock(BatchStorageService.class);
        }

        @Bean
        BatchStorageController batchStorageController(BatchStorageService service) {
            return new BatchStorageController(service);
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
