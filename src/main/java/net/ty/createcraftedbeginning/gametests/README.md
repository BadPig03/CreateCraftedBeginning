# GameTests

所有 GameTest、测试注册入口和测试辅助类统一放在本目录及其子目录，包名统一以 `net.ty.createcraftedbeginning.gametests` 开头，目录与包声明保持一致。

目录组织、跨包访问权限、内部接口标记、迁移与验证要求统一以 [Mod 统一代码规范第三节「GameTest 集中管理」](../../../../../../../docs/code-style-draft.md#gametest-集中管理) 为准。后续修改规则维护该规范正文。

从仓库根目录运行普通测试：

```powershell
./gradlew.bat runGameTestServer
```

包含已配置的可选联动模组：

```powershell
./gradlew.bat runGameTestServer -PcompatProfile=full
```

性能基准默认关闭，需要通过 `benchmarks/gas-network/run.ps1` 显式运行。详见 [气体网络性能基准说明](../../../../../../../benchmarks/gas-network/README.md)。
