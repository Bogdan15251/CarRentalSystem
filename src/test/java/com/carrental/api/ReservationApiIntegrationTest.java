package com.carrental.api;

import com.carrental.TestClockConfig;
import com.carrental.repository.ReservationRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end through HTTP with the real service and database.
 *
 * Deliberately NOT @Transactional: with open-in-view=false the controller maps the entity
 * to a DTO outside any transaction, so it touches Reservation.car (LAZY) after the
 * persistence context is closed. The slice test mocks the service (no JPA proxies) and
 * ReservationServiceTest runs inside a test transaction (lazy loading always works there),
 * so neither would notice if the repository stopped fetching the car eagerly
 * (e.g. findWithCarById replaced by findById) — this test would fail with
 * LazyInitializationException.
 *
 * MockMvc is built from the context instead of @AutoConfigureMockMvc so this class reuses
 * the same cached Spring context as the other @SpringBootTest classes (both point to the
 * same in-memory H2; a second context would recreate its schema under the first one).
 */
@SpringBootTest
@Import(TestClockConfig.class)
class ReservationApiIntegrationTest {

    // TestClockConfig.NOW is 2026-01-01T00:00, so this is safely in the future.
    private static final String START = "2026-03-01T10:00:00";
    private static final String END = "2026-03-03T10:00:00";

    @Autowired
    WebApplicationContext context;

    @Autowired
    ReservationRepository reservationRepository;

    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @AfterEach
    void cleanUp() {
        reservationRepository.deleteAll();
    }

    @Test
    void createThenGetThenCancelThroughHttp() throws Exception {
        String location = mvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"carType":"SEDAN","startAt":"%s","days":2}
                                """.formatted(START)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeader("Location");

        // GET loads the reservation in a new transaction; the car must already be fetched
        // when the controller builds the response.
        mvc.perform(get(location))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.carType").value("SEDAN"))
                .andExpect(jsonPath("$.plateNumber").value(startsWith("SEDAN-")))
                .andExpect(jsonPath("$.startAt").value(START))
                .andExpect(jsonPath("$.endAt").value(END))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        mvc.perform(delete(location))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plateNumber").value(startsWith("SEDAN-")))
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        // The cancellation was committed, not just changed in memory.
        mvc.perform(get(location))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }
}
