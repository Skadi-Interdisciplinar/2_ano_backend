package skadi.api.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import skadi.api.config.GlobalExceptionHandler;
import skadi.api.dto.NotificationResponse;
import skadi.api.exceptions.NotificationNotFoundException;
import skadi.api.model.User;
import skadi.api.service.NotificationService;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {
    @Mock private NotificationService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        User authenticatedUser = new User();
        authenticatedUser.setId(12L);
        HandlerMethodArgumentResolver userResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.getParameterType().equals(User.class);
            }

            @Override
            public Object resolveArgument(
                    MethodParameter parameter,
                    ModelAndViewContainer mavContainer,
                    NativeWebRequest webRequest,
                    WebDataBinderFactory binderFactory
            ) {
                return authenticatedUser;
            }
        };
        mockMvc = MockMvcBuilders.standaloneSetup(new NotificationController(service))
                .setCustomArgumentResolvers(userResolver)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void findAll_returnsNotificationsForAuthenticatedUser() throws Exception {
        when(service.findAllByUserId(12L)).thenReturn(List.of(response(null)));

        mockMvc.perform(get("/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(8))
                .andExpect(jsonPath("$[0].userId").value(12));

        verify(service).findAllByUserId(12L);
    }

    @Test
    void findById_returnsNotification() throws Exception {
        when(service.findById(8, 12L)).thenReturn(response(null));

        mockMvc.perform(get("/notifications/8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(8))
                .andExpect(jsonPath("$.alertId").value(22));

        verify(service).findById(8, 12L);
    }

    @Test
    void findById_returnsNotFoundForOtherUsersNotification() throws Exception {
        when(service.findById(8, 12L)).thenThrow(new NotificationNotFoundException(8));

        mockMvc.perform(get("/notifications/8"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Notificação não encontrada: 8"));
    }

    @Test
    void markAsRead_returnsUpdatedNotification() throws Exception {
        NotificationResponse readResponse = response(LocalDateTime.of(2026, 10, 16, 10, 30));
        when(service.markAsRead(8, 12L)).thenReturn(readResponse);

        mockMvc.perform(patch("/notifications/8/read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.readAt").value("2026-10-16T10:30:00"));

        verify(service).markAsRead(8, 12L);
    }

    @Test
    void markAsRead_returnsNotFoundForOtherUsersNotification() throws Exception {
        when(service.markAsRead(8, 12L)).thenThrow(new NotificationNotFoundException(8));

        mockMvc.perform(patch("/notifications/8/read"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Notificação não encontrada: 8"));
    }

    private NotificationResponse response(LocalDateTime readAt) {
        return new NotificationResponse(
                8,
                22,
                12L,
                LocalDateTime.of(2026, 10, 16, 10, 0),
                readAt
        );
    }
}
