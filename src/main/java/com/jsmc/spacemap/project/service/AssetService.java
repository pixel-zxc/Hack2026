package com.jsmc.spacemap.project.service;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.jsmc.spacemap.project.config.StorageProperties;
import com.jsmc.spacemap.project.dto.response.AssetResponse;
import com.jsmc.spacemap.project.entity.Asset;
import com.jsmc.spacemap.project.entity.AssetStatus;
import com.jsmc.spacemap.project.entity.User;
import com.jsmc.spacemap.project.mapper.AssetMapper;
import com.jsmc.spacemap.project.repository.AssetRepository;
import com.jsmc.spacemap.project.repository.FloorRepository;
import com.jsmc.spacemap.project.repository.MapProjectRepository;
import com.jsmc.spacemap.project.repository.PanoramaRepository;
import com.jsmc.spacemap.project.repository.UserRepository;
import com.jsmc.spacemap.project.service.exception.ConflictException;
import com.jsmc.spacemap.project.service.exception.ForbiddenOperationException;
import com.jsmc.spacemap.project.service.exception.PayloadTooLargeException;
import com.jsmc.spacemap.project.service.exception.ResourceNotFoundException;
import com.jsmc.spacemap.project.service.exception.UnsupportedMediaTypeException;

@Service
public class AssetService {
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/png",
            "image/jpeg",
            "image/webp",
            "image/svg+xml",
            "application/pdf"
    );
    private static final Map<String, String> EXTENSION_TO_CONTENT_TYPE = Map.of(
            "png", "image/png",
            "jpg", "image/jpeg",
            "jpeg", "image/jpeg",
            "webp", "image/webp",
            "svg", "image/svg+xml",
            "pdf", "application/pdf"
    );

    private final AssetRepository assetRepository;
    private final UserRepository userRepository;
    private final StorageService storageService;
    private final StorageProperties storageProperties;
    private final FloorRepository floorRepository;
    private final MapProjectRepository mapProjectRepository;
    private final PanoramaRepository panoramaRepository;

    public AssetService(
            AssetRepository assetRepository,
            UserRepository userRepository,
            StorageService storageService,
            StorageProperties storageProperties,
            FloorRepository floorRepository,
            MapProjectRepository mapProjectRepository,
            PanoramaRepository panoramaRepository) {
        this.assetRepository = assetRepository;
        this.userRepository = userRepository;
        this.storageService = storageService;
        this.storageProperties = storageProperties;
        this.floorRepository = floorRepository;
        this.mapProjectRepository = mapProjectRepository;
        this.panoramaRepository = panoramaRepository;
    }

    @Transactional
    public Asset upload(UUID userId, MultipartFile file, String purpose) {
        validateFile(file);
        String contentType = resolveContentType(file);
        String extension = extensionOf(file.getOriginalFilename());
        String key = buildStorageKey(userId, purpose, extension);

        storageService.putObject(key, file, contentType);

        try {
            User owner = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
            Asset asset = new Asset();
            asset.setOwner(owner);
            asset.setStorageKey(key);
            asset.setContentType(contentType);
            asset.setSizeBytes(file.getSize());
            asset.setStatus(AssetStatus.READY);
            return assetRepository.save(asset);
        } catch (RuntimeException e) {
            storageService.deleteObjectQuietly(key);
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public AssetResponse getResponse(UUID userId, UUID assetId) {
        Asset asset = requireOwnedAsset(userId, assetId);
        return AssetMapper.toResponse(asset, publicUrl(asset.getId()));
    }

    @Transactional(readOnly = true)
    public Asset requireOwnedAsset(UUID userId, UUID assetId) {
        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new ResourceNotFoundException("Файл не найден"));
        if (!asset.getOwner().getId().equals(userId)) {
            throw new ForbiddenOperationException("Нет доступа к файлу");
        }
        return asset;
    }

    @Transactional
    public void deleteIfUnused(UUID userId, UUID assetId) {
        Asset asset = requireOwnedAsset(userId, assetId);
        if (isInUse(assetId)) {
            throw new ConflictException("Файл используется и не может быть удалён");
        }
        String key = asset.getStorageKey();
        assetRepository.delete(asset);
        storageService.deleteObjectQuietly(key);
    }

    public boolean isInUse(UUID assetId) {
        return floorRepository.existsByPlanAssetId(assetId)
                || mapProjectRepository.existsByCoverAssetId(assetId)
                || panoramaRepository.existsByAssetId(assetId);
    }

    public String publicUrl(UUID assetId) {
        return "/api/assets/" + assetId + "/content";
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Файл не передан");
        }
        if (file.getSize() > storageProperties.getMaxFileSizeBytes()) {
            throw new PayloadTooLargeException(
                    "Размер файла превышает лимит " + storageProperties.getMaxFileSizeBytes() + " байт");
        }
        String extension = extensionOf(file.getOriginalFilename());
        if (extension == null || !EXTENSION_TO_CONTENT_TYPE.containsKey(extension)) {
            throw new UnsupportedMediaTypeException("Недопустимое расширение файла. Разрешены: png, jpg, jpeg, webp, svg, pdf");
        }
        String contentType = resolveContentType(file);
        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new UnsupportedMediaTypeException("Недопустимый тип файла: " + contentType);
        }
    }

    private String resolveContentType(MultipartFile file) {
        String fromName = EXTENSION_TO_CONTENT_TYPE.get(extensionOf(file.getOriginalFilename()));
        String declared = file.getContentType();
        if (declared != null && !declared.isBlank() && !"application/octet-stream".equalsIgnoreCase(declared)) {
            String normalized = declared.toLowerCase(Locale.ROOT).split(";")[0].trim();
            if (ALLOWED_CONTENT_TYPES.contains(normalized)) {
                if (fromName != null && !fromName.equals(normalized)
                        && !(fromName.equals("image/jpeg") && normalized.equals("image/jpg"))) {
                    // Prefer extension mapping when client Content-Type is inconsistent
                    return fromName;
                }
                return normalized;
            }
        }
        if (fromName != null) {
            return fromName;
        }
        throw new UnsupportedMediaTypeException("Не удалось определить тип файла");
    }

    private String extensionOf(String filename) {
        if (filename == null || !filename.contains(".")) {
            return null;
        }
        String ext = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT).trim();
        return ext.isEmpty() ? null : ext;
    }

    private String buildStorageKey(UUID userId, String purpose, String extension) {
        String safePurpose = purpose == null || purpose.isBlank() ? "files" : purpose.replaceAll("[^a-zA-Z0-9_-]", "");
        return "users/" + userId + "/" + safePurpose + "/" + UUID.randomUUID() + "." + extension;
    }
}
