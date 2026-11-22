---
title: 旋风人蓄风室
description: 旋风人蓄风室KubeJS投喂配方：增减蓄风时间、切换状态与设置返还物品。
editLink: false
---

# 旋风人蓄风室 {#breeze-chamber}

**语法：** `wind_charging(input)`

::: info 用途
投喂旋风人蓄风室可增减蓄风时间、清除萎靡或切换状态。
:::

**功能：**

- 输入为1项物品原料。
- 每条配方选择1种[投喂效果](#feeding-effect)。
- 支持[返还物品与优先级](#recipe-options)设置。
- 无匹配配方时，使用[默认蓄风时间算法](#default-wind)。

## 投喂效果 {#feeding-effect}

- 使用`.addWind(...)`或`.subtractWind(...)`设置蓄风时间变化量，单位为tick，传入值必须为正整数。

| 方法                     | 效果                  |
|------------------------|---------------------|
| `.addWind(ticks)`      | 增加剩余蓄风时间            |
| `.subtractWind(ticks)` | 扣减剩余蓄风时间            |
| `.clearIll()`          | 清除非创造状态下的萎靡，恢复无风状态  |
| `.cycleCreative()`     | 在无风、创造劲风和创造萎靡之间依次循环 |

## 返还物品与优先级 {#recipe-options}

- 使用`.returnItem(...)`指定返还物品，省略时沿用默认返还规则。
- 使用`.priority(...)`设置优先级，传入值必须为整数。省略此调用时默认为`0`。
- 匹配多条配方时，选优先级最高的一条；优先级相同时，按配方ID排序取第一条。

## 默认蓄风时间算法 {#default-wind}

- 仅适用于有食物属性、食物值$F$大于0且未含有`#createcraftedbeginning:wind_charging_excluded`标签的物品。
- 时间变化量$T$以tick为单位，向上取整，最多为2147483647tick：

$$
T = \min\left(2^{31}-1,\,\left\lceil 100\,F^{1.39858}\,|M| \right\rceil\right)
$$

::: details 具体计算过程

1. 食物值：$N$为营养值，$S$为饱和度。

   $$
   F = \frac{N}{2} + S
   $$

2. 效果分：把每项食物效果的得分相加。

   $$
   E = \sum_i s_i\,(a_i + 1)\,p_i\,d_i
   $$

   - $s_i$为效果方向，正面取1、负面取-1、中性取0。
   - $a_i$为效果的等级数减1。触发概率$p_i$限制在0~1之间。
   - 瞬间效果的时长系数$d_i$为1，无限时长为2；限时效果按持续的tick数$t_i$计算：

     $$
     d_i = \min\left(2,\,\frac{\ln\!\bigl(1+\max(1,\frac{t_i}{20})\bigr)}{\ln 31}\right)
     $$

   - 如果$|E| < 10^{-9}$，则按0计算。

3. 蓄风倍率：根据效果分$E$计算。

   $$
   M = \begin{cases}
     E + 1, & E \ge 0, \\
     2\,\min(1,-E)\,(E-1), & E < 0.
   \end{cases}
   $$

面包的营养值和饱和度分别为5和6，没有其他效果，所以$F=8.5$、$M=1$。代入上式：

$$
T = \left\lceil 100 \times 8.5^{1.39858} \right\rceil = 1995
$$

:::

## 示例 {#recipe}

```js
ServerEvents.recipes(event => {
    const ccb = event.recipes.createcraftedbeginning

    // 1建筑工茶饮→增加288tick蓄风时间，返还1玻璃瓶
    ccb.wind_charging('create:builders_tea')
        .addWind(288)
        .returnItem('minecraft:glass_bottle')
        .priority(10)
        .id('tutorial:wind_builders_tea')

    // 1腐肉→扣减100tick蓄风时间
    ccb.wind_charging('minecraft:rotten_flesh')
        .subtractWind(100)
        .id('tutorial:wind_rotten_flesh')

    // 1牛奶桶→清除萎靡，按默认规则返还空桶
    ccb.wind_charging('minecraft:milk_bucket')
        .clearIll()
        .id('tutorial:wind_clear_ill')

    // 1创造冰淇淋→在无风、创造劲风和创造萎靡之间循环
    ccb.wind_charging('createcraftedbeginning:creative_ice_cream')
        .cycleCreative()
        .id('tutorial:wind_cycle_creative')
})
```
