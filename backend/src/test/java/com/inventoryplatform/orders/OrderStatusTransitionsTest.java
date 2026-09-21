package com.inventoryplatform.orders;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OrderStatusTransitionsTest {

    @Test
    void happyPathProgressesThroughEveryStage() {
        assertThat(OrderStatusTransitions.isAllowed(OrderStatus.PENDING, OrderStatus.CONFIRMED)).isTrue();
        assertThat(OrderStatusTransitions.isAllowed(OrderStatus.CONFIRMED, OrderStatus.PROCESSING)).isTrue();
        assertThat(OrderStatusTransitions.isAllowed(OrderStatus.PROCESSING, OrderStatus.COMPLETED)).isTrue();
    }

    @Test
    void cancellationIsAllowedFromAnyNonTerminalState() {
        assertThat(OrderStatusTransitions.isAllowed(OrderStatus.PENDING, OrderStatus.CANCELLED)).isTrue();
        assertThat(OrderStatusTransitions.isAllowed(OrderStatus.CONFIRMED, OrderStatus.CANCELLED)).isTrue();
        assertThat(OrderStatusTransitions.isAllowed(OrderStatus.PROCESSING, OrderStatus.CANCELLED)).isTrue();
    }

    @Test
    void terminalStatesAllowNoFurtherTransitions() {
        for (OrderStatus target : OrderStatus.values()) {
            assertThat(OrderStatusTransitions.isAllowed(OrderStatus.COMPLETED, target)).isFalse();
            assertThat(OrderStatusTransitions.isAllowed(OrderStatus.CANCELLED, target)).isFalse();
        }
    }

    @Test
    void statusCannotBeSkipped() {
        assertThat(OrderStatusTransitions.isAllowed(OrderStatus.PENDING, OrderStatus.PROCESSING)).isFalse();
        assertThat(OrderStatusTransitions.isAllowed(OrderStatus.PENDING, OrderStatus.COMPLETED)).isFalse();
        assertThat(OrderStatusTransitions.isAllowed(OrderStatus.CONFIRMED, OrderStatus.COMPLETED)).isFalse();
    }
}
