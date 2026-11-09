package com.tpi.paseadores.review.controller;

import com.tpi.paseadores.review.dto.ReviewResponse;
import com.tpi.paseadores.review.service.ReviewService;
import com.tpi.paseadores.shared.config.SecurityConfig;
import com.tpi.paseadores.shared.security.JwtAuthenticationFilter;
import com.tpi.paseadores.shared.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ReviewController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class})
@TestPropertySource(properties = "app.jwt.secret=test-secret-not-for-production-use")
class ReviewControllerTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private JwtService jwt;
    @MockBean
    private ReviewService service;

    @Test
    void listingReviewsRequiresAToken() throws Exception {
        mvc.perform(get("/api/walks/{walkId}/reviews", UUID.randomUUID())).andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @Test
    void createsAReviewWithAComment() throws Exception {
        UUID walkId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        var response = new ReviewResponse(UUID.randomUUID(), walkId, userId, 5, "Excelente paseo", LocalDateTime.now());
        when(service.create(eq(userId), eq(walkId), any())).thenReturn(response);

        // este comentario es justo el caso que rompía con @Max(120) sobre un String
        mvc.perform(post("/api/walks/{walkId}/reviews", walkId).header("Authorization", bearer(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"rating":5,"comment":"Excelente paseo"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rating").value(5))
                .andExpect(jsonPath("$.comment").value("Excelente paseo"));
    }

    @Test
    void rejectsACommentLongerThan120Characters() throws Exception {
        String tooLong = "a".repeat(121);
        mvc.perform(post("/api/walks/{walkId}/reviews", UUID.randomUUID()).header("Authorization", bearer(UUID.randomUUID()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"rating":5,"comment":"%s"}
                                """.formatted(tooLong)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("comment"));
        verifyNoInteractions(service);
    }

    @Test
    void rejectsARatingOutOfRange() throws Exception {
        mvc.perform(post("/api/walks/{walkId}/reviews", UUID.randomUUID()).header("Authorization", bearer(UUID.randomUUID()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"rating":6}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("rating"));
        verifyNoInteractions(service);
    }

    @Test
    void listsReviewsForAWalk() throws Exception {
        UUID walkId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        var review = new ReviewResponse(UUID.randomUUID(), walkId, userId, 4, null, LocalDateTime.now());
        when(service.getAll(userId, walkId)).thenReturn(List.of(review));

        mvc.perform(get("/api/walks/{walkId}/reviews", walkId).header("Authorization", bearer(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].rating").value(4));
    }

    private String bearer(UUID userId) {
        return "Bearer " + jwt.generateToken(userId.toString(), "CLIENT");
    }
}
