# 1.12.2 开发环境

## 工具版本

- 上游分支：`1.12`。
- JDK：64 位 JDK 8，必须包含 `java` 和 `javac`。
- Gradle：仓库 Wrapper 固定为 4.9，不要用系统中的新版 Gradle 替代。
- Forge：`1.12.2-14.23.5.2784`。
- MCP mappings：`snapshot_20180814`。

在本目录（主仓库的 `FTB-Quests` 子目录）打开 PowerShell。当前电脑已安装的 JDK 可这样选择；其他电脑请改为实际路径。变量只影响当前终端：

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk1.8.0_8u202_x64'
& "$env:JAVA_HOME\bin\java.exe" -version
& "$env:JAVA_HOME\bin\javac.exe" -version
```

## 编译与打包

日常构建在 Minecraft 仓库根目录执行统一入口：

```powershell
py -3 tools/build.py --target ftbquests
```

依赖缓存完整后可添加 `--offline`；macOS / Linux 将 `py -3` 替换为 `python3`。最终模组包输出到主仓库的 `build/libs/FTBQuests-1202.9.3.jar`。

如需直接调用 Gradle，在本子目录执行 `./gradlew`（Windows 使用 `.\gradlew.bat`），任务为 `build`。子项目原生输出在本目录的 `build/libs/`，包括模组 JAR 和 `FTBQuests-1202.9.3-sources.jar` 源码包。

设置 `BUILD_NUMBER` 时版本会追加构建编号，统一入口会复制对应版本的模组包。完整用法、环境准备和测试流程见主仓库 [README](../README.md)。编译成功后，仍需在独立的游戏实例中验证运行行为。

## 旧依赖兼容修复

原配置依赖的 `maven.latmod.com` 在当前环境发生 TLS 握手失败；旧 CurseMaven 1.2.x 插件把网页保存为 JAR，导致兼容模块缺少类。构建已改为直接使用 [Curse Maven](https://www.cursemaven.com/) 的固定项目编号和文件编号。

| 依赖 | 版本 | CurseForge 文件编号 |
| --- | --- | --- |
| FTB Library | 5.4.7.2 | 2985811 |
| Item Filters | 1.0.4.2 | 3003364 |
| KubeJS | 1.1.0.65 | 3052392 |
| FTB Utilities | 5.4.1.130 | 3010492 |

其余兼容依赖沿用原来的文件编号，没有删除兼容模块。KubeJS 和 FTB Utilities 使用发布版 JAR，并通过 `deobfProvided` 反混淆后参与编译。Java 编译编码固定为 UTF-8。

首次构建需要联网。项目仍使用 ForgeGradle 2.3、旧发布插件及远程 changelog 脚本；Gradle 的旧版本警告不等于编译失败。

旧 ForgeGradle 的资源下载使用 HTTP 且会忽略下载失败；`gradle/legacy-assets.gradle` 将该步骤改为 Mojang 官方 HTTPS 地址，并校验每个文件的大小和 SHA-1。离线模式缺少资源时会明确报错。

## 任务功能与数据位置

以下依据仓库当前源码，供配置任务与后续适配时查阅；不代表已在整合包中配置或完成游戏验收。

| 内容 | 当前实现 |
| --- | --- |
| 内容结构 | `QuestFile → Chapter → Quest → Task / Reward`；任务有布局坐标、前置关系、可选与显示设置，奖励可以为空 |
| 完成关系 | 默认汇总任务条件进度；`orTasks` 可改为任一条件完成。没有条件的任务在前置满足后即完成，需玩家手动确认时应添加勾选条件 |
| 前置条件 | 支持全部/任一前置开始或完成，以及至少完成指定数量的前置；隐藏连线不等于取消前置 |
| 基础条件 | 物品、流体、FE 能量、经验、维度、统计、击杀、地点、勾选、原版进度、观察、交互、群系和自定义条件 |
| 共享进度 | 按玩家所属的 FTB Library 团队取得 `ServerQuestData`；本地公共团队模式在登录时统一归队，同队使用同一份任务进度 |
| 奖励归属 | 默认 `default_reward_team=false`；单项 `team_reward` 可覆盖。个人奖励按玩家 UUID 记录，团队奖励共用记录；可重复任务强制按团队奖励处理 |

任务定义默认读取 `config/ftbquests/normal/`。若世界有 `questfile` 游戏规则，则优先使用该值作为子目录；否则在安装 PackMode 时使用其当前模式名称，最后才回退到 `normal`。定义包含 `file.snbt`、`chapters/index.snbt`、各章节及任务文件，以及 `reward_tables/`。

玩家进度和领奖记录通过 FTB Library 的团队数据文件 `team.getDataFile("ftbquests")` 保存，与上述任务定义分开。具体存档绝对路径由 Library 决定；只复制任务配置不会带走团队进度。删除团队的事件处理会删除该队任务数据，因此保留进度的公共团队方案不能通过删除旧团队直接实现。

公共团队模式隐藏任务编辑入口，禁用 `/ftbquests editing_mode`，旧的已保存编辑状态不生效。任务由 `config/ftbquests/<当前模式>/` 内的配置文件维护，停服修改后启动加载；已有团队进度另行保存。关闭公共团队模式后恢复上游编辑入口和权限规则（单人允许，联机要求权限等级 2）。

源码入口：

- [Quest](src/main/java/com/feed_the_beast/ftbquests/quest/Quest.java)：布局、前置和完成关系。
- [任务注册](src/main/java/com/feed_the_beast/ftbquests/FTBQuestsEventHandler.java)：基础条件类型。
- [ServerQuestFile](src/main/java/com/feed_the_beast/ftbquests/quest/ServerQuestFile.java)、[QuestFile](src/main/java/com/feed_the_beast/ftbquests/quest/QuestFile.java)：定义目录选择及 SNBT 读写。
- [ServerQuestData](src/main/java/com/feed_the_beast/ftbquests/util/ServerQuestData.java)、[QuestData](src/main/java/com/feed_the_beast/ftbquests/quest/QuestData.java)、[Reward](src/main/java/com/feed_the_beast/ftbquests/quest/reward/Reward.java)：团队进度、持久化与领奖归属。
- [CommandEditingMode](src/main/java/com/feed_the_beast/ftbquests/command/CommandEditingMode.java)：编辑模式与权限。

## 公共团队隔离检查

从仓库根目录执行（先设置 JDK 8）：

```powershell
.\FTB-Quests\gradlew.bat -p FTB-Quests --offline --no-daemon --console=plain -I tests/shared_team/probe.gradle runSharedTeamProbe
```

探针使用真实 Library 团队、Quests 数据和 GUI action 接口，不启动完整游戏或网络，也不访问玩家存档。测试目录在系统临时目录创建。完整运行验收见[公共团队方案](../docs/ftb-shared-team-plan.md#最低验收范围)。
