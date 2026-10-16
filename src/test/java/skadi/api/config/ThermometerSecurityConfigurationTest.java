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
import skadi.api.controller.ThermometerController;
import skadi.api.service.ThermometerService;
import skadi.api.service.UserDetailsServiceImpl;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

class ThermometerSecurityConfigurationTest {

    private AnnotationConfigWebApplicationContext context;
    private ThermometerService thermometerService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        context.register(TestConfiguration.class);
        context.refresh();

        thermometerService = context.getBean(ThermometerService.class);
        mockMvc = webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    @Test
    void createRejectsRequestWithoutAuthentication() throws Exception {
        mockMvc.perform(post("/thermometer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "model": "DHT22" }
                                """))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(thermometerService);
    }

    @Test
    void findByIdRejectsRequestWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/thermometers/1"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(thermometerService);
    }

    @Configuration(proxyBeanMethods = false)
    @EnableWebMvc
    @Import(SecurityConfiguration.class)
    static class TestConfiguration {

        @Bean
        ThermometerService thermometerService() {
            return mock(ThermometerService.class);
        }

        @Bean
        ThermometerController thermometerController(ThermometerService thermometerService) {
            return new ThermometerController(thermometerService);
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
