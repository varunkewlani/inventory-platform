package com.inventoryplatform.inventory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * The four {@code @Modifying} methods below are the entire concurrency
 * story for this app (plan.md's "atomic conditional UPDATE" strategy): each
 * is a single {@code UPDATE ... WHERE <quantity column> >= :qty} statement.
 * MySQL takes an exclusive row lock for the duration of the UPDATE, so under
 * concurrent requests for the same row, the second one physically blocks
 * until the first commits, then re-evaluates the WHERE clause against the
 * now-current (already-decremented) value — there is no read-then-write gap
 * for a race to land in. A statement that matches 0 rows (insufficient
 * stock) is the caller's signal to fail cleanly; nothing here ever does a
 * SELECT-then-UPDATE in application code.
 *
 * These intentionally use default (REQUIRED) transaction propagation,
 * unlike RefreshTokenRepository's reuse-detection revoke — a transfer needs
 * its remove-from-source and add-to-destination to commit or roll back
 * together, so they must share the caller's transaction, not get their own.
 */
public interface InventoryRepository extends JpaRepository<Inventory, Long>, JpaSpecificationExecutor<Inventory> {

    Optional<Inventory> findByIdAndOrganizationId(Long id, Long organizationId);

    Optional<Inventory> findByOrganizationIdAndWarehouseIdAndProductId(Long organizationId, Long warehouseId, Long productId);

    // clearAutomatically: bulk UPDATEs bypass Hibernate's persistence
    // context, so without this, a findById() called later in the same
    // transaction would silently return the stale pre-update instance from
    // the first-level cache instead of re-querying. flushAutomatically:
    // covers the case where the row was just INSERTed (getOrCreateRow) in
    // this same transaction and hasn't hit the DB yet when this UPDATE runs.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("update Inventory i set i.availableQuantity = i.availableQuantity + :qty, i.version = i.version + 1 " +
            "where i.id = :id")
    int addAvailable(@Param("id") Long id, @Param("qty") int qty);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("update Inventory i set i.availableQuantity = i.availableQuantity - :qty, i.version = i.version + 1 " +
            "where i.id = :id and i.availableQuantity >= :qty")
    int removeAvailableIfSufficient(@Param("id") Long id, @Param("qty") int qty);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("update Inventory i set i.availableQuantity = i.availableQuantity - :qty, " +
            "i.reservedQuantity = i.reservedQuantity + :qty, i.version = i.version + 1 " +
            "where i.id = :id and i.availableQuantity >= :qty")
    int reserveIfAvailable(@Param("id") Long id, @Param("qty") int qty);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("update Inventory i set i.reservedQuantity = i.reservedQuantity - :qty, " +
            "i.availableQuantity = i.availableQuantity + :qty, i.version = i.version + 1 " +
            "where i.id = :id and i.reservedQuantity >= :qty")
    int releaseIfReserved(@Param("id") Long id, @Param("qty") int qty);

    // Order completion: reserved stock has physically left the warehouse, so
    // it's removed from reservedQuantity WITHOUT returning to
    // availableQuantity — distinct from release, which is for cancellation.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("update Inventory i set i.reservedQuantity = i.reservedQuantity - :qty, i.version = i.version + 1 " +
            "where i.id = :id and i.reservedQuantity >= :qty")
    int consumeReservedIfSufficient(@Param("id") Long id, @Param("qty") int qty);
}
