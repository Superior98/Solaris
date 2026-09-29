# Solaris

Snapshot of the Solaris Minecraft city, its Create_ Remastered Modrinth profile, the Fireheart City mod source, and the project handoff files, captured on 2026-09-29.

## Contents

- `profile/`: the full local profile, including installed mods, configuration, resource packs, shaders, skins, backups, and the `saves/Create!` city world.
- `fireheart-city/`: extracted Solaris v1.12.0 source (`src/`, `res/`), build scripts, tools, and the original `git.bundle` containing earlier development history.
- `handoff/`: the downloaded Solaris handoff package, including the source ZIP, compiled mod, datapacks, and server test scripts. Start with `handoff/SOLARIS_HANDOFF.md`.
- `handoff/downloads/`: related standalone handoff notes and Fireheart downloads. Some of these are older versions; prefer the main handoff package for development.
- `inventory.json`: SHA-256 hashes and sizes for the imported files, plus a record of credential redactions and exclusions.

## Download and restore

Install Git and Git LFS, then clone with LFS enabled:

```sh
git lfs install
git clone https://github.com/Superior98/Solaris.git
cd Solaris
git lfs pull
```

Files of 10 MiB or larger use Git LFS. Use the clone instructions to ensure you get their actual contents instead of pointer files.

The handoff specifies Minecraft **1.20.1** with **Forge 47.4.20**. Create a matching Modrinth profile, close the game, and copy the contents of `profile/` into that profile's game directory. Back up any existing destination world first. The world is named **Create!**.

API keys and the WATERMeDIA server access token have been removed from the uploaded copies. Supply your own credentials locally if needed. The original local files were not modified. Minecraft account identifiers and saved player/world state remain part of this snapshot.

## Source and build status

The extracted source declares version **1.12.0**. Its bundled Git history ends earlier, at v1.10.0; the extracted source is the newer snapshot. The original archive and history bundle are preserved as supplied.

The supplied `build.sh` expects the previous Linux build workspace at `/tmp/modbuild`, named Minecraft/Forge dependency JARs, `cp.txt`, `srg2named.srg`, and a ForgeAutoRenamingTool installation under `/mnt/user-data/uploads`. The source archive does not include `cp.txt`, `srg2named.srg`, or those dependency JARs. Follow the toolchain reconstruction notes in `handoff/Fireheart_Project_Handoff.md` before building. This upload did not rebuild or retest the mod.

Only the original profile's `.git` directory and runtime `session.lock` files were excluded. The original profile's Git history remains on the source computer; it is not uploaded because it contains unsanitized settings and oversized ordinary Git objects. The rest of the profile is included, including runtime caches and logs, to preserve the requested snapshot.

Third-party mods and assets retain their original licenses. Inclusion here does not grant additional redistribution rights.
