# 服务端无人暂停

`NewEarth-Server-Pause-1.0.0.jar` 是独立的 Minecraft 1.12.2 服务端补丁。仅放入服务端 `mods/`，客户端不安装，普通 NewEarth JAR 无需替换。

服务器正常准备世界和出生区后，无人在线即等待；第一位玩家进入后运行，最后一位玩家离线后立即保存并暂停，再有人进入时恢复。已有存档的时间和进度继续保留，重启后同样按在线人数判断。

暂停从 `MinecraftServer.tick()` 入口生效，跳过世界、实体、方块实体、全服 Tick 计数及 Forge 服务端／世界 Tick 事件。网络登录、主线程任务、异步区块加载回调和专用服务器控制台继续处理，因此仍可进服、查询、保存和正常 `stop`。模组自行创建的后台线程不受模拟 Tick 暂停控制。

## 构建与安装

使用 JDK 8，在仓库根目录执行：

```sh
python3 tools/build.py --target newearth-server --offline
```

构建复用 NewEarth 的 Forge 编译环境，运行状态转换及真实 MinecraftServer 字节码检查。最终包在 `build/libs/`，检查输出在 `build/checks/newearth/server-pause/`；不构建或改动地形及客户端包。

正常停服后把 JAR 放入服务端 `mods/`，启动后查看日志中的 `NewEarth Server Pause`。无人时出现 `Paused`，有人进入后出现 `Resumed`。控制台两次执行 `time query gametime` 可核对无人时世界时间保持不变。移除该 JAR 并重启即可恢复持续运行。
