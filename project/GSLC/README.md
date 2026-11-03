# GSLaunchCompat

GC／GS 的发射控制兼容与遥感显示补丁，提供目标维度、火箭状态、地表地图和右上角游戏时间。

## 构建

仓库根目录设置 JDK 8 后执行 `python3 tools/build.py --target gslc`。版本由本目录 `version.txt` 定义，最终包输出到根目录 `build/libs/`。

`build.py` 使用共用 GalaxySpace 工程编译本目录 `src/` 中的补丁源码，再由打包器选取遥感类、三个显示 Mixin 和语言资源，合入 `materials/GSLaunchCompat-1.3-public-controller.jar`。固定底包保留发射与公共控制器补丁，不能当缓存清理。

## 使用

客户端和服务端安装 GSLC。服务端使用 1.4.1 及以上版本时，后续显示更新只需更新客户端；从 1.3 升级需两端同时更新。

遥感目标重载存档后恢复链接，失联时显示不可用。地图仅绘制客户端已加载区块，每 30 秒扫描一次；区块卸载保留已绘地形，切换世界或维度不沿用旧地图。

## 检查

准备根目录 `.local/probe-runtime/` 的运行依赖与 `classpath.json`，先构建当前版本，再从仓库根目录执行：

```sh
python3 project/GSLC/tests/launch_controller/run.py
python3 project/GSLC/tests/telemetry_dimension/run_probe.py
python3 project/GSLC/tests/telemetry_dimension/run_surface_probe.py
python3 project/GSLC/tests/public_controller/run_probe.py
```

检查输出位于 `build/checks/gslc/`。实际画面、性能和多人航行仍需游戏内验证。
