---
title: Database Schema Design
description: "VocaTa PostgreSQL 数据库设计规范、真实表清单与 MyBatis 映射约定。"
inclusion: always
---

# VocaTa 数据库设计规范

> 数据库相关规则以本文件为权威源。通用协作规则见 `AGENTS.md`，目录结构见 `.ai-rules/structure.md`，后端编码见 `.ai-rules/backend.md`。

## 设计规范

### 命名
- 表名以 `vocata_` 开头；关联表以 `_relation` 结尾。
- 字段名小写下划线（`create_id`、`tag_ids`）。

### 主键与 ID
- 主键 `id` 类型 **`BIGINT`**，由应用层雪花算法生成（`@TableId(type = IdType.ASSIGN_ID)`），**不是 `BIGSERIAL` 自增**。
- ID 返回前端一律序列化为 **String**（防 JS 精度丢失）。

### 字段类型
- **禁用 ENUM**，枚举值用 `SMALLINT`（如 `status`、`is_delete`）。
- 时间字段用 **`TIMESTAMP WITH TIME ZONE`**。
- JSON 用 **`JSONB`**；数组用 PG 原生数组（如 `tag_ids BIGINT[]`、`tag_names TEXT[]`），实体侧映射为 `Long[]` / `String[]`，Mapper 上加 `@TableName(..., autoResultMap = true)` 并配合 TypeHandler。

### 关系
- **禁用物理外键**，多对多/一对多通过独立关联表实现。

### 索引
- 非必要不建索引，按实际查询后期手动添加。

### 审计字段（⚠️ 并非全表统一）
- 多数表有 `create_id` / `update_id` / `create_date` / `update_date` / `is_delete`，逻辑删除用 `@TableLogic`（0=活, 1=删）。
- **但字段名/有无不一致**：例如 `vocata_character` 的时间列是 **`created_at` / `updated_at`**（非 `create_date`/`update_date`），且**没有 `update_id`**。改表/写映射前**先看实体类的真实 `@TableField`**，不要假设统一。这与「不要假设所有实体继承 BaseEntity」是同一回事。

## 真实表清单（已核实 @TableName）

| 表 | 实体 | 说明 |
|---|---|---|
| `vocata_user` | User | 用户（继承 BaseEntity） |
| `vocata_character` | Character | 角色（**不**继承 BaseEntity，时间列 created_at/updated_at） |
| `vocata_character_tag` | CharacterTag | 角色-标签 |
| `vocata_tag` | Tag | 标签 |
| `vocata_tag_stats` | TagStats | 标签统计 |
| `vocata_user_favorite` | UserFavorite | 收藏（不继承 BaseEntity） |
| `vocata_conversations` | Conversation | 会话（继承 BaseEntity） |
| `vocata_messages` | Message | 消息（继承 BaseEntity，`autoResultMap=true`） |
| `vocata_tts_voices` | TtsVoice | TTS 音色 |
| `vocata_voice_profile` | VoiceProfile | 音色配置 |
| `vocata_login_log` | LoginLog | 登录日志 |

> 表名单复数不统一（`vocata_user` 单数、`vocata_messages` 复数）——这是历史现状，新表沿用就近表的风格，不要为统一而批量改名。

## MyBatis 映射约定

- 用 **MyBatis Plus + 注解**（`@Select` 等），**无 XML 映射文件**，`resources/` 下无 mapper XML。
- 简单查询用内置方法 + `LambdaQueryWrapper`；动态 SQL 用 `@Select("<script>...")` + `<if>`/`<foreach>`。

### ⚠️ 注解 SQL 即 XML —— `&` `<` `>` 必须转义（曾致启动崩溃）

`<script>` 内容在 Bean 创建阶段按 XML 解析。裸写 `&`/`<`/`>` 触发 `SAXParseException`，应用**启动即崩**：

| 原符号 | 用途 | 必须写成 |
|---|---|---|
| `&&` | PG 数组重叠运算符 | `&amp;&amp;` |
| `&` | 其它 | `&amp;` |
| `<` | 小于 | `&lt;` 或 `<![CDATA[ ]]>` |
| `>` | 大于 | `&gt;`（建议统一转） |

```java
// 标签过滤：数组重叠 && 必须转义为 &amp;&amp;
@Select("<script> SELECT * FROM vocata_character c WHERE 1=1 " +
        "<if test='tags != null and tags.size() > 0'>" +
        " AND c.tag_names &amp;&amp; ARRAY[<foreach collection='tags' item='t' separator=','>#{t}</foreach>]::text[]" +
        "</if> </script>")
```

XML 解析后还原为 `&&`，PG 行为不变。**写完带 `<script>` 的 Mapper 必须本地启动或跑解析测试**（见 `CharacterMapperSqlSourceTest`）——`mvn test` 的单元测试 mock 了 Mapper，不会暴露此类问题。这是 P0 级回归点。

## 建表方式

项目**无 SQL 迁移脚本**（无 Flyway/Liquibase、无 `db/migration/`、无 `.sql` 文件）。表结构当前由人工/云端维护。新增表时：先在数据库建表，再建对应 `@TableName` 实体，保持字段与实体 `@TableField` 一致；如引入迁移工具需团队先达成一致（属 ask-first）。
