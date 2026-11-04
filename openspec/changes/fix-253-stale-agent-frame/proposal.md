## Why

Issue #253：让 Agent「打开 App B 并截图」，返回的截图却是用户当前正在看的界面（App A），而不是刚打开的 B。

根因（读码定位，解释报告的每个细节）：Agent 的手机画面全部来自 Agent 虚拟屏（`AgentDisplay` 的 ImageReader 最新帧），启动路径也全部落在虚拟屏上——模型没有任何实时主屏通路。但：

- `AgentDisplay.latestFramePng()` 返回**至今收到的最新一帧**，没有新鲜度概念；
- 上一轮任务或「接管」常把用户常用的 App 留在虚拟屏上；该 App 的任务被用户拉回主屏后，虚拟屏不再来新帧，旧帧一直留在缓冲里；
- 用户此刻在主屏看 A，虚拟屏缓冲里恰好也是 A 的残影；
- 新任务启动 B 时 `awaitOpened` 只等「任务到顶 + 无障碍树稳定」，不等 B 的**首帧渲染**——B 的窗口在树里出现（含启动页）时，虚拟屏可能还没合成 B 的画面；
- 紧随其后的 `screenshot`/动作回看取「最新帧」→ A 的残影。用户看到的正是「截到了我正在看的界面」。

## What Changes

1. `AgentDisplay` 记录每帧的处理时刻，暴露「最新帧时刻」；新增一个小的纯 Kotlin 帮手（注入时钟/等待/取帧时刻，参照 `StillFrameRead` 的做法，JVM 可测）实现「等到比某时刻新的帧，限时」。
2. 启动 App 的收尾（`AndroidScreen.awaitOpened` 成功路径）在「树稳定」之后再限时等一帧**晚于启动开始**的新画面；多数情况下启动页/首帧会在时限内到来，透明消除残影窗口。
3. 截图口加短时保护：启动后的保护窗内，若还没有晚于该次启动的帧，`screenshotPng()` 返回 null（读屏退化为纯文本、单独截图报 `screenshot_unavailable`），**宁缺毋错**；新帧到来或保护窗过期后恢复原样，不影响长任务的正常截图。
4. 分享打开、打开 Mika 自己的入口共用 `awaitOpened`，同样受益。

## Capabilities

### New Capabilities
- `agent-frame-freshness`: Agent 虚拟屏画面的新鲜度保证——启动 App 之后的截图必须来自该次启动之后的渲染帧，不把上一屏的残影当新画面交给模型。

### Modified Capabilities
<!-- 无 -->
