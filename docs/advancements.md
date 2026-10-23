# Advancements

Ankerstone has four advancements in its own tab, titled **A Strange Stillness**.

| Name | Description |
|---|---|
| A Strange Stillness | Craft stillstone from deepslate and lapis. |
| Iron Resolve | Strengthen stillstone with iron. |
| Pearl of an Idea | Give an ender pearl a reason to stay put. |
| Stay a While | Keep a piece of the world from slipping away. |

## Earning Them

The first three advancements are earned by obtaining Stillstone, Reinforced Stillstone, and Ankerstone respectively. Crafting is the intended progression, but receiving the items from another player also counts. Stillstone and Reinforced Stillstone require the full-block item; slabs, stairs, and walls do not count toward those milestones.

The advancements form a single chain in the tab, but earning a parent is not required to earn its child. Recipe-book unlocks are separate.

## Activation Credit

**Stay a While** goes to the player who placed an Ankerstone when it first activates. Another player, a timer, or any other redstone circuit can supply the power. The placer does not need to be nearby or in the same dimension.

Activation means changing from Off to Single or Wide. Placing an Ankerstone into an already powered position also counts. Switching between Single and Wide is not a new activation. Creative placement by a player counts; blocks without a recorded placer, such as command-placed blocks or older placements, cannot award activation credit.

If the placer is offline when the block activates, their credit is saved and awarded when they return. That earned credit survives removal of the block and server restarts. It is consumed after delivery, so the same pending credit is not awarded again.

## Administrator Notes

Ankerstone has no block entity. `AttributionData` stores placer credit in the world's Overworld SavedData, matching each block's dimension and position to a player UUID.

- Off Ankerstone retains its placer across server restarts.
- Intact piston movement transfers the placer record to the new position.
- Breaking the block removes its placement record. The dropped item carries no placer data; placing it again assigns the new placer.
- Once activation credit is awarded or queued for an offline player, the block's placement record is removed.
- WorldEdit cloning and edits that bypass normal block-removal hooks are untested. Do not assume they copy or clear attribution correctly.

Chunk-loading ownership is separate from player credit. `OwnerData` saves active block positions and modes in each dimension; `ChunkTickets` manages the live tickets. Those owners are anchor blocks, not players.

On dedicated servers, set `pause-when-empty-seconds=0` if anchors should keep ticking with nobody connected. Ankerstone does not override whole-server pausing.

See [Lore](lore.md) for the story behind the advancement names.
