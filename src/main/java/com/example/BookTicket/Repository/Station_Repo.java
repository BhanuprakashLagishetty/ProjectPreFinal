package com.example.BookTicket.Repository;

import com.example.BookTicket.Entity.Stations;
import org.springframework.data.jpa.repository.JpaRepository;

public interface Station_Repo extends JpaRepository<Stations, Long> {
}
