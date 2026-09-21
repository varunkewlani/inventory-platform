package com.inventoryplatform.customers;

import com.inventoryplatform.common.exception.NotFoundException;
import com.inventoryplatform.common.tenant.TenantContext;
import com.inventoryplatform.customers.dto.CreateCustomerRequest;
import com.inventoryplatform.customers.dto.CustomerResponse;
import com.inventoryplatform.customers.dto.UpdateCustomerRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;

    @Override
    @Transactional
    public CustomerResponse create(CreateCustomerRequest request) {
        Customer customer = Customer.builder()
                .organizationId(TenantContext.getOrganizationId())
                .name(request.name())
                .email(request.email())
                .phone(request.phone())
                .build();

        return CustomerResponse.from(customerRepository.save(customer));
    }

    @Override
    @Transactional
    public CustomerResponse update(Long id, UpdateCustomerRequest request) {
        Customer customer = findTenantScoped(id);

        if (request.name() != null) {
            customer.setName(request.name());
        }
        if (request.email() != null) {
            customer.setEmail(request.email());
        }
        if (request.phone() != null) {
            customer.setPhone(request.phone());
        }

        return CustomerResponse.from(customerRepository.save(customer));
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerResponse getById(Long id) {
        return CustomerResponse.from(findTenantScoped(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CustomerResponse> list(Pageable pageable) {
        return customerRepository.findAllByOrganizationId(TenantContext.getOrganizationId(), pageable)
                .map(CustomerResponse::from);
    }

    private Customer findTenantScoped(Long id) {
        return customerRepository.findByIdAndOrganizationId(id, TenantContext.getOrganizationId())
                .orElseThrow(() -> new NotFoundException("Customer not found"));
    }
}
