package com.jsmc.spacemap.project.mapper;

import com.jsmc.spacemap.project.dto.response.OrganizationResponse;
import com.jsmc.spacemap.project.entity.Organization;

public final class OrganizationMapper {
    private OrganizationMapper() { }

    public static OrganizationResponse toResponse(Organization organization) {
        if (organization == null) return null;
        return new OrganizationResponse(organization.getId(), organization.getName(), organization.getOwner().getId(), organization.getCreatedAt(), organization.getUpdatedAt());
    }
}
