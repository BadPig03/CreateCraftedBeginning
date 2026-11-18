---
title: Airtight Forging Press
description: Airtight Forging Press KubeJS recipes with base and additional ingredients, press heads and chance-based item outputs.
editLink: false
---

# Airtight Forging Press {#forging-press}

**Syntax:** `forging_press(output, inputs[])`

::: info Usage
The Airtight Forging Press processes ingredients in its base, press head and addition slots. Recipes can also use a fluid or gas.
:::

**Features:**

- The first input is the required base item. The optional second input can be an item, fluid or gas ingredient.
- Sets an optional press head with `.pressHead(...)`. The press head is not consumed.
- Unused press head or additional item slots stay empty.
- Supports [gas tags and pressure conditions](./gas-conditions.md).
- Produces 1 to 8 item outputs, including chance-based item outputs.

Note: Custom press head items must belong to the `#createcraftedbeginning:press_head_tools` item tag to fit in the press head slot. To use an iron block as a press head, add it to the tag:
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

    // 1 iron nugget (base) + 1 gold nugget (addition) → 1 iron ingot
    // A heavy core serves as the press head.
    ccb.forging_press('minecraft:iron_ingot', [
        'minecraft:iron_nugget',
        'minecraft:gold_nugget'
    ]).pressHead('minecraft:heavy_core')
        .id('tutorial:forging_three_items')

    // 1 charcoal (base) + 2500 GU energized ultrawarm air (addition)
    // → 1 diamond (20% chance); a heavy core serves as the press head.
    ccb.forging_press(CCBItem.of('minecraft:diamond').withChance(0.2), [
        Ingredient.of('minecraft:charcoal'),
        CCBGas.of(
            'createcraftedbeginning:energized_ultrawarm_air', 2500
        ).minimumPressure(1000000)
    ]).pressHead('minecraft:heavy_core')
        .id('tutorial:forging_gas')

    // 1 cobblestone (base) + 100 mB lava (addition) → 1 stone
    // A heavy core serves as the press head.
    ccb.forging_press('minecraft:stone', [
        'minecraft:cobblestone',
        Fluid.of('minecraft:lava', 100)
    ]).pressHead('minecraft:heavy_core')
        .id('tutorial:forging_fluid')
})
```
