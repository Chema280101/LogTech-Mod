# Changelog

All notable changes to **LogTech** will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.3.0] - 2026-10-14 (Alpha Release)

### Migration Notice
- **Important:** Back up your world before upgrading from v0.1 or v0.2!
- Saves opened or created with v0.3 cannot be reopened in v0.1 or v0.2 without data loss or corruption (due to upgraded NBT schemas and `data_version: 8`).
- **Network Security & Perimeter Protection:** When a network is set to Private mode, only authorized players can access the Terminal, insert/extract items, modify rules, and break the Core block. However, peripheral network blocks (Backbone cables, Racks, Terminals) are not individually protected against breaking in the physical world; pairing with chunk claim mods (such as FTB Chunks, FLAN, or Open Parties and Claims) is recommended for full perimeter protection on multiplayer servers.
- **Data Preservation:** Worlds from v0.1 and v0.2 are automatically migrated with zero item loss. Stored items, hot/cold tiers, energy buffers, and rules are fully preserved. New features (Trash Bin, Quarantine, Saved Views, Security, Activity Log) initialize cleanly in safe defaults (Open mode, empty trash/quarantine/logs).

### Added
- **Thermal System & Dissipation:**
  - `HeatModel` tracking machine temperature (0 to 100 °C) updated every 20 ticks.
  - Thermal throttling: machines operate at 0.5x speed and consume 0.5x energy above 70 °C.
  - Overheat shutdown: machines shut down with `OVERHEATED` status upon reaching 100 °C, resuming automatically once cooled to $\le 40$ °C.
  - `heatsink` (Heatsink block): provides passive cooling (+1.0 °C/s per adjacent block) and water cooling (+0.5 °C/s per water contact face).
  - Real-time heat gauge bar, cooling rates, and tooltips in Crusher, Washer, Centrifuge, and Jade HUD.
- **Machine Firmware Chips:**
  - Dedicated firmware slot in Crusher, Washer, Centrifuge, and Mining Drill.
  - `firmware_eco` (Eco Firmware): 0.7x speed, 0.5x energy consumption, 0.5x heat generation.
  - `firmware_turbo` (Turbo Firmware): 1.5x speed, 2.0x energy consumption, 3.0x heat generation.
  - `firmware_safe` (Safe Firmware): 1.0x speed without intermediate throttling; shuts down at 85 °C and resumes at $\le 30$ °C.
  - `firmware_selective` (Selective Firmware): extraction filter for automated pipe/hopper routing (configured with Shift + Right Click).
  - Dedicated GUI for the Mining Drill (`DrillMenu`, `DrillScreen`) allowing internal buffer and firmware access.
  - Firmware badges and indicators in Terminal Stats tab and Jade HUD.
- **Storage Safety (Trash & Quarantine):**
  - `TrashBin` (Trash Bin): 4,096 items capacity with 30-minute retention (`trashLifetimeTicks = 36000`), FIFO expiration, and Terminal "Papelera" tab with item restoration and "Destruir" button.
  - `QuarantineBuffer` (Quarantine Buffer): 1,024 items emergency buffer absorbing automated backbone inputs when the network is full, unpowered, or overloaded, with automatic 64 items/cycle draining upon network recovery.
  - `DISCARD_EXCESS` automation rule action: sends excess items above $N$ to the trash bin cleanly without oscillation.
  - `QUARANTINE_GREATER` rule condition with anti-flicker hysteresis.
- **Compaction Automation:**
  - `COMPACT` automation rule action: automatically detects reversible 3×3 and 2×2 crafting recipes (e.g. ingots $\leftrightarrow$ blocks) and converts excess items into compact blocks atomically with configurable batch limits (`compactMaxConversionsPerCycle = 64`).
  - Configurable recipe blacklist (`compactBlacklist`) and strict data component / purity preservation.
- **Saved Views & Filter System:**
  - 8 quick-access saved view slots [1..8] persisted per network in the Core (`data_version: 6`).
  - Shift + Click on slot buttons to save current search query, sort mode, storage tier filter (All, Hot, Cold), and minimum purity filter (0-5); regular Click to restore.
  - Real-time view synchronization across all players viewing the Terminal.
- **Inventory Estimations & Rate Tracking:**
  - In-memory `RateTracker` tracking 5-minute rolling averages for net item flows and storage tiers.
  - Item tooltips in Terminal display net flow rates (e.g., "+120/min", "−45/min") and depletion warnings ("depletes in X").
  - Stats tab shows network-wide and per-tier fill forecasts ("fills in X").
- **Configuration Key:**
  - `config_key` item for copying and pasting configurations between blocks of the same type.
  - Shift + Right Click to copy, Right Click to paste.
  - Supports machine power priority, alarm channels, and Core automation rules/pinning policies with strict schema validation.
- **Network Security & Access Control:**
  - Ownership system (`ownerUuid`, `ownerName`) assigned when placing the Core; existing unowned cores can be claimed.
  - Network Security Modes: `OPEN` (default) or `PRIVATE`.
  - Granular permissions: View (`VIEW`), Extract (`EXTRACT`), Insert (`INSERT`), and Administrator (`ADMIN`).
  - "Acceso" (Access) tab in Core GUI for managing authorized players and toggling permissions.
  - Real-time permission revocation: immediately closes open menus if rights are revoked.
  - Core break protection: non-admins cannot break the Core in private networks.
- **Activity Audit Log:**
  - Circular buffer of 500 player activity entries (`ActivityLogBuffer`) tracking manual insertions, extractions, deletions, restorations, rule modifications, and permission changes.
  - 10-second temporal grouping for repetitive actions from the same player.
  - "Registro" (Log) tab in Terminal with on-demand paginated browsing and player name search filter.
- **Configuration & Migration:**
  - Centralized balance parameters in `logtech-server.toml` (firmware modifiers, dissipation rates, trash/quarantine capacities, compaction limits, rate tracking, security, and logging).
  - `data_version: 8` in CoreBlockEntity and `data_version: 4` in machines with comprehensive migration testing.

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
