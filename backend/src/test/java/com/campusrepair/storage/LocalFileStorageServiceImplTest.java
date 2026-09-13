package com.campusrepair.storage;

import com.campusrepair.common.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalFileStorageServiceImplTest {
    @TempDir Path tempDir;

    @Test
    void saveLoadAndDeleteUseOrderScopedPath() throws Exception {
        LocalFileStorageServiceImpl storage = new LocalFileStorageServiceImpl(tempDir.toString());
        byte[] content = new byte[]{1, 2, 3};

        String url = storage.save(42L, "photo.png", content);

        assertTrue(url.startsWith("/uploads/repair/42/"));
        assertArrayEquals(content, storage.load(url));
        storage.delete(url);
        assertThrows(BusinessException.class, () -> storage.load(url));
    }

    @Test
    void rejectsTraversalAndUnsupportedFiles() {
        LocalFileStorageServiceImpl storage = new LocalFileStorageServiceImpl(tempDir.toString());

        assertThrows(BusinessException.class, () -> storage.save(42L, "../secret.png", new byte[]{1}));
        assertThrows(BusinessException.class, () -> storage.save(42L, "document.pdf", new byte[]{1}));
        assertThrows(BusinessException.class, () -> storage.save(42L, "empty.png", new byte[0]));
    }
}
