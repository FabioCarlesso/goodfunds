package com.goodfunds.exception;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(GlobalExceptionHandlerIntegrationTest.TestErrorController.class)
class GlobalExceptionHandlerIntegrationTest {

    @RestController
    static class TestErrorController {
        @GetMapping("/test/unexpected-error")
        void triggerError() {
            throw new RuntimeException("unexpected");
        }

        @GetMapping("/test/max-upload-size")
        void triggerMaxUploadSize() {
            throw new MaxUploadSizeExceededException(1024);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser
    void unexpectedException_returnsProblemDetail500() throws Exception {
        mockMvc.perform(get("/test/unexpected-error"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:goodfunds:problem:internal-server-error"))
                .andExpect(jsonPath("$.title").value("Erro interno"))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.instance").value("/test/unexpected-error"));
    }

    @Test
    @WithMockUser
    void maxUploadSizeExceeded_returnsProblemDetail413() throws Exception {
        mockMvc.perform(get("/test/max-upload-size"))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:goodfunds:problem:max-upload-size-exceeded"))
                .andExpect(jsonPath("$.title").value("Arquivo muito grande"))
                .andExpect(jsonPath("$.status").value(413));
    }
}
