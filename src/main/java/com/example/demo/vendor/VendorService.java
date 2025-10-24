package com.example.demo.vendor;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Service layer for managing Vendor business logic.
 * This class abstracts the repository from the controller.
 */
@Service
public class VendorService {

    private final VendorRepository vendorRepository;

    // Inject the repository
    public VendorService(VendorRepository vendorRepository) {
        this.vendorRepository = vendorRepository;
    }

    /**
     * Retrieves all vendors.
     * @return A list of all vendors.
     */
    public List<Vendor> getAllVendors() {
        return vendorRepository.findAll();
    }

    /**
     * Retrieves a single vendor by its ID.
     * @param id The ID of the vendor.
     * @return An Optional containing the vendor if found, or empty if not.
     */
    public Optional<Vendor> getVendorById(Long id) {
        return vendorRepository.findById(id);
    }

    /**
     * Creates and saves a new vendor.
     * @param vendor The vendor data to save.
     * @return The saved vendor.
     */
    public Vendor createVendor(Vendor vendor) {
        return vendorRepository.save(vendor);
    }

    /**
     * Updates an existing vendor.
     * @param id The ID of the vendor to update.
     * @param vendorDetails The new details for the vendor.
     * @return An Optional containing the updated vendor if it was found, or empty if not.
     */
    public Optional<Vendor> updateVendor(Long id, Vendor vendorDetails) {
        Optional<Vendor> optionalVendor = vendorRepository.findById(id);
        if (optionalVendor.isPresent()) {
            Vendor existingVendor = optionalVendor.get();
            existingVendor.setName(vendorDetails.getName());
            existingVendor.setServiceType(vendorDetails.getServiceType());
            existingVendor.setTrade(vendorDetails.getTrade());
            existingVendor.setPhoneNumber(vendorDetails.getPhoneNumber());

            Vendor updatedVendor = vendorRepository.save(existingVendor);
            return Optional.of(updatedVendor);
        } else {
            return Optional.empty(); // Not found, so can't update
        }
    }

    /**
     * Deletes a vendor by its ID.
     * @param id The ID of the vendor to delete.
     * @return true if the vendor was found and deleted, false otherwise.
     */
    public boolean deleteVendor(Long id) {
        Optional<Vendor> vendorOptional = vendorRepository.findById(id);
        if (vendorOptional.isPresent()) {
            vendorRepository.delete(vendorOptional.get());
            return true; // Found and deleted
        } else {
            return false; // Not found
        }
    }
}
