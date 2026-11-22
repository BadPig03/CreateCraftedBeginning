---
title: 气体条件
description: 使用CCBGas设置配方的气体种类、气体的量与压力条件。
editLink: false
---

# 气体条件 {#gas-conditions}

**语法：** `CCBGas.of(gas, amount)`

用于设置配方的气体原料：选择气体种类，指定每份气体的用量，再按需要添加压力条件。

**功能：**

- 使用具体气体ID或气体标签。

## 气体的量与压力 {#pressure}

- 气体的量：单位为GU，需要为正整数。
- 压力：单位为Pa，需要为非负整数；游戏内`1atm=100000Pa`。
- 最低压力：`.minimumPressure(Pa)`设置扣气后保留的最低压力，可选。
- 最高压力：`.maximumPressure(Pa)`设置扣气前的供气压力上限，可选。

## 示例 {#examples}

```js
// 100GU自然空气
const air = CCBGas.of('createcraftedbeginning:natural_air', 100)

// 100GU高能气体标签中的气体
const energizedAir = CCBGas.of('#createcraftedbeginning:energized', 100)

// 按气体ID设置最低压力
const pressurizedAir = air.minimumPressure(1000000)

// 按气体标签同时设置最低和最高压力
const pressurizedEnergizedAir = energizedAir.minimumPressure(1000000).maximumPressure(2000000)
```
