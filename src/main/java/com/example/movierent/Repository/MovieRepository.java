package com.example.movierent.Repository;

import com.example.movierent.Model.Movie;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovieRepository extends JpaRepository<Movie, Long> {

    // Query khusus untuk Soft Delete:
    // Hanya ambil film yang deleted = false (yang masih aktif)
    Page<Movie> findByDeletedFalse(Pageable pageable);
}