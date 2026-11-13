# 落苗生根服务端修复

基于 [Saplanting 1.12.2 0.2](https://modrinth.com/mod/saplanting/version/aqCYcTNx)（CC0-1.0）制作最小二进制补丁。上游源码：[1.12.2 分支](https://github.com/MUYUTwilighter/saplanting_forge/tree/1.12.2)。

将两处仅客户端可用的 `EntityItem.getAge()` 调用改为通过 Forge ReflectionHelper 读取原始 `age` 字段，保留掉落物的年龄和过期规则。默认关闭后台线程，种植检查与放置方块在服务端主线程执行。

使用 JDK 8 执行 `py -3 tools/build.py --target saplanting`。构建固定校验官方原包 SHA1，缓存位于 `.local/saplanting/`，ASM 9.2 使用共用 Gradle 缓存。最终文件为 `build/libs/saplanting-forge-1.12.2-0.2-serverfix.jar`。

停服后替换原 Saplanting JAR，只保留一个版本。已有 `config/saplanting.json` 应设置 `"multiThread": false`。模组安装在服务端即可，客户端可选。保留默认种植条件：树苗落在适宜地面、附近没有阻挡种植的树木，且玩家离开检查范围后自动种植。
