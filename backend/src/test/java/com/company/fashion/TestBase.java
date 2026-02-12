package com.company.fashion;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * 测试基类
 * - 使用 H2 内存数据库（通过 test profile）
 * - 自动配置 MockMvc
 * - 每个测试方法后回滚数据
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public abstract class TestBase {
    // 测试基类，所有集成测试应继承此类
}
