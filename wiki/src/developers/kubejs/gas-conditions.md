---
title: 气体条件
description: 使用CCBGas设置配方的气体种类、气量与压力条件。
editLink: false
---

# 气体条件 {#gas-conditions}

**语法：** `CCBGas.of(gas, amount)`

用于设置配方的气体原料：选择气体种类，指定每份用量，再按需要添加压力条件。

**功能：**

- 使用具体气体ID或气体标签。
- 按GU指定每份配方的气量。
- 使用`.minimumPressure(...)`和`.maximumPressure(...)`设置压力。

## 示例 {#examples}

```js
// 100GU自然空气
const air = CCBGas.of('createcraftedbeginning:natural_air', 100)

// 100GU高能气体标签中的气体
const energizedAir = CCBGas.of(
    '#createcraftedbeginning:energized', 100
)

// 在标签条件上增加压力设置
const pressurizedAir = energizedAir
    .minimumPressure(1000000)
    .maximumPressure(2000000)
```

将这些值放入配方的气体原料位置，例如[注气室的输入列表](./gas-injection.md#recipe)。

气体标签在ID前加`#`。物品标签使用`'#标签'`，流体标签使用`Fluid.sizedIngredientOf(...)`。

## 气量与压力 {#pressure}

- **气量：** 单位为GU，使用正整数，表示每份配方的消耗量。
- **压力：** 单位为Pa，使用非负整数；游戏内`1atm=100000Pa`。
- **最低压力：** `.minimumPressure(Pa)`设置扣气后保留的最低压力。
- **最高压力：** `.maximumPressure(Pa)`设置开工时供压的上限。

压力条件可以单独设置，也可以组合使用；最低值≤最高值，端点包含在匹配范围中。示例中的两个值分别为10atm和20atm。

链式方法生成新的条件值。例中的`pressurizedAir`在`energizedAir`基础上增加压力设置，原条件可继续复用。

具体气体ID对应已注册的气体类型。新增类型在启动脚本中注册，完整重启后生效。
