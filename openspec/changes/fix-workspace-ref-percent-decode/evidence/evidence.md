# 取证：产出引用百分号转义不解码

## 2026-11-20（本地，main c4ef434c）

1. **库行为实证**（`commonmark-0.30.1-destinations.txt`，项目同版本 jar 直跑）：
   `[x](/workspace/my%20file.csv)` 的 destination 原样返回 `my%20file.csv`，中文
   `%E6%8A%A5%E5%91%8A.png` 同样原样——commonmark-java 不做百分号解码，应用层不解码
   就与真实文件名对不上。
2. **缺陷路径推演（代码走读）**：`workspaceRefs` → `outputRefsParsed` →
   `AttachFiles.relative` + `gate.openRead(workspace(conv), "my%20file.csv")` 字面路径
   找不到文件 → 点开返回 false 弹「文件不存在」；图片缩略图 `sentBytes` 同一闸门 →
   空占位盒。
3. **修复后单测**：
   - `:app:testDebugUnitTest --tests …MarkdownModelTest --tests …WorkspaceRefTest` 通过
     （含新增 5 个用例：%20、中文 %E6%8A%A5、字面 +、非法 % 序列、%2E%2E 越界丢弃）。
   - 全量 `:app:testDebugUnitTest` + `:core:test` BUILD SUCCESSFUL（4m55s）。
4. **真机走查**：本轮未做——测试机上有用户真实任务暂停等待本人处理，不再占用设备；
   PR 里如实注明。设备空闲后可按「沙箱生成带空格/中文名文件 → 模型回复引用 → 点卡片」
   复测。
