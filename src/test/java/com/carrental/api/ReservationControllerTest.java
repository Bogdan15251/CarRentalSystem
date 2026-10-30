package com.carrental.api;

import com.carrental.domain.Car;
import com.carrental.domain.CarType;
import com.carrental.domain.RentalPeriod;
import com.carrental.domain.Reservation;
import com.carrental.service.NoCarAvailableException;
import com.carrental.service.ReservationNotFoundException;
import com.carrental.service.ReservationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Web-layer slice: HTTP contract, validation and error mapping. Business logic is mocked. */
@WebMvcTest(ReservationController.class)
class ReservationControllerTest {

    private static final LocalDateTime START = LocalDateTime.of(2030, 6, 1, 10, 0);

    @Autowired
    MockMvc mvc;

    @MockitoBean
    ReservationService service;

    @Test
    void createReturns201WithLocation() throws Exception {
        when(service.reserve(CarType.SUV, START, 3)).thenReturn(reservation(42L, CarType.SUV, 3));

        mvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"carType":"SUV","startAt":"2030-06-01T10:00:00","days":3}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/reservations/42"))
                .andExpect(jsonPath("$.carType").value("SUV"))
                .andExpect(jsonPath("$.endAt").value("2030-06-04T10:00:00"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void invalidRequestReturns400AndDoesNotHitService() throws Exception {
        mvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"carType":"SUV","startAt":"2030-06-01T10:00:00","days":0}
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void unknownCarTypeReturns400() throws Exception {
        mvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"carType":"TRUCK","startAt":"2030-06-01T10:00:00","days":1}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void noAvailabilityReturns409ProblemDetail() throws Exception {
        when(service.reserve(eq(CarType.VAN), any(), anyInt()))
                .thenThrow(new NoCarAvailableException(CarType.VAN, RentalPeriod.of(START, 1)));

        mvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"carType":"VAN","startAt":"2030-06-01T10:00:00","days":1}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("No VAN available")));
    }

    @Test
    void unknownReservationReturns404() throws Exception {
        when(service.get(7L)).thenThrow(new ReservationNotFoundException(7L));

        mvc.perform(get("/api/reservations/7"))
                .andExpect(status().isNotFound());
    }

    @Test
    void availabilityEndpoint() throws Exception {
        when(service.countAvailable(CarType.SEDAN, START, 2)).thenReturn(4L);

        mvc.perform(get("/api/availability")
                        .param("carType", "SEDAN")
                        .param("startAt", "2030-06-01T10:00:00")
                        .param("days", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(4));
    }

    private static Reservation reservation(Long id, CarType type, int days) {
        Reservation r = new Reservation(new Car(type, type + "-001"), RentalPeriod.of(START, days));
        ReflectionTestUtils.setField(r, "id", id);
        return r;
    }
}
