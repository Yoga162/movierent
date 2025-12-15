package com.example.movierent.Controller;
import com.example.movierent.Model.*;
import com.example.movierent.Repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

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

    // FITUR: UPDATE MOVIE (Khusus Admin)
    // URL: http://localhost:8080/movie/update/1
    @PutMapping("/update/{movieId}")
    public Movie updateMovie(@PathVariable Long movieId, @RequestBody Map<String, Object> payload) {

        // 1. CEK OTORITAS (Satpam Admin)
        Long adminId = Long.valueOf(payload.get("adminId").toString());
        User admin = userRepo.findById(adminId).orElse(null);

        if (admin == null || !admin.getRole().equals("ADMIN")) {
            throw new RuntimeException("GAGAL: Kamu bukan Admin! Tidak boleh edit film.");
        }

        // 2. CARI FILM YANG MAU DIEDIT
        Movie movie = movieRepo.findById(movieId).orElse(null);
        if (movie == null) {
            throw new RuntimeException("Film tidak ditemukan!");
        }

        // 3. UPDATE DATA
        // Kita ambil data baru dari JSON, lalu timpa data lama
        String newTitle = payload.get("title").toString();
        String newGenre = payload.get("genre").toString();

        movie.setTitle(newTitle);
        movie.setGenre(newGenre);

        // 4. SIMPAN PERUBAHAN
        return movieRepo.save(movie);
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