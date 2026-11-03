# FTB

基于 FTB Quests 1.12 的公共团队与任务书。版本由 `gradle.properties` 定义。

## 公共团队

配置文件为游戏实例的 `config/ftbquests-shared-team.cfg`，修改后重启。客户端与服务端使用同版本模组。

- 新世界创建固定的 `ftb_public` 团队；已有世界优先复用唯一有任务数据的团队，只有一个现有团队时直接复用。
- 选中的团队 ID 写入该存档的 FTB universe 数据，后续启动继续使用；任务进度与领奖记录仍保存在原团队中。
- 多个团队都有任务数据时，停止自动选队并在日志提示；在配置的 `team_id` 填写要复用的既有团队 ID 后重启。其余旧队数据保留，不自动合并。
- 除“任务”外的所有 FTB 侧栏入口隐藏，包括“我的组”、客户端设置和管理面板，队伍成员、邀请、盟友、敌人、退队及转让等 action 收起；服务端同时拦截 `/team` 的队伍变更命令。
- 任务书内提供“编辑任务 / 结束编辑”开关，开启后显示编辑设置；所有公共团队成员都可操作，不需 OP。编辑状态按玩家保存，退出编辑后恢复普通浏览。任务定义也可停服后通过配置文件维护。
- 奖励沿用现有配置，不新增奖励；普通奖励默认个人领取，可重复任务仍受原版团队奖励规则约束。
- `enabled=false` 并重启可恢复原有入口和权限规则，但不会自动把已归队的玩家迁回原队。上线前应备份服务器存档。
## 任务内容

任务配置位于 `quest-config/solar-system/normal/`：六章、44 个可见目标，另 16 项保留原 ID 放在兼容章，无奖励。

- [任务定稿](ftb-quest-content.md)：采用的文字、目标和编排。
- [后期任务预备稿](ftb-quest-manuscript.md)：尚未接入的七阶及后期节点。
- [物品核对](quest-item-notes.md)：任务选材与获取条件。
- [配置安装](quest-config/solar-system/README.md)。

## 构建与检查

仓库根目录设置 JDK 8，执行 `python3 tools/build.py --target ftbquests`。最终包位于根目录 `build/libs/`。

独立运行公共团队检查：

```sh
bash project/FTB/gradlew -p project/FTB --offline --no-daemon --console=plain -I tests/shared_team/probe.gradle runSharedTeamProbe
```

Gradle 4.9、ForgeGradle 2.3；详细依赖与任务数据结构保留在 `DEVELOPMENT.md`。

## TODO

- [ ] 验证双人自动同队、共享完成和新玩家接续。
- [ ] 验证物品、Boss 与到达条件。
- [ ] 验证退出重进、服务器重启及旧团队数据保留。
