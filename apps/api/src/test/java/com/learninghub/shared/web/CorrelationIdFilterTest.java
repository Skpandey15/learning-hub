package com.learninghub.shared.web;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.ServletException;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class CorrelationIdFilterTest {
    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @Test
    void preservesValidCorrelationId() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.HEADER, "request-12345678");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> {});

        assertThat(response.getHeader(CorrelationIdFilter.HEADER)).isEqualTo("request-12345678");
    }

    @Test
    void replacesUnsafeCorrelationId() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.HEADER, "bad\r\nheader");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> {});

        assertThat(response.getHeader(CorrelationIdFilter.HEADER)).matches("[0-9a-f-]{36}");
    }

    @Test
    void generatesCorrelationIdWhenHeaderIsMissing() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> {});

        assertThat(response.getHeader(CorrelationIdFilter.HEADER)).matches("[0-9a-f-]{36}");
        assertThat(request.getAttribute(CorrelationIdFilter.ATTRIBUTE))
                .isEqualTo(response.getHeader(CorrelationIdFilter.HEADER));
    }
}
