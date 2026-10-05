# Modrinth listing — Grand Witch / 大女巫

Everything below is meant to be copied into the Modrinth "Create a project" page and the first version's upload form. English first, Chinese after.

---

## 1. Project fields

| Field | Value |
|---|---|
| **Name** | `Grand Witch` |
| **URL / slug** | `grand-witch` (if it is taken: `grandwitch-mod`) |
| **Summary** (max 256 chars) | `A witch in disguise swaps treats with villagers. Eat one and you become a mouse: tiny, fast and hunted by cats. Dig tunnels, hide in holes, spike her food, and turn the tables.` |
| **Project type** | Mod |
| **Loader** | Forge |
| **Game version** | 1.20.1 |
| **Environment** | Client: **required** · Server: **required** (it adds entities, blocks and items, and changes how a player is drawn) |
| **Primary categories** (pick up to 3) | `Mobs` · `Adventure` · `Magic` |
| **Additional categories** | `Game Mechanics` · `Worldgen` · `Transportation` |
| **License** | `MIT` |
| **Icon** | `images/icon.png` (512 × 512) |
| **Source / Issues / Wiki links** | fill in your own repository (the Issues link matters: it is where bug reports go) |
| **Donation links** | optional |

> The Chinese name goes into the description, not into the Name field (Modrinth shows one name; keep it English so people can find and link to it). The in-game name is already `大女巫 | Grand Witch`.

---

## 2. Description (paste into the "Description" box, Markdown)

```markdown
# Grand Witch · 大女巫

**Eat her treats. Become a mouse. Outsmart the witch.**

A Grand Witch comes to villages with children, dressed as a villager, a wandering trader, a girl with a basket, a granny or a nanny. She will happily swap food with you. But what she gives you may be spiked — and once you have eaten it, *you are a mouse*.

Being a mouse is a whole different game: the world gets big, the cats come for you, and the witch will stamp on you if she sees you. But a mouse can also get into places a player never could.

> **Status: early release (0.1.0).** The game is playable from start to finish and is covered by automated tests, but it has had little play-testing. Please report anything odd on the issue tracker.

## The witch

- **Four kinds, many faces.** A *gift girl*, a *potion granny*, a *cursed-gift trader* and a *nanny* who leads village children away. She looks like a villager or trader until she is found out.
- **She turns children into mice** (and, if you allow it, grown villagers; and any baby zombie she dislikes). Milk, the antidote, or killing her turns them back.
- **Her house.** A cottage with a cauldron brewing over a fire, shelves, a chest with antidote in it, a diary written in her own voice, and a **cage** where she keeps the mice she has caught. Walk in uninvited and she is on you at once.
- **She is not helpless prey.** She throws potions of the mouse at you, rides a broom after you if you run, opens doors, stamps on mice, and — if you hide in a hole — lies down and **reaches in with a long arm**, following the tunnel round its corners. Dig deep enough and she can only feel about in the dark.
- **She does not stay forever,** and if her house is empty, a new witch may come to it now and then.

## Life as a mouse

- You are **small, quick and see in the dark**, and cannot place, break or use blocks.
- You **squeak by yourself**, and the cats nearby come for you. Dangerous things (the witch, cats, monsters) **glow through walls** for you.
- **Dig cross-shaped tunnels** through soft ground; they keep their grass on top. Slip through half-block gaps, **hide in mouse holes**, nibble the food out of chests.
- **Mouse holes** also turn up on their own in the sides of hills, with a few wild mice about them.

## Fighting back

- **Spike her food.** A potion of the mouse and any food, in the crafting grid, make a treat that looks just the same. Give it to the witch and she becomes a mouse herself for a minute: five hearts of health, running for her life, with the cats after her.
- **The potion of the mouse** works on *any creature* (not bosses): cows, zombies, villagers. It lasts a few minutes, and they are themselves again afterwards.
- **Brew it** — an awkward potion and a **Plague Virus** (dropped by wild mice a player kills, and by the witch) in a brewing stand. Gunpowder makes it splash, as usual.
- **Mouse arrows** — eight arrows round a potion of the mouse.
- **The antidote** turns a mouse back into what it was: golden carrot + spider eye + glass bottle, or brewed in her own cauldron over a fire.

## Brooms

A **wooden broom** and a **golden broom**. Ride them low over the ground: hold **Space** to climb (up to 3 blocks for the wooden one, 6 for the golden one), look up or down to steer. They wear out as you fly. The witch rides one too, when she has somewhere to be.

## Commands (need operator rights)

`/grandwitch witch` — call a witch (with her house) · `/grandwitch hut` — build her house in front of you · `/grandwitch mouse` — become a mouse, again to turn back · `/grandwitch cure` — cure yourself

## Settings

Everything above can be changed in `config/grandwitch-common.toml`: how often she comes, how long a mouse stays a mouse, whether children / adults / baby zombies are turned, whether cats hunt you, her potions, her broom, her arm, the house, the holes in the world, the drop chances, and a switch for each of the four kinds of witch.

## Compatibility

- Minecraft **1.20.1**, **Forge** 47+. No other mod is needed.
- Singleplayer is what it has been played in. **Multiplayer / dedicated servers have not been tested yet**; if you try it, install it on both sides, and please tell us how it went.
- Not tested with other mods that change how players are drawn or add their own mob-turning effects.

## Frequently asked

**Is it hard to get back?** No: milk, the antidote, or killing the witch all work, and there is a command. A mouse also turns back by itself after a while.
**Can she kill me?** As a mouse you can be stamped on and hurt; as a player she throws potions of the mouse at you. Both can be switched off or toned down in the settings.
**Will it break my villages?** No block is placed apart from her cottage and the little holes; villagers who are turned become mice and come back to themselves.

---

# 中文说明

**吃了她的点心，你就变成一只老鼠。**

大女巫会扮成村民、流浪商人、提篮子的少女、老太婆或保姆，出现在有小孩的村子附近，用点心跟你换食物。她给你的东西**可能加了料**，吃下去，你就变成老鼠。

> **状态：早期版本（0.1.0）**。从头到尾可以玩，有自动测试，但实际游玩测试还不多，遇到奇怪的地方请在问题页反馈。

- **女巫**：四种（送礼少女、卖药婆、诅咒礼物商人、带小孩走的保姆），伪装成村民；会把小孩变成老鼠，也会讨厌小僵尸；有自己的小屋（药锅、日记、解药箱、关老鼠的笼子）；会骑扫把追你、开门、趴下来**伸长手臂顺着隧道抓**躲在洞里的老鼠。
- **老鼠视角**：身子小、跑得快、夜视，不能用方块；会自己吱吱叫，把猫招来；能在软土里**挖十字形隧道**、躲进老鼠洞、偷啃箱子里的食物；女巫、猫、怪物会**隔着墙发光**。
- **反击**：变鼠药水 + 任意食物，合成"加了料"的点心，给她吃，她会变成一分钟的老鼠。变鼠药水对**任何生物**有效（Boss 除外）；酿造台里**粗制药水 + 鼠疫病毒**（野生老鼠、女巫掉落）；**8 支箭围着 1 瓶药水**得到药水箭；**解鼠药剂**（金胡萝卜 + 蜘蛛眼 + 玻璃瓶）能变回来。
- **扫把**：木扫把、金扫把，低空飞，按住空格上升（木 3 格、金 6 格）。
- **世界**：山坡侧面会自然出现老鼠洞，洞边有野生老鼠。
- **命令**（需要管理员权限）：`/grandwitch witch|hut|mouse|cure`。
- **配置**：几乎所有数字和开关都在配置文件里。
- **版本**：Minecraft 1.20.1，Forge 47+；目前主要在单人游戏里玩过，**联机 / 专用服务器还没有测试**，联机的话两端都要装。

## License / 许可

Code and original textures: MIT. Some of the artwork on this page is made from the mod's own textures and from the game's item textures; none of it is an in-game screenshot.
```

---

## 3. Gallery (upload in this order; the first is the featured image)

| File | Title | Description |
|---|---|---|
| `01_title.png` | Grand Witch | Eat her treats. Become a mouse. Outsmart the witch. *(featured)* |
| `02_witches.png` | One witch, many faces | The witch's true face and some of her disguises. |
| `03_mouse.png` | Life as a mouse | Small, quick and hunted by cats; tunnels, holes, chests. |
| `04_recipes.png` | Brew it, shoot it, cure it | The potion of the mouse, mouse arrows and the antidote. |
| `05_brooms.png` | Brooms | A wooden and a golden broom, ridden low over the ground. |
| `06_features.png` | What's inside | A short list of what the mod adds. |

> These are **artwork made from the mod's textures, not in-game screenshots.** Modrinth (and players) trust real screenshots more. Once you have run the game, replace some of these with real screenshots. The best ones to take:
> 1. the witch (undisguised, purple coat) standing in front of her cottage;
> 2. the inside of the cottage with the cauldron and the cage;
> 3. you as a mouse, in a tunnel, with the witch glowing through the wall;
> 4. the witch lying down with her arm in a hole;
> 5. a hole in a hillside with a mouse at its mouth;
> 6. a broom ride.

---

## 4. First version (Versions → Create version)

| Field | Value |
|---|---|
| **Version number** | `0.1.0` |
| **Name** | `Grand Witch 0.1.0 (early release)` |
| **Release channel** | **Beta** (an Alpha is more honest still; do not call it Release until you have played it) |
| **Loader / game version** | Forge · 1.20.1 |
| **File** | `build/libs/grand-witch-forge-1.20.1-0.1.0.jar` |
| **Dependencies** | none |

**Changelog** (paste):

```markdown
First public release.

- A Grand Witch (four kinds, many disguises) that comes to villages with children, swaps food with you, and turns children into mice.
- Mouse form for players: small, fast, night vision, cats hunt you, dig tunnels, hide in mouse holes, nibble chests, see danger glow through walls.
- The witch's cottage: cauldron, diary, antidote, a cage; she opens doors, rides a broom, reaches into holes with a long arm.
- Spiked food turns the witch into a mouse; the potion of the mouse works on any creature; brewing with Plague Virus; mouse arrows; the antidote.
- Wooden and golden brooms.
- Mouse holes in the sides of hills; wild mice.
- Commands: /grandwitch witch | hut | mouse | cure.
```

---

## 4b. The order the site wants

1. Create the project (name, URL, summary).
2. **Upload the first version first** (Files → Environment → Metadata → Details). Environment: **Client and server → Required on both**.
3. Only then can you fill in the Description, icon, categories, license and gallery.
4. Last, **Submit for review**.

## 5. Before you press "Submit for review"

1. Read Modrinth's **Content Rules** page once more: titles/summaries/descriptions in English (a translation below is fine), no misleading images, no keyword stuffing, no placeholder text. I could not fetch it from here, so please check it yourself.
2. The project is checked by staff before it goes public (days to a couple of weeks). Fill every section so it is not sent back.
3. Check the slug is free (the site tells you), and the name is not too close to another project.
4. Do not claim anything you have not seen in the game. The text above says "early release" on purpose.
5. Posting also on **CurseForge** and **MC百科 (mcmod.cn)** reaches many more Chinese players; the Chinese section above can be reused there.
