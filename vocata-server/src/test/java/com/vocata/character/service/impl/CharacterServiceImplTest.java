package com.vocata.character.service.impl;

import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vocata.character.dto.response.CharacterAiGenerateResponse;
import com.vocata.character.entity.Character;
import com.vocata.character.mapper.CharacterMapper;
import com.vocata.common.utils.UserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CharacterServiceImplTest {

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void getPublicCharactersWithCreatorFiltersBlankTagsAndDefaultsOrdering() {
        CharacterMapper mapper = mock(CharacterMapper.class);
        CharacterServiceImpl service = new CharacterServiceImpl();
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
        ReflectionTestUtils.setField(service, "objectMapper", new ObjectMapper());

        Page<Map<String, Object>> pageResult = new Page<>(1, 15, 0);
        when(mapper.selectPublicCharactersWithCreator(any(), eq(1), eq(1), eq(List.of("动漫", "科幻")), eq("chat_count"), eq("desc")))
                .thenReturn(pageResult);

        IPage<Map<String, Object>> result = service.getPublicCharactersWithCreator(
                new Page<>(1, 15),
                1,
                1,
                List.of("  ", "动漫", "", "科幻"),
                null,
                null
        );

        assertEquals(pageResult, result);
    }

    @Test
    void getPublicCharactersAddsExactTagFilter() {
        CharacterMapper mapper = mock(CharacterMapper.class);
        CharacterServiceImpl service = spy(new CharacterServiceImpl());
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
        ReflectionTestUtils.setField(service, "objectMapper", new ObjectMapper());
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "test"), Character.class);

        Page<Character> pageResult = new Page<>(1, 15, 0);
        when(mapper.selectPage(any(Page.class), argThat(wrapper -> {
            if (!(wrapper instanceof AbstractWrapper<?, ?, ?> abstractWrapper)) {
                return false;
            }

            String sqlSegment = abstractWrapper.getExpression().getSqlSegment();
            return sqlSegment.contains("tag_names")
                    && sqlSegment.contains("ARRAY[")
                    && sqlSegment.contains("chat_count");
        }))).thenReturn(pageResult);

        IPage<Character> result = service.getPublicCharacters(
                new Page<>(1, 15),
                1,
                null,
                List.of("", "动漫", "科幻"),
                null,
                null
        );

        assertEquals(pageResult, result);
    }

    @Test
    void searchCharactersWithoutKeywordFallsBackToPublicCharactersWithTags() {
        CharacterServiceImpl service = mock(CharacterServiceImpl.class);
        Page<Character> page = new Page<>(1, 15);
        Page<Character> expected = new Page<>(1, 15, 0);

        when(service.searchCharacters(page, " ", 1, List.of("治愈"))).thenCallRealMethod();
        when(service.getPublicCharacters(page, 1, null, List.of("治愈"), "chat_count", "desc"))
                .thenReturn(expected);

        IPage<Character> result = service.searchCharacters(page, " ", 1, List.of("治愈"));

        assertEquals(expected, result);
        verify(service).getPublicCharacters(page, 1, null, List.of("治愈"), "chat_count", "desc");
    }

    @Test
    void createSyncsTagFieldsAfterSave() {
        CharacterServiceImpl service = spy(new CharacterServiceImpl());
        ReflectionTestUtils.setField(service, "objectMapper", new ObjectMapper());
        UserContext.set(new UserContext.UserContextDTO(99L, "tester", false, null));

        Character character = new Character();
        character.setId(123L);
        character.setTags("[\"动漫\",\"治愈\"]");

        doReturn(true).when(service).save(character);
        doReturn(true).when(service).syncCharacterTags(123L);

        Character result = service.create(character);

        assertEquals(character, result);
        verify(service).syncCharacterTags(123L);
    }

    @Test
    void updateSyncsTagFieldsAfterPersistingTags() {
        CharacterServiceImpl service = spy(new CharacterServiceImpl());
        ReflectionTestUtils.setField(service, "objectMapper", new ObjectMapper());
        UserContext.set(new UserContext.UserContextDTO(99L, "tester", false, null));

        Character existing = new Character();
        existing.setId(123L);
        existing.setCharacterCode("char_001");
        existing.setCreateId(99L);

        Character incoming = new Character();
        incoming.setId(123L);
        incoming.setTags("[\"科幻\"]");

        Character updated = new Character();
        updated.setId(123L);
        updated.setTags("[\"科幻\"]");

        doReturn(existing).doReturn(updated).when(service).getById(123L);
        doReturn(true).when(service).hasPermission(eq(123L), nullable(Long.class));
        doReturn(true).when(service).updateById(incoming);
        doReturn(true).when(service).syncCharacterTags(123L);

        Character result = service.update(incoming);

        assertEquals(updated, result);
        verify(service).syncCharacterTags(123L);
    }

    @Test
    void updateAiGeneratedFieldsSyncsTagFieldsWhenTagsAreGenerated() {
        CharacterServiceImpl service = spy(new CharacterServiceImpl());
        ReflectionTestUtils.setField(service, "objectMapper", new ObjectMapper());
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "test"), Character.class);

        CharacterAiGenerateResponse aiResponse = new CharacterAiGenerateResponse();
        aiResponse.setTags(List.of("治愈", "陪伴"));

        doReturn(true).when(service).update(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class));
        doReturn(true).when(service).syncCharacterTags(123L);

        boolean result = service.updateAiGeneratedFields(123L, aiResponse);

        assertEquals(true, result);
        verify(service).syncCharacterTags(123L);
    }

    @Test
    void syncCharacterTagsUsesRawRecordInsteadOfChatCountDecoratedGetById() {
        CharacterMapper mapper = mock(CharacterMapper.class);
        CharacterServiceImpl service = new CharacterServiceImpl();
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
        ReflectionTestUtils.setField(service, "objectMapper", new ObjectMapper());

        Character character = new Character();
        character.setId(123L);
        character.setTags("[\"动漫\",\"治愈\"]");

        when(mapper.selectById(123L)).thenReturn(character);
        when(mapper.updateCharacterTags(eq(123L), any(String.class), any(String.class), any(String.class), eq("动漫、治愈")))
                .thenReturn(1);

        assertTrue(service.syncCharacterTags(123L));
    }
}
