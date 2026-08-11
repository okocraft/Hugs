# Game Test Guidelines

## Scope

These tests provide the minimum in-game smoke coverage needed before releasing Hugs. A PASS means that the plugin's user-visible core behavior works on the current Paper runtime:

- Sneak + right-click hugging triggers only under the intended conditions.
- Player, non-player living entity, and self-hug targets produce the expected feedback.
- `/hug <player>` and its main rejection paths behave correctly.
- Heart particles, hug sounds, and translated messages are observable by the relevant players.
- Repeated hugs within one second are suppressed.
- No Hugs-related runtime exceptions occur during the test flow.

A Maven CI workflow exists, but no automated test suite covering these in-game behaviors was found in the inspected repository contents. Therefore, the platform-facing behavior below must be verified in-game.

## Core Behaviors

### CB-001: Sneak-right-click hugging

Description:
A player with the `hugs.hug` permission can hug a `LivingEntity` by sneaking and right-clicking it with the main hand. A normal right-click without sneaking must not trigger a hug.

Importance:
This is the plugin's primary interaction documented in the README. If it fails, the main feature is unavailable.

Related code:
- `src/main/java/net/okocraft/hugs/Hugs.java`

### CB-002: Target-specific hug feedback

Description:
A successful hug gives the initiating player heart particles, the hug sound, and a result message. When the target is another player, that player also receives particles, sound, and a notification. When the target is a non-player `LivingEntity`, only the initiator receives the target-name message. Self-hugging uses a dedicated message.

Importance:
If the action executes without visible, audible, or textual feedback, users cannot reliably tell that the hug succeeded.

Related code:
- `src/main/java/net/okocraft/hugs/Hugs.java`
- `src/main/java/net/okocraft/hugs/Messages.java`
- `src/main/resources/en.properties`
- `src/main/resources/ja_JP.properties`

### CB-003: `/hug` command

Description:
A player with the `hugs.command` permission can hug an online player with `/hug <player>`. Missing arguments, an unavailable target, and insufficient permission must reject the action and return the corresponding message.

Importance:
This is the second public hug path documented in the README. Because the permission defaults to `op`, broken permission handling would also be a server-administration regression.

Related code:
- `src/main/java/net/okocraft/hugs/Hugs.java`
- `src/main/resources/plugin.yml`

### CB-004: Repeated-hug suppression

Description:
A repeated hug from the same player within one second is ignored. After at least one second, the player can hug again.

Importance:
This prevents repeated event delivery or rapid input from generating duplicate particles, sounds, and messages. If it fails, an ordinary interaction may appear to execute multiple times.

Related code:
- `src/main/java/net/okocraft/hugs/Hugs.java`

## Platform Dependency Points

### DP-001: Entity interaction event

Platform:
Paper | Bukkit API

Usage:
The plugin handles `PlayerInteractEntityEvent` and begins hug processing only when the event uses the main hand, the player is sneaking, and the target is a `LivingEntity`. Cancelled events are ignored.

Affected behaviors:
- CB-001
- CB-002

Regression risks:
- Right-click interaction events stop reaching the plugin, so hugs no longer trigger.
- Hand semantics change and a single interaction is processed more than once.
- Sneaking or entity classification changes cause incorrect triggering.
- Cancelled interactions are processed unexpectedly.

Relevant APIs / concepts:
- `PlayerInteractEntityEvent`
- `EquipmentSlot.HAND`
- `Player#isSneaking`
- `LivingEntity`
- event cancellation

Related code:
- `src/main/java/net/okocraft/hugs/Hugs.java`

### DP-002: Commands and permissions

Platform:
Paper | Bukkit API

Usage:
`plugin.yml` registers the `hug` command and the `hugs.hug` / `hugs.command` permissions. Runtime behavior relies on Bukkit command dispatch and online-player lookup.

Affected behaviors:
- CB-001
- CB-003

Regression risks:
- `/hug` is not registered or cannot execute.
- Permission defaults behave differently and ordinary players gain or lose unintended access.
- Online-player lookup fails for a valid target.

Relevant APIs / concepts:
- `plugin.yml`
- `JavaPlugin#onCommand`
- `CommandSender`
- `Player#hasPermission`
- `Server#getPlayer`

Related code:
- `src/main/java/net/okocraft/hugs/Hugs.java`
- `src/main/resources/plugin.yml`

### DP-003: Particles and sounds

Platform:
Paper | Bukkit API

Usage:
The plugin uses Paper's `ParticleBuilder` to spawn `HEART` particles at the target location and Adventure `Sound` to play `ENTITY_CAT_PURR`.

Affected behaviors:
- CB-002

Regression risks:
- Hug processing succeeds but heart particles are not visible.
- The hug sound does not play.
- Recipient or location semantics change and feedback is shown to the wrong player or at the wrong position.
- Particle or sound changes produce runtime errors.

Relevant APIs / concepts:
- `com.destroystokyo.paper.ParticleBuilder`
- `Particle.HEART`
- `Player#playSound`
- Adventure `Sound`
- `Sound.ENTITY_CAT_PURR`

Related code:
- `src/main/java/net/okocraft/hugs/Hugs.java`

### DP-004: Adventure translation

Platform:
Paper

Usage:
The plugin registers English and Japanese `TranslationStore<MessageFormat>` data with Adventure's `GlobalTranslator` and sends translatable components to players.

Affected behaviors:
- CB-002
- CB-003

Regression risks:
- Translation keys such as `hugs.*` are displayed instead of translated text.
- The expected locale is not applied.
- The `{0}` target-name argument is missing or rendered incorrectly.

Relevant APIs / concepts:
- `GlobalTranslator`
- `TranslationStore`
- `Component.translatable`
- player locale
- `MessageFormat`

Related code:
- `src/main/java/net/okocraft/hugs/Messages.java`
- `src/main/resources/en.properties`
- `src/main/resources/ja_JP.properties`

### DP-005: Player lifecycle event

Platform:
Paper | Bukkit API

Usage:
The plugin stores each player's last hug time and removes that entry when `PlayerQuitEvent` fires.

Affected behaviors:
- CB-004

Regression risks:
- Interaction timing changes cause the cooldown to behave incorrectly.
- Player lifecycle changes leave stale player references in the cooldown map.

Relevant APIs / concepts:
- `PlayerQuitEvent`
- player lifecycle
- event delivery

Related code:
- `src/main/java/net/okocraft/hugs/Hugs.java`

### DP-006: Plugin metadata and loading

Platform:
Paper | Bukkit API

Usage:
`plugin.yml` declares the main class, API version, Folia support, command, and permissions. The plugin is loaded as a normal `JavaPlugin`.

Affected behaviors:
- CB-001
- CB-002
- CB-003
- CB-004

Regression risks:
- The plugin fails to enable.
- Command or event registration does not occur.
- Metadata compatibility changes cause a load-time exception.

Relevant APIs / concepts:
- `plugin.yml`
- `JavaPlugin`
- plugin lifecycle
- `folia-supported`

Related code:
- `src/main/resources/plugin.yml`
- `src/main/java/net/okocraft/hugs/Hugs.java`

## Baseline Smoke Tests

### BT-001: Right-click hug on a living entity

Purpose:
Verify that the primary sneak + right-click interaction works on the current Paper runtime and that an ordinary right-click does not trigger the plugin.

Setup:
1. Log in test player A.
2. Spawn one cow near A with `/summon minecraft:cow`.
3. Ensure A can use the `hugs.hug` permission.

Operation:
1. Have A right-click the cow without sneaking.
2. Wait at least one second.
3. Have A sneak and right-click the same cow with the main hand.

Pass criteria:
- Step 1 produces no Hugs message, heart particle, or hug sound.
- Step 3 shows heart particles at the cow's location.
- A hears the hug sound once.
- A receives a message identifying the cow as the hugged target.
- A single interaction does not produce duplicate hug messages.
- No Hugs-related exception appears in the server log.

Covers:
- CB-001
- CB-002
- DP-001
- DP-003
- DP-004

Typical duration:
About 1 minute

Notes:
Use `/summon` instead of waiting for a naturally generated entity.

### BT-002: Player-to-player hug and cooldown

Purpose:
Verify two-way player feedback and repeated-hug suppression in one interaction sequence.

Setup:
1. Log in test players A and B.
2. Place A and B within interaction range of each other.
3. Ensure A can use the `hugs.hug` permission.

Operation:
1. Have A sneak and right-click B.
2. Immediately right-click B again within one second.
3. Wait at least one second, then right-click B again while sneaking.

Pass criteria:
- The first interaction shows heart particles to both A and B.
- Both A and B hear the hug sound.
- A receives a message saying that A hugged B.
- B receives a message saying that A hugged B.
- The second interaction within one second produces no additional particles, sound, or hug message.
- The third interaction after at least one second works normally again.
- No Hugs-related exception appears in the server log.

Covers:
- CB-001
- CB-002
- CB-004
- DP-001
- DP-003
- DP-004
- DP-005

Typical duration:
About 1 minute

### BT-003: `/hug` success path and self-hug

Purpose:
Verify the command-based hug path and the special self-hug behavior.

Setup:
1. Log in test players A and B.
2. Make A an operator or otherwise grant A the `hugs.command` permission.

Operation:
1. Have A run `/hug B`.
2. Wait at least one second.
3. Have A run `/hug A`.

Pass criteria:
- Step 1 gives A and B the normal player-to-player hug feedback.
- Step 3 gives A heart particles and the hug sound.
- Step 3 displays the dedicated self-hug message instead of the normal player-target message.
- No exception appears in the server log as a result of either command.

Covers:
- CB-002
- CB-003
- DP-002
- DP-003
- DP-004

Typical duration:
About 1 minute

### BT-004: `/hug` rejection paths

Purpose:
Verify that invalid or unauthorized command usage does not trigger a hug and that permission and input failures are handled safely.

Setup:
1. Log in test player A.
2. Start with A as an operator.
3. Ensure an administrator can remove A's operator status before the final player-command check.

Operation:
1. Have A run `/hug` with no arguments.
2. Have A run `/hug HugsDefinitelyOffline`.
3. Remove A's operator status.
4. Have A run `/hug <online-player>`.
5. Run `/hug <online-player>` from the server console.

Pass criteria:
- Step 1 displays `/hug <player>` usage and does not trigger a hug.
- Step 2 reports that the target is unavailable and does not trigger a hug.
- Step 4 displays the no-permission message and produces no particles, sound, or notification for the target.
- Step 5 returns the player-only message to the console.
- None of the rejection paths produce a Hugs-related exception in the server log.

Covers:
- CB-003
- DP-002
- DP-004

Typical duration:
About 1 minute

Notes:
`hugs.command` defaults to `op`.

## Conditional Test Areas

### CT-001: Entity interaction semantics

Trigger:
A Minecraft or Paper update changes entity interaction, hand handling, sneaking, event cancellation, or `LivingEntity` semantics.

Related dependency points:
- DP-001

Possible regressions:
- Hugs no longer trigger.
- One click produces duplicate hugs.
- A hug triggers without sneaking.
- A cancelled interaction is processed.

Recommended verification:
Run BT-001 and BT-002. Add focused verification for cancelled `PlayerInteractEntityEvent` or off-hand behavior only if the upstream change specifically reaches those paths.

### CT-002: Particle / sound API

Trigger:
A Paper update changes `ParticleBuilder`, or Minecraft/Paper changes particle or sound registries, or the Adventure Sound API changes.

Related dependency points:
- DP-003

Possible regressions:
- `HEART` particles are not visible.
- Particle position or recipient is incorrect.
- The hug sound disappears, changes unexpectedly, or causes a runtime exception.

Recommended verification:
Emphasize particle and sound observations while running BT-001 and BT-002. Add a dedicated test only if the existing operations cannot expose the affected path.

### CT-003: Adventure translation

Trigger:
Paper changes its bundled Adventure version, `GlobalTranslator`, translation stores, or locale handling.

Related dependency points:
- DP-004

Possible regressions:
- `hugs.*` translation keys are shown directly.
- English or Japanese translation is not applied as expected.
- The `{0}` target-name substitution fails.

Recommended verification:
Run BT-002 with one English-locale client and one Japanese-locale client and verify that each recipient sees the expected localized message and target name.

### CT-004: Command / plugin metadata

Trigger:
Paper changes plugin loading, legacy `plugin.yml` handling, command registration, or permission-default behavior.

Related dependency points:
- DP-002
- DP-006

Possible regressions:
- Hugs fails to load.
- `/hug` is not registered.
- Permission defaults differ from the plugin's declared behavior.

Recommended verification:
In addition to normal startup checks, run BT-003 and BT-004.

## Test Design Notes

- The current `pom.xml` uses Java 25 and `paper-api 26.2.build.92-stable`.
- `plugin.yml` declares `api-version: "1.16"`.
- `plugin.yml` declares `folia-supported: true`.
- No config file, scheduler usage, persistent data, inventory manipulation, world/chunk manipulation, NMS access, or reflection was found in the inspected primary implementation files.
- Do not measure the one-second cooldown boundary precisely. Verify only that rapid repetition is suppressed and that the action works again after roughly one second.
- Do not split particle and sound verification into separate tests; observe them during the hug operations above.
- Locale behavior does not need a separate baseline test. Promote CT-003 when an upstream Adventure or locale change makes it relevant.

## Maintenance Rules

Update this file when:

- A Core Behavior is added, removed, or changed.
- The plugin's dependency on Minecraft or Paper changes.
- A new regression is found.
- A baseline test proves unstable or redundant.
- An important issue is found that the baseline tests cannot detect.
