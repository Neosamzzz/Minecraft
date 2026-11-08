# NewEarth

Minecraft 1.12.2 的 GC／GS／EP 整合附加模组，提供固定太阳系组合、阶段准入、星图调整、内容裁剪、七／九／十阶制造与终章维度。

## 构建与安装

旧 ForgeGradle 使用 JDK 8 调度，地形核心另需 JDK 25（设置 `NEWEARTH_JAVA_HOME`；Windows 可自动发现 Minecraft 的 java-runtime-epsilon）。仓库根目录执行：

```sh
python3 tools/build.py --target newearth
```

构建入口先从仓库内 GalaxySpace 源码生成开发依赖，再构建 NewEarth；两者共用已有 Gradle Wrapper。版本来自 `gradle.properties`，最终包位于根目录 `build/libs/`。客户端与服务端安装同版本 JAR，并同步根目录 `config/` 中的 EP 与 GS 配置。运行依赖 GC、GS 2.2.0、EP 0.8.0 及其依赖模组；新地形原型使用 Cleanroom + Java 25。

1.0 前的存档兼容规则见[根 README](../../README.md#安装)。

配置属于整合包固定玩法，逐项维护；禁止用原版默认、旧备份或测试实例配置整份覆盖。实例调整需比较差异后合入正式配置。

## 开发内容

- [物品与制造](items/README.md)：当前功能与未完成事项。
- [制造路线](items/route-manuscript.md)：后续图纸、制造与引擎设计。
- [EP 内容清单](items/ep-content-inventory.md)：物品与设备取舍。
- [终章维度](dimension/README.md)：景观、世界生成与结尾演出。
- [版本说明](RELEASES.md)。
