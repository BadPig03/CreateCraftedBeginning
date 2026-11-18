---
title: Gas Conditions
description: Set recipe gas types, amounts and pressure conditions with CCBGas.
editLink: false
---

# Gas Conditions {#gas-conditions}

**Syntax:** `CCBGas.of(gas, amount)`

Define a recipe's gas ingredient by choosing a gas, specifying the amount per recipe and adding pressure conditions as needed.

**Features:**

- Accepts a specific gas ID or a gas tag.
- Specifies the amount per recipe in GU.
- Sets pressure with `.minimumPressure(...)` and `.maximumPressure(...)`.

## Examples {#examples}

```js
// 100 GU of natural air
const air = CCBGas.of('createcraftedbeginning:natural_air', 100)

// 100 GU of a gas in the energized tag
const energizedAir = CCBGas.of(
    '#createcraftedbeginning:energized', 100
)

// Add pressure settings to the tag condition
const pressurizedAir = energizedAir
    .minimumPressure(1000000)
    .maximumPressure(2000000)
```

Use these values in a recipe's gas ingredient position, such as the [Gas Injection Chamber input list](./gas-injection.md#recipe).

Prefix gas tag IDs with `#`. Item tags use `'#tag'`; fluid tags use `Fluid.sizedIngredientOf(...)`.

## Amounts and pressure {#pressure}

- **Amount:** A positive integer in GU, consumed per recipe.
- **Pressure:** A non-negative integer in Pa; in-game, `1 atm = 100000 Pa`.
- **Minimum pressure:** `.minimumPressure(Pa)` sets the minimum pressure left after gas consumption.
- **Maximum pressure:** `.maximumPressure(Pa)` sets the supply pressure ceiling when processing starts.

Set either pressure condition on its own or combine them. The minimum is ≤ the maximum, and bounds are inclusive. The example's two values are 10 atm and 20 atm.

Chained methods create new condition values. In the example, `pressurizedAir` adds pressure settings to `energizedAir`, leaving the original available for reuse.

A specific gas ID refers to a registered gas type. Register new types in startup scripts and restart the game or server for them to take effect.
