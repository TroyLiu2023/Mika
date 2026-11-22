# design：沙箱存储限额可调（#774 后续）

## Context

PR #780 已把沙箱命令累计时长做成「任务运行」页的档位单选，模式是：
FeatureFlags 新键（只认档位内的值，档位外回落默认）→ 调用点读 flags 透传 →
设置页档位弹窗（`SandboxBudgetDialog` 模式）→ 新字符串 ×12 语言。
本次对 #774 剩下两个写死限额做同构改造：

- 单文件 200 MiB：`Attachments.MAX_BYTES`，在 `copyOne` 里逐块累计时检查；
- 工作区 4 GB：`Attachments.WORKSPACE_MAX`，在 `roomFailure`（聊天附件）以及
  `WechatImageFiles`/`PhoneTaskFiles` 自带的 room 检查里比对。

三条导入路径都汇到 `Attachments.importInto` / `Attachments.withTaskFiles`。

## Goals / Non-Goals

**Goals**
- 两个限额按档位可调，默认行为与现状逐字节一致（不设置 = 200 MiB / 4 GB）。
- 三条导入路径（聊天附件、微信图片、电脑发任务带文件）执行同一档位。
- 超限提示带实际档位值，不再硬编码 200 MiB。

**Non-Goals**
- 不动设备剩余空间安全线 `MIN_FREE`（500 MiB，设备安全）。
- 不动浏览器上传暂存 `UploadStaging`（web 自动化域，另一回事）、分享上限
  `ShareRules.MAX_BYTES` / `SHARE_MAX_BYTES`（沙箱→用户导出，防呆用途）、
  `AttachSources.MAX_BYTES`（UI 解码缓存，非用户契约）。
- 不做自定义数值输入，只做档位（与任务步数/时长/token 预算的档位惯例一致）。

## Decisions

1. **限额注入用小值对象，不用散参。**
   `Attachments` 里加 `data class AttachmentLimits(val maxFileBytes: Long, val workspaceMaxBytes: Long)`
   与伴生 `DEFAULT`（= 现常量）。`importInto`/`withTaskFiles`/`roomFailure` 加
   `limits: AttachmentLimits = AttachmentLimits.DEFAULT` 参数。
   备选（每个方法散两个 Long 参数）在 `withTaskFiles` 链路上要连穿三层，读着累；
   备选（把 Attachments 变成带状态的类）牵动面太大。常量本体保留——
   `MIN_FREE`、测试与 WechatImageFiles/PhoneTaskFiles 的现有引用继续用。

2. **档位取值：单文件 200/512/1024/2048 MiB，工作区 4/8/16/32 GB。**
   与时长档位一样「默认在第一档、向上翻倍」。32 GB 封顶：rootfs 与全部工作区共用
   手机用户分区，再大容易把系统盘吃穿；单文件 2 GiB 封顶：再大应引导用户走
   电脑连接/文件管理器，聊天附件通道不适合扛数 GB。
   prefs 存 MB 数（`attachment_max_mb`）与 GB 数（`workspace_max_gb`），
   读取时 `takeIf { it in 档位 } ?: 默认`——与 `sandboxBudgetMinutes` 同一防御。

3. **设置入口放「任务运行」页（DuringWorkScreen），紧挨沙箱时长档位。**
   #774 用户视角是「沙箱限额」一组三件（时间/空间/单文件），时长已在
   DuringWorkScreen；挪走反而拆散。UI 照抄 `SandboxBudgetDialog`（单选档位、
   无「不限制」）。显示值复用 `SandboxCopy.size` 已有的容量格式化。

4. **超限文案：`chat_attach_too_large` 加 `%2$s`（实际档位值），渲染点取
   当前档位格式化。** 渲染点（ChatScreen 的 AttachmentFailureReason.TOO_LARGE
   分支）已有 context，读一次 FeatureFlags 格式化即可；不在
   `AttachmentFailure` 里带字节数（原因枚举被 ChatViewModel 映射，扩模型
   牵动大）。档位读取是 SharedPreferences 一次 getInt，渲染频次低，无性能问题。

5. **接线点：三个调用方各自读一次 FeatureFlags。**
   `MikaApp.importAttachments`、`androidWechatImages` 的 room lambda、
   `PhoneTaskFiles.start`（含前置 sum 检查与 room lambda）。三处都能拿到
   context，不新增跨层传参。改设置即刻生效（下一次导入就读新值），
   与「下一个任务起生效」的时长档位语义不同但更自然——存储限额没有
   「进行中任务」的一致性问题，导入是逐文件瞬时的。

## Risks / Trade-offs

- [档位调大后把用户分区塞满] → `MIN_FREE` 500 MiB 安全线在 copyOne 每 16 MiB
  复查一次，不受档位影响，保持兜底。
- [改 `chat_attach_too_large` 加占位符漏改某语言] → `tools/i18n` 的 parity
  检查会红，CI 挡住。
- [WechatImageFiles/PhoneTaskFiles 的 room 检查漏接新档位] → 单测各覆盖一条
  「放宽后放行」路径。

## Migration Plan

纯新增 prefs 键，默认值 = 现行为，无需迁移；回滚 = 删设置行（prefs 里残留键无人读）。

## Open Questions

无——档位集合与入口位置沿用 #780 已被维护者接受的模式。
