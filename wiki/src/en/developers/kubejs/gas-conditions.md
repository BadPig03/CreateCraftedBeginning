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
const energizedAir = CCBGas.of('#createcraftedbeginning:energized', 100)

// Add pressure settings to the tag condition
const pressurizedAir = energizedAir.minimumPressure(1000000).maximumPressure(2000000)
```

## Amounts and pressure {#pressure}

- **Amount:** A positive integer in GU.
- **Pressure:** A non-negative integer in Pa. In-game, `1 atm` equals `100000 Pa`.
- **Minimum pressure:** `.minimumPressure(Pa)` sets the minimum pressure left after gas consumption.
- **Maximum pressure:** `.maximumPressure(Pa)` sets the supply pressure ceiling when processing starts.

Set either pressure condition on its own or use both together.
