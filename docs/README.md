# FTB 资料区

FTB 是整合包主线之外的一条大支线。主线本身是原本要玩的 GC 玩法，加上为完善体验做的自行修补；FTB 负责把已有玩法的发展过程可视化，让一起玩的朋友共同推进、看得见全服进度。

本目录只管 FTB 这一块，对应 [Issue #7：梳理并配置好游戏内的 FTB 相关功能](https://github.com/Neosamzzz/Minecraft/issues/7) 和看板 [FTB移植](https://github.com/users/Neosamzzz/projects/3)。GSLC 补丁与本目录无关，其记录见 [`../changelog/`](../changelog/README.md)。

核对日期：2026-11-02。本文集中记录文档、内容与实现状态；运行观察统一回填[玩法详解](research/planet-gameplay-guide.md#十三2026-11-02-运行日志补记)。

## 当前进展

- 公共团队最小实现已提交为 1202.9.1。本机已构建并安装 1202.9.3，包含关闭游戏内编辑、无奖励详情精简；后两版代码仍在代码线程工作区，未随此次文档推送提交。版本边界见[根目录说明](../README.md#当前进展)。
- 本机最新日志两次读取到 3 章、3 任务、3 条件、0 奖励，并记录公共团队 `singleplayer`。这能证明小样加载和单人选队，不能代替实际双人共享与 GUI 验收。
- 七章 53 个候选节点及 H01 终章占位已整理；只有三个节点小样转成配置并部署，完整任务书尚未部署。
- 星球研究保留完整细节。GS 系外当前注册 T6、EP Kepler 当前 T10；系外统一 T7、复用七阶及全新八阶属于[新地球物品线](../NewEarth/items/README.md)规划。
- 新地球拆为 `codex/newearth-items` 与 `codex/newearth-dimension`。既有系外按最低 T7 规划，终章新维度确定最低 T8、使用八阶火箭抵达；方向文档已有，火箭与新维度尚未实现。

## 目标

把 FTB 接入熟人服，让朋友共同推进、看得见全服进度，并知道接下来能做什么。入口和面板的用途清楚，普通玩家、管理、配置各归其位。

## 主基调

1. **按现有能力决定配置和改造。** 源码能力及公共团队接入已整理，剩余入口和面板仍需实际游戏核对；取舍以当前证据为前提。
2. **内容不追求打磨。** 星球资料已经足够，任务内容直接按现有资料组织，写出能用的版本就进入验证，不安排"试玩→反馈→调优"的迭代循环。
3. **重心放在功能梳理、团队机制和落地验证。** 这三块决定朋友能不能顺畅一起玩，优先于内容量。
4. **能用配置解决的不用代码。** 改动范围尽量小，复用 FTB 现有系统，不另造一套。
5. **不把未验证的能力写成承诺。** 静态调查、探针测试与游戏内实测分别记录。

## 工作路线

| 阶段 | 做什么 | 完成标志 | 对应 |
| --- | --- | --- | --- |
| 1. 摸清现状 | 核对全部入口、面板及服务端既有数据；源码能力已整理，实际界面与远程数据待补 | 功能清单：玩家用 / 管理用 / 用不上 | [#9](https://github.com/Neosamzzz/Minecraft/issues/9) |
| 2. 定用法 | 已定公共团队、配置维护与无奖励内容；其他入口按用途核对 | 改动清单：配置、代码、不做 | [#10](https://github.com/Neosamzzz/Minecraft/issues/10) |
| 3. 落地 | 最小实现、构建与隔离检查已有；补双人联机和旧存档验收 | 两人自动同队、同步并保存进度 | [#11](https://github.com/Neosamzzz/Minecraft/issues/11)、[方案](ftb-shared-team-plan.md) |
| 4. 照资料铺内容 | 从 GC 基础起步，按实际材料关系组织并行分支；已有文稿与三节点小样 | 一套能跑的任务书 | [#12](https://github.com/Neosamzzz/Minecraft/issues/12)、[内容方向](ftb-content-direction.md) |
| 5. 验证与说明 | 继续核对界面、判定、共享与重启保存；维护说明已补，验收未完成 | 实测记录 + 操作说明 | [#13](https://github.com/Neosamzzz/Minecraft/issues/13) |

阶段 1、2 主要服务维护者与协作者；阶段 3 起是玩家能感知的部分。阶段 3 与阶段 4 可以并行，但真正开玩要等阶段 3。每阶段验收后立即进游戏验证，不攒到最后补。

## 文档

| 文件 | 作用 | 状态 |
| --- | --- | --- |
| [ftb-shared-team-plan.md](ftb-shared-team-plan.md) | 团队系统怎么做：固定公共团队、自动归队、共享进度、分别领奖 | 最小代码与 GUI 已实现，构建及隔离探针通过，待游戏内联机验收 |
| [ftb-content-direction.md](ftb-content-direction.md) | 任务书内容做成什么样：轻量、有推进感、不设奖励 | 已对齐原版进度式目标与轻量主线；具体节点见内容稿 |
| [ftb-quest-manuscript.md](ftb-quest-manuscript.md) | 任务内容稿：节点标题、短文案、完成目标与编排备注 | 基础 7 章、53 个候选节点及 1 个终章占位；已有小样，完整内容尚未整体转配置或验收 |
| [任务小样](../quest-config/preview/README.md) | 三节点配置、增量部署与恢复说明 | 已部署本机；日志确认加载，显示与完成判定待验 |
| [research/planet-gameplay-audit.md](research/planet-gameplay-audit.md) | 星球与玩法的静态总览：配置、维度、共存问题 | 静态调查完成，待游戏内实测 |
| [research/planet-gameplay-guide.md](research/planet-gameplay-guide.md) | 逐个目的地的详细玩法和内容依据 | 静态调查完成，待游戏内实测 |
| [research/extraplanets-source-notes.md](research/extraplanets-source-notes.md) | EP 源码来源、版本对应、字节码核对与源码入口 | 已导入 0.8.0 上游快照，未接入构建 |

依据资料用来查，不需要通读。真正对接任务设计的是详解的[第十一节「交给 FTB 与向导文本线程的使用规则」](research/planet-gameplay-guide.md#十一交给-ftb-与向导文本线程的使用规则)，先看那一节即可。

阶段 1、2 的源码依据见 [FTB 开发说明](../FTB-Quests/DEVELOPMENT.md#任务功能与数据位置)，操作取舍见[公共团队方案](ftb-shared-team-plan.md)。维护与验证分别见开发说明、小样说明和对应 issue，不再复制一份任务清单。

## 状态记录口径

- 文档状态统一写在本页的文档表里，各文档开头不再单独维护状态行。
- 游戏内实测结果**只回填到 `research/planet-gameplay-guide.md` 的对应条目**。
- `research/planet-gameplay-audit.md` 只保留配置、维度注册这类静态事实，不写实测结论，避免两份文档各自演化。
- 实测时按详解第十二节的记录格式留档。

## 边界

- 本目录只记 FTB 相关内容；GSLC、构建与安装见根目录 [README](../README.md) 和 [`../changelog/`](../changelog/README.md)。
