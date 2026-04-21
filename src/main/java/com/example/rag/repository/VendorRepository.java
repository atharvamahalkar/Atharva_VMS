package com.example.rag.repository;

import com.example.rag.entity.Vendor;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface VendorRepository extends JpaRepository<Vendor, Integer> {
    Optional<Vendor> findByVendorName(String vendorName);
}