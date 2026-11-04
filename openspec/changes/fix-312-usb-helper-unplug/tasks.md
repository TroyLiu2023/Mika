## 1. core：按设备如实的脚本文案

- [x] 1.1 `HelperHandoff.usbScript` 增 `reapedOnUnplug` 参数：成功输出在「多数手机可继续用（含停了就插线重跑的恢复说法）」与「此手机拔线会停、插线重跑即恢复」之间二选一；默认版不打包票
- [x] 1.2 `usbFile`/`UsbStartScript` 链路把平台标志传进来；stub（助手已连）分支不动
- [ ] 1.4 单测：脚本文本两轨断言、口令仍不进参数、`UsbHelperHangupTest` 场景在新默认文案下仍通过

## 2. core：学标志的判定与平台接口

- [x] 2.1 新类型 `UsbUnplugReap`（core.shell）：记录电源断开时间、未配对助手死亡时间，60 秒窗口内先后发生即判定成立（一次即持久，重复命中不重复写）
- [x] 2.2 `BuiltinShellPlatform` 增 `unplugReapsHelper()/setUnplugReapsHelper()`；`BuiltinShellEngine` 订阅 helper 流处接上判定（仅未配对 && 标志未置时）
- [x] 2.3 单测：`UsbUnplugReap` 窗口边界/前提（已配对不判、先死后断电不判）；`BuiltinShellEngine` 假平台上置位路径

## 3. app：事件接线与文案

- [x] 3.1 `AndroidPlatform` 用 `builtin_shell` prefs 持久化标志（跨 unpair 保留）；`unpair()` 不清它
- [x] 3.2 `BuiltinShellManager.start()` 运行时注册 `ACTION_POWER_DISCONNECTED`，时间戳喂给 `UsbUnplugReap`
- [x] 3.3 `PhoneSetupCopy` 的 `usageNote`/`successNote`/`usbCard` 在「USB 启动 + 标志置位」时给如实说法；新增字符串进 `strings_phone.xml` 并同步全部语言文件
- [x] 3.4 app 单测：文案选择逻辑（假平台/假上下文）

## 4. 文档

- [x] 4.1 `docs/setup-guide.md` 与 `docs/setup-guide.en.md`：USB 启动一节注明部分机型拔线会回收助手、恢复办法；其余承诺措辞对齐
- [x] 4.2 `docs/dev/roadmap.md` 已知问题补一条（含根因一句话），指向 issue #312

## 5. 验证与收尾

- [x] 5.1 `./gradlew :core:test :core:cliTest :app:testDebugUnitTest` 全绿
- [x] 5.2 真机（TET-AN50）：装 debug 包，跑 usb-start.sh → 模拟「断电后杀助手」序列 → 验证标志置位、脚本与设置页文案切换、重跑脚本恢复手机操作
- [x] 5.3 用户真拔插线一次，金丝雀判定完成：SIGKILL（忽略 TERM 的也被杀）、adbd 拔线时重启；issue #312 根因评论已起草（**发布前需用户确认**）
- [ ] 5.4 分支 `fix/312-usb-helper-unplug`，中文提交信息；PR 描述关联 issue（**创建 PR 前需用户确认**）
