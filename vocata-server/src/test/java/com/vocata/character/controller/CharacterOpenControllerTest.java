package com.vocata.character.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vocata.character.dto.request.CharacterSearchRequest;
import com.vocata.character.dto.response.CharacterResponse;
import com.vocata.character.service.CharacterService;
import com.vocata.common.result.ApiResponse;
import com.vocata.common.result.PageResult;
import org.junit.jupiter.api.Test;
import org.postgresql.util.PGobject;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CharacterOpenControllerTest {

    @Test
    void publicCharacterListConvertsPostgresJsonObjectsToStrings() throws Exception {
        CharacterService characterService = mock(CharacterService.class);
        CharacterOpenController controller = new CharacterOpenController();
        ReflectionTestUtils.setField(controller, "characterService", characterService);

        PGobject tags = new PGobject();
        tags.setType("jsonb");
        tags.setValue("[\"动漫\",\"治愈\"]");

        Page<Map<String, Object>> page = new Page<>(1, 2, 1);
        page.setRecords(List.of(Map.ofEntries(
                Map.entry("id", 1L),
                Map.entry("character_code", "char_001"),
                Map.entry("name", "测试角色"),
                Map.entry("description", "简介"),
                Map.entry("greeting", "你好"),
                Map.entry("avatar_url", "/avatar.png"),
                Map.entry("tags", tags),
                Map.entry("language", "zh-CN"),
                Map.entry("status", 1),
                Map.entry("is_official", 1),
                Map.entry("is_featured", 0),
                Map.entry("is_trending", 0),
                Map.entry("trending_score", 10),
                Map.entry("chat_count", 42L),
                Map.entry("user_count", 3),
                Map.entry("is_private", false),
                Map.entry("create_id", 9L),
                Map.entry("created_at", LocalDateTime.of(2026, 5, 7, 8, 0)),
                Map.entry("updated_at", LocalDateTime.of(2026, 5, 7, 9, 0)),
                Map.entry("creator_name", "创建者")
        )));

        when(characterService.getPublicCharactersWithCreator(
                any(), eq(1), isNull(), isNull(), eq("chat_count"), eq("desc")))
                .thenReturn(page);

        CharacterSearchRequest request = new CharacterSearchRequest();
        request.setPageNum(1);
        request.setPageSize(2);

        ApiResponse<PageResult<CharacterResponse>> response = controller.getPublicCharacters(request);

        assertEquals(200, response.getCode());
        assertEquals("[\"动漫\",\"治愈\"]", response.getData().getList().get(0).getTags());
    }
}
