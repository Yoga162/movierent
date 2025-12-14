package com.example.movierent.Controller;
import com.example.movierent.Model.*;
import com.example.movierent.Repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/rental")
public class RentalController {

    @Autowired private RentalRepository rentalRepo;
    @Autowired private MovieRepository movieRepo;
    @Autowired private UserRepository userRepo;

    // Form inputan pinjam
    static class RentalForm {
        public Long userId;
        public Long movieId;
    }

    // 1. PINJAM MOVIE
    @PostMapping("/borrow")
    public String borrow(@RequestBody RentalForm form) {
        User user = userRepo.findById(form.userId).orElse(null);
        if (user == null) return "User tidak ditemukan";

        // --- CEK ROLE USER DISINI ---
        if (!user.getRole().equals("USER")) {
            return "GAGAL: Admin tidak boleh meminjam film!";
        }

        Movie movie = movieRepo.findById(form.movieId).orElse(null);
        if (movie == null) return "Movie tidak ditemukan";

        // Cek Ketersediaan
        if (!movie.isAvailable()) {
            return "GAGAL: Movie sedang dipinjam orang lain!";
        }

        // Proses Pinjam
        Rental rental = new Rental();
        rental.setUser(user);
        rental.setMovie(movie);
        rental.setStatus("BORROWED");
        rentalRepo.save(rental);

        movie.setAvailable(false); // Update status movie jadi habis
        movieRepo.save(movie);

        return "BERHASIL meminjam: " + movie.getTitle();
    }

    // 2. KEMBALIKAN MOVIE
    @PostMapping("/return")
    public String returnMovie(@RequestBody RentalForm form) {
        Rental rental = rentalRepo.findByUserIdAndMovieIdAndStatus(form.userId, form.movieId, "BORROWED");

        if (rental == null) return "Data peminjaman tidak ditemukan!";

        rental.setStatus("RETURNED");
        rentalRepo.save(rental);

        Movie movie = rental.getMovie();
        movie.setAvailable(true); // Update status movie jadi ada lagi
        movieRepo.save(movie);

        return "BERHASIL mengembalikan: " + movie.getTitle();
    }

    // 3. LIHAT HISTORY (Dengan Keamanan)
    // URL: localhost:8080/rental/history/2?requesterId=1
    @GetMapping("/history/{targetUserId}")
    public List<Rental> getUserHistory(
            @PathVariable Long targetUserId, // History siapa yang mau diintip?
            @RequestParam Long requesterId   // Siapa yang sedang request?
    ) {
        // Cek siapa yang request
        User requester = userRepo.findById(requesterId).orElse(null);
        if (requester == null) throw new RuntimeException("User tidak dikenal!");

        // ATURAN 1: Kalau dia ADMIN, boleh lihat punya siapa saja.
        if (requester.getRole().equals("ADMIN")) {
            return rentalRepo.findByUserId(targetUserId);
        }

        // ATURAN 2: Kalau dia USER BIASA, cuma boleh lihat punya diri sendiri.
        if (requester.getId().equals(targetUserId)) {
            return rentalRepo.findByUserId(targetUserId);
        }

        // Kalau melanggar aturan di atas:
        throw new RuntimeException("DILARANG: Kamu tidak boleh mengintip history orang lain!");
    }
}