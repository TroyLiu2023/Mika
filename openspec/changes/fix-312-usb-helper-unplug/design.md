# 设计：fix-312-usb-helper-unplug

## Context

「用电脑启动」路径：电脑经 USB 调试跑 `usb-start.sh`（由 App 写在外部文件目录），脚本以 shell 身份 `nohup setsid app_process …` 起助手，助手经 `HandoffProvider` 交接 Binder（见 `HelperHandoff`、`HelperProcess`、`HelperHub`）。App 侧 `UsbStartScript` 跟着助手在不在重写脚本；`BuiltinShellEngine` 在未配对场景用 `usbStarted` 标志推导「助手已停止」状态与恢复指引。

真机取证（华为 Mate X2，TET-AN50，Android 12 / HarmonyOS 底座，Mika 0.1.34）：

| 实验 | 结果 |
| --- | --- |
| `nohup setsid` 是否生效 | 生效：独立 session/pgrp（`/proc/<pid>/stat`），SIGHUP 已忽略（`SigIgn=…01`） |
| 助手所在 cgroup | `/sys/fs/cgroup/uid_0/pid_<adbd_pid>`——adbd 自己的 cgroup，全体 adb 派生进程共享 |
| shell 能否挪出 cgroup | 不能：所有 cgroup 节点 root/system 属主 |
| `adb kill-server` / 主机 USB 重置重枚举 / `dumpsys battery unplug` | helper 均存活（不触发清理） |
| 真实拔线 | helper 被杀（issue 两次复现；adbd 亦在重插时重启过，时间线吻合「adbd 退出→cgroup 清理」） |
| 无线调试兜底 | 不可用：`AdbDebuggingManager` 在，但确认组件 `com.android.systemui.wifi.WifiDebuggingActivity` 被删，`adb_wifi_enabled` 写 1 即被拨回 0 |
| helper 死后恢复 | Mika 存活时 4 秒内脚本换新口令，插线重跑脚本即恢复（无需先打开 Mika） |

结论：此设备类上 shell 身份进程躲不过真实拔线触发的清理，修复方向是「如实提示 + 最短恢复」。

## Goals / Non-Goals

**Goals**
- 脚本与界面不再对本机拔线会杀助手的设备说假话。
- App 学到「本机拔线回收助手」的事实（一次即持久），自动切换文案。
- 恢复指引精确到「插线后重跑一条命令」。
- 全部核心逻辑可在电脑上单测。

**Non-Goals**
- 不尝试让助手在这类设备上拔线幸存（机制上无路：cgroup 无法逃逸；金丝雀实验已证实清理是 SIGKILL——忽略 SIGTERM 的进程同被杀）。
- 不动无线调试路径。
- 不自动重跑脚本（需要电脑参与，无法自动化）。

## Decisions

1. **按设备学习，不做机型名单。**
   依据时间关联（电源断开事件 ↔ 未配对助手死亡）置位持久化标志，而不是维护「华为 HarmonyOS 底座会杀」的机型库。避免误伤其余同厂商设备，也覆盖未知厂商的同类行为。
   备选（否决）：无条件在所有设备用含糊措辞——惩罚了多数正常设备上的明确体验。

2. **关联判定收进 core 的纯 Kotlin 类型，平台只供时间戳与持久化。**
   新类型 `UsbUnplugReap`（core.shell）：`onPowerDisconnected(now)`、`onHelperLost(now)`（含「未配对」前提由调用方传入）→ 内部窗口判定 → 命中即要求平台持久化。窗口 60 秒（清理在拔线后数秒内发生，留足裕量）。`BuiltinShellPlatform` 增 `unplugReapsHelper()/setUnplugReapsHelper()`，实现落在 `AndroidPlatform` 的 `builtin_shell` prefs。
   备选（否决）：把判定写在 app 的 broadcast receiver 里——没法在电脑上单测，也绕开引擎已有的 helper 流订阅。

3. **文案双轨由 `HelperHandoff.usbFile/usbScript` 加参数。**
   `usbScript` 增 `reapedOnUnplug: Boolean`，成功输出二选一；`UsbStartScript.write` 从平台标志取值。口令、脱敏结构、失败分支指引（打开一次 Mika 再重跑——打开即顺带换新口令）不变。
   备选（否决）：失败分支按 `pidof` 主进程区分指引——失败分支罕见且现有指引对两种情况都有效；也否决 App 在写盘前做字符串替换——脆，core 生成器已有测试可直接扩展。

4. **电源断开事件用 `ACTION_POWER_DISCONNECTED`（运行时注册）。**
   USB 拔线必然伴随断电广播；比 `ACTION_USB_STATE`（protected broadcast，各厂商行为不一）稳。App 侧在 `BuiltinShellManager.start()` 里随引擎一起订阅，时间戳交给 `UsbUnplugReap`。

5. **学习到的标志跨解除配对保留。**
   它描述的是手机系统行为，不是配对状态；`unpair()` 不清除（usbStarted 标志照旧清除）。

6. **界面接入点收敛在 `PhoneSetupCopy`。**
   `usageNote`/`successNote`/`usbCard` 按 `HelperRestart.UsbCommand` + 新标志组合出如实版本；新增字符串走 `strings_phone.xml` 全语言文件（翻译时效与指纹按仓库约定处理）。

## Risks / Trade-offs

- **误判（其它原因的助手死亡恰好撞上断电）**：置位只会把文案变保守，不会破坏功能；接受。反向漏判（清理先于广播、或广播丢失）最多维持默认文案，可接受。
- **`pidof` 在个别系统不可用**：失败分支退化为现有统一指引（`pidof` 失败按「不在」处理）。
- **字符串新增涉及全部语言文件**：按 i18n 既有流程同步，避免 `ReadmeTranslationsTest`/资源检查报错。

## 测试与验证

- core 单测：`UsbUnplugReap` 的窗口/前提/一次性判定；`usbScript` 双轨文案与 `pidof` 分支（扩展现有 `HelperHandoffTest`/`UsbHelperHangupTest` 风格，脚本执行型测试沿用 Linux 假设）。
- app 单测：`BuiltinShellEngine` 喂「未配对助手 + 断电后死亡」→ 状态仍为 `UsbHelperStopped` 且平台标志被置位（假平台）。
- 真机（TET-AN50）：装 debug 包 → 跑 usb-start.sh 杀助手/断电序列触发标志 → 检查脚本与设置页文案切换；重跑脚本恢复手机操作。
- 留证：真拔插线一次后读 `/sdcard/mika_canary.log`，判定清理信号类型，结论回填 issue #312（评论需用户确认后发）。
