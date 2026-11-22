---
title: 注气室
description: 注气室KubeJS配方：物品、流体与概率产物示例。
editLink: false
---

# 注气室 {#gas-injection}

**语法：** `gas_injection(output, inputs[])`

::: info 用途
注气室使用传送带或置物台加工物品；涉及流体的配方使用工作盆。
:::

**功能：**

- 输入为1项物品原料或流体原料，以及1项气体原料。
- 输出为1项物品或流体。
- 支持[气体标签和压力条件](./gas-conditions.md)和概率物品产物。

## 示例 {#recipe}

```js
ServerEvents.recipes(event => {
    const ccb = event.recipes.createcraftedbeginning

    // 1泥土+2000GU潮湿空气→1泥巴
    ccb.gas_injection('minecraft:mud', [
        'minecraft:dirt',
        CCBGas.of('createcraftedbeginning:moist_air', 2000)
    ]).id('tutorial:injection_mud')

    // 200mB水+75GU炽热空气→100mB熔岩
    ccb.gas_injection(Fluid.of('minecraft:lava', 100), [
        Fluid.of('minecraft:water', 200),
        CCBGas.of('createcraftedbeginning:ultrawarm_air', 75)
    ]).id('tutorial:injection_fluid')

    // 1任意树苗+50GU高能自然空气→1金合欢树叶(50%概率)
    ccb.gas_injection(CCBItem.of('minecraft:acacia_leaves').withChance(0.5), [
        Ingredient.of('#minecraft:saplings'),
        CCBGas.of('createcraftedbeginning:energized_natural_air', 50)
    ]).id('tutorial:injection_chance')
})
```
