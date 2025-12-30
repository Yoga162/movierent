package com.example.movierent.Controller;
import com.example.movierent.Model.*;
import com.example.movierent.Repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@RestController
@RequestMapping("/movie")
public class MovieController {

    @Autowired private MovieRepository movieRepo;
    @Autowired private UserRepository userRepo;

    static class MovieForm {
        public Long adminId;
        public String title;
        public String genre;
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

        Pageable pageable = PageRequest.of(page - 1, limit, Sort.by("title"));
        Page<Movie> moviePage = movieRepo.findAll(pageable);

        Map<String, Object> response = new HashMap<>();

        // Masukkan List Filmnya
        response.put("data_movies", moviePage.getContent());

        // Masukkan Info Halaman yang PENTING saja
        response.put("halaman_sekarang", page);
        response.put("total_halaman", moviePage.getTotalPages());
        response.put("total_film", moviePage.getTotalElements());

        return response;
    }

    //update movie (only admin)
    @PutMapping("/update/{movieId}")
    public Movie updateMovie(@PathVariable Long movieId, @RequestBody Map<String, Object> payload) {

        Long adminId = Long.valueOf(payload.get("adminId").toString());
        User admin = userRepo.findById(adminId).orElse(null);

        if (admin == null || !admin.getRole().equals("ADMIN")) {
            throw new RuntimeException("GAGAL: Kamu bukan Admin! Tidak boleh edit film.");
        }

        Movie movie = movieRepo.findById(movieId).orElse(null);
        if (movie == null) {
            throw new RuntimeException("Film tidak ditemukan!");
        }

        String newTitle = payload.get("title").toString();
        String newGenre = payload.get("genre").toString();

        movie.setTitle(newTitle);
        movie.setGenre(newGenre);

        return movieRepo.save(movie);
    }

    // delete movie (only admin)
    @DeleteMapping("/delete/{id}")
    public String deleteMovie(@PathVariable Long id, @RequestParam Long adminId) {
        User admin = userRepo.findById(adminId).orElse(null);

        if (admin == null || !admin.getRole().equals("ADMIN")) {
            return "GAGAL: Hanya Admin yang boleh menghapus!";
        }

        movieRepo.deleteById(id);
        return "Movie berhasil dihapus";
    }
}