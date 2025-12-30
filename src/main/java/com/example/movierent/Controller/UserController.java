package com.example.movierent.Controller; // Sesuaikan huruf besar/kecil folder kamu

import com.example.movierent.Model.Rental;
import com.example.movierent.Model.User;
import com.example.movierent.Repository.RentalRepository;
import com.example.movierent.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RentalRepository rentalRepository;

    // register user (admin/user)
    @PostMapping("/register")
    public User registerUser(@RequestBody User user) {
        return userRepository.save(user);
    }

    // login (admin/user)
    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> payload) {
        String email = payload.get("email");
        String password = payload.get("password");

        // 1. Cek User di Database
        User user = userRepository.findByEmail(email); // Sesuaikan nama repo kamu (userRepo/userRepository)

        if (user == null || !user.getPassword().equals(password)) {
            throw new RuntimeException("Email atau Password salah!");
        }

        // 2. Ambil Data History (Mentah)
        List<Rental> rawHistory;
        if (user.getRole().equals("ADMIN")) {
            rawHistory = rentalRepository.findAll(); // Admin lihat semua
        } else {
            rawHistory = rentalRepository.findByUserId(user.getId()); // User lihat punya sendiri
        }

        // 3. --- PROSES MERAPIKAN HISTORY (Mapping) ---
        List<Map<String, Object>> cleanHistory = new ArrayList<>();

        for (Rental r : rawHistory) {
            Map<String, Object> item = new HashMap<>();

            item.put("id_transaksi", r.getId());
            item.put("judul_film", r.getMovie().getTitle());
            item.put("status", r.getStatus());

            // Tampilkan nama peminjam (Penting buat Admin, buat User biasa info ini bonus aja)
            item.put("peminjam", r.getUser().getName());

            // Handle Tanggal (Cek Null)
            item.put("tgl_pinjam", r.getRentalDate());
            item.put("jatuh_tempo", r.getDueDate() != null ? r.getDueDate() : "-");
            item.put("tgl_kembali", r.getReturnDate() != null ? r.getReturnDate() : "Belum Kembali");

            // Handle Uang (Cek Null -> Ubah jadi 0)
            item.put("biaya_sewa", r.getRentalCost() != null ? r.getRentalCost() : 0);
            item.put("denda", r.getPenalty() != null ? r.getPenalty() : 0);

            cleanHistory.add(item);
        }

        // 4. --- PROSES MERAPIKAN INFO USER (Biar Password Gak Bocor) ---
        Map<String, Object> cleanUser = new HashMap<>();
        cleanUser.put("id", user.getId());
        cleanUser.put("name", user.getName());
        cleanUser.put("email", user.getEmail());
        cleanUser.put("role", user.getRole());
        // Password SENGAJA TIDAK DIMASUKKAN di sini

        // 5. Bungkus Response Akhir
        Map<String, Object> response = new HashMap<>();
        response.put("user_info", cleanUser);
        response.put("rental_history", cleanHistory);

        return response;
    }

    @GetMapping("/list/{adminId}")
    public List<Map<String, Object>> getAllUsers(@PathVariable Long adminId) {

        // 1. Cek Admin
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Admin tidak ditemukan"));

        if (!admin.getRole().equals("ADMIN")) {
            throw new RuntimeException("AKSES DITOLAK: Kamu bukan Admin!");
        }

        // 2. Ambil & Filter Data
        List<User> allUsers = userRepository.findAll();
        List<Map<String, Object>> cleanList = new ArrayList<>();

        for (User u : allUsers) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", u.getId());
            item.put("nama", u.getName());
            item.put("email", u.getEmail());
            item.put("role", u.getRole());
            // Password tidak dimasukkan
            cleanList.add(item);
        }

        return cleanList;
    }
}