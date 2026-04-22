package com.ptithcm.ptitmeet.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldAuthenticateUsingAccessTokenCookie() throws Exception {
        UUID userId = UUID.randomUUID();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new jakarta.servlet.http.Cookie("access_token", "valid-token"));

        when(jwtTokenProvider.validateToken("valid-token")).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromToken("valid-token")).thenReturn(userId);

        jwtAuthenticationFilter.doFilterInternal(
                request,
                new MockHttpServletResponse(),
                new MockFilterChain());

        assertEquals(userId.toString(), SecurityContextHolder.getContext().getAuthentication().getName());
        verify(jwtTokenProvider).validateToken("valid-token");
    }

    @Test
    void shouldLeaveSecurityContextEmptyWhenTokenIsMissing() throws Exception {
        jwtAuthenticationFilter.doFilterInternal(
                new MockHttpServletRequest(),
                new MockHttpServletResponse(),
                new MockFilterChain());

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
