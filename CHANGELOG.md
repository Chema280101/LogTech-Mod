# Changelog

All notable changes to **LogTech** will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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
