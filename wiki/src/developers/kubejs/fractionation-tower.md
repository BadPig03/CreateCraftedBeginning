---
title: 气密分离塔
description: 气密分离塔KubeJS配方：混合原料、分层产物与温度条件。
editLink: false
---

# 气密分离塔 {#fractionation-tower}

**语法：** `fractionation_tower(inputs[])`

::: info 用途
气密分离塔通过加热分馏或冷却冷凝，将产物送到不同塔层。
:::

**功能：**

- 输入可混合物品、流体和气体，至少1项。
- 输入每类最多64项；输出一共2~8项。
- 使用`.processingTime(...)`设置加工时间，单位为tick，需要为正整数，默认为200tick。
- 支持[温度档位与匹配方式](#temperature)和[气体标签和压力条件](./gas-conditions.md)。

## 分层产物 {#layer-outputs}

- 使用`.outputAt(层偏移, 产物)`指定出料层。
- 进料层的偏移为`0`：加热从塔底向上数，而冷凝从塔顶向下数。
- 出料的偏移量只能取`1~8`的整数。
- 每个出料层只能设置1项产物，可以跳层。
- 分离塔的高度至少为最大偏移量加1。

以4层高的分离塔为例，表中的数字表示层偏移：

| 塔层     | 加热（分馏）  | 冷却（冷凝）  |
|--------|:-------:|:-------:|
| 塔顶     | `3`（出料） | `0`（进料） |
| 塔顶下方一层 | `2`（出料） | `1`（出料） |
| 塔底上方一层 | `1`（出料） | `2`（出料） |
| 塔底     | `0`（进料） | `3`（出料） |

## 温度档位与匹配方式 {#temperature}

- 使用`.temperature(...)`选择温度档位，使用`.temperatureMatching(...)`设置匹配方式。
- 温度档位不能为`none`，不允许省略；匹配方式默认为`exact`。

| 温度档位           | `exact`（精确匹配） | `compatible`（兼容匹配） |
|----------------|---------------|--------------------|
| `chilled`      | 仅冷却           | 冷却或超级冷却            |
| `superchilled` | 仅超级冷却         | 仅超级冷却              |
| `heated`       | 仅加热           | 加热或超级加热            |
| `superheated`  | 仅超级加热         | 仅超级加热              |

## 示例 {#recipe}

```js
ServerEvents.recipes(event => {
    const ccb = event.recipes.createcraftedbeginning

    // 100mB硫磺火→500mB熔岩+500GU炽热空气
    // 需要超级加热，在塔底上方第1、2层出料
    ccb.fractionation_tower([
        Fluid.of('createcraftedbeginning:brimstone', 100)
    ]).outputAt(1, Fluid.of('minecraft:lava', 500))
        .outputAt(2, CCBGas.of('createcraftedbeginning:ultrawarm_air', 500))
        .temperature('superheated')
        .id('tutorial:tower_brimstone')

    // 10000GU潮湿空气→9800GU自然空气+100mB水
    // 需要冷却，在塔顶下方第1、2层出料
    ccb.fractionation_tower([
        CCBGas.of('createcraftedbeginning:moist_air', 10000)
    ]).outputAt(1, CCBGas.of('createcraftedbeginning:natural_air', 9800))
        .outputAt(2, Fluid.of('minecraft:water', 100))
        .temperature('chilled')
        .processingTime(100)
        .id('tutorial:tower_moist_air')

    // 1冰+100mB水+1000GU潮湿空气→1雪块+900GU自然空气
    // 需要冷却或超级冷却，在塔顶下方第1、3层出料，至少需要4层
    ccb.fractionation_tower([
        Ingredient.of('minecraft:ice'),
        Fluid.of('minecraft:water', 100),
        CCBGas.of('createcraftedbeginning:moist_air', 1000)
    ]).outputAt(1, 'minecraft:snow_block')
        .outputAt(3, CCBGas.of('createcraftedbeginning:natural_air', 900))
        .temperature('chilled')
        .temperatureMatching('compatible')
        .id('tutorial:tower_mixed')
})
```
