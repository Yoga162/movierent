package com.example.movierent.Repository;

import com.example.movierent.Model.Movie;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovieRepository extends JpaRepository<Movie, Long> {


    Page<Movie> findByDeletedFalse(Pageable pageable);
}