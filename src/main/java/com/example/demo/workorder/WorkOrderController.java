package com.example.demo.workorder;

import com.example.demo.property.Property;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/workorders")
public class WorkOrderController {

    @Autowired
    private WorkOrderService workOrderService;

    // We need a simple constructor for the @ControllerAdvice in the test
    public WorkOrderController(WorkOrderService workOrderService) {
        this.workOrderService = workOrderService;
    }

    @GetMapping
    public ResponseEntity<List<WorkOrder>> getAllWorkOrders() {
        List<WorkOrder> workOrders = workOrderService.getAllWorkOrders();
        return new ResponseEntity<>(workOrders, HttpStatus.OK);
    }

    /**
     * Create a new work order.
     * This endpoint is a bit different. It takes the JSON body for the WorkOrder
     * but also takes required URL parameters for the relationships.
     *
     * e.g., POST /api/workorders?propertyId=1&vendorId=2
     */
    @PostMapping
    public ResponseEntity<WorkOrder> createWorkOrder(
            @Valid @RequestBody WorkOrder workOrder,
            @RequestParam Long propertyId,
            @RequestParam(required = false) Long vendorId) {

        WorkOrder createdWorkOrder = workOrderService.createWorkOrder(workOrder, propertyId, vendorId);
        return new ResponseEntity<>(createdWorkOrder, HttpStatus.CREATED);
    }

    // --- Validation Error Handler ---
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

    // --- General Error Handler (like for "Property not found") ---
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(RuntimeException.class)
    public Map<String, String> handleRuntimeExceptions(RuntimeException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("error", ex.getMessage());
        return error;
    }
}
