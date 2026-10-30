package com.carrental.api;

import com.carrental.api.dto.AvailabilityResponse;
import com.carrental.api.dto.CreateReservationRequest;
import com.carrental.api.dto.ReservationResponse;
import com.carrental.domain.CarType;
import com.carrental.domain.Reservation;
import com.carrental.service.ReservationService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping("/reservations")
    public ResponseEntity<ReservationResponse> create(@Valid @RequestBody CreateReservationRequest request) {
        Reservation reservation = reservationService.reserve(request.carType(), request.startAt(), request.days());
        return ResponseEntity
                .created(URI.create("/api/reservations/" + reservation.getId()))
                .body(ReservationResponse.from(reservation));
    }

    @GetMapping("/reservations/{id}")
    public ReservationResponse get(@PathVariable Long id) {
        return ReservationResponse.from(reservationService.get(id));
    }

    @DeleteMapping("/reservations/{id}")
    public ReservationResponse cancel(@PathVariable Long id) {
        return ReservationResponse.from(reservationService.cancel(id));
    }

    @GetMapping("/availability")
    public AvailabilityResponse availability(
            @RequestParam CarType carType,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startAt,
            @RequestParam int days) {
        long available = reservationService.countAvailable(carType, startAt, days);
        return new AvailabilityResponse(carType, startAt, days, available);
    }
}
