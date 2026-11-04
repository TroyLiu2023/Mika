## Why

Issue #312：华为 Mate X2（HarmonyOS 底座，Android 12）上，「用电脑启动（USB 数据线）」起的手机助手在拔掉数据线后被系统杀掉，与脚本自己的输出（"It keeps running after you unplug the cable"）和 `docs/setup-guide.md` 的「之后拔掉数据线也能继续用」相悖。

在报告人的同款真机（TET-AN50）上已定位到机制，结论是**这类设备上 shell 身份的进程无法在拔线中幸存**：

- 脚本已经用 `nohup setsid` 起助手，且确实生效（实测 `/proc/<pid>/stat`：独立 session/pgrp，SIGHUP 已忽略）。
- 但这台机器的 adbd 不为 shell 子进程单独建 cgroup，所有 adb 派生进程（含 setsid 过的助手）都在 adbd 自己的 cgroup `/sys/fs/cgroup/uid_0/pid_<adbd_pid>` 里；真拔线后 adbd 退出/重启，这个 cgroup 被整体清理。
- cgroup 全部节点归 root/system，shell（uid 2000）无法把自己挪出去。
- 备选通道也不可用：无线调试的 `AdbDebuggingManager` 存在，但确认弹窗组件 `com.android.systemui.wifi.WifiDebuggingActivity` 被厂商删掉，`adb_wifi_enabled` 写 1 立即被拨回 0——与「开发者选项里没有无线调试」一致。
- 主机侧模拟（`adb kill-server`、USB 重置重枚举、`dumpsys battery unplug`）都不会触发清理（实测 helper 全部存活），只有真实拔线会。

因此本变更不追求「拔线后幸存」（此设备类做不到），而是把产品对这个事实变得诚实、把恢复路径缩到最短。恢复路径本身已实测可用：Mika 活着时 helper 一死，`usb-start.sh` 4 秒内自动换新口令，**重新插线后只需再跑一次脚本**（不必先打开 Mika）。

## What Changes

1. **脚本文案不再无条件承诺「拔线也能用」**（core `HelperHandoff.usbScript`）：
   - 默认（未观察到拔线杀助手）保持乐观提示，但措辞改为不打包票的版本。
   - 观察到本机拔线会杀助手后（见 2），换成如实提示：此手机拔线会停掉助手，重新插线后再运行一次本命令即可恢复。
   - 失败分支按 Mika 主进程是否存活（`pidof`）区分指引：活着就说「再运行一次本命令」，不在才说「先打开一次 Mika」。
2. **学到「本机拔线会杀助手」这个事实**（core + app）：
   - app 记录电源断开事件时间；USB 启动的助手（未配对）在断开后的一小段时间内死掉，就持久化记下「此设备拔线回收助手」。
   - 该标志影响 1 中的脚本文案与设置页/成功页的提示语（`PhoneSetupCopy` 相关文案），不再让用户被假承诺误导。
3. **文档同步**（`docs/setup-guide.md` / `.en`）：注明部分机型（如 HarmonyOS 底座的华为）拔线会回收助手，恢复办法是插线后重跑一次命令。
4. **留证后续**（不在本变更实现）：报告人真机上已布好金丝雀实验（普通/忽略 SIGTERM 的 shell 进程心跳记录到 `/sdcard/mika_canary.log`），等一次真实拔插线后读取，判断清理是 SIGKILL 还是可躲的 SIGTERM；若可躲，另立变更做存活加固。

## Capabilities

### New Capabilities
- `usb-helper-unplug`: 「用电脑启动」的助手在拔线后的死亡检测、按机如实提示与恢复指引（脚本文案、学习到的设备标志、设置页提示）。

### Modified Capabilities
<!-- 无：仓库尚无 openspec 规格，本变更是第一个。 -->
