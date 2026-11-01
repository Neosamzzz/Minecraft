# FTB Quests 本地改动

上游：[FTBTeam/FTB-Quests](https://github.com/FTBTeam/FTB-Quests) 分支 `1.12`，导入提交见 [upstream.json](../upstream.json)。

当前状态：本地版本 **1202.9.1**，已加入熟人服公共团队与界面精简；构建与隔离探针通过，尚未做游戏内联机验证。

## 相对上游的改动

| 项 | 改动 | 原因 |
| --- | --- | --- |
| 依赖来源 | `maven.latmod.com` 改为 Curse Maven 固定项目编号和文件编号 | 原仓库 TLS 握手失败；旧 CurseMaven 插件会把网页存成 JAR，导致兼容模块缺类 |
| 资源下载 | `gradle/legacy-assets.gradle` 改用 Mojang 官方 HTTPS 地址，并校验每个文件的大小与 SHA-1 | 旧 ForgeGradle 走 HTTP 且忽略下载失败 |
| 编译编码 | 固定 UTF-8 | |
| 构建入口 | 并入根目录 `tools/build.py` | |

细节见 [FTB-Quests/DEVELOPMENT.md](../FTB-Quests/DEVELOPMENT.md)。

`FTB-Quests/README.md` 是上游自带的 CurseForge 徽章页，未做改动；本仓库的构建与开发说明以 `DEVELOPMENT.md` 为准。

## 后续注意

- 更新上游时要先审阅本地改动，避免直接覆盖构建配置。
- 若开始修改团队与任务相关代码，在此追加条目，写清改了哪个类、为什么、影响哪些行为。
- 任务书内容本身不属于本文件，见 [docs/README.md](../docs/README.md)。

## 1202.9.1 / 2026-11-02 公共团队最小版

- **改动**：`SharedTeamManager` 在 Library 加载完成后选择或创建公共团队，在任务数据同步前自动归队；选队 ID 由 `FTBQuestsWorldData` 随存档保存。迁移不调用会删除空队的 `removeMember`。多队有数据或指定队不存在时停止自动接管并报日志。`SharedTeamConfig` 提供默认启用开关与既有团队 ID。
- **影响面**：服务端拦截队伍变更命令并隐藏 Library 的成员、关系、退队与转让 action；客户端收到公共团队模式后隐藏“我的组”，任务书添加编辑模式开关并收起捐助按钮。FTB Quests 管理命令向全体玩家开放，Minecraft OP 和其他模组权限不变。普通奖励、可重复任务和任务检测继续用现有逻辑，不添加任务内容。
- **验证状态**：JDK 8 离线完整构建通过；`tests/shared_team` 探针使用实际 Library 团队和 Quests 数据检查选队、双人归队、重复归队、领奖数据保留、旧队保留、保存 ID、GUI action 拦截和禁用恢复。探针未启动完整 Forge 服务器、网络传输或 GUI 渲染；两人实际联机、界面画面和服务器重启后的落盘恢复待验。
- **是否需要服务端同步**：需要。任务同步包增加公共团队模式字段，两端均需更换为本地 `1202.9.1`，版本握手拒绝旧 `1202.9.0`。安装和回退方式见 [公共团队方案](../docs/ftb-shared-team-plan.md#最小版的使用方式)。
