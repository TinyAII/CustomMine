# CustomMine 自定义区域矿场

> 在服务器里圈一块区域，按概率表生成矿物，可自定义掉落倍率，定时/挖空自动刷新。RPG 服务器资源矿场神器。

## 功能

| 功能 | 说明 |
| --- | --- |
| 📐 4 种选区方式 | 站立选点 / 木棍圈地 / 直接输坐标 / 中心+半径 |
| ⚖️ 矿物概率表 | 每个矿场独立配置方块概率（如 石头80% 煤15% 铁5%） |
| 💎 掉落倍率 | 每方块独立设置掉落数量（默认原版 ×1，上限 1000） |
| 🔄 定时刷新 / 挖空刷新 | 定时按秒刷新；挖空到阈值自动回填，双模式可开关 |
| 🎲 随机 / 固定生成 | 默认随机布局；切固定 seed 刷新后布局一致 |
| 👁️ 区域边界预览 | 粒子画出矿场范围，一眼看清 |
| ⚡ 性能保护 | 只刷已加载区块，不卡服；刷新把玩家顶出安全区+提示 |
| 🔥 热重载 | `/矿场 重载` 只重载本插件配置，不卡服 |

## 命令（仅 OP）

```
/矿场 点1 / 点2          站立记录两个角
/矿场 棒                拿木棍选区棒（左键点1 右键点2）
/矿场 创建 <名字>         用已选区创建（默认全石头）
/矿场 创建 <名字> <x1 y1 z1> <x2 y2 z2>   坐标创建
/矿场 创建 <名字> 半径 <r> 以你为中心半径 r
/矿场 方块 <名字> <方块> <权重>   设矿物概率（如 STONE 80）
/矿场 爆率 <名字> <方块> <倍率>   设掉落倍率（默认1，上限1000）
/矿场 定时 <名字> <秒|关>      定时刷新开关
/矿场 挖空 <名字> <开|关> [阈值%]  挖空自动刷新
/矿场 模式 <名字> <随机|固定>   刷新模式
/矿场 刷新 <名字>          立即刷新
/矿场 预览 <名字>          粒子预览边界
/矿场 列表 / 信息 / 删除 / 重载
```

## 快速上手

1. `/矿场 点1` → 站对角 `/矿场 点2` → `/矿场 创建 main`
2. `/矿场 方块 main STONE 80`、`/矿场 方块 main COAL_ORE 15`、`/矿场 方块 main IRON_ORE 5`
3. `/矿场 爆率 main IRON_ORE 3`（挖1个铁矿石掉3个）
4. `/矿场 定时 main 3600`（每1小时刷新）
5. `/矿场 刷新 main` → 看区域按 80/15/5 概率填出矿石

## 配置

矿场数据存 `plugins/CustomMine/data.yml`，手动改后 `/矿场 重载` 热生效。

## 兼容

- Paper 1.21.8（及 Purpur / Leaves 1.21.8）
- Java 17+
- 零依赖，无需 WorldEdit

## 技术亮点

- 增量刷新：只改已加载区块，超大矿场不卡服
- 粒子边界预览：无 WorldEdit 也能直观选区
- 坐标重叠检测：创建时警告与已有矿场重叠

---

# CustomMine (English)

Generate custom ore regions: pick a box area, set block probabilities, set drop multipliers, auto-refresh on a timer or when mined empty. Great for RPG servers.

## Features

- 4 selection methods: stand points / stick wand / direct coords / center + radius
- Per-region block probability table (e.g. stone 80%, coal 15%, iron 5%)
- Per-block drop multiplier (default vanilla ×1, max 1000)
- Timer refresh / refresh when mined empty (switchable)
- Random / fixed layout (fixed seed = same layout each refresh)
- Particle region preview
- Performance-safe: only refreshes loaded chunks; ejects players + ActionBar notice
- Hot reload via `/矿场 重载`

## Commands (OP only)

`/矿场 点1` `/矿场 点2` `/矿场 棒` `/矿场 创建` `/矿场 方块` `/矿场 爆率` `/矿场 定时` `/矿场 挖空` `/矿场 模式` `/矿场 刷新` `/矿场 预览` `/矿场 列表/信息/删除/重载`

## Compatibility

- Paper 1.21.8 (Purpur / Leaves 1.21.8)
- Java 17+
- Zero dependencies, no WorldEdit required

## Author

TinyAII · 免费开源 · 零依赖