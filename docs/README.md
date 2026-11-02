# FTB 与星球研究资料

FTB 用任务书呈现整合包已有玩法的发展，让朋友共同推进、看见全服进度。星球研究同时服务整合包和 [NewEarth](../NewEarth/README.md)，不局限于 FTB。

关联 [Issue #7](https://github.com/Neosamzzz/Minecraft/issues/7) 与 [Project「FTB移植」](https://github.com/users/Neosamzzz/projects/3)。代码、配置和文档统一在 `main` 维护。

## 当前进展

公共团队及任务书界面已有实现，太阳系任务已按定稿表部署；双人共享、目标判定与重启后进度保存仍待验收。版本和构建、界面验证见 [FTB 开发记录](../changelog/ftb-quests.md)，部署范围与恢复方式见[太阳系配置](../quest-config/solar-system/README.md)。七阶起的后期内容尚未接入。

## 工作路线

| 阶段 | 工作与完成标志 | 对应 |
| --- | --- | --- |
| 摸清现状 | 核对入口、面板、任务能力和服务端数据，区分玩家、管理与配置用途 | [#9](https://github.com/Neosamzzz/Minecraft/issues/9) |
| 确定用法 | 明确公共团队、编辑与入口取舍，区分配置、代码改动和不做的内容 | [#10](https://github.com/Neosamzzz/Minecraft/issues/10)、[公共团队方案](ftb-shared-team-plan.md) |
| 落地公共团队 | 两人自动同队、同步进度，并验证重进、重启和旧数据保留 | [#11](https://github.com/Neosamzzz/Minecraft/issues/11) |
| 组织任务内容 | 按现有资料维护轻量、无奖励的任务，核对配置与定稿一致 | [#12](https://github.com/Neosamzzz/Minecraft/issues/12)、[任务定稿表](ftb-quest-content.md) |
| 验证与说明 | 验证显示、条件判定、共享及保存；维护必要的使用说明 | [#13](https://github.com/Neosamzzz/Minecraft/issues/13)、[FTB 开发说明](../FTB-Quests/DEVELOPMENT.md) |

已有玩法资料用于查阅，内容够用即可进入验证；优先使用现有配置和任务类型，不为丰富文档额外开发系统，也不把构建、静态检查当成游戏验收。

## 文档分工

| 文档 | 维护内容 |
| --- | --- |
| [整合包玩法定稿](modpack-gameplay.md) | 整合包采用的玩法规则，包含路线、星球、解锁、材料、设备与公共进度；示例初版持续补全 |
| [公共团队方案](ftb-shared-team-plan.md) | 自动同队、共享进度、编辑入口、旧数据处理与联机验收要求 |
| [任务内容方向](ftb-content-direction.md) | 轻量主线、任务选取与表达原则 |
| [任务定稿表](ftb-quest-content.md) | 当前采用的节点、游戏内文字、目标与编排；不再另写逐条操作手册 |
| [太阳系配置](../quest-config/solar-system/README.md) | 实际配置、ID 映射、部署和恢复；兼容定义的保留原因 |
| [任务预备稿](ftb-quest-manuscript.md) | 新太阳系调整候选及七阶、系外、多人飞船、开普勒与终章预留，不重复当前定稿 |
| [任务物品核对](research/quest-item-notes.md) | 任务选材、注册 ID、配方与获取限制的依据 |
| [星球静态调查](research/planet-gameplay-audit.md) | 首轮配置、维度注册和共存问题的调查快照 |
| [星球玩法详解](research/planet-gameplay-guide.md) | 共用的资源、采集限制、加工与玩法事实；相关运行观察回填对应条目 |
| [EP 源码说明](research/extraplanets-source-notes.md) | 上游来源、版本对应、字节码核对范围与源码入口 |

FTB 界面、功能与构建验证结论随版本维护在[开发记录](../changelog/ftb-quests.md)，星球资源、环境和航行实测维护在玩法详解。日志、截图等检查输出按[目录规范](project-layout.md)保存，不另建逐次测试文档。静态调查保留原快照，不随运行记录改写。

GSLC、构建与安装见[根 README](../README.md)和[开发记录](../changelog/README.md)；新地球设计由 `NewEarth/` 维护，本目录不重复保存开发任务清单。
