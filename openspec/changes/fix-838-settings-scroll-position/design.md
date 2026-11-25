## Context

- `SettingsScreen`（`app/.../settings/ui/SettingsScreen.kt`）用一个 `rememberSaveable` 的 `page`
  枚举加 `when (visiblePage)` 在首页和各子页之间切换；翻页时当前页的 composable 离开组合，
  目标页进入组合。
- 各页都经 `PageScaffold`（`app/.../ui/components/Page.kt`）渲染，它的
  `scrollState: ScrollState = rememberScrollState()` 默认值在**页面自己的组合**里创建。进子页时
  当前页离开组合，它的 `ScrollState` 随之丢弃；返回时新建，`value` 从 0 开始——这就是
  #838。实测（Mate X2，main @ e8479330 即 0.1.52）：设置首页滚到「语言与外观」在 y=1254，
  点进去再返回，该行回到 y=2420（与未滚动时相同），顶栏返回箭头和系统返回键都是如此。
- 有子页可进、返回时要停回原位的页面：首页 `Home`（所有子页返回到它）、`Advanced`
  （沙箱/恢复/电脑访问/MCP/Root/开发者/环境变量返回到它）、`DuringWork`（→ AppPace）、
  `Developer`（→ RunStats/DeviceDirectories）、`Recovery`（→ SelfCheck）。
  「常用任务」→ 清单/流程返回的是首页（`toHome`），不在其列。
- `rememberScrollState()` 本身是 `rememberSaveable` 的：换界面语言时 Activity 重建（转屏、深浅色由
  `MainActivity` 的 `configChanges` 自行处理，不重建），页面留在组合时位置靠它接上；进程被回收后
  恢复也是靠它。修复不能丢掉这一点。
- 仓库里同类「可保存的状态容器」先例：`ui/Navigation.kt` 的 `NavStack`（`rememberSaveable(saver = NavStack.Saver)`，
  有纯 JVM 的 `NavStackTest`）。`app/src/test` 没有 Robolectric / Compose 测试能力，单测只能测
  不依赖渲染的部分。

## Goals / Non-Goals

**Goals:**
- 从子页返回首页、高级与其他、任务运行、恢复、开发者选项时，该页停在离开时的滚动位置——
  包括停在页面底部的情况（那里会被晚一帧的卡片脚注截短，见决策 4）。
- 换界面语言页面重建、进程被回收后恢复时这些位置照旧接上（不比现状差）。
- 子页自身的其他状态语义一律不变；不显式传入滚动状态的页面行为不变。

**Non-Goals:**
- 不改设置页整体离开再回来的行为（`NavSlide`/`AnimatedContent` 在旧路由退场后丢弃其保存状态，
  从模型页等回到设置是全新首页——既有设计）。
- 不改「常用任务」→ 清单/流程返回首页的去向。
- 不给其他多页屏幕做同样的修复；不引入 Robolectric / 设备 UI 用例。

## Decisions

1. **把父页的滚动状态提升到 `SettingsScreen` 持有，经 `PageScaffold(scrollState = …)` 传下去。**
   `SettingsScreen` 在翻页期间始终在组合里，它持有的 `ScrollState` 不会随子页进出而丢弃；
   `PageScaffold` 本来就留了 `scrollState` 参数这个口子。
   - 备选「整个 `when` 包 `SaveableStateProvider(key = page)`」：会把每个子页里所有
     `rememberSaveable` 状态也跨访问保留——`PhoneSetupScreen` 的 `repairStarted/askedNotifications/
     accessibilitySkipped`、`CallSettingsScreen` 的 `dialog/taskDraft/tab`、`ModelSetupScreen` 的
     `signInSheet/picking` 等「只问一次」标志和弹窗状态，语义上会从「每次进入都是新的」变成
     「同一次停留在设置期间一直留着」，超出本修复的范围，且有回归风险。不采用。
   - 备选「五页各写一个 `rememberScrollState()`」：最短，但没有可单测的单元。不采用。
2. **新增 `PageScrolls`（按枚举键保存 `ScrollState` 的小容器，带 `Saver`），放在 `ui/components`
   与 `PageScaffold` 同处。** 可保存是为了不丢 `rememberScrollState()` 原有的换配置重建行为：
   `Saver` 把取过的页按枚举名存成 `名字 → 位置` 的表；恢复时先记下各页位置，某页第一次被取到时
   新建状态并顶上该位置。键限定为枚举，存取不需要额外的键编码。
3. **五个父页统一处理。** `SettingsContent`、`AdvancedSettingsContent`、`DuringWorkScreen`、
   `DeveloperOptionsScreen`、`RecoveryScreen` 各加一个 `scrollState` 参数，默认值仍是
   `rememberScrollState()`，其他调用方（如有）不受影响。
4. **`PageScaffold` 对带进来的滚动状态做一次有界的「截短补救」。** 实测（Mate X2）发现：简约列表的
   卡片脚注是行在 `SideEffect` 里登记、下一帧才画出来的（`Surfaces.kt` 的 `rememberCardNote` /
   `GroupCard`），所以页面首帧内容偏矮（缺全部脚注）。带着位置回来时若位置靠近底部，
   `ScrollState` 在首帧布局按偏小的上限把值截短，脚注出现后不会自己补回：
   - 「任务运行」滚到「按应用设置」再返回，实测低了 434px（这页脚注多、入口在底部）；
   - 「高级与其他」滚到最底再返回，低了 72px（24dp，正好一个脚注行）；停在中间时逐像素一致；
   - 首页最后两张卡没有脚注，到底也逐像素一致。
   位置不在底部时滚动值不被截：首帧行偏高一帧，脚注出现后即复位，最终一致。
   做法：`PageScaffold` 在组合时记下带着的位置；首次布局后若值变小且正落在当前上限上，判定为被
   截短，等上限长回去（至多 500ms）且期间值没被动过，再 `scrollTo` 回去。只对调用方显式传入
   `scrollState` 的页面生效——该参数默认值由 `rememberScrollState()` 改为 null，不给时行为不变，
   其余 40 多个调用方不受影响。
   - 备选「根治脚注时序」（`rememberCardNote` 不经 `SideEffect` 直接登记，或 `GroupCard` 改子组合）：
     动的是简约列表设计系统的核心，影响全 App 的卡片，超出本修复范围；如实记录，由维护者定。
   - 已知残留：被截短的那两种情况下，返回后会有一两帧停在截短处再落回原位（首帧内容本身不全，
     无法避免）；比不带补救时停在偏离处好。

## Risks / Trade-offs

- [容器单测测不到 `SettingsScreen` 的接线本身] → 真机走查覆盖接线（修复前后同一组操作对照），
  取证进 `evidence/`。
- [换配置后恢复的位置大于新布局的可滚动上限（如折叠屏收起后视口变矮）] → `ScrollState` 在布局量出
  `maxValue` 时自己收敛，与 `rememberScrollState()` 的恢复路径相同。
- [`PageScrolls` 对同一实例里不同枚举的同名页会存成同一个键] → 一处翻页只用一个枚举
  （`SettingsPage`），注释写明。
- [开发者选项只在调试/性能版列出，`visiblePage` 可能把 `Developer` 折成首页] → 提升的
  状态按页取，折回首页时取的是首页的状态，不受影响。
