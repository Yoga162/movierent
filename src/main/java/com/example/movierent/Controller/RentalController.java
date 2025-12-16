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

    static class RentalForm {
        public Long userId;
        public Long movieId;
    }

    //borrow movie
    @PostMapping("/borrow")
    public String borrow(@RequestBody RentalForm form) {
        User user = userRepo.findById(form.userId).orElse(null);
        if (user == null) return "User tidak ditemukan";

        if (!user.getRole().equals("USER")) {
            return "GAGAL: Admin tidak boleh meminjam film!";
        }

        Movie movie = movieRepo.findById(form.movieId).orElse(null);
        if (movie == null) return "Movie tidak ditemukan";

        if (!movie.isAvailable()) {
            return "GAGAL: Movie sedang dipinjam orang lain!";
        }

        Rental rental = new Rental();
        rental.setUser(user);
        rental.setMovie(movie);
        rental.setStatus("BORROWED");
        rentalRepo.save(rental);

        movie.setAvailable(false);
        movieRepo.save(movie);

        return "BERHASIL meminjam: " + movie.getTitle();
    }

    //return movie
    @PostMapping("/return")
    public String returnMovie(@RequestBody RentalForm form) {
        Rental rental = rentalRepo.findByUserIdAndMovieIdAndStatus(form.userId, form.movieId, "BORROWED");

        if (rental == null) return "Data peminjaman tidak ditemukan!";

        rental.setStatus("RETURNED");
        rentalRepo.save(rental);

        Movie movie = rental.getMovie();
        movie.setAvailable(true);
        movieRepo.save(movie);

        return "BERHASIL mengembalikan: " + movie.getTitle();
    }

    //see history
    @GetMapping("/history/{targetUserId}")
    public List<Rental> getUserHistory(
            @PathVariable Long targetUserId,
            @RequestParam Long requesterId
    ) {

        User requester = userRepo.findById(requesterId).orElse(null);
        if (requester == null) throw new RuntimeException("User tidak dikenal!");

        if (requester.getRole().equals("ADMIN")) {
            return rentalRepo.findByUserId(targetUserId);
        }

        if (requester.getId().equals(targetUserId)) {
            return rentalRepo.findByUserId(targetUserId);
        }

        throw new RuntimeException("DILARANG: Kamu tidak boleh mengintip history orang lain!");
    }
}