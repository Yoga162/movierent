package com.example.movierent.Repository;

import com.example.movierent.Model.Rental;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RentalRepository extends JpaRepository<Rental, Long> {

    List<Rental> findByUserId(Long userId);
    Rental findByUserIdAndMovieIdAndStatus(Long userId, Long movieId, String status);

}