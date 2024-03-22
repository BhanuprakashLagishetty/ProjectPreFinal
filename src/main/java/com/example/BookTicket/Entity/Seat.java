package com.example.BookTicket.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Setter
@Getter
public class Seat {
    @Id
    @GeneratedValue
    private Long seatNumber;
    private String typeOfSeat;

    private boolean available;
    private String seatStatus;
    @ManyToOne
    @JoinColumn(name = "train_id")
    @JsonIgnore
    private Train train;


    @ManyToOne
    @JoinColumn(name = "booking_id")
    @JsonIgnore
    private Booking bookingSeats;

    @OneToMany(mappedBy = "seat", cascade =  CascadeType.MERGE)
//    @JsonManagedReference
    private List<BookingDate> bookingDates = new ArrayList<>();
}
