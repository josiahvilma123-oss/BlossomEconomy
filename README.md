# ❀ BlossomEconomy

The all-in-one economy plugin for Blossom SMP (Paper 1.21.11, needs Vault).

## Features
- Money system that every Vault plugin uses (auction house, Jobs, Orders, TAB scoreboard)
- `/balance [player]` (aliases `/bal`, `/money`)
- `/pay <player> <amount>` (supports `2.5k`, `1m`)
- `/baltop` - top 10 richest players
- `/shop` - pink shop menu with categories and prices already set
- `/sell` (menu), `/sell hand`, `/sell all`
- `/worth` - what the item in your hand sells for
- `/eco give|take|set|reset <player> <amount>` and `/eco reload` (permission `blossom.admin`)
- PvP kill rewards with an anti-farm cooldown
- Starting balance for new players
- Everything (prices, messages, colours) is editable in `config.yml`

Balances are saved in `plugins/BlossomEconomy/balances.yml`.

## Building the .jar

### Option 1: IntelliJ IDEA (on your PC)
1. Install **IntelliJ IDEA Community** (free). When it asks, let it download **JDK 21**.
2. File > Open > select this `BlossomEconomy` folder (the one with `pom.xml`).
3. Wait for it to finish loading (bottom-right progress bar).
4. Open the **Maven** tab on the right > BlossomEconomy > Lifecycle > double-click **package**.
5. Your plugin is at `target/BlossomEconomy.jar`.

### Option 2: GitHub (no installing)
1. Make a new repository on GitHub and upload all these files (including the `.github` folder).
2. Go to the **Actions** tab and wait for the green tick.
3. Click the finished run and download **BlossomEconomy** at the bottom.

## Installing on the server
1. Make a backup.
2. Remove the old `BlossomSMP.jar` and `EconomyShopGUI` if installed.
3. In `plugins/Essentials/config.yml` add to `disabled-commands`: pay, eco, baltop, balance, sell, worth.
4. Remove the `sell: - sellgui` alias from `commands.yml`.
5. Put `BlossomEconomy.jar` in `plugins` and restart.
