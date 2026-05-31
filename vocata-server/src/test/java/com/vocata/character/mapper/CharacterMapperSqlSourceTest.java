package com.vocata.character.mapper;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisXMLLanguageDriver;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.scripting.LanguageDriver;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 保护性测试：CharacterMapper 中带 &lt;script&gt; 的注解 SQL 必须是合法 XML。
 *
 * MyBatis 在 Bean 创建阶段会把 &lt;script&gt; 注解按 XML 解析，任何裸露的 '&'
 * （例如 PostgreSQL 数组重叠运算符 &amp;&amp;）都会触发 SAXParseException 导致应用启动失败。
 * 普通单元测试 mock 了 Service，不会解析真实 Mapper SQL，因此无法发现该问题。
 */
class CharacterMapperSqlSourceTest {

    @Test
    void selectPublicCharactersScriptIsValidXml() {
        Method method = Arrays.stream(CharacterMapper.class.getMethods())
                .filter(m -> m.getName().equals("selectPublicCharactersWithCreator"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("未找到 selectPublicCharactersWithCreator 方法"));

        Select select = method.getAnnotation(Select.class);
        assertNotNull(select, "selectPublicCharactersWithCreator 应带有 @Select 注解");
        String sql = select.value()[0];

        MybatisConfiguration configuration = new MybatisConfiguration();
        LanguageDriver driver = new MybatisXMLLanguageDriver();

        // 与生产一致的解析路径：MybatisXMLLanguageDriver.createSqlSource 会把 <script> 当作 XML 解析。
        // 若 SQL 含未转义的 '&'，这里会抛出 BuilderException(SAXParseException)。
        assertDoesNotThrow(
                () -> driver.createSqlSource(configuration, sql, Object.class),
                "CharacterMapper 的 <script> 注解 SQL 必须是合法 XML（'&' 需转义为 '&amp;'）");
    }
}
