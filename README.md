# Grand Witch · 大女巫

[English](README.md) · [简体中文](README.zh-CN.md)

![Grand Witch](docs/modrinth/images/01_title.png)

*(Artwork made from the mod's own textures; not an in-game screenshot.)*

**Eat her treats. Become a mouse. Outsmart the witch.**

A Minecraft **Forge 1.20.1** mod. A Grand Witch comes to villages with children, dressed as a villager, a wandering trader, a girl with a basket, a granny or a nanny. She will happily swap food with you — but what she gives you may be spiked, and once you have eaten it, *you are a mouse*.

Being a mouse is a whole different game: the world gets big, the cats come for you, and the witch will stamp on you if she sees you. But a mouse can also get into places a player never could — and the witch can be beaten at her own game.

| | |
|---|---|
| Minecraft | 1.20.1 |
| Loader | Forge 47.x (no other mod needed) |
| Sides | Required on both client and server |
| License | MIT |
| Version | 0.1.0 — **early release** |

> **Status.** The game is playable from start to finish and is covered by automated tests (headless GameTests). It has had little real play-testing: how things *look* and *feel* (poses, the long arm, brooms, the cage, glow) has not been seen much, and **multiplayer / dedicated servers have not been tested**. Please open an issue for anything odd.
>
> **AI disclosure.** The code, text and artwork of this project were made with the help of AI (Claude).

---

## Contents

- [The witch](#the-witch)
- [Her house](#her-house)
- [Life as a mouse](#life-as-a-mouse)
- [Fighting back](#fighting-back)
- [Recipes](#recipes)
- [Brooms](#brooms)
- [In the world](#in-the-world)
- [Commands](#commands)
- [Settings](#settings)
- [Install](#install)
- [Build from source](#build-from-source)
- [Known limits](#known-limits)
- [License](#license)

---

## The witch

She comes by herself, now and then: about once a minute the game looks at each player, and if a **child villager** is within 48 blocks (and no Grand Witch within 160), she has a 30 % chance to come (5 % for grown villagers only). She does not stay forever: if nothing comes of it, she is gone after about eight minutes in a puff of smoke.

She is one of **four kinds**, each deceiving in its own way (the shares are settings):

| Kind | What she does |
|---|---|
| **Gift girl** (girl with a basket) | Gives you a cake. *What it is is not decided until you eat it:* half the time a reward (a golden apple, emeralds…), half the time it makes you a mouse. She goes to the children first. |
| **Potion granny** | Sells potions for 2 emeralds. All look good; 40 % of them also carry the potion of the mouse. |
| **Cursed-gift trader** | Gives you something good — that is cursed. Carry it for about 90 seconds and you become a mouse. (You are warned at one third and two thirds.) |
| **Nanny** | Gathers up to 3 village children and leads them away to her house, where they are made mice and put in the cage. Only appears where there are children. |

She looks like a villager, a trader or one of her own faces until she is found out. When she shows what she is, **the village bells ring** and she shines for ten seconds.

What else she does:

- **Turns children into mice** — and, if the setting is on, grown villagers (one per visit) and **any baby zombie** she comes across (for good). They are themselves again after a few minutes, with milk (given to the mouse), the antidote, or when she dies.
- **Rages at mice.** A player who is a mouse, or a child turned into one, sets her off: she runs faster, stamps on mice (3 hearts about every 0.7 s), and **throws potions of the mouse** at players in sight.
- **Opens doors**, and **rides a broom** after anyone more than 6 blocks away.
- **Reaches into holes.** If you hide in a tunnel, she goes to an opening at the side of it, **lies down and puts her arm in along the tunnel, round its corners**, up to 5 blocks, and pulls you out. At the 6th block she tries now and then and sometimes (1 in 5) gets hold of you; at the 7th, 1 in 10; beyond that, never. A tunnel with no opening at the side, she cannot reach at all.
- **Does not stand over you for ever.** If you are in a tunnel she cannot reach, she gives up after a few seconds, and leaves a little while after you have got away.
- **Can be fooled.** She sometimes smells spiked food (30 %); when she does not, she eats it.

## Her house

A little cottage, built only on flat open ground: a spruce roof, a cauldron brewing over a fire, shelves, a chest with **antidote** in it, a lectern with a **diary** (nine pages, in her own voice), a brewing stand, a **cage with two caught mice** (iron bars with a gate), a mouse hole in the wall, and a porch with lanterns.

- **Anyone who walks in** sets her on them: she shows what she is, runs back (from far away, she is at the door in a puff of smoke) and goes for them for thirty seconds.
- The nanny puts the children she turns **in the cage**. They stay mice until someone lets them out and cures them.
- If the house stands empty — she is dead or gone — a new witch may come to it now and then (2 % a minute, while a player is within 96 blocks).

## Life as a mouse

- You are **small, 60 % faster, and see in the dark.** You cannot place, break or use blocks (or ride a broom).
- You **squeak by yourself** about every half minute, and every cat within 32 blocks turns to you. A crouching mouse keeps quiet.
- **Things dangerous to you — the witch, cats, monsters — glow through walls** within 20 blocks, for you only.
- **Dig cross-shaped tunnels** through soft ground (dirt, grass…), as long as you like. They keep their grass on top; plants can stand on them.
- Slip through **half-block gaps**, walk through **mouse holes**, and **nibble the food out of chests**.
- The view is cold and drained of colour (red and yellow stay bright), narrower, with dark corners.
- You turn back after **four minutes**, or sooner: drink the **antidote**, or the witch who did it dies. (Milk can be switched on in the settings.)

## Fighting back

- **Spike her food.** A potion of the mouse and any food, in the crafting grid, make a treat that looks just the same. Give it to the witch (when she is dressed up she may smell it): **she becomes a mouse herself** for a minute, with 5 hearts, running for her life, with every cat after her.
- **The potion of the mouse works on any creature** — cows, zombies, villagers… except bosses. They become mice that are *still themselves*, and change back after about five minutes (or with milk). Drunk, splashed, or on an arrow.
- **Kill wild mice** (yourself, not by cats) for a **Plague Virus**, with a 25 % chance (+10 % per level of Looting); the witch drops one 50 % of the time. It is what the potion is brewed from.

## Recipes

| Item | How |
|---|---|
| **Potion of the Mouse** | Brewing stand: awkward potion + **Plague Virus**. Gunpowder makes it a splash potion, dragon's breath a lingering one, as usual. |
| **Mouse arrows** (×8) | Crafting table: 8 arrows round 1 potion of the mouse (drinkable, splash or lingering). The bottle comes back. |
| **Spiked food** | Crafting grid: 1 potion of the mouse + 1 food. The bottle comes back. |
| **Antidote** | Golden carrot + spider eye + glass bottle (1 bottle); or throw them into a water-filled cauldron over a fire (2 bottles). Drink it to turn back; right-click a mouse to give it one. |
| **Mouse hole** | Cobblestone + string. |
| **Wooden broom** | 2 sticks over 2 wheat (stick, stick, wheat wheat). |
| **Golden broom** | 2 sticks over 3 gold ingots. |

## Brooms

Ride them **low over the ground**. Hold **Space** to climb, look up or down to steer, crouch to get off (crouch + use to pick it up). The wooden broom hovers about 1 block up and climbs to 3 (about 5.6 blocks a second, 300 seconds of flight); the golden one hovers 1.5 up, climbs to 6, and is faster (about 10 blocks a second, 900 seconds). The witch rides one too, when she has somewhere to be.

## In the world

- **Mouse holes** come up on their own in the sides of banks of soft ground in the plains, forests and hills (a tunnel of two or three blocks runs in from each), with a few **wild mice** about. (Only in chunks made after the setting is on.)
- **Wild mice** also come up on the ground in any biome of the overworld, a few now and then.

## Commands

Need operator rights (permission level 2):

| Command | Does |
|---|---|
| `/grandwitch witch` | Calls a witch (with her house). |
| `/grandwitch hut` | Builds her house in front of you. |
| `/grandwitch mouse` | Turns you into a mouse; again to turn back. |
| `/grandwitch cure` | Cures you. |

## Settings

`config/grandwitch-common.toml`. Almost every number above is a setting. A few of them:

| Setting | Default | |
|---|---|---|
| `enabled` | `true` | Whether she ever comes by herself |
| `chance` / `checkSeconds` | `0.3` / `60` | How often she comes |
| `mouseSeconds` | `240` | How long a player stays a mouse |
| `childMouseSeconds` | `300` | How long a child (or any creature) stays a mouse |
| `milkCures` | `false` | Whether milk turns a mouse player back |
| `giftPoisonChance` | `0.5` | Chance a gift cake is the mouse |
| `stompDamage` | `6.0` | Damage of her stamp (2 = one heart) |
| `catsHunt`, `squeakSeconds` | `true`, `30` | Cats hunting mice, how often a mouse squeaks |
| `dangerSense` | `true` | Danger glows through walls for a mouse |
| `witchReach` | `true` | The long arm into holes |
| `witchBroom`, `witchBroomSpeed` | `true`, `0.33` | The witch on a broom |
| `turnsChildren`, `turnsAdults`, `turnsBabyZombies` | `true` | Who she turns into mice |
| `kindGift` / `kindPotion` / `kindCurse` / `kindNanny` | `35 / 25 / 20 / 20` | Shares of the four kinds (set one to 0 to switch it off) |
| `plagueVirusChance`, `plagueVirusWitchChance` | `0.25`, `0.5` | Plague virus drops |
| `worldMouseHoles`, `worldMouseHoleChance` | `true`, `0.03` | Holes in the world |
| `hutRespawn`, `hutRespawnChance` | `true`, `0.02` | New witches for empty houses |
| `witchHut`, `hutAlarm`, `villageAlarm` | `true` | The house and the alarms |

## Install

1. Install **Minecraft 1.20.1** and **Forge 47.x**.
2. Put `grand-witch-forge-1.20.1-0.1.0.jar` in the `mods` folder (on the server too).
3. Start the game. Try `/grandwitch witch` in a creative world.

## Build from source

Needs Java 17.

```sh
./gradlew build                  # build/libs/grand-witch-forge-1.20.1-<version>.jar
./gradlew runGameTestServer      # runs the automated in-game tests (headless)
./gradlew runClient              # starts the game with the mod
```

> `gradle.properties` contains `org.gradle.java.home=...` pointing at a Homebrew Java 17 on the author's Mac. Change it, or remove the line, if your Java lives elsewhere.

The code is in `src/main/java/com/xiaofeiwu/grandwitch` (the witch is `GrandWitch`, the mouse state is `Mice`, the house is `WitchHut`); the tests are `GrandWitchGameTests`, one per behaviour.

## Known limits

- Multiplayer / dedicated servers: **not tested**.
- Not seen much in real play: the witch's lying-down pose and long arm, the broom's feel, the cage, the glow, the hole generation. They are written to the best of what could be checked headless.
- A few tests (the witch reaching into holes) have failed now and then, rarely, for reasons not found yet.
- The mouse potion on an arrow is covered by a test of the effect, not by shooting a real arrow.
- Not tested with other mods.

## License

[MIT](LICENSE). Minecraft and Forge are the property of their owners; this is an unofficial mod, not affiliated with Mojang or Microsoft.
