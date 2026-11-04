## 1. 纯逻辑：FrameFreshness

- [x] 1.1 新建 `FrameFreshness`（app a11y 包）：`awaitNewFrame(now, sleep, frameAt, since, timeoutMs)` 与 `serveStale(now, frameAt, since, guardMs)`，时钟/等待/帧时刻注入（参照 `StillFrameRead`）
- [x] 1.2 JVM 单测：等到新帧、超时、无启动（since=null 直通）、保护窗内无新帧不给旧帧、新帧到来解除、保护窗过期恢复、从未有帧（frameAt=null）

## 2. 接线：AgentDisplay 与 AndroidScreen

- [x] 2.1 `AgentDisplay` 补 `lastFrameAt`（uptimeMillis，processLatest 打点，`@Volatile` 读）
- [x] 2.2 `AndroidScreen.open()`/分享路径记 `startedAt` 传入 `awaitOpened`；成功收尾在 `waitStable` 后 `awaitNewFrame(1.5s)`
- [x] 2.3 `screenshotPng()` 加 `serveStale` 保护（guard 5s：窗内无新帧返回 null，新帧到/过期恢复）

## 3. 验证

- [x] 3.1 `./gradlew :app:testDebugUnitTest :core:test` 全绿
- [x] 3.2 真机端到端（TET-AN50）：Agent 屏留残留画面 → 主屏前台看着同一 App → 新任务「打开另一 App 并截图」→ 修复后返回新 App 画面或文本回退，不再是残留画面；run 记录取证
- [ ] 3.3 分支 `fix/253-stale-agent-frame`，中文提交；issue #253 评论根因与 PR（**对外动作先确认**）
