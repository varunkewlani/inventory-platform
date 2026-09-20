package com.inventoryplatform.common.web;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.config.PageableHandlerMethodArgumentResolverCustomizer;
import org.springframework.data.web.config.SortHandlerMethodArgumentResolverCustomizer;

/**
 * Makes every {@code Pageable} controller parameter match the spec's exact
 * query contract: {@code ?page=1&limit=20&sort=createdAt:desc} — 1-indexed
 * pages, {@code limit} instead of Spring's default {@code size}, and a
 * colon instead of a comma between sort field and direction.
 */
@Configuration
public class PaginationConfig {

    @Bean
    public PageableHandlerMethodArgumentResolverCustomizer pageableCustomizer() {
        return resolver -> {
            resolver.setOneIndexedParameters(true);
            resolver.setSizeParameterName("limit");
            resolver.setMaxPageSize(100);
            resolver.setFallbackPageable(PageRequest.of(0, 20));
        };
    }

    @Bean
    public SortHandlerMethodArgumentResolverCustomizer sortCustomizer() {
        return resolver -> resolver.setPropertyDelimiter(":");
    }
}
