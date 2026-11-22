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

## Amounts and pressure {#pressure}

- Amount: A positive integer in GU.
- Pressure: A non-negative integer in Pa. In-game, `1 atm` equals `100000 Pa`.
- Minimum pressure (optional): `.minimumPressure(Pa)` sets the minimum pressure left after gas consumption.
- Maximum pressure (optional): `.maximumPressure(Pa)` sets the upper limit on supply pressure before gas is consumed.

## Examples {#examples}

```js
// 100 GU of natural air
const air = CCBGas.of('createcraftedbeginning:natural_air', 100)

// 100 GU of a gas in the energized tag
const energizedAir = CCBGas.of('#createcraftedbeginning:energized', 100)

// Set a minimum pressure for a specific gas ID
const pressurizedAir = air.minimumPressure(1000000)

// Set both pressure limits for a gas tag
const pressurizedEnergizedAir = energizedAir.minimumPressure(1000000).maximumPressure(2000000)
```
