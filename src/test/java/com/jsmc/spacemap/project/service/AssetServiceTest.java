package com.jsmc.spacemap.project.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import com.jsmc.spacemap.project.config.StorageProperties;
import com.jsmc.spacemap.project.entity.Asset;
import com.jsmc.spacemap.project.entity.User;
import com.jsmc.spacemap.project.repository.AssetRepository;
import com.jsmc.spacemap.project.repository.FloorRepository;
import com.jsmc.spacemap.project.repository.MapProjectRepository;
import com.jsmc.spacemap.project.repository.PanoramaRepository;
import com.jsmc.spacemap.project.repository.UserRepository;
import com.jsmc.spacemap.project.service.exception.ForbiddenOperationException;
import com.jsmc.spacemap.project.service.exception.PayloadTooLargeException;
import com.jsmc.spacemap.project.service.exception.ResourceNotFoundException;
import com.jsmc.spacemap.project.service.exception.UnsupportedMediaTypeException;

@ExtendWith(MockitoExtension.class)
class AssetServiceTest {

    @Mock AssetRepository assetRepository;
    @Mock UserRepository userRepository;
    @Mock StorageService storageService;
    @Mock StorageProperties storageProperties;
    @Mock FloorRepository floorRepository;
    @Mock MapProjectRepository mapProjectRepository;
    @Mock PanoramaRepository panoramaRepository;

    @InjectMocks AssetService assetService;

    UUID userId;
    User user;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = new User();
        user.setId(userId);
        org.mockito.Mockito.lenient().when(storageProperties.getMaxFileSizeBytes()).thenReturn(1024L);
    }

    @Test
    void upload_success() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "plan.png", "image/png", new byte[] {1, 2, 3, 4});
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(assetRepository.save(any(Asset.class))).thenAnswer(inv -> {
            Asset a = inv.getArgument(0);
            a.setId(UUID.randomUUID());
            return a;
        });

        Asset asset = assetService.upload(userId, file, "floor-plans");

        assertEquals("image/png", asset.getContentType());
        assertEquals(4L, asset.getSizeBytes());
        verify(storageService).putObject(anyString(), eq(file), eq("image/png"));
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(storageService).putObject(keyCaptor.capture(), eq(file), eq("image/png"));
        org.junit.jupiter.api.Assertions.assertTrue(keyCaptor.getValue().contains("floor-plans"));
    }

    @Test
    void upload_tooLarge() {
        byte[] data = new byte[2048];
        MockMultipartFile file = new MockMultipartFile("file", "plan.png", "image/png", data);
        assertThrows(PayloadTooLargeException.class, () -> assetService.upload(userId, file, "floor-plans"));
        verify(storageService, never()).putObject(anyString(), any(), anyString());
    }

    @Test
    void upload_invalidMime() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "virus.exe", "application/octet-stream", new byte[] {1});
        assertThrows(UnsupportedMediaTypeException.class, () -> assetService.upload(userId, file, "floor-plans"));
    }

    @Test
    void upload_storageFailure() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "plan.png", "image/png", new byte[] {1, 2, 3});
        doThrow(new IllegalStateException("Не удалось загрузить файл в хранилище"))
                .when(storageService).putObject(anyString(), eq(file), anyString());

        assertThrows(IllegalStateException.class, () -> assetService.upload(userId, file, "floor-plans"));
        verify(assetRepository, never()).save(any());
    }

    @Test
    void upload_dbFailure_compensatesStorage() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "plan.png", "image/png", new byte[] {1, 2, 3});
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(assetRepository.save(any(Asset.class))).thenThrow(new RuntimeException("DB down"));

        assertThrows(RuntimeException.class, () -> assetService.upload(userId, file, "floor-plans"));
        verify(storageService).deleteObjectQuietly(anyString());
    }

    @Test
    void requireOwnedAsset_foreign_forbidden() {
        UUID assetId = UUID.randomUUID();
        User other = new User();
        other.setId(UUID.randomUUID());
        Asset asset = new Asset();
        asset.setId(assetId);
        asset.setOwner(other);
        when(assetRepository.findById(assetId)).thenReturn(Optional.of(asset));

        assertThrows(ForbiddenOperationException.class, () -> assetService.requireOwnedAsset(userId, assetId));
    }

    @Test
    void requireOwnedAsset_missing() {
        UUID assetId = UUID.randomUUID();
        when(assetRepository.findById(assetId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> assetService.requireOwnedAsset(userId, assetId));
    }
}
