# 终章地形与渲染：工作全流程

本文交接本会话实际完成的工作及操作方法，按可复用的成功路径组织。核对日期：2026-11-03。当前功能与后续设计由 [README](README.md) 维护；这里说明测试环境如何建立、如何完成验证，以及本会话工作的边界。

## 1. 将任务拆成可以独立验证的阶段

先确定保留 Minecraft 1.12.2 的 GC／GS 玩法，以 Reimagined 提供光影、DH 提供远景。验证顺序为：独立渲染环境 → 加入现有模组 → 普通世界地形验证 → 终章实现 → 跨维度与多人完整旅程。

前期全部使用普通主世界，先确认渲染和地形能工作，再接 GC 星球。景观与渲染的测试不等待多人飞船完成，也不提前改正式存档。

先阅读项目文档、Issue #8 与相关上游资料，再固定具体版本；通过发布元数据核对下载哈希，并对 JAR 做 ZIP CRC 检查。共存测试使用实际客户端的模组文件，复制后逐个核对哈希，而不是另外寻找同名版本代替。

## 2. 在 Windows 建立独立渲染环境

测试根目录：

```text
C:\Users\qq106\project\Minecraft\.local\newearth-render\
```

复用本机已有的 Java 25.0.1，通过启动入口指定可执行文件：

```text
C:\Users\qq106\AppData\Roaming\.minecraft\runtime\java-runtime-epsilon\bin\java.exe
```

不修改全局 Java 设置或原 Java 8 启动器配置。构建 NewEarth 仍使用 JDK 8；运行独立 Cleanroom 测试实例使用上述 Java 25，两者用途分开。

固定的渲染组合为 Cleanroom 0.6.13-alpha、Actinium alpha-0.0.13、官方 DH 3.3.3（1.12.2 Forge）和 Complementary Reimagined r5.9.3。测试机为 Windows 11、RTX 3060 Laptop，首轮驱动为 NVIDIA 610.88。

搭建时按运行清单准备 Cleanroom 类库、Minecraft 资源和本地库；本轮准备并校验了 100 项类库、1305 项资源。它们放在测试根目录的 `libraries/`、`assets/`、`natives/`，供各实例复用。光影 ZIP 放入各实例自己的 `shaderpacks/`，模组与配置也按实例隔离。

启动参数保存在根目录的 `.args` 文件，由同名 `.cmd` 设置工作目录并调用指定 Java。复用现有参数文件，不重新拼装整条类路径：

| 用途 | 启动入口 | 游戏目录 | 本会话使用的存档 |
| --- | --- | --- | --- |
| 独立光影与 DH | `launch.cmd` | `game/` | `New WorldRenderTest` |
| GC／GS 共存 | `launch-compat.cmd` | `game-compat/` | `New WorldCompatRenderTest` |
| RTG 地形对照 | `launch-rtg.cmd` | `game-rtg/` | `新的世界RTG-Terrain`、`新的世界RTG-Internal` |
| OTG 地形对照 | `launch-otg.cmd` | `game-otg/` | `Biome Bundle` |

例如，PowerShell 启动共存实例：

```powershell
& 'C:\Users\qq106\project\Minecraft\.local\newearth-render\launch-compat.cmd'
```

这些是本机测试启动入口，使用离线测试身份进入单人世界，不经正式启动器登录。后续接手优先复用已有环境；它们未纳入 Git，另一台机器需要重新准备本机依赖。

## 3. 完成光影、远景与现有模组共存验证

先在 `game/` 仅加载 Actinium 和 DH，进入新建普通世界，开启 Reimagined HIGH。通过游戏画面检查树木阴影、天空、云、水面和近远景；通过日志确认加载器、渲染器及 DH 接口加载，通过 LOD 数据库增长确认远景数据实际产生。

验证保存时先正常退出世界、关闭客户端，再重新启动并进入同一存档，核对画面与光影设置。用户实际进入检查近远景和水体，确认当前效果可接受。

随后复制为 `game-compat/`，加入正式客户端使用的 GC 4.0.6、GS 2.2.0 flight-fix、AsmodeusCore 1.0.5、EP 0.8.0、MJRLegendsLib、GSLC 1.4.8、GC Compatibility，以及 FTB／JEI 等现有依赖；补充 Fugue 0.23.4 和 Scalar Legacy 1.0.1。复制相关现有配置后，按相同方法验证进入世界、画面、保存及完整重启重进。

本会话完成的是普通世界共存验证，没有据此验收全部星球、多人或正式客户端。原共存验证也未包含后来加入的 NewEarth 地形原型。

首轮观察参数为 Reimagined HIGH、原版视距 12、DH 半径 256、帧率上限 120。这些用于固定观察条件，尚未确定为最终性能默认值。正式性能比较必须只运行一个测试游戏，并分别测首次生成、缓存完成后的移动和重进。

2026-11-03 接手核对发现实例已有后续改动：`game-compat/` 加入了 `NewEarth-0.2.10.jar` 和“新地球地形原型”存档；`game-rtg/` 当前光影开关为关闭；`game-otg/` 原版视距为 20。继续对照前应重新核对配置，不能按首轮参数假定当前环境一致。

## 4. 建立两组地形对照并作出景观取舍

从共存环境分别复制两个新实例，保留相同渲染组合；RTG 与 OTG 分开安装、分别新建存档，避免生成器混用。

RTG 实例使用 RTG Plus 9.0.4，选择 RTG 世界类型、种子 `20261103`、创造模式。OTG 实例使用 CurseForge 官方 OTG 9.9 与 Biome Bundle 6.1，在 OTG 创建界面选择 Biome Bundle、同一数值种子、创造模式，预生成半径设为 0。跨生成器使用相同种子只固定输入，不表示生成相同地貌。

进入世界后分别从地面看花草、河岸与建设空间，从空中看山体、森林及海岸的整体组合，并完成保存和重启重进。用户可继续留在测试游戏里观察，接手者不要擅自关闭或接管正在玩的窗口。

本会话还读取了解包后的 Biome Bundle 配置和已安装 JAR 的公开接口，确认预设除了地表植被还引用建筑、洞穴等对象；读取 RTG 世界生成参数，确认河湖与树木分布有调整入口。检查接口使用 `javap`，没有修改上游模组。

景观取舍已明确：当前 Biome Bundle 不再作为主要候选。用户更认可 Terralith 中宽大山体与开阔地的关系，但不接受大部分陆地被森林覆盖。地表目标以 Issue 参考图的连续草甸、局部林片、河岸及远山为准，树种和颜色不固定。

## 5. 从参考源码形成 NewEarth 生成方案

阅读 Terralith 仓库的世界生成与群系配置，确认其中引用现代原版生成函数，不能把 JSON 直接放进 1.12.2 使用。因此选择学习景观组织方式，在 NewEarth 内实现范围较小的持续生成方案。

本会话完成的是设计与文档提交：按种子和全局坐标组织大地貌、河湖过渡、独立植被分布和地表装饰；先验证裸地形，再加草甸与森林，随后验证保存、生成开销及光影远景。具体规则维护在 README，避免在接手文件复制另一份设计。

后续会话已实现地形原型，本次读取当前仓库确认代码位于：

```text
project/NewEarth/src/main/java/newearth/dimension/
    Landscape.java
    LandscapeBiomes.java
    LandscapeGenerator.java
    LandscapeWorldType.java
```

原型及其检查不是本会话编写或验收的成果。接续时从已有源码继续，不另造同用途生成器。当前入口是新建世界时选择“新地球地形原型”；具体创建与检查方式见 README 的“创建原型世界”。

当前构建入口已核对为在仓库根目录设置 JDK 8 后运行：

```sh
python3 tools/build.py --target newearth --offline
```

首次缓存不足时去掉 `--offline`。构建脚本会执行 `landscapeCheck`，结果位于 `build/checks/newearth/landscape/`，最终 JAR 位于 `build/libs/`。接手仍需实际看图、进入世界验证；本次仅核对入口，没有重新构建或运行原型。

## 6. 接续工作与仍有效的边界

先复用 `game-compat/` 中已有原型存档观察山体与草甸布局，再按 README 的验收顺序推进。修改生成规则后使用新测试存档或明确隔离的新区域，已有存档不随意更换生成器。终章维度、飞船接入与多人完整旅程最后验证。

光影的固定终章模式与全局启用选项、九阶完成后的低负载地形／DH 准备均仍待实现。后台准备以正常游玩优先，忙时减速或暂停；区块生成、LOD 构建和客户端加载要分别验证。GC Compatibility 的光影选项只控制相关天空开关，不能当作外部光影包的维度切换接口。

DH 近远景之间偶尔出现缺口或竖直断面，根因未确认。用户已接受当前瑕疵并要求停止专项排查；临时可尝试拉大原版视距后再缩小，不保证自动恢复。后续只在出现影响游玩的新问题时重新定位。

本机测试日志、截图与输出在 Windows 仓库 `build/checks/newearth/` 下的 `render/`、`compat/`、`terrain/`；当前日志在各实例 `logs/latest.log`，游戏截图在各实例 `screenshots/`。本机配置、存档和这些输出不提交。检查进程时只读取 PID、窗口标题和响应状态，避免输出正式启动器含访问令牌的完整 Java 命令行。

项目已迁至 `project/NewEarth/`；同步和提交使用当前 `main`，先核对工作区与远端，只提交本任务文件。Issue 按最小匹配项维护，不向总览 Issue 追加操作流水。
