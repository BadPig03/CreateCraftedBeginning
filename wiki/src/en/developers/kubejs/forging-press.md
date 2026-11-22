---
title: Airtight Forging Press
description: Airtight Forging Press KubeJS recipes with base and additional ingredients, press heads and chance-based item outputs.
editLink: false
---

# Airtight Forging Press {#forging-press}

**Syntax:** `forging_press(output, inputs[])`

::: info Usage
The Airtight Forging Press uses separate slots for the base item, press head and additional ingredient. Recipes can also use a fluid or gas.
:::

**Features:**

- The first input is the required base item. The optional additional ingredient goes second and can be an item, fluid or gas.
- Produces 1 to 8 item outputs.
- Unused slots stay empty.
- Supports [press head configuration](#pressHead), [gas tags and pressure conditions](./gas-conditions.md) and chance-based item outputs.

## Press head items {#pressHead}

- Use `.pressHead(...)` to set the press head. The press head is not consumed.
- The press head slot accepts items in the `#createcraftedbeginning:press_head_tools` tag and smithing templates.

To allow an iron block as a press head, add it to the tag:

```js
ServerEvents.tags('item', event => {
    event.add('createcraftedbeginning:press_head_tools', 'minecraft:iron_block')
})
```

## Examples {#recipe}

```js
ServerEvents.recipes(event => {
    const ccb = event.recipes.createcraftedbeginning

    // 1 iron ingot (base) → 1 iron sheet
    // No press head or additional ingredient is used.
    ccb.forging_press('create:iron_sheet', [
        'minecraft:iron_ingot'
    ]).id('tutorial:forging_base')

    // 1 iron nugget (base) + 1 gold nugget (additional ingredient) → 1 iron ingot
    // A heavy core serves as the press head.
    ccb.forging_press('minecraft:iron_ingot', [
        'minecraft:iron_nugget',
        'minecraft:gold_nugget'
    ]).pressHead('minecraft:heavy_core')
        .id('tutorial:forging_three_items')

    // 1 charcoal (base) + 2500 GU energized ultrawarm air (additional ingredient)
    // → 1 diamond (20% chance); a heavy core serves as the press head.
    ccb.forging_press(CCBItem.of('minecraft:diamond').withChance(0.2), [
        Ingredient.of('minecraft:charcoal'),
        CCBGas.of(
            'createcraftedbeginning:energized_ultrawarm_air', 2500
        ).minimumPressure(1000000)
    ]).pressHead('minecraft:heavy_core')
        .id('tutorial:forging_gas')

    // 1 cobblestone (base) + 100 mB lava (additional ingredient) → 1 stone
    // A heavy core serves as the press head.
    ccb.forging_press('minecraft:stone', [
        'minecraft:cobblestone',
        Fluid.of('minecraft:lava', 100)
    ]).pressHead('minecraft:heavy_core')
        .id('tutorial:forging_fluid')
})
```
