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

| Permission                         | Description                                                | Default   |
|-------------------------------------|------------------------------------------------------------|-----------|
| nclaim.use                         | Access basic plugin features                               | true      |
| nclaim.help                        | Access help commands                                       | false     |
| nclaim.balance                     | View claim balance                                         | false     |
| nclaim.buy                         | Buy new claims                                             | false     |
| nclaim.level                       | View claim levels and info                                 | false     |
| nclaim.admin                       | Access all admin commands                                  | op        |
| nclaim.adminmenu                   | Access admin menu                                          | false     |
| nclaim.reload                      | Reload plugin configuration                                | false     |
| nclaim.add                         | Add a new claim                                            | false     |
| nclaim.remove                      | Remove an existing claim                                   | false     |
| nclaim.set                         | Set claim parameters                                       | false     |
| nclaim.change                      | Change plugin settings (language, blockvalue etc.)         | false     |
| nclaim.bypass.*                    | Access all bypass permissions                              | false     |
| nclaim.bypass.blacklisted_worlds   | Bypass world blacklist restriction                         | false     |
| nclaim.bypass.blacklisted_regions  | Bypass region blacklist restriction                        | false     |
| nclaim.bypass.max_claim_count      | Bypass maximum number of claims limit                      | false     |
| nclaim.bypass.claim_buy_price      | Bypass claim purchase price                                | false     |
| nclaim.bypass.land_buy_price       | Bypass land expansion price                                | false     |
| nclaim.bypass.pvp                  | Bypass PvP restrictions in claims                          | false     |
| nclaim.bypass.interact             | Bypass interaction restrictions in claims                  | false     |
| nclaim.bypass.break                | Bypass block break restrictions in claims                  | false     |
| nclaim.bypass.place                | Bypass block place restrictions in claims                  | false     |
| nclaim.maxclaim.<amount>           | Set the maximum claim count for a player (permission node) | false     |
| nclaim.*                           | All plugin permissions                                     | false     |

---

## 🧩 Placeholders

| Placeholder                                            | Description                                                                                   |
|--------------------------------------------------------|-----------------------------------------------------------------------------------------------|
| `%nclaim_player_balance%`                              | Shows the player's balance (Vault or the plugin's own system)                                 |
| `%nclaim_get_string_path%`                             | Gets a string value from `config.yml` (`path` = config key)                                   |
| `%nclaim_get_int_path%`                                | Gets an integer value from `config.yml` (`path` = config key)                                 |
| `%nclaim_get_boolean_path%`                            | Gets a boolean value from `config.yml` (`path` = config key)                                  |
| `%nclaim_get_list_path_index%`                         | Gets a list value (by index) from `config.yml` (`path` = key, `index` = position)             |
| `%nclaim_claim_main_value_world_chunkX_chunkZ%`        | Gets the value of the main claim chunk at given world, X, Z coordinates                       |
| `%nclaim_claim_total_value_world_chunkX_chunkZ%`       | Gets the total value of all chunks in the claim at given world, X, Z coordinates              |
| `%nclaim_block_value_material%`                        | Gets the configured value for the specified block material (e.g. `diamond_block`)             |
| `%nclaim_expiration_world_chunkX_chunkZ%`              | Shows the expiration time for the claim at X, Z chunk in given world                          |
| `%nclaim_owner_world_chunkX_chunkZ%`                   | Shows the owner of the claim at X, Z chunk in given world                                     |
| `%nclaim_coop_count_world_chunkX_chunkZ%`              | Shows the co-op member count for the claim at X, Z chunk in given world                       |
| `%nclaim_total_size_world_chunkX_chunkZ%`              | Shows the total chunk count for the claim at X, Z chunk in given world                        |

> Replace variables (like `path`, `index`, `world`, `chunkX`, `chunkZ`, `material`) with actual values.  
> Example: `%nclaim_block_value_diamond_block%` or `%nclaim_get_list_blacklisted_worlds_0%` or `%nclaim_total_size_world_0_0%`

---

## ⚙️ Configuration Example

Here’s a sample from `config.yml`:

```yaml

blacklisted_worlds:
  - world  
  - spawn   
  - pvp     

blacklisted_regions:
  - spawnarea 

claim_settings:
  max_count: 3        
  buy_price: 1500    
  expand_price: 2000 
  max_coop: 3         
  expiry_days: 7      

auto_save: 30

time_extension:
  price_per_minute: 25.0
  price_per_hour: 1500.0
  price_per_day: 5000.0
  tax_rate: 0.1

database:
  enable: true
  type: mysql # or sqlite
  sqlite:
    file: database.db
  mysql:
    host: localhost
    port: 3306
    database: nclaim
    user: root
    password: root
    maximum_pool_size: 10
    minimum_idle: 2
    idle_timeout: 60000
    max_lifetime: 1800000
    connection_timeout: 30000
```

You can fully customize claim prices, limits, language files, blacklisted worlds, and much more.

---

## 🖼️ GUI & User Interface

> NClaim comes with advanced in-game GUIs for both players and admins. Use `/nclaim` and related commands to open interactive menus for managing claims, co-op members, and more.

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

![1](https://github.com/user-attachments/assets/20e6aeb3-5d69-490f-b67d-cae84d38d02a)
![2](https://github.com/user-attachments/assets/dc3c25c3-87c1-4539-bb88-5ca3fd8b0ce6)
![3](https://github.com/user-attachments/assets/4e5807ce-998f-4121-91c5-a4ceb1d17248)
![4](https://github.com/user-attachments/assets/d87d0166-6d59-449d-9fd7-bbceb55102f5)
![5](https://github.com/user-attachments/assets/3ac88a5b-df70-4cc4-be00-215d6eadbf3a)
![6](https://github.com/user-attachments/assets/7456bb35-bab1-4dda-93d6-b926b8790191)
![7](https://github.com/user-attachments/assets/a8770a50-4d63-4d09-a31e-a0d10aed1eec)




**For more details, check the source code or request a special section for your use case!**
