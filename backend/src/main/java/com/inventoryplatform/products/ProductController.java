package com.inventoryplatform.products;

import com.inventoryplatform.common.response.ApiResponse;
import com.inventoryplatform.products.dto.CreateProductRequest;
import com.inventoryplatform.products.dto.ProductResponse;
import com.inventoryplatform.products.dto.UpdateProductRequest;
import com.inventoryplatform.roles.Permission;
import com.inventoryplatform.roles.RequiresPermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping
    @RequiresPermission(Permission.PRODUCT_WRITE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ProductResponse> create(@Valid @RequestBody CreateProductRequest request) {
        return ApiResponse.success(productService.create(request));
    }

    @GetMapping
    @RequiresPermission(Permission.PRODUCT_READ)
    public ApiResponse<Page<ProductResponse>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) ProductStatus status,
            Pageable pageable) {
        return ApiResponse.success(productService.search(search, status, pageable));
    }

    @GetMapping("/{id}")
    @RequiresPermission(Permission.PRODUCT_READ)
    public ApiResponse<ProductResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(productService.getById(id));
    }

    @PatchMapping("/{id}")
    @RequiresPermission(Permission.PRODUCT_WRITE)
    public ApiResponse<ProductResponse> update(@PathVariable Long id, @Valid @RequestBody UpdateProductRequest request) {
        return ApiResponse.success(productService.update(id, request));
    }
}
