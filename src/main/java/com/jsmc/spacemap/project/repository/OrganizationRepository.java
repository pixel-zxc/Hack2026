package com.jsmc.spacemap.project.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jsmc.spacemap.project.entity.Organization;

public interface OrganizationRepository extends JpaRepository<Organization, UUID> {
    List<Organization> findByOwnerIdOrderByCreatedAtDesc(UUID ownerId);
    List<Organization> findByNameContainingIgnoreCase(String name);
}
