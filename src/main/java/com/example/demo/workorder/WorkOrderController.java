package com.example.demo.workorder;

import jakarta.validation.Valid; // Ensure validation annotations are imported
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException; // For handling access errors
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException; // For cleaner error responses

import java.util.List;
import java.util.Map; // For error messages

// --- ADD @RestController and @RequestMapping ---
@RestController
@RequestMapping("/api/workorders") // Base path for all work order endpoints
public class WorkOrderController {

    private final WorkOrderService workOrderService;

    public WorkOrderController(WorkOrderService workOrderService) {
        this.workOrderService = workOrderService;
    }

    // GET /api/workorders - Get all work orders (tenant-filtered by service)
    @GetMapping
    public ResponseEntity<List<WorkOrder>> getAllWorkOrders() {
        List<WorkOrder> workOrders = workOrderService.getAllWorkOrders();
        return ResponseEntity.ok(workOrders);
    }

    // GET /api/workorders/{id} - Get a single work order by ID (tenant-filtered by service)
    @GetMapping("/{id}")
    public ResponseEntity<WorkOrder> getWorkOrderById(@PathVariable Long id) {
        return workOrderService.getWorkOrderById(id)
                .map(ResponseEntity::ok) // If found, return 200 OK with the work order
                .orElse(ResponseEntity.notFound().build()); // If not found (or wrong tenant), return 404
    }

    // POST /api/workorders?propertyId=...&vendorId=... - Create a new work order
    @PostMapping
    public ResponseEntity<WorkOrder> createWorkOrder(
            @Valid @RequestBody WorkOrder workOrder, // Request body contains description, etc.
            @RequestParam Long propertyId,           // Property ID from query parameter
            @RequestParam(required = false) Long vendorId) { // Vendor ID is optional
        try {
            WorkOrder createdWorkOrder = workOrderService.createWorkOrder(workOrder, propertyId, vendorId);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdWorkOrder); // Return 201 Created
        } catch (AccessDeniedException e) {
            // If service throws AccessDeniedException (e.g., property/vendor not found for tenant)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
        } catch (IllegalStateException e) {
            // Handle cases like user not authenticated properly
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, e.getMessage());
        }
        // Other exceptions (like validation) will be handled globally or by default Spring Boot handlers
    }

    // PUT /api/workorders/{id}?propertyId=...&vendorId=... - Update an existing work order
    @PutMapping("/{id}")
    public ResponseEntity<WorkOrder> updateWorkOrder(
            @PathVariable Long id,
            @Valid @RequestBody WorkOrder workOrderDetails, // Request body contains updated fields
            @RequestParam Long propertyId,                 // New Property ID
            @RequestParam(required = false) Long vendorId) { // New optional Vendor ID
        try {
            return workOrderService.updateWorkOrder(id, workOrderDetails, propertyId, vendorId)
                    .map(ResponseEntity::ok) // If update successful, return 200 OK
                    .orElse(ResponseEntity.notFound().build()); // If work order not found for tenant, return 404
        } catch (AccessDeniedException e) {
            // If property/vendor ID is invalid for the tenant
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, e.getMessage());
        }
    }

    // PUT /api/workorders/{id}/status?status=... - Update only the status of a work order
    @PutMapping("/{id}/status")
    public ResponseEntity<WorkOrder> updateWorkOrderStatus(
            @PathVariable Long id,
            @RequestParam String status) { // New status from query parameter
        try {
            return workOrderService.updateWorkOrderStatus(id, status)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, e.getMessage());
        }
        // Consider adding specific exception handling if status transitions are invalid
    }


    // DELETE /api/workorders/{id} - Delete a work order
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWorkOrder(@PathVariable Long id) {
        try {
            boolean deleted = workOrderService.deleteWorkOrder(id);
            if (deleted) {
                return ResponseEntity.noContent().build(); // Return 204 No Content on success
            } else {
                return ResponseEntity.notFound().build(); // Return 404 if not found for tenant
            }
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, e.getMessage());
        }
    }

    // Consider adding a specific @ExceptionHandler for AccessDeniedException if not handled globally
    // @ExceptionHandler(AccessDeniedException.class)
    // @ResponseStatus(HttpStatus.FORBIDDEN)
    // public Map<String, String> handleAccessDenied(AccessDeniedException ex) {
    //     return Map.of("message", ex.getMessage());
    // }
}

