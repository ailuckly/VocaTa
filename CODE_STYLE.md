# Code Style

这份文档定义 VocaTa 的基础代码风格。具体模块实现仍应优先匹配现有代码，再逐步收敛到这里的规范。

## 命名规范

### 通用命名

- 变量、函数、方法使用清晰的业务含义，避免 `data`、`info`、`temp` 这类模糊命名。
- 常量使用能够表达约束或业务语义的名称。
- 不使用拼音缩写或只有当前作者知道的缩写。

### 后端 Java

- 类名使用 `PascalCase`。
- 方法、字段、局部变量使用 `camelCase`。
- DTO 命名表达用途，例如 `CreateCharacterRequest`、`CharacterDetailResponse`。
- Controller 方法名表达动作和资源，例如 `listCharacters`、`createConversation`。
- API 返回给前端的长整型 ID 优先序列化为 `String`，避免 JavaScript 精度问题。

### 前端 Vue / TypeScript

- Vue 组件文件使用 `PascalCase.vue`。
- 组合式函数使用 `useXxx.ts`。
- Pinia store 按业务模块命名。
- API 模块按资源命名，例如 `user.ts`、`role.ts`、`conversation.ts`。
- TypeScript 类型优先使用表达业务含义的 interface/type 名称。

## 目录规范

### 后端

后端按业务模块组织，常见结构：

```text
com.vocata.{module}/
├── controller/
├── service/
│   └── impl/
├── mapper/
├── entity/
├── dto/
└── constants/
```

通用能力放入 `common` 或 `config`，不要把业务逻辑塞进工具类。

### 用户端和管理端

前端按职责拆分：

```text
src/
├── api/
├── assets/
├── components/
├── composables/
├── layouts/
├── router/
├── store/
├── types/
├── utils/
└── views/
```

页面级组件放在 `views`，可复用组件放在 `components`，纯函数工具放在 `utils`，可组合的状态/副作用逻辑放在 `composables`。

## 函数 / 组件 / 模块拆分原则

- 一个函数只表达一个明确动作。
- 一个组件优先只负责一个交互场景或展示单元。
- 页面组件可以编排数据和布局，但不要承载大量底层协议、复杂计算或重复 API 细节。
- 业务流程过长时，优先提取为 service、composable 或明确命名的私有方法。
- 避免为了“看起来抽象”提前创建复杂层级。

## 错误处理原则

### 后端

- 业务错误使用 `BizException` 和 `ApiCode`。
- Controller 返回统一 `ApiResponse<T>`。
- 不向前端暴露底层异常堆栈或第三方服务敏感响应。
- 数据写入、外部 API 调用、WebSocket 流程要考虑失败路径和清理逻辑。

### 前端

- API 错误应给出用户可理解的提示。
- 鉴权失败统一清理 token 并跳转登录。
- 组件内不要吞掉关键错误；至少保留可调试的错误上下文。
- 网络请求、录音、WebSocket、音频播放等异步流程要处理取消、重试或关闭状态。

## 日志原则

- 后端使用 SLF4J logger，不使用 `System.out.println`。
- 日志要包含定位问题所需上下文，但不要输出密码、token、密钥、完整授权头。
- 高频路径使用 debug 级别，业务关键状态变化使用 info，异常路径使用 warn 或 error。
- 前端开发期可以使用 `console` 辅助调试，提交前应移除无意义或包含敏感信息的日志。

## 测试原则

- 优先测试纯函数、协议序列化、权限边界、核心业务分支和错误路径。
- 不为了覆盖率写无意义测试。
- 新增 bugfix 应尽量先补能复现问题的测试。
- 前端组件测试优先覆盖用户可见行为，而不是内部实现细节。
- 后端测试优先覆盖 service 业务规则、Controller 契约、鉴权边界和数据转换。

## 注释原则

- 注释解释“为什么”，不要重复“代码正在做什么”。
- 复杂业务规则、第三方协议约束、兼容性补丁应写注释。
- 临时方案必须标注原因和后续处理条件。
- 不保留过期注释、大段废弃代码或无意义 TODO。

## 依赖引入原则

- 优先使用项目已有依赖和标准库。
- 新增依赖前必须说明为什么需要、替代方案是什么、对复杂度的影响。
- 不为少量简单逻辑引入重型依赖。
- 后端不引入 Lombok，保持现有手写 getter/setter 风格。
- 前端不随意替换现有 ESLint、Prettier、Vite、Vitest 工具链。

