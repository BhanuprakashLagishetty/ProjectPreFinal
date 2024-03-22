package com.example.BookTicket.Entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
    private int numerOfSeats;
    private String bookingStatus;
    private LocalDateTime bookingTime;
    private LocalDate BookedDate;

    private String arrivalStation;
    private String DestinationStation;
    private double price;
    private String bookingType;
    @OneToMany(mappedBy = "bookingSeats", cascade = CascadeType.MERGE)
    private Set<Seat>seats = new HashSet<>();


    @OneToMany(mappedBy = "bookingSeats",cascade = CascadeType.MERGE)
    private Set<WaitingList>waitingLists=new HashSet<>();
    //one to one mapping
    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "payment_Id")
//    @JsonManagedReference
    private Payment payment;

    @Override
    public String toString() {
        return "Booking{" +
                "id=" + id +
                ", user=" + user +
                ", numerOfSeats=" + numerOfSeats +
                ", bookingStatus='" + bookingStatus + '\'' +
                '}';
    }
}

