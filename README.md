# 自定义区域矿场 CustomMine

> 区域内方块按概率生成 + 掉落倍率 + 定时/挖空刷新 + 随机/固定 seed，零依赖。MIT 开源。

RPG 服"资源矿场"：在指定区域里，按设定的概率表洗出矿物布局，玩家循环挖矿；支持掉落倍率（挖 1 出 N）、定时刷新 / 挖空刷新、随机/固定 seed（固定模式每次刷新布局一致）。

- 📐 **4 种选区方式**：站两点 / 拿选区棒左右键 / 直接输坐标 / 以自身为中心半径
- 🎲 **方块概率**：每矿场独立的方块概率表（石头 80% / 煤矿 15% / 铁矿 5% ...）
- ⚒ **掉落倍率（爆率 = 数量倍率）**：默认原版 ×1；设了才生效。例：石头 ×10，挖 1 个石头掉 10 个圆石
- ⏰ **定时 / 挖空刷新**：每 N 秒刷新 / 剩余 ≤ 阈值 % 自动刷新（可只开一个或都开，默认都关，命令可开关）
- 🔢 **随机 / 固定 seed**：随机模式每次刷新布局变，固定模式用同一 seed 布局一致（不存整片布局只存 seed，省内存）
- 👀 **粒子预览**：创建后周围刷粒子让玩家看见矿场范围
- 🛡 **增量刷新 + 大区域保护 + 挖空保护**：刷新不卡服；>10 万方块大区域跳过挖空检测；刷新瞬间顶起区域内玩家防卡人
- 🚧 **爆率上限 1000 + 重叠检测警告**：爆率最高 1000；区域重叠时警告但不崩溃（掉落取先建区域）
- 🎨 品牌横幅 TinyAII；**MIT 开源**

---

## 安装

1. 下载 `custommine-1.0.0.jar`
2. 放入 `plugins/`，重启
3. `/矿场 help` 查看命令

## 命令

别名：`/矿场`、`/custommine`、`/cm`、`/mine`、`/区域矿场`

| 命令 | 权限 | 说明 |
|---|---|---|
| `/矿场 点1` / `/矿场 点2` | OP | 站两个角点记录选区 |
| `/矿场 棒` | OP | 拿选区棒（左键=点1，右键=点2） |
| `/矿场 创建 <名字> [x1 y1 z1 x2 y2 z2]` | OP | 创建矿场（不填坐标用点1/点2选区） |
| `/矿场 创建 <名字> 半径 <r>` | OP | 以自身为中心半径创建 |
| `/矿场 方块 <名字> <方块> <概率%> [<方块> <概率%>...]` | OP | 设置方块概率表 |
| `/矿场 爆率 <名字> <方块> <倍率>` | OP | 设置掉落倍率（≤1=原版） |
| `/矿场 模式 <名字> <随机/固定>` | OP | 切随机/固定 seed |
| `/矿场 定时 <名字> <秒>/<关>` | OP | 开/关定时刷新 |
| `/矿场 挖空 <名字> <开/关> [<阈值%>]` | OP | 开/关挖空刷新 |
| `/矿场 刷新 <名字>` | OP | 立即刷新 |
| `/矿场 重载` | OP | 重载插件数据（不卡服） |
| `/矿场 列表` | OP | 列出所有矿场 |
| `/矿场 信息 <名字>` | OP | 查看矿场详情 |
| `/矿场 删除 <名字>` | OP | 删除矿场 |

## 配置 / 数据

- 矿场配置存 `plugins/CustomMine/data.yml`，重启不丢。
- 定时/挖空刷新**默认关闭**，用户主动用 `/矿场 定时` / `/矿场 挖空` 开启。

## 实现原理（开源可读）

- `CustomMinePlugin`：选区 + 区域创建 + 命令分发 + 定时刷新 scheduler + 粒子预览 + 重叠/大区域保护 + 数据存 data.yml
- `MineRegion`：区域数据结构（方块概率表 + 掉落倍率 + 刷新开关 + seed）；刷新时按概率表洗出布局，固定 seed 用同一随机种子复现布局
- `MineListener`：拦截区域内 BlockBreakEvent，取消原版掉落按倍率产出 + 触发挖空检测

## 兼容

- Paper 1.21+（用 `api-version: 1.21`；纯 Bukkit API 支持 1.13+）
- Java 21
- 零依赖

## 开源许可

**MIT License** — Copyright (c) 2026 TinyAII。源码见 `src/main/java/com/mcadmin/custommine/`，可自由使用/修改/分发，请保留版权与许可声明。

---

# CustomMine (English)

Region-bounded mine with block probability, drop multiplier, timed/empty refresh, random/fixed seed. MIT open source, zero deps.

## Commands
Aliases: `/custommine`, `/cm`, `/mine`. All commands under `custommine.admin` permission.
Key ones: `/矿场 点1|点2`, `/矿场 棒` (wand), `/矿场 创建 <name> [coords|半径 <r>]`, `/矿场 方块 <name> <material> <%>…`, `/矿场 爆率 <name> <material> <×>`, `/矿场 模式 <name> <random|fixed>`, `/矿场 定时 <name> <sec|off>`, `/矿场 挖空 <name> <on|off> [%]`, `/矿场 刷新 <name>`, `/矿场 重载` (reload), `/矿场 列表`, `/矿场 信息 <name>`, `/矿场 删除 <name>`.

## Compatibility
Paper 1.21+, Java 21, zero dependencies

## License
**MIT** — Copyright (c) 2026 TinyAII. Source in `src/`. Free to use/modify/distribute; keep the copyright notice.

## Author
TinyAII · MIT 开源 · 零依赖
