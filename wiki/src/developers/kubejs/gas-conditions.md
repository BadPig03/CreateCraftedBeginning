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
const energizedAir = CCBGas.of('#createcraftedbeginning:energized', 100)

// 在标签条件上增加压力设置
const pressurizedAir = energizedAir.minimumPressure(1000000).maximumPressure(2000000)
```

## 气量与压力 {#pressure}

- **气量：** 单位为GU，使用正整数。
- **压力：** 单位为Pa，使用非负整数；游戏内`1atm`与`100000Pa`等价。
- **最低压力：** `.minimumPressure(Pa)`设置扣气后保留的最低压力。
- **最高压力：** `.maximumPressure(Pa)`设置开工时供压的上限。

压力条件可以单独设置，也可以组合使用。
