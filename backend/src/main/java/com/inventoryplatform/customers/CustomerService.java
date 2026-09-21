package com.inventoryplatform.customers;

import com.inventoryplatform.customers.dto.CreateCustomerRequest;
import com.inventoryplatform.customers.dto.CustomerResponse;
import com.inventoryplatform.customers.dto.UpdateCustomerRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CustomerService {

    CustomerResponse create(CreateCustomerRequest request);

    CustomerResponse update(Long id, UpdateCustomerRequest request);

    CustomerResponse getById(Long id);

    Page<CustomerResponse> list(Pageable pageable);
}
