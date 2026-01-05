package com.example.movierent.Controller;

import com.example.movierent.Model.*;
import com.example.movierent.Repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/movie")
public class MovieController {

    @Autowired private MovieRepository movieRepo;
    @Autowired private UserRepository userRepo;

    static class MovieForm {
        public Long adminId;
        public String title;
        public String genre;
        public Double price;
    }


    // Tambah movie (only Admin)
    @PostMapping("/add")
    public String addMovie(@RequestBody MovieForm form) {

        User admin = userRepo.findById(form.adminId).orElse(null);

        if (admin == null || !admin.getRole().equals("ADMIN")) {
            return "GAGAL: Anda bukan Admin!";
        }

        Movie movie = new Movie();
        movie.setTitle(form.title);
        movie.setGenre(form.genre);
        movie.setPrice(form.price);

        movie.setDeleted(false);

        movieRepo.save(movie);
        return "SUKSES menambahkan film: " + form.title;
    }

    // list movie (public)
    @GetMapping({"/list", "/list/{page}"})
    public Map<String, Object> getListMovies(@PathVariable(required = false) Integer page) {

        if (page == null || page < 1) {
            page = 1;
        }

        int limit = 5;

        Pageable pageable = PageRequest.of(page - 1, limit, Sort.by("id"));

        Page<Movie> moviePage = movieRepo.findByDeletedFalse(pageable);
        List<Map<String, Object>> formattedMovies = new ArrayList<>();

        for (Movie m : moviePage.getContent()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", m.getId());
            item.put("title", m.getTitle());
            item.put("genre", m.getGenre());
            item.put("available", m.isAvailable());

            // Format Price Konsisten
            item.put("price", formatRupiah(m.getPrice()));

            formattedMovies.add(item);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("halaman_sekarang", page);
        response.put("total_halaman", moviePage.getTotalPages());
        response.put("total_film", moviePage.getTotalElements());
        response.put("data_movies", formattedMovies);

        return response;
    }

    // update movie (only admin)
    @PutMapping("/update/{movieId}")
    public Map<String, Object> updateMovie(@PathVariable Long movieId, @RequestBody Map<String, Object> payload) {

        Long adminId = Long.valueOf(payload.get("adminId").toString());
        User admin = userRepo.findById(adminId).orElse(null);

        if (admin == null || !admin.getRole().equals("ADMIN")) {
            throw new RuntimeException("GAGAL: Kamu bukan Admin! Tidak boleh edit film.");
        }

        Movie movie = movieRepo.findById(movieId).orElse(null);
        if (movie == null) {
            throw new RuntimeException("Film tidak ditemukan!");
        }

        if (payload.get("title") != null) {
            movie.setTitle(payload.get("title").toString());
        }

        if (payload.get("genre") != null) {
            movie.setGenre(payload.get("genre").toString());
        }

        if (payload.get("price") != null) {
            Double newPrice = Double.valueOf(payload.get("price").toString());
            movie.setPrice(newPrice);
        }

        Movie savedMovie = movieRepo.save(movie);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", savedMovie.getId());
        response.put("title", savedMovie.getTitle());
        response.put("genre", savedMovie.getGenre());
        response.put("available", savedMovie.isAvailable());
        response.put("price", formatRupiah(savedMovie.getPrice()));

        return response;
    }

    // delete movie (only admin)
    @DeleteMapping("/delete/{id}")
    public String deleteMovie(@PathVariable Long id, @RequestParam Long adminId) {
        User admin = userRepo.findById(adminId).orElse(null);

        if (admin == null || !admin.getRole().equals("ADMIN")) {
            return "GAGAL: Hanya Admin yang boleh menghapus!";
        }

        Movie movie = movieRepo.findById(id).orElse(null);
        if (movie == null) {
            return "Film tidak ditemukan!";
        }

        movie.setDeleted(true);
        movie.setAvailable(false);

        movieRepo.save(movie);

        return "Movie berhasil dihapus.";
    }

    private String formatRupiah(Double angka) {
        if (angka == null) return "Rp0";
        return String.format("Rp%,.0f", angka).replace(',', '.');
    }
}