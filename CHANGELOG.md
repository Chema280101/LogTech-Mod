# Changelog

All notable changes to **LogTech** will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.2.0] - 2026-10-13 (Alpha Release)

### Migration Notice
- **Important:** Back up your world before upgrading from v0.1!
- Saves opened or created with v0.2 cannot be reopened in v0.1 without data loss or corruption.
- All v0.1 items and networks are preserved: 100% of stored items are automatically migrated into the Hot tier (`rack`).

### Added
- **Ore & Item Purity:**
  - `logtech:purity` data component (`Codec.intRange(1, 5)`) persisting mineral purity across mining, transport, and processing.
  - Drill deposits inherit vein purity deterministically (1 to 5).
  - Crusher propagates and preserves input purity on output dusts.
  - Client tooltips display purity level on ores and dusts in Spanish and English.
- **Refining Machinery & Materials:**
  - `washer` (Washer machine): Consumes 250 mB water and 60 FE/t to upgrade dust purity ($p \to p+1$, up to 5) and produces `sludge` (lodo).
  - `centrifuge` (Centrifuge machine): Extracts `slag` (escoria) at 80 FE/t with probability scaled inversely to purity ($80\% \times (6-p)/5$); centrifuges `sludge` into `crystal_shard` (fragmento de cristal).
  - `coal_generator` (Coal Generator): Burns solid fuel (coal/charcoal) generating 40 FE/t for independent or standalone network setups.
  - Materials: `crystal_shard`, `slag`, `sludge`, and decorative `slag_block`.
- **JSON Processing Recipes:**
  - Registered `logtech:crushing`, `logtech:washing`, and `logtech:centrifuging` recipe types and serializers.
  - Dynamic `RecipeManager` lookups with caching and datapack customization support.
- **Hierarchical Storage & Tier Automation:**
  - `cold_rack` (Cold Rack): Server rack with 4x base capacity per module (16,384 items default).
  - Tiered storage media: `data_module_advanced` (Tier 2, 4x capacity) and `data_module_master` (Tier 3, 16x capacity).
  - Dynamic rebalancing: usage scoring (+1 per extraction, 0.9x decay per cycle) and demotion when hot tier exceeds 90%.
  - Pinning policies: `AUTO`, `FIJAR_CALIENTE`, and `SOLO_FRIO` toggleable with `Ctrl + Right Click` in Terminal.
  - Cold item retrieval queue with animated visual progress bar, countdown timer, and atomic anti-duplication delivery.
- **Network Automation Rules & Alarms:**
  - Core automation GUI (`CoreScreen`) supporting up to 8 rules (`ITEM_COUNT`, `TOTAL_SPACE` conditions).
  - Rule anti-flicker hysteresis (5% item count, 2% space) and single-shot rearming notifications.
  - `alarm` (Redstone Alarm): Network block supporting channels 1-4, emitting signal strength 15 with illuminated model.
- **Network Energy & Priority System:**
  - Backbone energy reception (`BackboneEnergyStorage`) routing FE directly to Core buffer (200,000 FE default).
  - Strict proportional energy distribution from Core with zero energy leakage.
  - Machine energy priorities (`HIGH`, `NORMAL`, `LOW`) with dynamic shedding (`SHED`) and stability hold (`holdCycles = 3`).
  - Terminal Energy Tab: 60-minute historical incoming/outgoing dual bar chart, flow meters, branch breakdown, and interactive machine priority toggles.
- **Mod Compatibility (Optional Soft Dependencies):**
  - **Jade:** Block HUD displaying status, cause, progress, FE energy, water levels, vein purity, and rack capacity.
  - **JEI & EMI:** Recipe categories for Crushing, Washing, and Centrifuging with purity effects, water usage, and chance tooltips; machines registered as workstation catalysts.

### Removed
- Hardcoded `CrusherRecipes` class in favor of dynamic datapack JSON recipes.

## [0.1.0] - 2026-10-12 (Alpha Release)

### Added
- **Smart Storage Network:**
  - `core` (Core block): Manages network data, draws 20 FE/t, and coordinates telemetry.
  - `backbone` (Cable/Bus): Connects network nodes and accepts external automated insertion via `Capabilities.ItemHandler.BLOCK`.
  - `rack` (Rack block): Holds up to 4 `data_module` items with capacity aggregation and overload prevention.
  - `data_module` (Item): 4,096 items capacity per module.
  - `terminal` (Terminal block): GUI with dual tabs:
    - **Items Tab:** Full inventory search with text filter, count/name sorting, and shift-click insertion/extraction.
    - **Statistics Tab:** 60-minute network activity bar chart, FE energy monitor, capacity bar, and top 10 machine telemetry list.
- **Ore Veins & Prospecting:**
  - Deterministic Overworld ore vein system for Neoferrite and Crystalite deposits.
  - `scanner` (Scanner item): 96-block radial search and persistence of discovered veins.
  - Interactive 2D Vein Map with zoom, pan, player heading, and vein indicators.
- **Machinery:**
  - `crusher` (Crusher machine): 2x ore duplication into dusts at 40 FE/t with directional input/output capabilities.
  - `drill` (Mining Drill machine): Surface extraction from discovered ore veins at 80 FE/t, automatic item pushing into adjacent inventories or cables.
- **Materials & Crafting:**
  - Raw ores, dusts, and ingots for Neoferrite and Crystalite, plus Data Crystals.
  - Smelting, blasting, and crafting table progression recipes.
- **Telemetry System:**
  - Circular telemetry buffer (`ActivityCounter`) tracking items and active/idle minutes without tick allocations.
  - Real-time `MachineStatus` states with detailed translated explanations.
- **Configuration:**
  - Server config (`logtech-server.toml`) with options for capacities, FE energy draw, machine speeds, scanner range, and infinite veins.
