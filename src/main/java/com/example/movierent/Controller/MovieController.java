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


    // create movie (only Admin)
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

        // --- [SOFT DELETE] Set default status deleted = false ---
        movie.setDeleted(false);
        // --------------------------------------------------------

        movieRepo.save(movie);
        return "SUKSES menambahkan film: " + form.title;
    }

    // all movies (public)
    @GetMapping({"/list", "/list/{page}"})
    public Map<String, Object> getListMovies(@PathVariable(required = false) Integer page) {

        if (page == null || page < 1) {
            page = 1;
        }

        int limit = 2;

        // Sort by ID Ascending
        Pageable pageable = PageRequest.of(page - 1, limit, Sort.by("id"));

        // --- [SOFT DELETE] Gunakan findByDeletedFalse ---
        // Agar film yang sudah dihapus tidak muncul di list
        Page<Movie> moviePage = movieRepo.findByDeletedFalse(pageable);
        // ------------------------------------------------

        // --- Pakai LinkedHashMap agar urutan JSON rapi ---
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

        //Validasi Admin
        if (admin == null || !admin.getRole().equals("ADMIN")) {
            throw new RuntimeException("GAGAL: Kamu bukan Admin! Tidak boleh edit film.");
        }

        //Validasi movie
        Movie movie = movieRepo.findById(movieId).orElse(null);
        if (movie == null) {
            throw new RuntimeException("Film tidak ditemukan!");
        }

        // Cek dan update Title
        if (payload.get("title") != null) {
            movie.setTitle(payload.get("title").toString());
        }

        // Cek dan update Genre
        if (payload.get("genre") != null) {
            movie.setGenre(payload.get("genre").toString());
        }

        // Cek dan Update Harga
        if (payload.get("price") != null) {
            Double newPrice = Double.valueOf(payload.get("price").toString());
            movie.setPrice(newPrice);
        }

        Movie savedMovie = movieRepo.save(movie);

        // --- Pakai LinkedHashMap agar urutan rapi ---
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", savedMovie.getId());
        response.put("title", savedMovie.getTitle());
        response.put("genre", savedMovie.getGenre());
        response.put("available", savedMovie.isAvailable());
        response.put("price", formatRupiah(savedMovie.getPrice()));

        return response;
    }

    // --- [SOFT DELETE] Ubah Method Delete ---
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

        // BUKAN deleteById, tapi update status
        movie.setDeleted(true);      // Tandai terhapus
        movie.setAvailable(false);   // Pastikan tidak bisa dipinjam lagi

        movieRepo.save(movie);

        return "Movie berhasil dihapus.";
    }

    // --- Helper Function ---
    private String formatRupiah(Double angka) {
        if (angka == null) return "Rp0";
        return String.format("Rp%,.0f", angka).replace(',', '.');
    }
}