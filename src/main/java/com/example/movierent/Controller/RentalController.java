package com.example.movierent.Controller;

import com.example.movierent.Model.*;
import com.example.movierent.Repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/rental")
public class RentalController {

    @Autowired private RentalRepository rentalRepo;
    @Autowired private MovieRepository movieRepo;
    @Autowired private UserRepository userRepo;


    //pinjam movie
    @PostMapping("/borrow")
    public Map<String, Object> borrowMovie(@RequestBody Map<String, Object> payload) {

        Long userId = Long.valueOf(payload.get("userId").toString());
        Long movieId = Long.valueOf(payload.get("movieId").toString());
        Integer days = Integer.valueOf(payload.get("days").toString());

        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User tidak ada"));
        Movie movie = movieRepo.findById(movieId)
                .orElseThrow(() -> new RuntimeException("Movie tidak ada"));

        if (!movie.isAvailable()) {
            throw new RuntimeException("Film sedang kosong!");
        }

        double hargaPerHari = (movie.getPrice() != null) ? movie.getPrice() : 20000.0;

        double totalBiaya = hargaPerHari * days;


        Rental rental = new Rental();
        rental.setUser(user);
        rental.setMovie(movie);
        rental.setStatus("BORROWED");

        LocalDate today = LocalDate.now();
        rental.setRentalDate(today);
        rental.setDueDate(today.plusDays(days));

        rental.setRentalCost(totalBiaya);
        rental.setPenalty(0.0);


        movie.setAvailable(false);
        movieRepo.save(movie);
        rentalRepo.save(rental);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id_transaksi", rental.getId());

        response.put("message", "Transaksi Berhasil!");
        response.put("judul_film", movie.getTitle());
        response.put("peminjam", user.getName());
        response.put("lama_pinjam", days + " hari");
        response.put("biaya_per_hari", formatRupiah(hargaPerHari));
        response.put("TOTAL_BAYAR", formatRupiah(totalBiaya));

        response.put("jatuh_tempo", rental.getDueDate());

        return response;
    }

    //kembalikan movie
    @PutMapping("/return/{id}")
    public Map<String, Object> returnMovie(@PathVariable Long id) {

        Rental rental = rentalRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Data transaksi tidak ditemukan!"));

        if (rental.getStatus().equals("RETURNED")) {
            throw new RuntimeException("Film ini sudah dikembalikan sebelumnya!");
        }

        LocalDate today = LocalDate.now();
        rental.setReturnDate(today);
        rental.setStatus("RETURNED");

        long terlambat = ChronoUnit.DAYS.between(rental.getDueDate(), today);

        double denda = 0.0;

        if (terlambat > 0) {
            denda = terlambat * 10000;
        }

        rental.setPenalty(denda);

        Movie movie = rental.getMovie();
        movie.setAvailable(true);
        movieRepo.save(movie);

        rentalRepo.save(rental);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "Berhasil dikembalikan");
        response.put("id_transaksi", rental.getId());
        response.put("judul_film", movie.getTitle());
        response.put("nama_peminjam", rental.getUser().getName());
        response.put("tgl_jatuh_tempo", rental.getDueDate());
        response.put("tgl_dikembalikan", rental.getReturnDate());

        if (terlambat > 0) {
            response.put("keterangan", "TERLAMBAT " + terlambat + " HARI");
            response.put("TOTAL_DENDA", formatRupiah(denda));
        } else {
            response.put("keterangan", "Tepat Waktu (Terima kasih)");
            response.put("TOTAL_DENDA", "Rp0");
        }

        return response;
    }


    // rekap mingguan
    @GetMapping("/report/weekly/{adminId}")
    public Map<String, Object> getWeeklyReport(@PathVariable Long adminId) {
        return generateReport(adminId, "WEEKLY");
    }

    // rekap bulanan
    @GetMapping("/report/monthly/{adminId}")
    public Map<String, Object> getMonthlyReport(@PathVariable Long adminId) {
        return generateReport(adminId, "MONTHLY");
    }

    private Map<String, Object> generateReport(Long adminId, String type) {
        // Cek Admin
        User admin = userRepo.findById(adminId).orElse(null);
        if (admin == null || !admin.getRole().equals("ADMIN")) {
            throw new RuntimeException("GAGAL: Hanya Admin yang boleh lihat laporan!");
        }

        LocalDate now = LocalDate.now();
        LocalDate startDate;

        if (type.equals("WEEKLY")) {
            startDate = now.minusWeeks(1);
        } else {
            startDate = now.with(TemporalAdjusters.firstDayOfMonth());
        }

        List<Rental> rawRentals = rentalRepo.findByRentalDateBetween(startDate, now);

        List<Map<String, Object>> cleanData = new ArrayList<>();

        for (Rental rental : rawRentals) {
            Map<String, Object> item = new LinkedHashMap<>();

            item.put("id_transaksi", rental.getId());
            item.put("tanggal_sewa", rental.getRentalDate());
            item.put("nama_peminjam", rental.getUser().getName());
            item.put("judul_film", rental.getMovie().getTitle());
            item.put("status", rental.getStatus());

            item.put("biaya_sewa", formatRupiah(rental.getRentalCost()));
            item.put("denda", formatRupiah(rental.getPenalty()));

            cleanData.add(item);
        }

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("judul_laporan", "Laporan " + type);
        report.put("periode_awal", startDate);
        report.put("periode_akhir", now);
        report.put("total_transaksi", rawRentals.size());
        report.put("data_transaksi", cleanData);

        return report;
    }

    private String formatRupiah(Double angka) {
        if (angka == null) return "Rp0";
        return String.format("Rp%,.0f", angka).replace(',', '.');
    }
}