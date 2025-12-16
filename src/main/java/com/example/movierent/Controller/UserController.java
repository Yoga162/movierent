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

        User user = userRepository.findByEmail(email);

        if (user == null || !user.getPassword().equals(password)) {
            throw new RuntimeException("Email atau Password salah!");
        }

        List<Rental> history;

        if (user.getRole().equals("ADMIN")) {
            history = rentalRepository.findAll();
        } else {
            history = rentalRepository.findByUserId(user.getId());
        }

        Map<String, Object> response = new HashMap<>();
        response.put("user_info", user);
        response.put("rental_history", history);

        return response;
    }
}