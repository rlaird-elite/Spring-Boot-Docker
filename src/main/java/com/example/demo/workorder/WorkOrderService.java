package com.example.demo.workorder;

import com.example.demo.property.Property;
import com.example.demo.property.PropertyRepository;
import com.example.demo.vendor.Vendor;
import com.example.demo.vendor.VendorRepository;
import org.springframework.stereotype.Service;

@Service
public class WorkOrderService {

    private final WorkOrderRepository workOrderRepository;
    private final PropertyRepository propertyRepository;
    private final VendorRepository vendorRepository;

    public WorkOrderService(WorkOrderRepository workOrderRepository,
                            PropertyRepository propertyRepository,
                            VendorRepository vendorRepository) {
        this.workOrderRepository = workOrderRepository;
        this.propertyRepository = propertyRepository;
        this.vendorRepository = vendorRepository;
    }

    public WorkOrder createWorkOrder(WorkOrder workOrder, Long propertyId, Long vendorId) {
        // 1. Find the parent property
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new RuntimeException("Property not found with id: " + propertyId));

        // 2. Find the vendor (if a vendorId was provided)
        Vendor vendor = null;
        if (vendorId != null) {
            vendor = vendorRepository.findById(vendorId)
                    .orElseThrow(() -> new RuntimeException("Vendor not found with id: " + vendorId));
        }

        // 3. Set the relationships
        workOrder.setProperty(property);
        workOrder.setVendor(vendor);

        // --- FIX: Set the default status in the service ---
        workOrder.setStatus("PENDING");

        // 4. Save and return
        return workOrderRepository.save(workOrder);
    }
}

