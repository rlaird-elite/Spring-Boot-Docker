package com.example.demo.vendor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vendors")
public class VendorController {

    @Autowired
    private VendorRepository vendorRepository;

    @GetMapping
    public List<Vendor> getAllVendors() {
        return vendorRepository.findAll();
    }

    @PostMapping
    public Vendor createVendor(@RequestBody Vendor vendor) {
        return vendorRepository.save(vendor);
    }
}
