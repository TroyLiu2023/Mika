# 设计：fix-253-stale-agent-frame

## Context

Agent 手机画面链路：`AgentDisplay`（虚拟屏 → ImageReader，≤10fps 处理帧）→ `AndroidScreen.screenshotPng()`（最新帧）→ core 的 `observe`/`screenshot`/动作回看。启动链路：`AndroidScreen.open()` → `shell.startApp(displayId,…)`（全部落在 Agent 屏）→ `awaitOpened`（等任务到顶 + `waitStable` 树稳定）。无任何「帧属于哪个时刻」的信息；`StillFrameRead` 已有「注入时钟/等待/帧内容、JVM 可测」的先例与 `FrameMath` 帧比较工具，但那是 OCR 静止判定，不是新鲜度。

根因见 proposal：虚拟屏残留帧 + 启动只等树不等帧。

## Goals / Non-Goals

**Goals**
- 启动后的截图不再可能是上一屏残影。
- 等待有界（不能让启动收尾无限拖长）、保护窗短（不影响长任务正常截图）。
- 判定逻辑 JVM 单测覆盖。

**Non-Goals**
- 不改 core 的截图工具协议（`screenshot_unavailable` 失败路径已存在，直接复用）。
- 不处理 Web 屏（无此问题）。
- 不做帧内容对比（`FrameMath.differs` 对「启动页 vs 上一屏」不可靠：启动页可能近似、动画中途帧可能已变）。

## Decisions

1. **新鲜度用「帧到达时刻」而不是帧内容。**
   虚拟屏内容静止时 ImageReader 不来新帧，所以「晚于启动开始时刻的帧」精确等价于「该次启动产生过画面」。比内容对比可靠、便宜。
   备选（否决）：比较截图与启动前帧的像素——启动页/动画/同 App 换页都会误判。

2. **等待逻辑收进纯 Kotlin 帮手 `FrameFreshness`（a11y 包），时钟/等待/帧时刻注入。**
   参照 `StillFrameRead` 的既有形态。两个入口：
   - `awaitNewFrame(now, sleep, frameAt, since, timeoutMs): Boolean`——轮询到 `frameAt() > since` 或超时；
   - `serveStale(now, frameAt, since, guardMs): Boolean`——截图保护窗判定：有 `since`（最近一次启动）且 `frameAt <= since` 且 `now - since < guardMs` 时不给旧帧。
   备选（否决）：把等待写进 `AgentDisplay`——绑死 Android 时钟，测不了。

3. **`AgentDisplay` 只补一个事实：`lastFrameAt`（最近一次处理帧的 uptimeMillis，处理线程写、读侧 `@Volatile`）。**
   帧在 `processLatest` 里落盘时打点；没来过帧为 0。

4. **`AndroidScreen` 的接线：**
   - `open()`（及分享路径）记 `startedAt`（launch 前），传给 `awaitOpened`；
   - `awaitOpened` 成功路径末尾（`waitStable` 之后）`FrameFreshness.awaitNewFrame(…, startedAt, FRAME_WAIT_MS=1_500)`，结果只影响是否多等，不再另做处理；
   - `screenshotPng()` 走 `serveStale` 保护：`@Volatile frameSince`（launch 前设为 startedAt；判定为可给旧帧或过期时清空）。`guardMs=5_000`。
   备选（否决）：把保护放 core 截图工具——接口要加参数，动面大；问题只存在手机屏。

5. **超时/保护的取值**：等帧 1.5s（冷启动首帧绝大多数 <1.5s，启动页动画也在此内）；保护窗 5s（覆盖「回看紧跟启动返回」的窗口，又远小于正常任务节奏）。

## Risks / Trade-offs

- **极慢首帧的 App**：保护窗内截图退化为纯文本，模型会重试读屏——按现有 `screenshot_unavailable` 语义可接受，比给错图好。
- **虚拟屏在无人操作时也可能来帧**（壁纸动画、光标）：只会在「已来新帧」方向上解除保护，方向安全。
- **保护窗判定依赖 uptime 时钟**：`AgentDisplay` 与 `AndroidScreen` 都用 `SystemClock.uptimeMillis`，同一时基。

## 测试与验证

- JVM 单测 `FrameFreshnessTest`（app 单测源集）：等到新帧/超时/无启动/保护窗内无新帧不给旧帧/新帧到解除/过期恢复/从未有帧。
- 真机（TET-AN50）端到端：先在 Agent 屏留下一个 App 的画面（跑一个打开它的任务），把它拉回主屏在前台看着，再发「打开另一个 App 并截图」——修复前截图是残留画面，修复后是新 App 的画面或文本回退；对照 run 记录里的截图。
