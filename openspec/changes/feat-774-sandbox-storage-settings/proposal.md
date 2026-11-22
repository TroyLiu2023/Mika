## Why

issue #774 报了三处写死的沙箱限额：预算时间（PR #780 已做成可调档位）、工作区空间（4 GB）、
单个上传文件体积（200 MiB）。后两个仍写死在 `Attachments` 常量里：聊天附件 / 微信图片 /
电脑发任务带文件都会撞上 200 MiB 单文件与 4 GB 工作区上限，用户没有任何放宽途径，只能眼睁睁
看着「上传失败：超过 200 MiB」。照已合入的沙箱时长档位同一模式，把这两个存储限额也开放成
设置里的档位单选。

## What Changes

- 新增 FeatureFlags 键与读取：`attachment_max_mb`（档位 200/512/1024/2048，默认 200，存量行为零变化）、
  `workspace_max_gb`（档位 4/8/16/32，默认 4）。
- `Attachments` 的单文件上限与工作区上限从对象常量改为可注入参数（默认值 = 现常量，现有调用方与
  测试不传参时行为不变）；聊天附件（MikaApp）、微信图片（WechatImageFiles）、电脑发任务带文件
  （PhoneTaskFiles）三条路统一从 FeatureFlags 取限额。
- 「任务运行」设置页新增两行档位单选（照 SandboxBudgetDialog 模式）：单文件上限、工作区上限。
- 聊天里「超过 200 MiB 限制」的失败文案改为带实际上限值（`chat_attach_too_large` 加占位符并同步
  全部翻译）。
- 设备剩余空间下限 `MIN_FREE`（500 MiB）是设备安全线，不开放。

## Capabilities

### New Capabilities

- `sandbox-storage-limits`: 单个上传文件体积上限与工作区空间上限可由用户在设置里按固定档位调整，
  默认与既有 200 MiB / 4 GB 一致；三条导入路径（聊天附件、微信图片、电脑发任务）统一执行同一限额。

### Modified Capabilities

（无——`sandbox-time-budget` 是上一片已合入的能力，本次不动它的需求。）

## Impact

- `app/src/main/java/com/wangzan101/mika/cap/sandbox/Attachments.kt`：常量 → 注入参数。
- `app/src/main/java/com/wangzan101/mika/settings/FeatureFlags.kt`：两个新键 + 档位校验。
- `app/src/main/java/com/wangzan101/mika/MikaApp.kt`、`wechat/WechatImageFiles.kt`、
  `task/PhoneTaskFiles.kt`：调用点接 FeatureFlags。
- `app/src/main/java/com/wangzan101/mika/ui/SettingsSubPages.kt`、`SettingsScreen.kt`：设置行 + 档位弹窗。
- `app/src/main/res/values*/strings_chat.xml`：`chat_attach_too_large` 加 `%2$s` 占位符，全部语言同步。
- `app/src/main/res/values*/strings_settings.xml`：新字符串 ×12 语言。
- 单测：FeatureFlags 档位解析、Attachments 限额注入（小限额拒绝/放宽后放行）、文案格式化。
