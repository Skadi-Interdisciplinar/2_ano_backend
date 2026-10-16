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
import skadi.api.controller.ColdRoomController;
import skadi.api.dto.ColdRoomResponse;
import skadi.api.service.ColdRoomService;
import skadi.api.service.UserDetailsServiceImpl;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

class ColdRoomSecurityConfigurationTest {

    private AnnotationConfigWebApplicationContext context;
    private ColdRoomService coldRoomService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        context.register(TestConfiguration.class);
        context.refresh();

        coldRoomService = context.getBean(ColdRoomService.class);
        mockMvc = webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    @Test
    void createAllowsRequestWithoutAuthentication() throws Exception {
        when(coldRoomService.create(any())).thenReturn(coldRoom());

        mockMvc.perform(post("/cold-room")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "model": "Câmara vertical",
                                  "location": "Corredor A",
                                  "minimumTemperature": -20.00,
                                  "maximumTemperature": -10.00,
                                  "distributionCenterId": 1,
                                  "thermometerId": 2
                                }
                                """))
                .andExpect(status().isCreated());

        verify(coldRoomService).create(any());
    }

    @Test
    void findByIdAllowsRequestWithoutAuthentication() throws Exception {
        when(coldRoomService.findById(3L)).thenReturn(coldRoom());

        mockMvc.perform(get("/cold-rooms/3"))
                .andExpect(status().isOk());

        verify(coldRoomService).findById(3L);
    }

    private ColdRoomResponse coldRoom() {
        return new ColdRoomResponse(
                3L, "Câmara vertical", "Corredor A",
                new BigDecimal("-20.00"), new BigDecimal("-10.00"),
                1L, "Centro Principal", 2L, "DHT22"
        );
    }

    @Configuration(proxyBeanMethods = false)
    @EnableWebMvc
    @Import(SecurityConfiguration.class)
    static class TestConfiguration {

        @Bean
        ColdRoomService coldRoomService() {
            return mock(ColdRoomService.class);
        }

        @Bean
        ColdRoomController coldRoomController(ColdRoomService coldRoomService) {
            return new ColdRoomController(coldRoomService);
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
