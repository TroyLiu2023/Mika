## 1. 修复

- [x] 1.1 `MarkdownModel.workspaceRefs`：抽出 Link/Image 目标后按 `ProxyAddress` 先例做
  宽容百分号解码（`+` 预替换成 `%2B`、`runCatching` 失败保留原串），解码后去重返回
- [x] 1.2 确认 `WorkspaceRef.outputRefsParsed` 无需改动（normalize 过滤在解码后仍把关
  `..` 与越界）

## 2. 测试

- [x] 2.1 `MarkdownModelTest`：`%20`、中文 `%E6%8A%A5`、字面 `+`、非法 `%` 序列、
  `<>` 裸形式不受影响、`%2F` 解码后仍被 normalize 圈住
- [x] 2.2 `WorkspaceRefTest`：编码引用解析出的 `Item.path` 为解码路径、照片分类正确、
  越界编码引用（如 `%2E%2E`）被丢弃
- [x] 2.3 跑 `:app:testDebugUnitTest` 相关过滤与 `:core:test` 全量，确认无回归

## 3. 交付

- [ ] 3.1 提交（中文提交信息）、推分支 `fix/workspace-ref-percent-decode`、建 PR 关联
  产出引用功能背景，正文附单测结果与 commonmark 0.30.1 行为实证说明
- [ ] 3.2 等 CI 四组（app/core/cli/relay）全绿；如设备空闲则补真机走查取证存
  `evidence/`
