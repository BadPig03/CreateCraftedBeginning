---
title: Airtight Reactor Kettle
description: "Airtight Reactor Kettle KubeJS recipes: mix items, fluids and gases and set processing time and temperature conditions."
editLink: false
---

# Airtight Reactor Kettle {#reactor-kettle}

**Syntax:** `reactor_kettle(outputs[], inputs[])`

::: info Usage
The Airtight Reactor Kettle processes a mix of items, fluids and gases. It can also run fluid-only or gas-only reactions.
:::

**Features:**

- Both inputs and outputs can mix items, fluids and gases. Each list needs at least one entry.
- Inputs: up to 64 item, 3 fluid and 3 gas entries. Outputs: up to 4 item, 2 fluid and 2 gas entries.
- Use `.processingTime(...)` to set the processing time in ticks. Pass a positive integer.
- Supports [temperature tiers and matching modes](#temperature), [gas tags and pressure conditions](./gas-conditions.md) and chance-based item outputs.

## Temperature tiers and matching modes {#temperature}

- Use `.temperature(...)` to select a temperature tier and `.temperatureMatching(...)` to choose the matching mode.
- The default tier is `none`, and the default matching mode is `exact`.

| Temperature tier | `exact` (exact match) | `compatible` (compatible match) |
|------------------|-----------------------|---------------------------------|
| `none`           | Ambient only          | Any temperature                 |
| `chilled`        | Chilled only          | Chilled or superchilled         |
| `superchilled`   | Superchilled only     | Superchilled only               |
| `heated`         | Heated only           | Heated or superheated           |
| `superheated`    | Superheated only      | Superheated only                |

## Examples {#recipe}

```js
ServerEvents.recipes(event => {
    const ccb = event.recipes.createcraftedbeginning

    // 1 Breeze Core + 1 stone ingredient → 1000 GU natural air; the Breeze Core is returned.
    // Also produces 1 gravel with a 25% chance. Requires ambient temperature.
    ccb.reactor_kettle([
        'createcraftedbeginning:breeze_core',
        CCBItem.of('minecraft:gravel').withChance(0.25),
        CCBGas.of('createcraftedbeginning:natural_air', 1000)
    ], [
        'createcraftedbeginning:breeze_core',
        Ingredient.of('#c:stones')
    ]).processingTime(50)
        .id('tutorial:reactor_natural_air')

    // 1 stone + 100 mB lava + 100 GU natural air → 25 mB water + 50 GU ultrawarm air.
    // Also produces 1 obsidian with a 50% chance. Requires chilled or superchilled conditions.
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

    // 100 GU natural air → 30 GU ultrawarm air + 20 GU energized ultrawarm air
    // Works at any temperature.
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
