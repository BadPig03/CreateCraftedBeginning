---
title: Breeze Chamber
description: "Breeze Chamber KubeJS feeding recipes: add or subtract wind time, switch states and set return items."
editLink: false
---

# Breeze Chamber {#breeze-chamber}

**Syntax:** `wind_charging(input)`

::: info Usage
Feed the Breeze Chamber to add or subtract wind time, clear its ill state or switch states.
:::

**Features:**

- Takes one item ingredient.
- Choose one [feeding effect](#feeding-effect) per recipe.
- Supports [return items and recipe priority](#recipe-options).
- If no recipe matches, the chamber uses the [default wind time calculation](#default-wind).

## Feeding effects {#feeding-effect}

- Use `.addWind(...)` or `.subtractWind(...)` to add or subtract wind time in ticks. Pass a positive integer.

| Method                 | Effect                                                                |
|------------------------|-----------------------------------------------------------------------|
| `.addWind(ticks)`       | Adds to the remaining wind time.                                      |
| `.subtractWind(ticks)`  | Subtracts from the remaining wind time.                                |
| `.clearIll()`           | Clears the ill state and restores calm in non-creative states.          |
| `.cycleCreative()`      | Cycles from calm to creative gale, then creative ill, then back to calm. |

## Return items and recipe priority {#recipe-options}

- Use `.returnItem(...)` to set the item to return. Omit it to use the default return rules.
- Use `.priority(...)` to set the priority. Pass an integer or omit the call to use the default of `0`.
- If multiple recipes match, the highest priority wins. Ties go to the first recipe in recipe ID order.

## Default wind time calculation {#default-wind}

- Applies only to items with food properties and a food value $F$ above 0 that are not in the `#createcraftedbeginning:wind_charging_excluded` tag.
- The time change $T$ is rounded up to a whole tick and capped at 2147483647 ticks:

$$
T = \min\left(2^{31}-1,\,\left\lceil 100\,F^{1.39858}\,|M| \right\rceil\right)
$$

::: details Calculation steps

1. Food value. $N$ is nutrition and $S$ is saturation.

   $$
   F = \frac{N}{2} + S
   $$

2. Effect score. Add up the scores for all food effects.

   $$
   E = \sum_i s_i\,(a_i + 1)\,p_i\,d_i
   $$

   - The sign $s_i$ is 1 for beneficial effects, -1 for harmful effects and 0 for neutral effects.
   - $a_i$ is the effect level minus 1. The probability $p_i$ is clamped to 0–1.
   - The duration factor $d_i$ is 1 for instant effects and 2 for effects with an infinite duration. For timed effects, use their duration $t_i$ in ticks:

     $$
     d_i = \min\left(2,\,\frac{\ln\!\bigl(1+\max(1,\frac{t_i}{20})\bigr)}{\ln 31}\right)
     $$

   - If $|E| < 10^{-9}$, use 0.

3. Wind multiplier. Use the effect score $E$ to calculate the multiplier.

   $$
   M = \begin{cases}
     E + 1, & E \ge 0, \\
     2\,\min(1,-E)\,(E-1), & E < 0.
   \end{cases}
   $$

Bread has 5 nutrition, 6 saturation and no food effects, so $F=8.5$ and $M=1$. Substituting those values gives:

$$
T = \left\lceil 100 \times 8.5^{1.39858} \right\rceil = 1995
$$

:::

## Examples {#recipe}

```js
ServerEvents.recipes(event => {
    const ccb = event.recipes.createcraftedbeginning

    // 1 Builder's Tea → add 288 ticks of wind and return 1 glass bottle
    ccb.wind_charging('create:builders_tea')
        .addWind(288)
        .returnItem('minecraft:glass_bottle')
        .priority(10)
        .id('tutorial:wind_builders_tea')

    // 1 rotten flesh → subtract 100 ticks of wind
    ccb.wind_charging('minecraft:rotten_flesh')
        .subtractWind(100)
        .id('tutorial:wind_rotten_flesh')

    // 1 milk bucket → clear the ill state; returns an empty bucket by default
    ccb.wind_charging('minecraft:milk_bucket')
        .clearIll()
        .id('tutorial:wind_clear_ill')

    // 1 Creative Ice Cream → cycle between calm, creative gale and creative ill
    ccb.wind_charging('createcraftedbeginning:creative_ice_cream')
        .cycleCreative()
        .id('tutorial:wind_cycle_creative')
})
```
