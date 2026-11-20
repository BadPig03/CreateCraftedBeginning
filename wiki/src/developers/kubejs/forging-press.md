---
title: 气密锻造机
description: 气密锻造机KubeJS配方：基底、附加原料、压头与概率产物示例。
editLink: false
---

# 气密锻造机 {#forging-press}

**语法：** `forging_press(output, inputs[])`

::: info 用途
气密锻造机按基底、压头和附加物槽位加工原料，也可使用流体或气体。
:::

**功能：**

- 输入第一项为必需的基底物品；第二项可选，可使用物品、流体或气体中的一种。
- 输出为1~8项物品。
- 未使用的槽位会保持为空。
- 支持[压头物品](#pressHead)设置、[气体标签和压力条件](./gas-conditions.md)和概率物品产物。

## 压头物品 {#pressHead}

- 使用`.pressHead(...)`设置压头物品，压头物品不会被消耗。
- 压头物品必须要有`#createcraftedbeginning:press_head_tools`的物品标签，否则将无法放入压头槽位中。

允许铁块作为压头时可使用如下示例：
```js
ServerEvents.tags('item', event => {
    event.add('createcraftedbeginning:press_head_tools', 'minecraft:iron_block')
})
```

## 示例 {#recipe}

```js
ServerEvents.recipes(event => {
    const ccb = event.recipes.createcraftedbeginning

    // 1铁锭(基底)→1铁板，不使用压头或附加原料
    ccb.forging_press('create:iron_sheet', [
        'minecraft:iron_ingot'
    ]).id('tutorial:forging_base')

    // 1铁粒(基底)+1金粒(附加物)→1铁锭，沉重核心作为压头
    ccb.forging_press('minecraft:iron_ingot', [
        'minecraft:iron_nugget',
        'minecraft:gold_nugget'
    ]).pressHead('minecraft:heavy_core')
        .id('tutorial:forging_three_items')

    // 1木炭(基底)+2500GU高能炽热空气(附加物)→1钻石(20%概率)，沉重核心作为压头
    ccb.forging_press(CCBItem.of('minecraft:diamond').withChance(0.2), [
        Ingredient.of('minecraft:charcoal'),
        CCBGas.of(
            'createcraftedbeginning:energized_ultrawarm_air', 2500
        ).minimumPressure(1000000)
    ]).pressHead('minecraft:heavy_core')
        .id('tutorial:forging_gas')

    // 1圆石(基底)+100mB熔岩(附加物)→1石头，沉重核心作为压头
    ccb.forging_press('minecraft:stone', [
        'minecraft:cobblestone',
        Fluid.of('minecraft:lava', 100)
    ]).pressHead('minecraft:heavy_core')
        .id('tutorial:forging_fluid')
})
```
