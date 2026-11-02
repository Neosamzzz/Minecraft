# FTB Quests 本地改动

当前维护分支为 [codex/ftb](https://github.com/Neosamzzz/Minecraft/tree/codex/ftb)，代码、任务配置与文档同步维护；`main` 保存已合并版本。下方旧分支名、提交号与版本信息属于历史记录，不代表当前入口。

上游：[FTBTeam/FTB-Quests](https://github.com/FTBTeam/FTB-Quests) 分支 `1.12`，导入提交见 [upstream.json](../upstream.json)。

当前状态：**源码无本地改动**，只调整了构建配置；构建通过，尚未做游戏内验证。

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
