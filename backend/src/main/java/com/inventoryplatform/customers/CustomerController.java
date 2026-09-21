package com.inventoryplatform.customers;

import com.inventoryplatform.common.response.ApiResponse;
import com.inventoryplatform.customers.dto.CreateCustomerRequest;
import com.inventoryplatform.customers.dto.CustomerResponse;
import com.inventoryplatform.customers.dto.UpdateCustomerRequest;
import com.inventoryplatform.roles.Permission;
import com.inventoryplatform.roles.RequiresPermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @PostMapping
    @RequiresPermission(Permission.CUSTOMER_WRITE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CustomerResponse> create(@Valid @RequestBody CreateCustomerRequest request) {
        return ApiResponse.success(customerService.create(request));
    }

    @GetMapping
    @RequiresPermission(Permission.CUSTOMER_READ)
    public ApiResponse<Page<CustomerResponse>> list(Pageable pageable) {
        return ApiResponse.success(customerService.list(pageable));
    }

    @GetMapping("/{id}")
    @RequiresPermission(Permission.CUSTOMER_READ)
    public ApiResponse<CustomerResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(customerService.getById(id));
    }

    @PatchMapping("/{id}")
    @RequiresPermission(Permission.CUSTOMER_WRITE)
    public ApiResponse<CustomerResponse> update(@PathVariable Long id, @Valid @RequestBody UpdateCustomerRequest request) {
        return ApiResponse.success(customerService.update(id, request));
    }
}
