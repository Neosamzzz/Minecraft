# Minecraft 1.12.2

航天玩法整合包，开发项目集中在 `project/`。

| 项目 | 功能 |
| --- | --- |
| [GSLC](project/GSLC/README.md) | 发射控制兼容、遥感、地图与时钟 |
| [FTB](project/FTB/README.md) | 公共团队与任务书 |
| [NewEarth](project/NewEarth/README.md) | 太阳系整合、系外制造与终章开发 |

玩法条目见[整合包玩法 Wiki](docs/modpack-gameplay.md)。

## 快速开始

需要 **64 位 JDK 8**（含 `java` 和 `javac`）、**Python 3.8+** 和 **Git**。FTB 使用项目内的 Gradle Wrapper，GSLC 与 NewEarth 使用共用 GalaxySpace Wrapper，无需安装全局 Gradle 或 Maven。访问本私有仓库需要相应的 GitHub 权限和本机 Git 凭据。

```sh
git clone https://github.com/Neosamzzz/Minecraft.git
cd Minecraft
```

选择对应系统设置 `JAVA_HOME` 并构建；示例 JDK 路径请替换为实际安装目录。

<details open>
<summary>Windows · PowerShell</summary>

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk1.8.0_8u202_x64'
py -3 tools/build.py --target all
```

</details>

<details>
<summary>macOS / Linux</summary>

```sh
export JAVA_HOME=/path/to/jdk8
python3 tools/build.py --target all
```

</details>

`JAVA_HOME` 应指向 JDK 根目录。脚本优先使用该目录中的 Java 工具；未设置时从 `PATH` 查找，并检查版本是否为 Java 8。

> [!NOTE]
> 首次构建需要联网下载工具、依赖并处理 Minecraft 开发类库。已安装的游戏实例与这套开发缓存分开管理；缓存默认位于用户目录的 `.gradle/`，后续构建会复用。

成功后，按命令打印的 `Built:` 路径取包。GSLC 当前为 1.4.8；FTB 文件名以 `project/FTB/gradle.properties` 中的 `mod_version` 为准，不以本文的本机安装版本推定：

```text
build/libs/
├── GSLaunchCompat-1.4.8.jar
├── FTBQuests-<mod_version>.jar
└── NewEarth-<mod_version>.jar
```

## 构建目标

以下命令均在仓库根目录执行。macOS / Linux 将 `py -3` 替换为 `python3`。

| 命令 | 作用 |
| --- | --- |
| `py -3 tools/build.py` | 默认构建 GSLC，保持原有用法 |
| `py -3 tools/build.py --target gslc` | 只构建 GSLC |
| `py -3 tools/build.py --target ftbquests` | 只构建 FTB Quests |
| `py -3 tools/build.py --target newearth` | 只构建 NewEarth，自动运行七阶与太阳系隔离检查 |
| `py -3 tools/build.py --target all` | 依次构建 GSLC、FTB Quests、NewEarth，任一目标失败就停止 |
| `py -3 tools/build.py --help` | 查看完整参数 |

工具与依赖已完整缓存后，可以为任意目标添加 `--offline`：

```powershell
py -3 tools/build.py --target all --offline
```


## 安装

最终包取自 `build/libs/`。退出游戏或停服后替换相应模组；FTB 和 NewEarth 的客户端、服务端使用同版本。NewEarth 同时分发根目录 `config/` 的固定配置。更新已有实例前保留原 JAR、配置与存档，避免覆盖游玩进度。

正式游戏内测试使用 PCL 实例 `C:\Users\qq106\Desktop\游戏\Release\.minecraft\versions\1.12.2-NewEarth-Cleanroom`。客户端 JAR 替换该实例 `mods/` 中的同名文件。

FTB 任务定义由 `project/FTB/quest-config/solar-system/normal/` 分发到实例 `config/ftbquests/normal/`；合入已有任务书前核对章节索引和 ID。

## 固定配置交付约定

`config/` 属于整合包固定玩法，与模组一起交付。后续逐项修改，禁止原版默认、历史备份或测试实例配置整份覆盖正式配置。实例调整比较差异后合入仓库。

## 目录

```text
Minecraft/
├── project/                    # 开发项目
│   ├── GSLC/
│   │   ├── src/                # 补丁源码与资源
│   │   ├── tests/              # 专项检查
│   │   ├── materials/          # 固定构建底包
│   │   ├── build.py            # GSLC 构建逻辑
│   │   ├── version.txt
│   │   ├── README.md
│   │   └── RELEASES.md         # 版本说明
│   ├── FTB/
│   │   ├── src/                # FTB Quests 源码
│   │   ├── tests/
│   │   ├── quest-config/       # 任务配置
│   │   ├── build.gradle
│   │   ├── build.py            # FTB 构建逻辑
│   │   ├── gradle.properties   # 版本与依赖配置
│   │   ├── README.md
│   │   └── RELEASES.md
│   └── NewEarth/
│       ├── src/                # 模组源码与测试
│       ├── items/              # 物品与制造设计
│       ├── dimension/          # 维度与演出设计
│       ├── build.gradle
│       ├── build.py            # NewEarth 构建逻辑
│       ├── gradle.properties
│       ├── README.md
│       └── RELEASES.md
├── dependencies/               # 上游源码与共用编译依赖
│   ├── GalaxySpace/
│   ├── Galacticraft-Legacy/
│   ├── ExtraPlanets/
│   └── upstream.json           # 上游来源
├── tools/                      # 统一构建入口与检查
├── config/                     # 整合包运行配置
├── docs/                       # 共用玩法与资源资料
│   ├── research/
│   └── README.md
├── build/
│   ├── libs/                   # 最终模组包
│   └── checks/                 # 按模块划分的检查输出
├── .local/                     # 本机环境
├── server/                     # 服务端实例
├── AGENTS.md
└── README.md
```

共用资料从 [docs](docs/README.md) 查阅。构建工具检查：`python3 -m unittest discover -s tools/tests`。GSLC 路径检查：`python3 -m unittest discover -s project/GSLC/tests`。
