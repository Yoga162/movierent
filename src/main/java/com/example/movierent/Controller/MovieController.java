package com.example.movierent.Controller;
import com.example.movierent.Model.*;
import com.example.movierent.Repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/movie")
public class MovieController {

    @Autowired private MovieRepository movieRepo;
    @Autowired private UserRepository userRepo;

    // Form kecil untuk menangkap data inputan + ID Admin
    static class MovieForm {
        public Long adminId; // <--- Admin wajib setor ID buat pengecekan
        public String title;
        public String genre;
    }


    // 1. TAMBAH MOVIE (Hanya Admin)
    @PostMapping("/add")
    public String addMovie(@RequestBody MovieForm form) {
        // Cek apakah ID yang dikirim itu Admin?
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

    // 2. LIHAT SEMUA (Bebas Siapa Saja)
    @GetMapping("/all")
    public List<Movie> getAll() {
        return movieRepo.findAll();
    }

    // 3. HAPUS MOVIE (Hanya Admin)
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