package com.vocata.character.controller;

import com.vocata.character.service.CharacterService;
import com.vocata.common.result.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CharacterAdminControllerTest {

    @Test
    void syncTagsUsesBatchSyncService() {
        CharacterService characterService = mock(CharacterService.class);
        CharacterAdminController controller = new CharacterAdminController();
        ReflectionTestUtils.setField(controller, "characterService", characterService);

        when(characterService.syncCharacterTagsBatch(List.of(1L, 2L))).thenReturn(2);

        ApiResponse<Void> response = controller.syncTags(List.of(1L, 2L));

        assertEquals(200, response.getCode());
        assertEquals("成功同步 2 个角色的标签筛选字段", response.getMessage());
    }
}
