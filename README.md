---

# Block Block Action!

A mod that allows operators to protect blocks and entities from destruction, explosions, interaction, and placement of blocks next to them.

The mod is inspired by [Block That Action](https://modrinth.com/mod/block-that-action), but has been ported to NeoForge and expands on the idea: it adds multiple protection modes, colored visualization, entity protection, area selection, and convenient mode switching.

Perfect for building adventure maps where players shouldn't break certain blocks but should still be able to use their own.

---

## 🔒 Four Block Protection Modes
- 🟡 Prevent destruction.
- 🔴 Prevent destruction and interaction.
- 🔵 Prevent destruction and block placement on faces.
- 🟣 Full protection: prevent destruction, interaction, and block placement on faces.

---

💥 **Protected blocks** cannot be destroyed by players, explosions, fluid transformations, or tool actions like stripping bark with an axe.

🐄 **Entity protection:** animals, villagers, item frames, and other entities can be protected from damage, attacks, and explosions.

🚫 In interaction‑prevention mode, you cannot trade with villagers, tame animals, change items in frames, or perform other interactions.

---

📦 Entity protection persists across world restarts.

💾 Block and entity data is stored in the world save folder.

---

## 🎮 Usage
Take the *Protection Wand* from the mod's creative tab.

Select a protection mode: hold the assigned key (default `Left Alt`);

scroll the mouse wheel;

or use the `<` and `>` keys.

---

The name of the selected mode will appear above the hotbar.

Right‑click the wand on a block or entity to apply the selected mode.

For entities, modes that prevent block placement are automatically replaced with the corresponding standard modes: entities cannot be "protected from block placement", so protection against destruction, or against destruction and interaction, is applied instead.

---

## 📐 Area Protection

`Shift + Right‑click` the wand on a block — first point of the area.

`Right‑click` without `Shift` on another block — second point and application of the selected mode to the entire area.

Pressing `Shift + Right‑click` again while selecting cancels the selection.

The selected area is shown with a simple colored outline.

---

## 🔑 Permissions

Only players with operator level 2+ can change protection. Regular players cannot modify settings with the wand.

---

The client and server must use the same version of the mod in multiplayer.