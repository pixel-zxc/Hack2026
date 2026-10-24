package com.jsmc.spacemap.project.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jsmc.spacemap.project.entity.Asset;
import com.jsmc.spacemap.project.entity.AssetStatus;

public interface AssetRepository extends JpaRepository<Asset, UUID> {
    List<Asset> findByOwnerIdOrderByCreatedAtDesc(UUID ownerId);
    List<Asset> findByOwnerIdAndStatusOrderByCreatedAtDesc(UUID ownerId, AssetStatus status);
    Optional<Asset> findByStorageKey(String storageKey);
    boolean existsByStorageKey(String storageKey);
}
