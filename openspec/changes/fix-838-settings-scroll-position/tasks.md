## 1. 按页保存滚动状态的容器

- [x] 1.1 在 `app/src/main/java/com/wangzan101/mika/ui/components/Page.kt` 新增 `PageScrolls`：
  `of(page: Enum<*>): ScrollState` 按页取/建滚动状态；`PageScrolls.Saver` 把取过的页按枚举名
  存成 `名字 → 位置`，恢复时各页第一次被取到即顶上存下来的位置。
- [x] 1.2 单测 `app/src/test/java/com/wangzan101/mika/ui/components/PageScrollsTest.kt`：
  翻到别的页再回来仍是同一状态且位置不变；不同页互不影响；`Saver` 存取一轮后位置接上、
  没取过的页从 0 开始。

## 2. 设置页接线

- [x] 2.1 `SettingsScreen` 用 `rememberSaveable(saver = PageScrolls.Saver)` 持有一个 `PageScrolls`。
- [x] 2.2 `SettingsContent`、`AdvancedSettingsContent` 增加 `scrollState` 参数（默认
  `rememberScrollState()`）并传给 `PageScaffold`；首页、高级与其他两页从 `PageScrolls` 取。
- [x] 2.3 `DuringWorkScreen`、`DeveloperOptionsScreen`、`RecoveryScreen` 同样增加 `scrollState`
  参数并在 `SettingsScreen` 里从 `PageScrolls` 取。
- [x] 2.4 `PageScaffold.scrollState` 默认值改为 null（不给时仍用本页自己的状态）；对显式传入的
  状态做有界的首帧截短补救：首次布局后若值被偏小的上限截短，等上限长回去（≤500ms）且期间没人
  动过再放回。

## 3. 验证

- [x] 3.1 `./gradlew :app:testDebugUnitTest --tests '*PageScrollsTest*'` 全绿。
- [x] 3.2 全量 `:core:test :core:cliTest :app:testDebugUnitTest` 通过；`./gradlew :app:assembleDebug` 通过。
- [x] 3.3 真机（Mate X2）对照：未改动的 main 包复现（返回后回顶部），修复包上同一组操作
  首页停在原位；再走「高级与其他 → 子页 → 返回」（中间位置和最底各一次）、「任务运行 →
  按应用设置 → 返回」（底部）、「开发者选项 → 运行统计 → 返回」、「恢复与修复 → 自检 → 返回」。
  截图、uiautomator 坐标、时间线写入 `evidence/evidence.md`。
- [x] 3.4 验证完把手机恢复到原先的前台应用。
- [x] 3.5 模拟器（只读 AVD，Android 14 / 1080×2340）跑设备用例：新增 `PageScrollsDeviceTest` 3 条全过；两组对照（不保留滚动状态、关掉截短补救）确认用例能判别缺陷；现有 `SettingsScreenTest` 的 8 条失败
  与未改动的 main 一致。
