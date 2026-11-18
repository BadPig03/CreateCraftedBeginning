---
title: Gas Injection Chamber
description: Gas Injection Chamber KubeJS recipes with mixed inputs and item, fluid and chance-based outputs.
editLink: false
---

# Gas Injection Chamber {#gas-injection}

**Syntax:** `gas_injection(output, input[])`

::: info Usage
The Gas Injection Chamber processes items on a Belt or Depot. Recipes involving fluids use a Basin.
:::

**Features:**

- Takes one item ingredient or fluid ingredient and one gas ingredient.
- Produces one item or fluid result.
- Supports [gas tags and pressure conditions](./gas-conditions.md).
- Supports chance-based item outputs.

## Examples {#recipe}

```js
ServerEvents.recipes(event => {
    const ccb = event.recipes.createcraftedbeginning

    // 1 dirt + 2000 GU moist air → 1 mud
    ccb.gas_injection('minecraft:mud', [
        'minecraft:dirt',
        CCBGas.of('createcraftedbeginning:moist_air', 2000)
    ]).id('tutorial:injection_mud')

    // 200 mB water + 75 GU ultrawarm air → 100 mB lava
    ccb.gas_injection(Fluid.of('minecraft:lava', 100), [
        Fluid.of('minecraft:water', 200),
        CCBGas.of('createcraftedbeginning:ultrawarm_air', 75)
    ]).id('tutorial:injection_fluid')

    // 1 sapling + 50 GU energized natural air
    // → 1 acacia leaf block (50% chance)
    ccb.gas_injection(CCBItem.of('minecraft:acacia_leaves').withChance(0.5), [
        Ingredient.of('#minecraft:saplings'),
        CCBGas.of('createcraftedbeginning:energized_natural_air', 50)
    ]).id('tutorial:injection_chance')
})
```
