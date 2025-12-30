package com.example.movierent.Controller;
import com.example.movierent.Model.*;
import com.example.movierent.Repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    public Map<String, Object> borrowMovie(@RequestBody Map<String, Object> payload) {

        // 1. Ambil Data
        Long userId = Long.valueOf(payload.get("userId").toString());
        Long movieId = Long.valueOf(payload.get("movieId").toString());
        Integer days = Integer.valueOf(payload.get("days").toString());

        // 2. Validasi
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User tidak ada"));
        Movie movie = movieRepo.findById(movieId)
                .orElseThrow(() -> new RuntimeException("Movie tidak ada"));

        if (!movie.isAvailable()) {
            throw new RuntimeException("Film sedang kosong!");
        }

        // 3. --- HITUNG BIAYA SEWA (TRANSAKSI AWAL) ---
        // Kalau di database harga kosong, kita kasih default 20.000
        double hargaPerHari = (movie.getPrice() != null) ? movie.getPrice() : 20000.0;

        // Rumus: Harga x Hari
        double totalBiaya = hargaPerHari * days;

        // 4. Simpan ke Rental
        Rental rental = new Rental();
        rental.setUser(user);
        rental.setMovie(movie);
        rental.setStatus("BORROWED");

        // Set Waktu
        LocalDate today = LocalDate.now();
        rental.setRentalDate(today);
        rental.setDueDate(today.plusDays(days));

        // Set Uang
        rental.setRentalCost(totalBiaya); // <--- UANG MASUK SINI
        rental.setPenalty(0.0);

        // 5. Update Movie & Save
        movie.setAvailable(false);
        movieRepo.save(movie);
        rentalRepo.save(rental);

        // 6. Buat Respon Struk Pembayaran (JSON Rapi)
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Transaksi Berhasil!");
        response.put("judul_film", movie.getTitle());
        response.put("peminjam", user.getName());
        response.put("lama_pinjam", days + " hari");
        response.put("biaya_per_hari", hargaPerHari);
        response.put("TOTAL_BAYAR", totalBiaya); // <--- INI YG PENTING
        response.put("jatuh_tempo", rental.getDueDate());

        return response;
    }

    //return movie
    @PutMapping("/return/{id}")
    public Map<String, Object> returnMovie(@PathVariable Long id) {

        // 1. Cari Data Rental berdasarkan ID Transaksi
        Rental rental = rentalRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Data transaksi tidak ditemukan!"));

        // 2. Validasi: Jangan sampai dikembalikan 2 kali
        if (rental.getStatus().equals("RETURNED")) {
            throw new RuntimeException("Film ini sudah dikembalikan sebelumnya!");
        }

        // 3. Set Tanggal Pengembalian (HARI INI)
        LocalDate today = LocalDate.now();
        rental.setReturnDate(today);
        rental.setStatus("RETURNED");

        // 4. LOGIKA TIMER / DENDA
        // Menghitung jarak hari antara Jatuh Tempo (Due Date) vs Hari Ini
        long terlambat = ChronoUnit.DAYS.between(rental.getDueDate(), today);

        double denda = 0.0;

        if (terlambat > 0) {
            // Jika terlambat (angka positif), kena denda Rp 10.000 per hari
            // Kamu bisa ganti 10000 dengan angka lain sesuai keinginan
            denda = terlambat * 10000;
        }

        rental.setPenalty(denda);

        // 5. Update Status Film jadi AVAILABLE (Bisa dipinjam orang lain lagi)
        Movie movie = rental.getMovie();
        movie.setAvailable(true);
        movieRepo.save(movie);

        // 6. Simpan Perubahan Rental
        rentalRepo.save(rental);

        // 7. Buat Struk / Respon JSON
        Map<String, Object> response = new HashMap<>();
        response.put("status", "Berhasil dikembalikan");
        response.put("judul_film", movie.getTitle());
        response.put("nama_peminjam", rental.getUser().getName());
        response.put("tgl_jatuh_tempo", rental.getDueDate());
        response.put("tgl_dikembalikan", rental.getReturnDate());

        if (terlambat > 0) {
            response.put("keterangan", "TERLAMBAT " + terlambat + " HARI");
            response.put("TOTAL_DENDA", denda);
        } else {
            response.put("keterangan", "Tepat Waktu (Terima kasih)");
            response.put("TOTAL_DENDA", 0);
        }

        return response;
    }


    // 1. REKAP MINGGUAN
    @GetMapping("/report/weekly/{adminId}")
    public Map<String, Object> getWeeklyReport(@PathVariable Long adminId) {
        return generateReport(adminId, "WEEKLY");
    }

    // 2. REKAP BULANAN
    @GetMapping("/report/monthly/{adminId}")
    public Map<String, Object> getMonthlyReport(@PathVariable Long adminId) {
        return generateReport(adminId, "MONTHLY");
    }

    // LOGIKA UTAMA (Private)
    private Map<String, Object> generateReport(Long adminId, String type) {
        // Cek Admin
        User admin = userRepo.findById(adminId).orElse(null);
        if (admin == null || !admin.getRole().equals("ADMIN")) {
            throw new RuntimeException("GAGAL: Hanya Admin yang boleh lihat laporan!");
        }

        // Hitung Tanggal
        LocalDate now = LocalDate.now();
        LocalDate startDate;

        if (type.equals("WEEKLY")) {
            startDate = now.minusWeeks(1); // Mundur 7 hari
        } else {
            startDate = now.with(TemporalAdjusters.firstDayOfMonth()); // Tanggal 1 bulan ini
        }

        // Ambil Data
        List<Rental> rawRentals = rentalRepo.findByRentalDateBetween(startDate, now);

        List<Map<String, Object>> cleanData = new ArrayList<>();

        for (Rental rental : rawRentals) {
            Map<String, Object> item = new HashMap<>();

            // Kita pilih manual field apa saja yang mau ditampilkan
            item.put("id_transaksi", rental.getId());
            item.put("tanggal_sewa", rental.getRentalDate());
            item.put("nama_peminjam", rental.getUser().getName()); // Ambil Namanya saja
            item.put("judul_film", rental.getMovie().getTitle());  // Ambil Judulnya saja
            item.put("status", rental.getStatus());

            cleanData.add(item);
        }

        Map<String, Object> report = new HashMap<>();
        report.put("judul_laporan", "Laporan " + type);
        report.put("periode_awal", startDate); // Bonus: Biar tau dari tanggal brp
        report.put("periode_akhir", now);
        report.put("total_transaksi", rawRentals.size());
        report.put("data_transaksi", cleanData); // Masukkan

        return report;
    }
}