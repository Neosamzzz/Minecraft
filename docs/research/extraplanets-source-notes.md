# ExtraPlanets 源码导入与证据说明

记录日期：2026-11-02。

## 来源与用途

| 项目 | 内容 |
| --- | --- |
| 上游 | [MJRLegends/ExtraPlanets](https://github.com/MJRLegends/ExtraPlanets) |
| 分支 | `dev_1.12.2` |
| 导入提交 | [fc4a4cb830897da032477210db458a0567948957](https://github.com/MJRLegends/ExtraPlanets/tree/fc4a4cb830897da032477210db458a0567948957) |
| 目录 | [ExtraPlanets](../../ExtraPlanets/README.md) |
| 版本声明 | `build.gradle` 为 `1.12.2-0.8.0`，`Constants.java` 为 0.8.0 |
| 当前用途 | 阅读和核对天体、材料、注册条件与兼容行为；尚未接入统一构建 |

本机已安装包为 `ExtraPlanets-1.12.2-0.8.0.jar`。此次选择版本声明相同的 `dev_1.12.2`；上游的 `dev_1.12.2_fg3` 和 `dev_1.12.2_fg5` 当前仍声明 0.7.4，没有按构建工具新旧替换版本。

导入的是该提交的完整受版本控制文件，采用与本仓库其他模组相同的目录快照方式，不带嵌套 `.git`。上游 README、许可说明、源码、资源和构建文件保留原样；导入来源同时记入根目录 [upstream.json](../../upstream.json)。后续注释放在本资料区，避免修改上游快照。

版本号相同不证明整个源码提交与发行 JAR 完全对应。本次没有重建 EP，也没有证明二进制可重现；具体结论仍结合本机 JAR 的字节码和配置核对。

## 证据口径

- 2026-11-01 首轮调查时尚未导入 EP 源码，注册条件等结论来自本机实际 JAR 的 `javap -c -p` 输出，作者 Wiki 只作补充。
- 2026-11-02 已有本地源码，可直接追踪方法、注册和配置关系。既有字节码证据继续保留，用于确认实际安装包的关键分支。
- 本机 EP JAR 的调查基准 SHA-256：`60c0a1726c799d5dce3838272be644ffd07e102152befbc274cfdf48452837b9`。
- 已核对的重点包括 Kepler-22b T10 要求、普通及电动 T10 注册条件、Phobos 生成器和其化石方块初始化条件；没有完成全项目逐类或全部配方比对。
- 配置开启、代码存在、运行中可用分别记录。源码导入不代表地形、飞行、装备或生存路线已经验收。

## 常用源码入口

| 内容 | 文件 |
| --- | --- |
| 版本号 | [Constants.java](../../ExtraPlanets/src/main/java/com/mjr/extraplanets/Constants.java)、[build.gradle](../../ExtraPlanets/build.gradle) |
| 开关与兼容配置 | [Config.java](../../ExtraPlanets/src/main/java/com/mjr/extraplanets/Config.java) |
| 行星、Kepler 与等级 | [ExtraPlanets_Planets.java](../../ExtraPlanets/src/main/java/com/mjr/extraplanets/planets/ExtraPlanets_Planets.java) |
| 卫星与 Phobos | [ExtraPlanets_Moons.java](../../ExtraPlanets/src/main/java/com/mjr/extraplanets/moons/ExtraPlanets_Moons.java) |
| T10 火箭与物品注册 | [ExtraPlanets_Items.java](../../ExtraPlanets/src/main/java/com/mjr/extraplanets/items/ExtraPlanets_Items.java) |
| 方块注册及 FOSSIL | [ExtraPlanets_Blocks.java](../../ExtraPlanets/src/main/java/com/mjr/extraplanets/blocks/ExtraPlanets_Blocks.java) |
| Phobos 矿物生成 | [BiomeDecoratorPhobos.java](../../ExtraPlanets/src/main/java/com/mjr/extraplanets/moons/Phobos/worldgen/BiomeDecoratorPhobos.java) |
| Kepler 通行与环境 | [WorldProviderKepler22b.java](../../ExtraPlanets/src/main/java/com/mjr/extraplanets/planets/Kepler22b/WorldProviderKepler22b.java) |

## 核对注释

### Kepler-22b 与 T10

行星注册调用 `setTierRequired(10)`，Provider 将传入火箭等级与天体等级比较。普通 T10 在 `Config.ERIS` 分支中初始化；电动 T10 还同时要求 `KEPLER22B` 和 `ELECTRIC_ROCKET`。这些条件与此前实际 JAR 的字节码调查一致。只开启 Kepler 或电动火箭，不会恢复关闭的 T10 火箭链。

### Phobos 对 FOSSIL 的隐含依赖

`ExtraPlanets_Blocks` 仅在下列条件满足时初始化 `FOSSIL`：

```java
Config.MERCURY || Config.CERES || Config.PLUTO || Config.ERIS
```

`BiomeDecoratorPhobos` 却在构造函数中无条件用 `FOSSIL` 创建化石生成器；它位于 `GENERATE_ORES_PHOBOS` 判断之外。关闭上述四颗 EP 天体、保留 Phobos 时，会留下对未初始化方块的引用。源码分支已与实际 JAR 核对。

这解释了为何关闭某些 EP 主星还可能影响保留卫星的生成器；也说明仅关闭 Phobos 的普通矿物开关不足以绕过该引用。对应运行证据与待验证状态统一记录在[玩法详解的 EP 火卫一条目](planet-gameplay-guide.md#23-火卫一-phobosep先辨明入口再决定内容)。此次只导入和注释，没有修补该逻辑或改变星球配置。

## 构建边界

上游构建使用 ForgeGradle 2.3、JDK 8，包含旧仓库地址与本地 Galacticraft 开发 JAR 引用；此次没有修复依赖或建立 EP 构建环境。根目录 `tools/build.py --target all` 仍只构建 GSLC 和 FTB Quests。

本次文档同步将此前导入的原样快照与来源记录一并推送，便于 GitHub 上直接阅读研究链接；这不代表已发布修改后的 EP 模组。后续七阶复用与八阶新造的方向另见 [NewEarth 物品线](../../NewEarth/items/README.md)，不在本快照里提前实现。
