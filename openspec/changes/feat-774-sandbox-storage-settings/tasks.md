## 1. 核心限额注入

- [x] 1.1 `Attachments` 加 `AttachmentLimits(maxFileBytes, workspaceMaxBytes)` + `DEFAULT`（=现常量）；`importInto`/`withTaskFiles`/`roomFailure`/`copyOne` 增加 `limits` 参数（默认 DEFAULT，行为零变化）
- [x] 1.2 `FeatureFlags` 加 `attachment_max_mb`（档位 200/512/1024/2048，默认 200）与 `workspace_max_gb`（档位 4/8/16/32，默认 4）读取/写入 + 档位外回落默认
- [x] 1.3 接线三处调用方：`MikaApp.importAttachments`、`androidWechatImages` 的 room lambda、`PhoneTaskFiles.start`（前置 sum 检查与 room lambda）

## 2. 设置 UI 与文案

- [x] 2.1 `DuringWorkScreen` 加两行（单文件上限/工作区上限）+ 两个档位弹窗（照 `SandboxBudgetDialog` 模式），`SettingsScreen` 透传状态与回调
- [x] 2.2 `chat_attach_too_large` 加 `%2$s` 实际档位占位符；渲染点（ChatScreen TOO_LARGE 分支）格式化当前档位；全部语言同步该串
- [x] 2.3 新设置字符串（标题×2、档位文案）×12 语言，跑 i18n 检查

## 3. 测试与验证

- [x] 3.1 单测：FeatureLimits 档位解析（未设/档位内/档位外回落）；Attachments 注入小限额拒绝、DEFAULT 放行的 A/B；放宽工作区上限后 room 检查放行
- [x] 3.2 本地全量：`./gradlew :core:test :core:cliTest :app:testDebugUnitTest` + relayTest（Go）+ i18n 检查全绿
- [x] 3.3 真机走查：默认档位显示 → 调档回写 → 恢复默认；截屏取证入 evidence/

## 4. PR

- [x] 4.1 推分支、建 PR（标题「现状 → 改动」句式、关联 #774、首屏实测证据、如实写明测试覆盖）
- [ ] 4.2 等 CI 四组全绿
