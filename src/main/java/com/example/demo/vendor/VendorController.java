package com.example.demo.vendor;

import com.example.demo.property.Property;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import javax.swing.text.html.Option;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/vendors")
public class VendorController {

    private final VendorService vendorService;

    public VendorController(VendorService vendorService) {
        this.vendorService = vendorService;
    }

    @PostMapping
    public ResponseEntity<Vendor> createVendor(@Valid @RequestBody Vendor vendor) {
        Vendor savedVendor = vendorService.createVendor(vendor);
        return new ResponseEntity<>(savedVendor, HttpStatus.CREATED);
    }

    @GetMapping
    public List<Vendor> getAllVendors() {
        return vendorService.getAllVendors();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Vendor> getVendorById(@PathVariable Long id) {
        // Find the vendor in the repository by its ID
        Optional<Vendor> vendorOptional = vendorService.getVendorById(id);

        // Check if the vendor was found and return the appropriate response
        return vendorOptional
                .map(ResponseEntity::ok) // If present, wrap in 200 OK
                .orElse(ResponseEntity.notFound().build()); // If empty, return 404 Not Found
    }

    @PutMapping("/{id}")
    public ResponseEntity<Vendor> updateVendor(@PathVariable Long id, @Valid @RequestBody Vendor vendorDetails) {
        Optional<Vendor> updatedVendorOptional = vendorService.updateVendor(id, vendorDetails);

        return updatedVendorOptional
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVendor(@PathVariable Long id) {
        boolean wasDeleted = vendorService.deleteVendor(id);

        if (wasDeleted) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Map<String, String> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        return errors;
    }
}

