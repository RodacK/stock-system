package com.rob.products.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ApiKeyAuthFilterTest {

    private ApiKeyAuthFilter filter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        ApiKeyProperties properties = new ApiKeyProperties();
        properties.setApiKeys(Map.of(
                "admin-key", "ADMIN,READ",
                "read-only-key", "READ"
        ));
        filter = new ApiKeyAuthFilter(properties);
    }

    @Test
    void validAdminKey_setsAuthenticationWithAdminAndReadRoles() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer admin-key");

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getPrincipal()).isEqualTo("admin-key");
        assertThat(auth.getAuthorities()).extracting(Object::toString)
                .containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_READ");
    }

    @Test
    void validReadOnlyKey_setsAuthenticationWithReadRole() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer read-only-key");

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getAuthorities()).extracting(Object::toString)
                .containsExactly("ROLE_READ");
    }

    @Test
    void unknownKey_doesNotSetAuthentication() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer unknown-key");

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void noAuthorizationHeader_doesNotSetAuthentication() throws Exception {
        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void nonBearerHeader_doesNotSetAuthentication() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Basic admin-key");

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void filterAlwaysCallsChain() throws Exception {
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), chain);

        assertThat(chain.getRequest()).isNotNull();
    }
}