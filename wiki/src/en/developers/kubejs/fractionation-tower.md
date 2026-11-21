---
title: Airtight Fractionation Tower
description: "Airtight Fractionation Tower KubeJS recipes: mixed inputs, layer outputs and temperature conditions."
editLink: false
---

# Airtight Fractionation Tower {#fractionation-tower}

**Syntax:** `fractionation_tower(inputs[])`

::: info Usage
The Airtight Fractionation Tower uses heat for fractionation or cooling for condensation and sends products to different layers.
:::

**Features:**

- Inputs can mix items, fluids and gases. At least one input is required.
- Inputs: up to 64 entries per type. Outputs: 2–8 entries in total.
- Use `.processingTime(...)` to set the processing time in ticks. Pass a positive integer; the default is 200 ticks.
- Supports [temperature tiers and matching modes](#temperature) and [gas tags and pressure conditions](./gas-conditions.md).

## Layer outputs {#layer-outputs}

- Use `.outputAt(layerOffset, product)` to specify the output layer.
- The input layer has offset `0`. Count up from the bottom when heating and down from the top when condensing.
- Output offsets must be integers from `1` to `8`.
- Set one product per output layer. You can skip layers.
- The minimum tower height is the largest offset plus one layer.

The numbers in this table are the offsets for a 4-layer tower:

| Tower layer                | Heating (fractionation) | Cooling (condensation) |
|----------------------------|:-----------------------:|:----------------------:|
| Top                        |      `3` (output)       |      `0` (input)       |
| One layer below the top    |      `2` (output)       |      `1` (output)      |
| One layer above the bottom |      `1` (output)       |      `2` (output)      |
| Bottom                     |       `0` (input)       |      `3` (output)      |

## Temperature tiers and matching modes {#temperature}

- Use `.temperature(...)` to select a temperature tier and `.temperatureMatching(...)` to choose the matching mode.
- A temperature tier is required and cannot be `none`. The default matching mode is `exact`.

| Temperature tier | `exact` (exact match) | `compatible` (compatible match) |
|------------------|-----------------------|---------------------------------|
| `chilled`        | Chilled only          | Chilled or superchilled         |
| `superchilled`   | Superchilled only     | Superchilled only               |
| `heated`         | Heated only           | Heated or superheated           |
| `superheated`    | Superheated only      | Superheated only                |

## Examples {#recipe}

```js
ServerEvents.recipes(event => {
    const ccb = event.recipes.createcraftedbeginning

    // 100 mB brimstone → 500 mB lava + 500 GU ultrawarm air
    // Requires superheated conditions. Outputs are 1 and 2 layers above the bottom.
    ccb.fractionation_tower([
        Fluid.of('createcraftedbeginning:brimstone', 100)
    ]).outputAt(1, Fluid.of('minecraft:lava', 500))
        .outputAt(2, CCBGas.of('createcraftedbeginning:ultrawarm_air', 500))
        .temperature('superheated')
        .id('tutorial:tower_brimstone')

    // 10000 GU moist air → 9800 GU natural air + 100 mB water
    // Requires chilled conditions. Outputs are 1 and 2 layers below the top.
    ccb.fractionation_tower([
        CCBGas.of('createcraftedbeginning:moist_air', 10000)
    ]).outputAt(1, CCBGas.of('createcraftedbeginning:natural_air', 9800))
        .outputAt(2, Fluid.of('minecraft:water', 100))
        .temperature('chilled')
        .processingTime(100)
        .id('tutorial:tower_moist_air')

    // 1 ice block + 100 mB water + 1000 GU moist air → 1 snow block + 900 GU natural air
    // Requires chilled or superchilled conditions and at least 4 layers.
    // Outputs are 1 and 3 layers below the top.
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
