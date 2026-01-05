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

        User user = userRepository.findByEmail(email); // Sesuaikan nama repo kamu (userRepo/userRepository)

        if (user == null || !user.getPassword().equals(password)) {
            throw new RuntimeException("Email atau Password salah!");
        }

        List<Rental> rawHistory;
        if (user.getRole().equals("ADMIN")) {
            rawHistory = rentalRepository.findAll(); // Admin lihat semua
        } else {
            rawHistory = rentalRepository.findByUserId(user.getId()); // User lihat punya sendiri
        }

        List<Map<String, Object>> cleanHistory = new ArrayList<>();

        for (Rental r : rawHistory) {
            Map<String, Object> item = new HashMap<>();

            item.put("id_transaksi", r.getId());
            item.put("judul_film", r.getMovie().getTitle());
            item.put("status", r.getStatus());

            item.put("peminjam", r.getUser().getName());

            item.put("tgl_pinjam", r.getRentalDate());
            item.put("jatuh_tempo", r.getDueDate() != null ? r.getDueDate() : "-");
            item.put("tgl_kembali", r.getReturnDate() != null ? r.getReturnDate() : "Belum Kembali");

            item.put("biaya_sewa", r.getRentalCost() != null ? r.getRentalCost() : 0);
            item.put("denda", r.getPenalty() != null ? r.getPenalty() : 0);

            cleanHistory.add(item);
        }

        Map<String, Object> cleanUser = new HashMap<>();
        cleanUser.put("id", user.getId());
        cleanUser.put("name", user.getName());
        cleanUser.put("email", user.getEmail());
        cleanUser.put("role", user.getRole());

        Map<String, Object> response = new HashMap<>();
        response.put("user_info", cleanUser);
        response.put("rental_history", cleanHistory);

        return response;
    }

    //list user (admin only)
    @GetMapping("/list/{adminId}")
    public List<Map<String, Object>> getAllUsers(@PathVariable Long adminId) {

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Admin tidak ditemukan"));

        if (!admin.getRole().equals("ADMIN")) {
            throw new RuntimeException("AKSES DITOLAK: Kamu bukan Admin!");
        }

        List<User> allUsers = userRepository.findAll();
        List<Map<String, Object>> cleanList = new ArrayList<>();

        for (User u : allUsers) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", u.getId());
            item.put("nama", u.getName());
            item.put("email", u.getEmail());
            item.put("role", u.getRole());
            cleanList.add(item);
        }

        return cleanList;
    }
}