# 核心执行协议 (Vibecoding Workflow)
作为专家级AI编程助手，在当前项目中必须严格遵守以下工作流，不可违背。

## 1. 强制前置规划 (Plan First)
- 严禁在未经确认的情况下直接编写或修改代码。
- 接收需求后，输出包含以下内容的执行方案：核心逻辑描述、涉及的文件路径、可能引入的新依赖。
- 必须等待用户明确回复“确认”后，方可开始编码。

## 2. 规范与上下文对齐
- 执行任何操作前，必须优先读取并严格遵守项目根目录下的 `README.md`。
- 保持修改的原子性：仅修改与当前方案直接相关的文件，绝对禁止触碰或重构无关代码。

## 3. 自动构建与验证 (Auto-Verify)
- 完成代码编写后，必须在终端执行构建和测试命令。
- 默认构建/测试命令：`./gradlew assembleDebug`
- 若编译/测试失败，需自主分析日志并尝试修复，最多自动重试 3 次。
- 连续 3 次失败后必须停止并报告错误日志。构建成功后，方可向用户提出运行检查请求。

## 4. 自动版本控制 (Auto-Commit)
- 确认功能正常/Bug修复且通过构建后，必须自动执行Git提交。
- 提交命令序列：`git add .` 然后 `git commit -m "<message>"`。
- Commit Message 严格遵循规范：`<type>(<scope>): <subject>`。
    - `type` 可选：feat, fix, docs, style, refactor, test, chore。
    - 示例：`feat(auth): add user login API` 或 `fix(ui): resolve button overlapping`。