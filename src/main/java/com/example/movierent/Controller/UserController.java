package com.example.movierent.Controller; // Sesuaikan huruf besar/kecil folder kamu

import com.example.movierent.Model.Rental;
import com.example.movierent.Model.User;
import com.example.movierent.Repository.RentalRepository;
import com.example.movierent.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserRepository userRepository; // Variabel (huruf kecil)

    @Autowired
    private RentalRepository rentalRepository; // Variabel (huruf kecil)

    @PostMapping("/register")
    public User registerUser(@RequestBody User user) {
        return userRepository.save(user);
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> payload) {
        String email = payload.get("email");
        String password = payload.get("password");

        // 1. CARI USER
        User user = userRepository.findByEmail(email);

        // 2. CEK PASSWORD
        if (user == null || !user.getPassword().equals(password)) {
            throw new RuntimeException("Email atau Password salah!");
        }

        // --- BAGIAN INI YANG KITA UBAH ---
        List<Rental> history; // Siapkan wadah kosong

        if (user.getRole().equals("ADMIN")) {
            // JIKA ADMIN: Ambil SEMUA data rental (Global History)
            history = rentalRepository.findAll();
        } else {
            // JIKA USER BIASA: Ambil history milik dia saja
            history = rentalRepository.findByUserId(user.getId());
        }
        // ---------------------------------

        // 4. BUNGKUS PAKET
        Map<String, Object> response = new HashMap<>();
        response.put("user_info", user);
        response.put("rental_history", history); // Isinya dinamis tergantung siapa yang login

        return response;
    }
}