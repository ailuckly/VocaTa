package com.vocata;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Context 加载冒烟测试。
 *
 * 用完整的 Spring context 启动一次，验证所有 Bean 能装配、所有 Mapper 的注解 SQL
 * 能被解析。这能在 CI 阶段捕获“启动即崩”类问题——例如 Mapper 注解里的 `<script>`
 * SQL 含未转义的 `&` 导致 SAXParseException（曾在 staging 启动时崩溃）。
 *
 * 依赖 CI 提供的 PostgreSQL + Redis service 容器（见 ci.yml backend-ci 的 services）。
 * 使用 `ci` profile：关闭启动期查库副作用（缓存预热），因为 CI 数据库是空库
 * （项目无 SQL 迁移）。本测试只验证 context 能否加载，不验证业务数据。
 *
 * 与普通单元测试（mock 掉 Mapper/DB）不同，本测试真正加载完整 context，
 * 因此是 `mvn test` 中唯一能发现 Bean 装配 / Mapper SQL 解析问题的测试。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("ci")
class ContextLoadSmokeTest {

    /**
     * context 能成功加载本身即为断言：若任一 Bean 装配失败或任一 Mapper 注解 SQL
     * 无法解析，@SpringBootTest 会在 context 初始化阶段抛异常使本测试失败。
     */
    @Test
    void contextLoads() {
    }
}
