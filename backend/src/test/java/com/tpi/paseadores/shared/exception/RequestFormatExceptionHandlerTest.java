package com.tpi.paseadores.shared.exception;

import com.tpi.paseadores.shared.security.JwtAuthenticationFilter;
import com.tpi.paseadores.shared.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = RequestFormatTestController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({JwtAuthenticationFilter.class, JwtService.class})
@TestPropertySource(properties = "app.jwt.secret=test-secret-not-for-production-use")
class RequestFormatExceptionHandlerTest {
    @Autowired
    private MockMvc mvc;

    @Test
    void invalidNumberIsBadRequestAndDoesNotEchoInput() throws Exception {
        mvc.perform(get("/test/request/number").param("value", "private-internal-input"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("value"))
                .andExpect(content().string(not(containsString("private-internal-input"))));
    }

    @Test
    void missingParameterUsesUniformBody() throws Exception {
        mvc.perform(get("/test/request/number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("value"))
                .andExpect(jsonPath("$.path").value("/test/request/number"));
    }

    @Test
    void invalidJsonDoesNotBecomeInternalServerError() throws Exception {
        mvc.perform(post("/test/request/body").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"count\":\"password_hash=secret\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid request body"))
                .andExpect(content().string(not(containsString("password_hash"))));
    }

    @Test
    void unsupportedMethodRetainsAllowHeader() throws Exception {
        mvc.perform(put("/test/request/number"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string("Allow", "GET"))
                .andExpect(jsonPath("$.status").value(405));
    }

    @Test
    void unsupportedContentTypeUsesUniformBody() throws Exception {
        mvc.perform(post("/test/request/body").contentType(MediaType.TEXT_PLAIN).content("private"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.status").value(415));
    }
}
