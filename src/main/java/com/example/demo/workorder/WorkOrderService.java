package com.example.demo.workorder;

import com.example.demo.property.Property;
import com.example.demo.property.PropertyRepository;
import com.example.demo.vendor.Vendor;
import com.example.demo.vendor.VendorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class WorkOrderService {

    @Autowired
    private WorkOrderRepository workOrderRepository;

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private VendorRepository vendorRepository;

    // --- NEW METHOD ---
    public List<WorkOrder> getAllWorkOrders() {
        return workOrderRepository.findAll();
    }
    // --- END NEW METHOD ---

    public WorkOrder createWorkOrder(WorkOrder workOrder, Long propertyId, Long vendorId) {
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new NoSuchElementException("Property not found with id: " + propertyId));

        Vendor vendor = null;
        if (vendorId != null) {
            vendor = vendorRepository.findById(vendorId)
                    .orElseThrow(() -> new NoSuchElementException("Vendor not found with id: " + vendorId));
        }

        workOrder.setProperty(property);
        workOrder.setVendor(vendor);
        workOrder.setStatus("PENDING");

        return workOrderRepository.save(workOrder);
    }
}

