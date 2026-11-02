# 构建固定材料

本目录保存当前构建流程必需的固定输入，与 `build/` 中可重新生成的输出分开管理。

| 文件 | 用途 | 来源与维护入口 |
| --- | --- | --- |
| `GSLaunchCompat-1.3-public-controller.jar` | GSLC 打包底包，保留已有飞行与公共控制器补丁 | 项目保留的 GSLC 1.3 公共控制器基线；补丁与打包方式见 [公共控制器说明](../GalaxySpace/tests/public_controller/README.md)，历史版本查该文件的 Git 记录 |

`tools/build.py` 将该底包与当前编译的 GalaxySpace 内容交给遥测打包脚本，生成 `build/libs/` 中的当前版本 GSLC。文件名的 1.3 是底包版本，不是最终交付版本。调整底包时需验证原有补丁保留及新的候选包；日常清理不要删除该文件。

NewEarth 使用 `NewEarth/build.gradle`，复用 GalaxySpace 的 Gradle Wrapper 和 `GalaxySpace/libs/Galacticraft-1.12.2-4.0.2.283.jar` 编译依赖，通过统一入口 `--target newearth` 构建。它不使用 GSLC 底包，不新增固定材料；GS/EP 仍由运行环境提供，不打进 NewEarth JAR。太阳系检查读取根目录 `config/` 的固定运行配置，并在检查输出目录中解析副本；这些配置随整合包分发，不属于本目录的构建二进制材料。

新增材料时在此登记用途及来源，临时下载、测试日志和本机路径配置分别放入 `.local/` 或 `build/checks/`，不堆积到此目录。完整规则见[项目目录规范](../docs/project-layout.md)。
