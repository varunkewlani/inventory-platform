package com.inventoryplatform.products;

import com.inventoryplatform.products.dto.CreateProductRequest;
import com.inventoryplatform.products.dto.ProductResponse;
import com.inventoryplatform.products.dto.UpdateProductRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductService {

    ProductResponse create(CreateProductRequest request);

    ProductResponse update(Long id, UpdateProductRequest request);

    ProductResponse getById(Long id);

    Page<ProductResponse> search(String search, ProductStatus status, Pageable pageable);
}
