package com.vocata.file.config;

import com.qiniu.storage.UploadManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class QiniuConfigTest {

    @Test
    void uploadManagerFallsBackToAutoRegionWhenRegionIsMissing() {
        QiniuConfig config = new QiniuConfig();

        UploadManager uploadManager = assertDoesNotThrow(config::uploadManager);

        assertNotNull(uploadManager);
    }
}
