package com.example.movierent.Controller;
import com.example.movierent.Model.User;
import com.example.movierent.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired private UserRepository userRepo;

    // REGISTER (Admin & User daftar lewat sini)
    @PostMapping("/register")
    public User register(@RequestBody User user) {
        // Tips: Di Frontend/Hoppscotch, field 'role' harus diisi manual "ADMIN" atau "USER"
        return userRepo.save(user);
    }

    // LOGIN
    @PostMapping("/login")
    public String login(@RequestBody User loginData) {
        User userDb = userRepo.findByEmail(loginData.getEmail());

        if (userDb == null) return "Email salah!";

        if (userDb.getPassword().equals(loginData.getPassword())) {
            // Beri info ID dan Role agar User/Admin tahu identitasnya
            return "LOGIN SUKSES! ID Anda: " + userDb.getId() + ", Role: " + userDb.getRole();
        } else {
            return "Password salah!";
        }
    }
}