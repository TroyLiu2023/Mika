## Why

设置页往下滚一段，点进任意二级页再返回，页面回到顶部，刚才的位置没了，每次都得重新往下翻
（issue #838，0.1.51 起的简约列表设置页；最新 main 即 0.1.52 仍在）。根因是 `SettingsScreen`
用 `when (visiblePage)` 在首页和各子页之间切换组合，页面骨架 `PageScaffold` 的 `scrollState`
默认值是页面自己组合里的 `rememberScrollState()`——进子页时当前页离开组合，它的滚动状态随之
丢弃，返回时新建的状态从 0 开始。

## What Changes

- 新增 `PageScrolls`：按页（枚举键）保存滚动状态的小容器，带 `Saver`（换配置重建后接上各页位置），
  放在 `ui/components`（与 `PageScaffold` 同处），供「在一处切页显示」的页面共用。
- `SettingsScreen` 持有一个 `PageScrolls`，有子页可进的五页——首页、高级与其他、任务运行、
  恢复、开发者选项——把各自的滚动状态经 `PageScaffold(scrollState = …)` 传下去，返回时停在
  原来的位置。
- 子页自身的其他状态语义不变（不包 `SaveableStateProvider`，各子页里 `rememberSaveable` 的
  「只问一次」标志、弹窗状态照旧每次进入都是新的）。
- 单元测试 `PageScrollsTest`：翻到别的页再回来仍是同一页的同一状态且位置不变；不同页互不
  影响；`Saver` 存取一轮后位置接上。

## Capabilities

### New Capabilities

- `settings-scroll-position`: 设置页内翻页时的滚动位置契约——从子页返回有子页可进的页面时，
  该页 MUST 停在离开时的滚动位置；换配置重建后位置也 MUST 接上。

### Modified Capabilities

（无——`openspec/specs/` 目前没有任何规格。）

## Impact

- `app/src/main/java/com/wangzan101/mika/ui/components/Page.kt`：新增 `PageScrolls`。
- `app/src/main/java/com/wangzan101/mika/settings/ui/SettingsScreen.kt`：持有 `PageScrolls`，
  `SettingsContent`、`AdvancedSettingsContent` 增加 `scrollState` 参数并传给骨架。
- `app/src/main/java/com/wangzan101/mika/settings/ui/DuringWorkScreen.kt`、`DeveloperOptionsScreen.kt`、
  `SettingsSubPages.kt`（`RecoveryScreen`）：各增加 `scrollState` 参数（默认仍取页面自己的状态，
  其他调用方不受影响）。
- `app/src/test/java/com/wangzan101/mika/ui/components/PageScrollsTest.kt`：新增。
- 不涉及字符串资源、`core` 模块、协议层。
