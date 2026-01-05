package com.example.movierent.Model;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity @Data @Table(name = "rentals")
public class Rental {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne @JoinColumn(name = "movie_id")
    private Movie movie;
    private String status;
    private LocalDate rentalDate;
    private Double rentalCost;
    private LocalDate dueDate;
    private LocalDate returnDate;
    private Double penalty;
}