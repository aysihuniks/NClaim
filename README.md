# NClaim – Advanced Chunk Claim Plugin for Minecraft

[![spigot](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/compact/available/spigot_vector.svg)](https://www.spigotmc.org/resources/nclaim-advanced-claim-system.122527/)
[![builtbybit](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/compact/available/builtbybit_vector.svg)](https://builtbybit.com/resources/nclaim-advanced-claim-system.60265/)
[![modrinth](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/compact/available/modrinth_vector.svg)](https://modrinth.com/plugin/nclaim)

NClaim is a fully customizable chunk claim plugin for Minecraft servers, designed to protect players’ builds and items. With features like co-op claims, claim expiration, world blacklisting, and a flexible configuration system, NClaim provides a robust solution for both server owners and players.

[![discord](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/compact/social/discord-plural_vector.svg)](https://discord.gg/qcW6YrxwqJ)

---

## 📁 Project Structure

```
src/
└── main/
    └── java/
        └── nesoi/
            └── aysihuniks/
                └── nclaim/
                    ├── commands/      # All commands & executors
                    ├── integrations/  # Plugin integrations
                    ├── model/         # Data models (Claim, Coop, etc.)
                    ├── service/       # Claim & coop management systems
                    ├── ui/            # GUI and menu system
                    └── utils/         # Helpers, configuration, language management
```

---

## ⚙️ Features

- 🏷️ **Claiming Chunks:** Players can claim chunks to protect their builds and items.
- 🛡️ **Claim Management:** Players can remotely view, expand, extend or delete their own claims.
- 👥 **Co-op System:** Add friends to your claim and set individual permissions for each co-op member.
- ⏳ **Claim Expiration:** Claims automatically expire after a configurable period, unless extended.
- 🌍 **World Blacklisting:** Prevent claims in specified worlds.
- 🗺️ **Region Blacklisting (WorldGuard):** Block claiming in specific WorldGuard regions for extra control.
- 🔑 **Advanced Permissions:** Detailed permission system for both players and admins.
- 🛠️ **Highly Configurable:** Customize messages, gui texts, claim settings, and plugin behavior via configuration files.
- 🧩 **PlaceholderAPI Support:** Use various placeholders for in-game information and external integrations.
- 📦 **Flexible Storage:** Supports YAML, SQLite, and MySQL for claim data storage.

---

## 🛠️ Installation

1. Download the latest version of NClaim from [GitHub Releases](https://github.com/aysihuniks/NClaim).
2. Place the `.jar` file in your server’s `plugins` directory.
3. Restart your server.
4. Edit the generated `config.yml` and other files in the plugin folder to suit your needs.
5. (Optional) Install [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) for in-game placeholders.

---

## 📝 Commands

| Command                | Description                           | Permission          | 
|------------------------|---------------------------------------|---------------------|
| `/nclaim help`         | Shows the help menu                   | `nclaim.help`       | 
| `/nclaim about`        | Plugin information                    |  `-`                | 
| `/nclaim level`        | View claim value and info             | `nclaim.level`      |
| `/nclaim balance`      | Shows your balance                    | `nclaim.balance`    |       
| `/nclaim admin`        | Access admin commands                 | `nclaim.admin`      |          

---

## 🔐 Permissions

> Version column shows the version in which the placeholder was added (e.g. 1.3.1 means added in 1.3.1 and above)

| Permission                         | Description                                                          | Default   | Version |
|------------------------------------|----------------------------------------------------------------------|-----------|---------|
| nclaim.help                        | Access help commands                                                 | false     | 1.0     |
| nclaim.balance                     | View claim balance                                                   | false     | 1.0     |
| nclaim.buy                         | Buy new claims                                                       | false     | 1.0     |
| nclaim.admin                       | Access all admin commands                                            | false     | 1.0     |
| nclaim.reload                      | Reload plugin configuration                                          | false     | 1.0     |
| nclaim.add                         | Access /nclaim admin add command                                     | false     | 1.0     |
| nclaim.remove                      | Access /nclaim admin remove command                                  | false     | 1.0     |
| nclaim.set                         | Access /nclaim admin set command                                     | false     | 1.0     |
| nclaim.change                      | Change plugin settings (language, blockvalue etc.)                   | false     | 1.0     |
| nclaim.bypass.*                    | Access all bypass permissions                                        | false     | 1.0     |
| nclaim.bypass.claim_buy_price      | Bypass claim purchase price                                          | false     | 1.0     |
| nclaim.bypass.land_buy_price       | Bypass land expansion price                                          | false     | 1.0     |
| nclaim.bypass.max_claim_count      | Bypass maximum number of claims limit                                | false     | 1.3     |
| nclaim.adminmenu                   | Access admin menu                                                    | false     | 1.3.1   |
| nclaim.bypass.blacklisted_worlds   | Bypass world blacklist restriction                                   | false     | 2.0     |
| nclaim.bypass.blacklisted_regions  | Bypass region blacklist restriction                                  | false     | 2.0     |
| nclaim.bypass.pvp                  | Bypass PvP restrictions in claims                                    | false     | 2.0     |
| nclaim.bypass.interact             | Bypass interaction restrictions in claims                            | false     | 2.0     |
| nclaim.bypass.break                | Bypass block break restrictions in claims                            | false     | 2.0     |
| nclaim.bypass.place                | Bypass block place restrictions in claims                            | false     | 2.0     |
| nclaim.maxclaim.<amount>           | Set the maximum claim count for a player (permission node)           | false     | 2.0     |
| nclaim.use                         | Access basic plugin features (buy a claim, manage claim etc.)        | true      | 2.0     |
| nclaim.level                       | View claim levels and info                                           | false     | 2.0     |
| nclaim.*                           | All plugin permissions                                               | op        | 2.0     |
| nclaim.bypass.max_coop_count       | Bypass maximum number of claim coop player limit                     | false     | 2.1     |
| nclaim.manage_claim_block          | Access open the claim block manager menu                             | false     | 2.1     |
| nclaim.maxcoop.<amount>            | Set the maximum claim coop count for a claim owner (permission node) | false     | 2.1     |
| nclaim.bypass.axsellwand           | Bypass AxSellWand usage restrictions in claims                       | false     | 2.1.4   |

---

## 🧩 Placeholders

> Version column shows the version in which the placeholder was added (e.g. 2.0 means added in 2.0 and above)

| Placeholder                                            | Description                                                                          | Version              |
|--------------------------------------------------------|--------------------------------------------------------------------------------------|----------------------|
| `%nclaim_player_balance%`                              | Shows the player's balance (Vault or the plugin's own system)                        | 1.0                 |
| `%nclaim_get_string_path%`                             | Gets a string value from `config.yml` (`path` = config key)                          | 1.0                 |
| `%nclaim_get_int_path%`                                | Gets an integer value from `config.yml` (`path` = config key)                        | 1.0                 |
| `%nclaim_get_boolean_path%`                            | Gets a boolean value from `config.yml` (`path` = config key)                         | 1.0                 |
| `%nclaim_get_list_path_index%`                         | Gets a list value (by index) from `config.yml` (`path` = key, `index` = position)    | 1.0                 |
| `%nclaim_expiration_world_chunkX_chunkZ%`              | Shows the expiration time for the claim at X, Z chunk in given world                 | 1.0                 |
| `%nclaim_owner_world_chunkX_chunkZ%`                   | Shows the owner of the claim at X, Z chunk in given world                            | 1.0                 |
| `%nclaim_coop_count_world_chunkX_chunkZ%`              | Shows the co-op member count for the claim at X, Z chunk in given world              | 1.0                 |
| `%nclaim_total_size_world_chunkX_chunkZ%`              | Shows the total chunk count for the claim at X, Z chunk in given world               | 1.0                 |
| `%nclaim_claim_main_value_world_chunkX_chunkZ%`        | Gets the block value of the main claim chunk at given world, X, Z coordinates        | 2.0                 |
| `%nclaim_claim_total_value_world_chunkX_chunkZ%`       | Gets the total value of all chunks in the claim at given world, X, Z coordinates     | 2.0                 |
| `%nclaim_block_value_material%`                        | Gets the configured value for the specified block material (e.g. `diamond_block`)    | 2.0                 |
| `%nclaim_owner%`                                       | Shows the claim owner of the chunk where the player is                               | 2.1                 | 

> Replace variables (like `path`, `index`, `world`, `chunkX`, `chunkZ`, `material`) with actual values.  
> Example: `%nclaim_block_value_diamond_block%` or `%nclaim_get_list_blacklisted_worlds_0%` or `%nclaim_total_size_world_0_0%`

---

## ⚙️ Configuration Example

Here’s a sample from `config.yml`:

```yaml
# World Restrictions
# Configure which worlds and WorldGuard regions are restricted from claiming
blacklisted_worlds:
  - world   # Example: Main world
  - spawn   # Example: Spawn world
  - pvp     # Example: PvP arena world

blacklisted_regions:
  - spawnarea  # Example: spawn protection area

# Claim Settings
claim_settings:
  max_count: 3        # Maximum number of claims per player
  buy_price: 1500     # Cost to create a new claim
  expand_price: 2000  # Cost to expand an existing claim by one chunk
  max_coop_count:     # Maximum number of co-op players per claim
    default: 3
    vip: 5
  # Permissions will be need like this "nclaim.max_coop_count.default"
  expiry_days: 7      # Days until an inactive claim expires

# Auto-Save Configuration
auto_save: 30  # How often to save data (in minutes)

# Time Extension Settings
time_extension:
  price_per_minute: 25.0
  price_per_hour: 1500.0
  price_per_day: 5000.0
  tax_rate: 0.1

# Database Configuration
# Chooses between MySQL and SQLite for data storage
database:
  enable: false  # Set to true to use database storage instead of YAML
  type: "mysql"  # Options: "mysql" or "sqlite"

  # SQLite Configuration
  sqlite:
    file: database.db  # Database file name

  # MySQL Configuration
  mysql:
    host: localhost
    port: 3306
    database: nclaim
    user: root
    password: ""       # Replace with your database password
    maximum_pool_size: 10  # Maximum number of connections in the pool
    minimum_idle: 5        # Minimum number of idle connections
    idle_timeout: 300000   # Time (ms) before an idle connection is closed (default: 5 minutes)
    max_lifetime: 1800000  # Maximum lifetime (ms) of a connection (default: 30 minutes)
    connection_timeout: 30000  # Time (ms) to wait for a connection (default: 30 seconds)

```

You can fully customize claim prices, limits, language files, blacklisted worlds, and much more.

---

## 🤝 Contributing & Issues

Feel free to fork, submit pull requests, or open [issues](https://github.com/aysihuniks/NClaim/issues) for any bugs or feature requests!

> For questions or suggestions, contact [aysihuniks](https://github.com/aysihuniks).

---

## 👥 Contributors

Many thanks to everyone who has contributed to this project, including:
- [desaxxx](https://github.com/desaxxx)

---

## 📄 License

This project is licensed under the NESOI Plugin License v1.0.

---

## 📸 Screenshots

Here are some screenshots from the plugin:
![2025-06-17_15 36 48](https://github.com/user-attachments/assets/3dbfbaf5-a3a4-4dc5-98a4-45ab483742cd)
![2025-06-17_15 37 52](https://github.com/user-attachments/assets/c5705bba-8e42-4877-b53c-4379fcd379a4)
![1](https://github.com/user-attachments/assets/20e6aeb3-5d69-490f-b67d-cae84d38d02a)
![2](https://github.com/user-attachments/assets/dc3c25c3-87c1-4539-bb88-5ca3fd8b0ce6)
![3](https://github.com/user-attachments/assets/4e5807ce-998f-4121-91c5-a4ceb1d17248)
![4](https://github.com/user-attachments/assets/d87d0166-6d59-449d-9fd7-bbceb55102f5)
![5](https://github.com/user-attachments/assets/6b5067f4-c191-41ae-bfd0-ce8e04151a57)
![6](https://github.com/user-attachments/assets/3ac88a5b-df70-4cc4-be00-215d6eadbf3a)
![7](https://github.com/user-attachments/assets/7456bb35-bab1-4dda-93d6-b926b8790191)
![8](https://github.com/user-attachments/assets/a8770a50-4d63-4d09-a31e-a0d10aed1eec)


**For more details, check the source code or request a special section for your use case!**
