package com.jsmc.spacemap.project.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record TransferOwnershipRequest(
        @NotNull UUID newOwnerUserId
) {
}
