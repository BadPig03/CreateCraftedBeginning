---
title: 气密反应釜
description: 气密反应釜KubeJS配方：混合物品、流体和气体，设置加工时间与温度条件。
editLink: false
---

# 气密反应釜 {#reactor-kettle}

**语法：** `reactor_kettle(outputs[], inputs[])`

::: info 用途
气密反应釜混合加工釜内的物品、流体和气体，也可进行纯流体或纯气体反应。
:::

**功能：**

- 输入和输出均可混合物品、流体和气体，每侧至少1项。
- 输入最多64项物品、3项流体和3项气体；输出最多4项物品、2项流体和2项气体。
- 使用`.processingTime(...)`设置加工时间，单位为tick，传入值必须为正整数。省略此调用时，加工阶段按1tick计时。
- 支持[温度档位与匹配方式](#temperature)、[气体标签和压力条件](./gas-conditions.md)和概率物品产物。

## 温度档位与匹配方式 {#temperature}

- 使用`.temperature(...)`选择温度档位，使用`.temperatureMatching(...)`设置匹配方式。
- 温度档位默认为`none`，匹配方式默认为`exact`。

| 温度档位           | `exact`（精确匹配） | `compatible`（兼容匹配） |
|----------------|---------------|--------------------|
| `none`         | 仅常温           | 所有温度               |
| `chilled`      | 仅冷却           | 冷却或超级冷却            |
| `superchilled` | 仅超级冷却         | 仅超级冷却              |
| `heated`       | 仅加热           | 加热或超级加热            |
| `superheated`  | 仅超级加热         | 仅超级加热              |

## 示例 {#recipe}

```js
ServerEvents.recipes(event => {
    const ccb = event.recipes.createcraftedbeginning

    // 1旋风核心+1石头类原料→1000GU自然空气+1砂砾(25%概率)，返还旋风核心
    // 需要常温下加工
    ccb.reactor_kettle([
        'createcraftedbeginning:breeze_core',
        CCBItem.of('minecraft:gravel').withChance(0.25),
        CCBGas.of('createcraftedbeginning:natural_air', 1000)
    ], [
        'createcraftedbeginning:breeze_core',
        Ingredient.of('#c:stones')
    ]).processingTime(50)
        .id('tutorial:reactor_natural_air')

    // 1石头+100mB熔岩+100GU自然空气→25mB水+50GU炽热空气+1黑曜石(50%概率)
    // 需要在冷却或超级冷却状态下加工
    ccb.reactor_kettle([
        CCBItem.of('minecraft:obsidian').withChance(0.5),
        Fluid.of('minecraft:water', 25),
        CCBGas.of('createcraftedbeginning:ultrawarm_air', 50)
    ], [
        'minecraft:stone',
        Fluid.of('minecraft:lava', 100),
        CCBGas.of('createcraftedbeginning:natural_air', 100)
    ]).processingTime(160)
        .temperature('chilled')
        .temperatureMatching('compatible')
        .id('tutorial:reactor_mixed')

    // 100GU自然空气→30GU炽热空气+20GU高能炽热空气
    // 所有温度下均可加工
    ccb.reactor_kettle([
        CCBGas.of('createcraftedbeginning:ultrawarm_air', 30),
        CCBGas.of('createcraftedbeginning:energized_ultrawarm_air', 20)
    ], [
        CCBGas.of('createcraftedbeginning:natural_air', 100)
    ]).processingTime(100)
        .temperature('none')
        .temperatureMatching('compatible')
        .id('tutorial:reactor_gas_only')
})
```
