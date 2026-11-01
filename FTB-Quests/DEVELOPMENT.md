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

```powershell
.\gradlew.bat --no-daemon --console=plain build
```

产物在 `build/libs/`：

- `FTBQuests-1202.9.0.jar`：重混淆后的模组 JAR。
- `FTBQuests-1202.9.0-sources.jar`：源码包。

设置了 `BUILD_NUMBER` 时，版本名会追加构建编号。依赖和工具默认缓存到用户目录的 `.gradle/`。

编译成功后，仍需在独立的游戏实例中验证运行行为。

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
