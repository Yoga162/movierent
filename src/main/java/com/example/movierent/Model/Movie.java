package com.example.movierent.Model;
import jakarta.persistence.*;
import lombok.Data;

@Entity @Data @Table(name = "movies")
public class Movie {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String genre;
    private boolean available = true;
    private Double price;
}