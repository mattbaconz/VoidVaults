# VoidVault

> Modern, performance-first Ender Chest replacement for Minecraft 1.21+

[![Version](https://img.shields.io/badge/version-1.1.0-blue.svg)](https://github.com/yourusername/voidvault)
[![Minecraft](https://img.shields.io/badge/minecraft-1.21+-green.svg)](https://papermc.io)
[![Java](https://img.shields.io/badge/java-21-orange.svg)](https://adoptium.net)
[![License](https://img.shields.io/badge/license-MIT-lightgrey.svg)](LICENSE)

A feature-rich vault plugin with dual modes, inventory management tools, and extensive customization options.

## ✨ Features

### 📦 Inventory Management (v1.1.0)
- **Sort Button** - Organize items automatically by type
- **Quick Deposit** - Stack items from inventory into vault
- **Search/Filter** - Find items with intuitive GUI
  - Quick search buttons (Diamond, Iron, Gold, etc.)
  - Custom search with multi-keyword support
  - Visual filtering with gray glass panes

### 🎮 Dual Mode System
- **SIMPLE** - Single-page vault (9-54 slots)
- **PAGED** - Multi-page vault (up to 5 pages, 50 slots each)
- Switch modes without data loss

### 💾 Storage Options
- **MySQL** - Production-ready with HikariCP connection pooling
- **YAML** - File-based for small servers
- Easy migration between storage types

### 🔒 Permission System
- Granular slot control (9-54 slots)
- Page-based permissions (1-5 pages)
- Custom per-player overrides
- Remote access control

### 💰 Economy Integration
- Vault API support
- Configurable access fees
- Automatic refunds on errors
- Cooldown system with bypass permissions

### 🚀 Performance
- Async data loading and saving
- Smart caching with dirty tracking
- Optimized database operations
- Virtual thread support (Java 21)
- Full Folia support with automatic detection

### 🎨 Customization
- Full GUI customization (materials, names, lore)
- Custom messages with color codes
- Configurable buttons and features
- Feature toggles for all utilities

### 🔧 Admin Tools
- View and edit player vaults
- Override slot/page permissions
- Real-time configuration reload
- Performance monitoring
- bStats integration (Plugin ID: 28100)

## 📥 Installation

### New Installation
1. Download `VoidVaults-1.1.0.jar`
2. Place in your server's `plugins` folder
3. Start/restart your server
4. Configure `plugins/VoidVaults/config.yml`
5. Reload with `/vv reload` or restart

### Upgrading from v1.0.0
1. **Backup your data** (important!)
2. Stop your server
3. Replace old JAR with new version
4. Start your server (config auto-updates)
5. Test the new features

## 🎮 Commands

### Player Commands
```
/echest          - Open your vault remotely
/pv              - Alias for /echest
/vault           - Alias for /echest
```

### Admin Commands
```
/voidvaults open <player> [page]     - View player's vault
/voidvaults reload                   - Reload configuration
/voidvaults setslots <player> <amt>  - Override slot permissions
/voidvaults setpages <player> <amt>  - Override page permissions
```

## 🔑 Permissions

### Slot Permissions
```
voidvaults.slots.9    - 9 slots
voidvaults.slots.18   - 18 slots
voidvaults.slots.27   - 27 slots (default)
voidvaults.slots.36   - 36 slots
voidvaults.slots.45   - 45 slots
voidvaults.slots.50   - 50 slots (PAGED max)
voidvaults.slots.54   - 54 slots (SIMPLE max)
```

### Page Permissions (PAGED mode)
```
voidvaults.page.1     - Page 1 (default: true)
voidvaults.page.2     - Page 2
voidvaults.page.3     - Page 3
voidvaults.page.4     - Page 4
voidvaults.page.5     - Page 5 (max)
```

### Other Permissions
```
voidvaults.remote                    - Use remote access (default: op)
voidvaults.remote.bypasscooldown     - Bypass cooldown (default: op)
voidvaults.admin.openother           - View other vaults (default: op)
voidvaults.admin.reload              - Reload config (default: op)
voidvaults.admin.setslots            - Override slots (default: op)
voidvaults.admin.setpages            - Override pages (default: op)
```

## ⚙️ Configuration

### Basic Setup (config.yml)
```yaml
# Plugin mode: SIMPLE or PAGED
plugin-mode: PAGED

# Storage: YAML or MYSQL
storage-type: YAML

# Remote access
remote-access:
  enabled: true
  cooldown-seconds: 60
  economy-fee: 100.0

# Auto-save interval (minutes)
auto-save-interval: 5

# Search settings (v1.1.0)
search:
  grayout-enabled: true    # Show gray glass for filtered items
  search-mode: all         # 'name' or 'all'
  case-sensitive: false

# Feature toggles (v1.1.0)
features:
  sort-enabled: true
  quick-deposit-enabled: true
  search-enabled: true
  locked-slots-enabled: true
```

### MySQL Setup
```yaml
storage-type: MYSQL

mysql:
  host: localhost
  port: 3306
  database: voidvault
  username: root
  password: password
  pool-size: 10
```

### GUI Customization
```yaml
gui-titles:
  simple: "&5&lVault"
  paged: "&5&lVoidVault &8- &7Page {page}"

items:
  sort-button:
    material: HOPPER
    display-name: "&6&lSort Items"
    lore:
      - "&7Click to organize"
```

## 🔌 PlaceholderAPI

```
%voidvault_slots%    - Player's max slots
%voidvault_pages%    - Player's max pages
%voidvault_mode%     - Plugin mode (SIMPLE/PAGED)
```

## 📋 Requirements

- **Minecraft**: 1.21+
- **Server**: Paper, Purpur, Folia, or Spigot
- **Java**: 21 or higher

### Optional
- **Vault** - Economy integration
- **PlaceholderAPI** - Placeholder support

## 🆘 Support

- **Issues**: [GitHub Issues](https://github.com/yourusername/voidvault/issues)
- **Changelog**: [CHANGELOG.md](CHANGELOG.md)
- **Discord**: [Join Server]((https://discord.com/invite/VQjTVKjs46))
- **Paypal**: [Donate](https://www.paypal.com/paypalme/MatthewWatuna)
- **Statistics**: [bStats](https://bstats.org/plugin/bukkit/VoidVaults/28100)

## 📜 License

MIT License - see [LICENSE](LICENSE) file

## 🙏 Credits

- [Paper API](https://papermc.io) - Server API
- [HikariCP](https://github.com/brettwooldridge/HikariCP) - Connection pooling
- [bStats](https://bstats.org) - Plugin metrics

---

**Made with ❤️ for the Minecraft community**

