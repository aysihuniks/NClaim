# NClaim – Advanced Land Claim Plugin

[![License](https://img.shields.io/badge/license-NESOI%20Plugin%20License%20v1.0-blue.svg)](./LICENSE)

NClaim is a customizable land claim plugin for Minecraft servers, designed to protect players' builds and items. With features like co-op systems, claim expiration, and blacklisted worlds, NClaim offers a comprehensive solution for server administrators.

## ⚙️ Features

- **Co-op System**: Add friends to your claim, manage permissions, and kick them from the claim.
- **Expand System**: Expand your claim's chunks without purchasing a new claim.
- **Blacklisted Worlds**: Prevent claim purchases in specified worlds.
- **Claim Expiry**: Set expiration dates for claims; currently, only admins can extend this duration.
- **Permissions**: Configure permissions for players added to the co-op.
- **Customizable Messages**: Modify in-game messages via `messages.yml` and settings in `config.yml`.
- **Placeholders**: Utilize placeholders to fetch data from `config.yml` in-game.

## 🛠️ Installation

1. Download the latest version of NClaim.
2. Place the `.jar` file into your server's `plugins` directory.
3. Restart your server.

## 📝 Commands

- `/nclaim help` – Displays available commands.
- `/nclaim admin` – Shows admin commands.
- `/nclaim about` – Provides plugin information.
- `/nclaim balance` – Shows the player's balance.

## 🔐 Permissions

- `nclaim.help` – Access to help commands.
- `nclaim.admin` – Access to all admin commands.
- `nclaim.reload` – Access to the reload command.
- `nclaim.add` – Access to the add command.
- `nclaim.remove` – Access to the remove command.
- `nclaim.delete` – Access to the delete command.

## 🧩 Placeholders

- `%nclaim_get_string_path%` – Retrieves string values from `config.yml`.
- `%nclaim_get_int_path%` – Retrieves integer values from `config.yml`.
- `%nclaim_get_boolean_path%` – Retrieves boolean values from `config.yml`.
- `%nclaim_get_list_path_index%` – Retrieves list values from `config.yml` by index.
- `%nclaim_expiration_chunkX_chunkZ%` – Displays expiration time of a chunk at given coordinates.
- `%nclaim_owner_chunkX_chunkZ%` – Displays owner name of a chunk at given coordinates.
- `%nclaim_player_balance%` – Displays the player's balance.

## ⚙️ Configuration

Customize the plugin's behavior by editing the `config.yml` file. Key settings include:

- `max_claim_count`: Maximum number of claims a player can own.
- `claim_buy_price`: Amount deducted from a player's account when purchasing a new claim.
- `each_land_buy_price`: Cost to expand an existing claim.
- `max_coop_players`: Number of people who can co-op your claim.
- `claim_expiry_days`: Number of days until the claim expires after purchase.

Example configuration:

```yaml
max_claim_count: 3
claim_buy_price: 1500
each_land_buy_price: 2000
max_coop_players: 3
claim_expiry_days: 7
