# FIREHEART CITY – Project Handoff (Minecraft Create world)

This document carries the whole project into a new chat. **Read Part 8 first (latest: session 13 – voices, furniture, library, pets, Skyliner rides, motorbike in progress, and the next task: resident memory). Then Part 7 (the custom mod, crash fixes, toolchain) and Part 6 (sky island, Skyliner).** The mod source is in `Claude outputs\fireheartcity_mod_source.zip`. It is written for the assistant picking the project up.
- Part 1 is the **master briefing**: read it first.
- Parts 2 to 5 are the full working documents, reproduced word for word:
  - Part 2: build log and coordinates.
  - Part 3: mod knowledge base.
  - Part 4: generator code.
  - Part 5: full block-ID list.

Coordinate convention: `x y z`. The ground on the island is **y70** (top surface) and players stand at **y71**.

---

# PART 1 – MASTER BRIEFING

## 1. The user and how to work with them
- **User:**
  - Daniel. His Minecraft name is **Fireheart_4743** and he plays in creative mode.
  - He writes casually and with typos; read his messages for intent.
  - He prefers short, professional work, and code without comments unless they're needed.
- **Tone:** he's enthusiastic, likes to be impressed, and gets frustrated when things don't work.
  - Always verify a build in-game (screenshot, data check) before calling it done.
  - Keep replies short and friendly, with clear "how to use" steps.
- **Interruptions:** he sometimes interrupts, e.g. "whoops continue", or the low-battery popup covering the screen. Just resume.
- **Pets:** his wolf **Benson** died in Sept 2026 (see Part 6). There is a memorial to him on Neon Heights. Be gentle about it; don't bring it up casually.
- **His own fixes:** he said he would fix the garage hazard stripe pattern himself.
- **Asking questions:** use the AskUserQuestion tool for bigger jobs, as was done before the "automate the city while I'm at the gym" job. For quick fixes, just do them.

## 2. Setup
- **Game:**
  - Minecraft **Forge 1.20.1**, launched from the **Modrinth App**, profile **"Create_ Remastered"**, single-player world **"Create!"**.
  - Window title "Minecraft* Forge 1.20.1 - Singleplayer". The screen frame is 1389x868 (the real display is 1920x1094).
- **Paths on the user's PC (Windows, user `whosh`):**
  - Profile: `C:\Users\whosh\AppData\Roaming\ModrinthApp\profiles\Create_ Remastered\`
  - World: `...\saves\Create!\`; regions are in `...\saves\Create!\region\` (r.-1.-1, r.-1.0, r.0.-1, r.0.0 cover the island).
  - Datapack: `...\saves\Create!\datapacks\statue\` (namespace `statue`, pack_format 15). Functions go in `data\statue\functions\`. Tick/load tags are in `data\minecraft\tags\functions\tick.json` / `load.json`.
  - Resource pack: `...\resourcepacks\FireheartCarSounds\` (sound event `fireheart:car_engine`). The user enabled it.
  - Logs: `...\logs\latest.log`. Tellraw/say output lands here, so stage and grep it to read long results.
  - Mods folder: `...\mods\`
- **Mods (jar names):**
  - clockwork-0.5.6 (VS Clockwork)
  - Create Encased 1.8
  - create 6.0.8
  - create-enchantment-industry 2.5.2
  - createaddition 1.3.3
  - createbigcannons 5.11.4
  - createdeco 2.0.3
  - createdieselgenerators 1.3.12
  - CreateDragonsPlus 1.11.8
  - drivebywire 0.1.1
  - embeddium 0.3.31
  - FarmersDelight 1.3.4
  - ferritecore
  - kotlinforforge 4.12
  - oculus 1.8.0, with the shaderpack MakeUp-UltraFast
  - ritchiesprojectilelib
  - sliceanddice 3.6.0
  - Steam_Rails 1.7.3
  - trackwork 1.2.3
  - valkyrienskies-120 2.4.11
- **Tools and sandbox:**
  - The remote-devices bridge gives computer use: screenshot, batch key/type/click, `device_commit_files`, `device_stage_files` and `device_list_dir`.
  - There is **no device_bash** in this setup.
  - Computer-use access expires after 30 minutes idle. Re-grant it with `computer_resolve_access(["Modrinth App","javaw.exe"])`, then `computer_request_access` with the returned apps and willHide list.
- **Cloud working dirs:**
  - Generators and scratch: `/tmp/claude-0/city/`.
  - Datapack mirror: `/mnt/user-data/outputs/statue_datapack/statue/data/statue/functions/`.
  - Notes: `/mnt/user-data/outputs/factory_rebuild_plan.md`.
  - Unpacked jars (for javap/decompiling): `/tmp/claude-0/vs`, `cdg`, `cj`, `dbw`, `cw`, `tw`.
  - These may not exist in a new session. If needed, re-stage the jars from the mods folder and unzip them.

## 3. THE WORKFLOW (use this for everything)
1. **Generate:** write a Python generator (use `lib.py`, Part 4) that emits a `.mcfunction` into the datapack mirror.
2. **Commit:** `device_commit_files` it to the world's datapack path.
3. **Load and run:** in-game, press `t`, type `/reload`, Enter, wait ~12–15 s. Then `t`, type `/function statue:<name>`, Enter. Each function ends with `say NAME_DONE`.
4. **Verify:** check the result.
   - Screenshot with F1 toggled to hide the HUD.
   - `/data get block ...` or `/execute if block ...` ("Test passed").
   - For many checks, `tellraw` into the log, then stage `latest.log` and grep it.

### Computer-use quirks (important)
- **Chat input:**
  - Open chat with `t`, type (long text goes via the clipboard automatically), then Enter.
  - The chat limit is 256 characters. For longer NBT, place the block first, then `/data merge block`.
  - To clear a half-typed line: `ctrl+a`, then type the new command (it overwrites).
- **Escape does not work.** Close chat by sending a harmless command such as `/vs get-ship`.
  - If the Game Menu (pause) is showing, click **Back to Game** at (694,258).
  - Menu clicks are unreliable: sometimes the first click only focuses the window.
- **Clicking rotates the camera.**
  - Any right/left click through the tool turns the camera to pitch +90 (looking straight down).
  - So to click a block, stand directly above it: `/tp @s X.5 Y+1 Z.5 0 90`, then right_click at (694,420).
  - Aim precisely with `/execute anchored eyes run tp @s <pos> facing <x y z>`. F3 then shows "Targeted Block" or "Targeted Ship".
- **Focus:** if a key action fails with "Claude's own window still has keyboard focus", click the title bar at (400,12) first.
- **Hotbar:**
  - Number keys select slots.
  - `/item replace entity @s hotbar.N with ...` gives items.
  - `/item replace entity @s weapon.mainhand ...` puts an item in the hand.
- **Game mode:**
  - **Spectator mode** is good for camera shots (flying through blocks), but **clicks do nothing in spectator**.
  - Always return with `/gamemode creative`.
- **Other screens:**
  - F5 cycles the camera view.
  - F3+I copies block data (not very useful here).
  - The Windows volume overlay or the low-battery popup can hide the screen.
  - Pressing `Escape` sometimes opened "Open to LAN" by accident. Click Cancel at (926,764).
- **Time:** the user plays at night sometimes; `/time set 6000` gives daylight screenshots (ask before changing the time for him).

## 4. Island layout (world coordinates)
- **Island:** a flat grass island with ground at y70.
  - A perimeter stone-brick wall runs along **x-45 and x40, z-40 and z60**, with lanterns on top every 6 blocks.
  - Ocean surrounds it: surface y62, floor ~y62 near shore, deeper out.
  - A forest hill lies east of x40.
- **Roads:**
  - Asphalt (`createdieselgenerators:asphalt_block`) with yellow concrete centre lines.
  - Main N–S road **x-6..-2** from the factory (z13) south to the old pier (z60–75).
  - Road **x9..13** z13..40.
  - E–W road **z40..44**, with smooth-stone sidewalks at z38–39 and z45–46.
  - Sidewalks x-8..-7 and x-1..0 along the main road.
  - A path x-34..-32 z14..39.
  - The old long wooden pier runs south from x-3..1, z61..75 at deck ~y70; it's high above the water.
- **Factory** (red-brick industrial) x-12..8 z-28..12, containing the machines below.
- **Garage:** x-11..7 z-27..-8, north part of that block.
  - Three bays on its east side with hazard frames.
  - An asphalt apron x8..30 z-28..-7.
  - A fuel island x20..21 z-21..-13, with a yellow canopy x16..25 z-24..-10.
- **Kitchen / Auto Bakery:** x-41..-25 z-2..12.
- **Statue:** a giant statue of the user's own skin (3× scale, 96 tall) north of the garage.
  - Statue x-26..21 y77..172 z-88..-65, facing south.
  - Pedestal platform x-30..26 z-92..-58.
  - Stairs x-8..4 z-57..-60.
  - A "sacred path" x-6..2 y70 z-57..-29 with lantern posts and an arch at z-30.
  - The old statue site in the ocean (south) was removed at the user's request because he likes looking at the ocean from the pier.
- **Session 9 builds (while the user was at the gym):**
  - **Ember Heights apartments** x18..34 z16..32, 6 floors (floor slabs y70,74,…,94), roof y95.
    - Entrance on the west (z24).
    - Ladder shaft at x31 z31 (facing north) to a roof trapdoor.
    - Rooftop water tower, vents, and a "FIREHEART MOTORS" billboard.
  - **Shop row** (doors on the north side, facing the E–W road) z49..58: Diesel Diner (x3–12), Create Supply Co. (x15–24), Green Leaf Market (x27–36). Each has a striped awning and an upstairs loft with a ladder.
  - **Fireheart Plaza** park x-31..-9 z14..37: round fountain at (-20,25), gate with sign at z37, cherry and oak trees, flower beds, Create seats and lamp posts.
  - **Clock tower** at (-26,18) with 4 working cuckoo clocks at y88/89.
  - **Fireheart Aggregates** works x-42..-14 z49..58 (machine, see below), with chimneys and shipping containers.
  - **Container Port** x-43..-16 z-38..-6: stacked colourful containers, a yellow gantry crane, an office hut, and a gate plus asphalt to x-13 at z-21..-18.
- **Marina** (session 10), east of the old pier:
  - Stairs x4–5 down from the sea wall.
  - Walkway y63 z67–68 x2..26.
  - Finger piers x6–7, 14–15, 22–23 (z69..82).
  - Dockmaster hut x20–25 z61–64 ("FIREHEART MARINA" sign), fuel tank at 26 64–65 66, and buoys.
- **Benson's dog house** (built by the user, then upgraded):
  - Spruce/dark-oak hut x-39..-37 z34..37, door facing south at z37 onto the z38 sidewalk.
  - Sign "Benson" at -38 74 38, bone item frame.
  - Front: red mat, water cauldron, barrel, lantern posts and azaleas.
  - Back doorway at -38 71–72 34.
  - Fenced yard x-42..-35 z30..33 with a gate at -42 71 32, a red bed, bone block, hay and water. Stone-brick floor.

## 5. Automated machines (all verified working as of session 10)
**Diesel engine unit** (the standard power source):
- A `create:creative_fluid_tank{Size:1,Height:1,Uninitialized:1b,TankContent:{FluidName:"createdieselgenerators:diesel",Amount:8000}}` at y-2.
- `create:mechanical_pump[facing=up]` at y-1.
- `createdieselgenerators:diesel_engine[facing=F]{Tanks:[{TankContent:{FluidName:"createdieselgenerators:diesel",Amount:1000}}]}` at y.
- A gearbox (axis perpendicular to F) in front, with a `cogwheel[axis=y]` under the gearbox driving the pump.
- The engine gives 96 RPM.
- **Rules:**
  - The engine must be placed WITH fuel in its Tanks NBT, or it never starts.
  - Never set the tank amount above 1000.
  - Do NOT `/data merge` a running engine: it stalls. Re-place the block instead (setblock air, then setblock with NBT).
  - If a pump is ever placed facing the wrong way, re-place it `facing=up`.

**Machines:**
1. **Water wheel press:** wheel -3 72 2, press -1 72 2, depot -1 70 2. Runs at 8 RPM.
2. **Wheat farm** (piston-driven harvester frame).
   - Engine -5 71 -2 (facing west), gearbox -6 71 -2 (axis z), cog -6 70 -2, pump -5 70 -2, tank -5 69 -2.
   - Piston -8 71 -2; gearshift -7 71 -2 axis x; timer 600, extender 400.
   - Output chest -4 70 -3.
   - A **self-repair command chain** at x-12 y44 z-10..-6 re-clones the machine from a template at -10 40 -7..-4 40 0 if the Create contraption vanishes.
   - Forceload is set on -16 -16 15 15.
3. **Iron sheet belt line:** belt x0..5 y71 z6, engine 0 71 3.
   - Supply chest 0 73 6 (iron ingots; refilled with 27×64 in session 10).
   - Press 3 73 6, powered by engine 3 73 4.
   - Output chest 6 70 7.
4. **Sliding door:** engine 3 72 15, piston 3 72 12, pressure plates.
5. **Auto bakery:** engine -28 71 1.
   - Wheat input chest -28 75 4 → millstone -28 73 4.
   - Slow belt at 4 RPM through encased-fan wash and bake stations.
   - Bread chest -39 70 5.
6. **Fireheart Aggregates:** engine -38 72 54 (east), cog/millstone chain y73 z54 x-37..-25.
   - Input chests at y75: cobble at x-36,-34,-30,-26 and gravel at -32,-28.
   - Output chests at y71 (gravel/flint).
   - Flywheel -23 76 54.
   - Refill with `statue:refill_gravel`.
7. **Clock tower:** tank -26 64 17, pump -26 65 17, engine -26 66 17 (south), gearbox -26 66 18 (axis x), shaft column x-26 z18 y67..87.
   - Gearboxes -26 88 18 (x) and -26 89 18 (z) drive cuckoo clocks at -26 88 16/20 and -28/-24 89 18.
   - Rebuild with `clockfix1` then `clockfix2`.
8. **Apartment elevator:** see Vehicles & Elevator below.

**Maintenance functions:**
- `statue:checkup` prints speeds and chest contents with tellraw.
- `statue:maint` refuels and refills.
- `statue:verify1/2` scans ~73k blocks against the original plans and prints `CHK x y z block` for each mismatch; read them from latest.log.
- `statue:city_fix` holds the repairs made in session 10.

## 6. Vehicles (Valkyrien Skies ships) and the elevator
**General VS facts:**
- A ship is made with the **Area Assembler**.
  - `/item replace entity @s weapon.mainhand with valkyrienskies:area_assembler{firstPosX:..,firstPosY:..,firstPosZ:..}`
  - Stand on the opposite top-corner block, looking down, and right-click once. Chat says "Shipified to <random-slug>!"
  - Then `/vs rename <slug> <name>`. Must be in creative (not spectator).
- **Moving ships:** `/vs teleport <ship> x y z (0 0 0)` (the euler angle in brackets resets rotation; the position is the centre of mass). This is the reliable way to re-park a vehicle.
- **Other commands:** `/vs get-ship` (what you're looking at), `/vs set-static`.
- **Ship blocks** live in shipyard coordinates (~±28.7 million). `/data`, `/setblock` and `/execute if block` work there.
- **Disabled settings:** world splitting is off, and pocket buoyancy is off.
- **Wheels:** Trackwork wheels only work on ships.
  - Steering = redstone/15 × facing.
  - A wheel is driven by a shaft on its facing side.
  - Paired wheels facing opposite ways on one axle spin opposite, so reverse one of them with a powered gearshift.
- **Thrusters:** `valkyrienskies:test_thruster[facing=F,powered]` pushes 100 kN in its facing direction when powered.
- **Drive By Wire (DBW):**
  - The `drivebywire:controller_hub` is sat on or stood next to.
  - Bind a `create:linked_controller` by right-clicking the hub with it (writes the `{Hub:<BlockPos long>L}` tag).
  - The rider activates the controller with right-click (click again to toggle). Opening chat or a menu deactivates it.
  - The wiring is stored in a `drivebywire:backup_block{WireNetwork:{Name,BackupOffset:0L,Network:{name:{keyUp:[L; src,sink,dir,...],keyDown,keyLeft,keyRight,keyJump,keyShift}}}}`.
    - Positions are BlockPos.asLong relative to the backup block. dir 1 = UP, 5 = EAST.
    - The sink is the block RECEIVING the signal.
    - It loads when the ship has no network.
  - See `network()` in Part 4.
- **Stuck key:** if chat opens while a key is held, the key stays "pressed" and the vehicle runs away.
  - Always wait about 1 s after releasing keys before opening chat.
  - To clear it: activate the controller, then tap W/S/A/D.
  - Dismount with `/ride @s dismount`.
- **Controllers:** the hotbar holds named controllers: slot 1 Car (red), slot 2 Boat (aqua), slot 3 Motorbike (gold).

### Car "fireheart-cr01" (ship id 36)
- **Shipyard origin:** shipyard = (-28669957+lx, 127+ly, 12290045+lz). Local +x = forward and lz0 = left.
- **Drive:** rear diesel engine (1,2,2) with pump and creative tank → gearbox/cog down → shaft → **gearshift (4,0,2)** (reverse, powered by keyDown) → **clutch (5,0,2)** (powered = disengaged; a comparator in subtract mode with a redstone block releases it when W/S is held) → shafts → front gearbox (8,0,2) → front `simple_wheel`s FL (8,0,0) facing south and FR (8,0,4) facing north.
  - Rear wheels (2,0,0) and (2,0,4) spin freely.
  - A powered gearshift at (8,0,3) reverses the right front wheel. This fixed the spinning.
- **Controls:** red seat (4,1,2), hub (6,1,2), backup block (6,1,1), horn (6,1,3) on Space.
- **Floor:** polished_blackstone top slabs, so the car rides on its wheels.
- **Speed:** W goes ~17 blocks per 1.5 s. A/D steer.
- **Controller Hub tag:** `-7880736072727007104L`.
- **Park:** `/vs teleport fireheart-cr01 15 75 -29 (0 0 0)` puts it on the garage apron next to the fuel canopy, facing east.
- **Engine sound:**
  - The diesel mod's own sound didn't follow the ship and sounded like a train.
  - Now a datapack tick system plays `fireheart:car_engine`: a synthesized 1 s 4-cylinder loop from the resource pack.
  - It plays while the player rides a `create:seat` holding a `create:linked_controller`, and the clutch block exists.
  - Seven rev steps with pitch 0.5→2.0; rev rises while the clutch is engaged.
  - A start-up sound plays on getting in and a clunk on getting out.
  - Functions: car_engine_tick/sound/play/start/shift/off/load.

### Motorbike "fireheart-mb01"
- **Build:** 3-wide, quad-style, with 4 `simple_wheel`s (a real 2-wheeler tips over).
  - Built at world 32..37 y71..74 z-2..0 before assembly (`statue:veh_bike`).
  - W/S = two thrusters (local (1,1,1) facing east and (1,0,1) facing west).
  - A/D steer the front wheels.
  - Headlight, tail light, flag.
  - Hub tag `-7878484547791228801L`.
- **User complaints:**
  - It has **no brakes and reverse is unreliable**, and it "went haywire" (most likely a stuck key).
  - I offered to rebuild it with a wheel drive train like the car (gearshift + clutch). Not done yet: a pending option.
- **Park:** `/vs teleport fireheart-mb01 28 72 -17 (0 0 0)` (garage apron). Before re-parking, clear its keys (activate the controller and tap all four keys).

### Boat "fireheart-sea01"
- **Build:** birch plank hull with white/light-blue wool, a cabin roof on fences, a windshield, red and green nav lamps, a horn, a flag and seats. Mass 66 t; it floats.
  - Built at world 8..12 y64..69 z70..80 (`statue:veh_boat`).
- **Controls:** 4 test_thrusters.
  - W = stern thruster (local (1,0,0) facing south = forward +z).
  - S = (3,0,0) facing north.
  - A/D = bow side thrusters (1,0,9) east and (3,0,9) west.
  - Space = horn.
- **Hub tag:** `-7879610722575974274L` (hub shipyard -28665857 126 12290048).
- **Park:** `/vs teleport fireheart-sea01 10.5 63 75 (0 0 0)` (in its slip at the marina, bow south).
- **Boarding:** the driver seat is create:white_seat. Right-click the seat with an empty hand, then switch to the Boat controller and right-click to activate.

### Elevator (Ember Heights)
- **Shaft:** glass shaft x35..37 z23..25 y70..98. Landings through x34 at z24. Floors y0 = 70,74,…,94, named G, 1–5, R.
- **Machine room:** on the roof (x30..34 z22..26 y95..100). Diesel engine 32 99 24 → shafts → `create:elevator_pulley[facing=north]` at 36 99 24 (-96 RPM).
- **Cabin:** assembled at the roof stop.
  - Floor y94, ceiling with sea lantern y98.
  - Framed glass door at 35 95–96 24 (opens automatically).
  - `contraption_controls` at 37 95 24.
  - Cabin redstone_contact at 35 97 23.
  - Glued with a summoned `create:super_glue` at 35.0 94.0 23.0, `From:[0,0,0] To:[3,5,3]`.
- **Landings:** each has a redstone contact at 34 (y0+3) 23 (it turns into an elevator_contact), a call button at 33 (y0+3) 23, and a sign at 33 (y0+3) 24.
- **Using it:** press the button to call the cabin. Inside, look at the panel, scroll to choose a floor, then right-click.
- **Warning:** right-clicking the pulley with an empty hand DISASSEMBLES the elevator.
- **Rebuild procedure** (used once when the user broke it):
  1. Disassemble: `/setblock 36 100 24 air`, tp to 36.5 100 24.5 looking down, right-click with an empty hand (hotbar slot 3).
  2. `/function statue:city_elevator`.
  3. Right-click the pulley again from the same spot. Check with `/data get block 36 99 24 Running` → 1b.
  4. `/function statue:city_elevator_names` (names and closes the roof hole).
- **Test:** place a redstone block briefly at 34 72 23 to call the cabin to G, then check `create:elevator_contact[powering=true]`.

## 7. Datapack function inventory (namespace statue)
- **Statue:** northpedestal, north1, north2, removeold (part1/part2/pedestal are retired).
- **Car:** car, carfix, cardbw, carwheels, cardiff, cardiag, carhelper, and the car_engine_* set (tick and load tags are active).
- **City:** city_apartment, city_shops, city_park, city_works, city_yard, city_decor, city_elevator, city_elevator_names, city_fix.
- **Maintenance:** checkup, maint, refill_gravel, clockdiag, clockfix1, clockfix2, verify1, verify2.
- **Vehicles:** veh_bike, veh_boat, veh_marina.
- **Dog house:** doghouse.
- **Warning:** re-running a `city_*` builder clears its area with `fill … air` first. Don't run one where the user has since changed things without checking.

## 8. Lessons learned (cumulative)
- **Chat and commands:**
  - The chat limit is 256 characters; use `/data merge`.
  - In 1.20.1 there is no `execute … run return`.
  - Item NBT uses `Count:1b`.
- **Belts:** place all belt segments in the same tick. The belt end can't insert into a chest, so use a floor hopper.
- **Pistons:** mechanical piston ScrollValue:1. An extended piston consumes its poles. In 1.20.1 a powered gearshift makes the piston extend.
- **Create diodes:** `facing` points at the input side, and they need a block under them.
- **Redstone links:** set the frequency items, or links don't pair.
- **Contraption bug:** Create moving contraptions can vanish on reload or chunk unload (the farm bug) → the self-repair chain and forceload exist for this.
- **Clutch and gearshift:** clutch powered = disengaged. Gearshift powered = reversed.
- **Light blocks:** place them before crops. Farmland under placed blocks turns to dirt.
- **Diesel engines:** see §5 (needs fuel NBT, max 1000, don't data-merge a running engine).
- **The Clockwork Command Seat never received input:** don't use it. Use Drive By Wire.
- **Sounds on ships:** mod sounds bound to a ship block may not be heard, so play sounds at the player through the datapack instead.
- **`place feature minecraft:fancy_oak|birch|cherry|oak X Y Z`** plants trees. Check that no trunk lands on a path; one did, and was removed.
- **Glass panes/bars** need explicit connection states (lib.resolve_panes handles this). Ladders must attach to a solid block, not glass.
- **Integrity scan:** the verify scan works well. The only false positives are plant/grass decoration and the pedestal cracked bricks (randomised on purpose).
- **Hiding the HUD:** F1 hides the HUD for clean screenshots. Press F1 again to restore it.
- **Stalled machines:** when a network shows Speed 0 but the engine has fuel, re-place the engine block, then the gearbox/shaft column (`clockfix` style).

## 9. Open ideas / possible next steps
- Rebuild the motorbike with a wheel drive train (brakes and reverse), like the car.
- Offer more city districts (the north lot beside the container port is mostly used; the NE corner x30..39 z-40..-30 is still grass).
- The user may ask for more vehicles, re-parking, fixes, or build explanations. For explanations, give step-by-step guides where he picks the blocks, as with the dog house.

---

# PART 2 – BUILD LOG & COORDINATES (factory_rebuild_plan.md, verbatim)

# Create Factory – Rebuild Plan (1.20.1 Forge)

All coordinates below are from the original 1.21.1 build. Original floor level = y172, player stands at y173.
In the new world pick a flat spot and apply an offset: new = old + (DX, DY, DZ).
Offset used in new world: **(recorded in the Progress log at the bottom)**

## 1.20.1 syntax notes
- Item NBT: `{id:"minecraft:iron_ingot",Count:32b,Slot:0b}` (capital `Count`, byte).
- Chat limit 256 chars; split long `/data` commands with `append`.
- Type commands with `t` (focus must be on the game), Enter to send.

## Lessons learned (don't repeat)
1. Belts break if placed one piece at a time → place all segments in ONE tick via a chain of command blocks (impulse + chain, `auto:1b`), triggered by a redstone block. Remove afterwards.
2. Belt end cannot insert into a chest → let items fall off the end into a floor hopper → chest.
3. Hopper above belt start inserts items onto the belt fine.
4. Mechanical piston: use `ScrollValue:1` (place only when returned). Default mode drops blocks mid-way and breaks.
5. Extended piston CONSUMES the poles behind it → never fill the pole slot with blocks.
6. Sequenced Gearshift loses its network after finishing → use plain Gearshift + Pulse Timer + Pulse Extender instead.
7. Create diodes (pulse timer/extender): `facing` points to the INPUT side; they need a solid block underneath.
8. Redstone links with empty frequency do not pair → set FrequencyFirst/FrequencyLast items.
9. Farmland under placed blocks turns to dirt; wheat breaks.
10. Snowy biome: water with sky above freezes → roof it.
11. Changing a creative motor's ScrollValue with /data doesn't update speed → re-place the motor with NBT.
12. Super glue entity: `/summon create:super_glue X Y Z` then set From/To (relative to entity pos) — covers a box.

## Structures (old coordinates)
### Factory shell
- Clear: `fill 49 173 -17 67 188 1 air` (avoid machine area once built)
- Floor y172 x49..67 z-17..1 polished_andesite; foundation y171 stone replace air
- Walls bricks y173..178 on x49, x67, z-17, z1; y179 layer create:andesite_casing; roof y180 dark_oak_planks (x48..68 z-18..2); y181 dark_oak_slab
- Corner pillars stripped_dark_oak_log y172..180 at (48,-18) (68,-18) (48,2) (68,2)
- Door frame: stripped_dark_oak_log x56..60 y173..176 z1, opening x57..59 y173..175
- Windows glass_pane: z1 x51-53 & x63-65 y175-177; z-17 x51-65 y175-177; x49 & x67 z-14..-2 y175-177
- Ceiling decor shafts y178 z-12 and z-4, x50..66 axis x; hanging lanterns y178
- Chimneys: `fill 62 181 -15 64 186 -13 bricks hollow` + campfire (63,186,-14); `fill 52 181 -15 54 188 -13 bricks hollow` + campfire (53,188,-14)
- Front: large_cogwheel axis z at (58,178,2); dirt_path x57-59 z2-9; lamp posts dark_oak_fence x55/x61 z3 y173-175 + lantern y176

### Machine 1 – water wheel press
- water_wheel facing=east (57,174,-8); shaft axis x (58,174,-8); mechanical_press facing=east (59,174,-8); depot (59,172,-8)
- Water: drain air 57 169..172 -9; cap stone_bricks 56..58 y175 z-10..-8 + (57,176,-9); water source (57,175,-9)

### Machine 2 – wheat farm (x50-55, z-17..-4)
- Pole column x52 y173 z-17..-13 facing south; piston (52,173,-12) facing=south axis_along_first=true ScrollValue:1
- linear_chassis axis=x x50..53 y173 z-11; (54,173,-11) sticky_top & sticky_bottom true; chest (55,173,-11); portable_storage_interface facing=north (56,173,-11)
- harvesters facing=south x50..54 y173 z-10
- water y172 z-10 and z-4 (x50-54); farmland moisture=7 x50-54 z-9..-5; wheat age=7 y173
- light blocks level 15 y176 over field
- gearshift axis=x (53,173,-12); creative_motor facing=west (54,173,-12); pulse_extender facing=north ScrollValue:400 (53,173,-13); pulse_timer facing=north ScrollValue:600 (53,173,-14)
- PSI facing=south (56,173,-12); hopper facing=north (56,172,-12); output chest (56,172,-13)
- Cover: andesite_casing y174 x52-55 z-16..-12, x55 y173 z-16..-12, x51 y173 z-14..-12, x53-54 y173 z-16..-15

### Machine 3 – iron sheet belt line
- belt facing=east y173 z-4: start x60, middle x61-64, end x65 (command-block chain at y165 z-1)
- creative_motor facing=south ScrollValue:-16 (60,173,-5) (flip sign if items go west)
- press facing=north (63,175,-4) + creative_motor facing=south (63,175,-5)
- hopper facing=down (60,174,-4) + supply chest (60,175,-4)
- output: hopper facing=south (66,172,-4) → chest (66,172,-3)

### Machine 4 – sliding door
- Panel andesite_casing x60-62 y173-175 z2 (open position) + super glue box over it
- poles facing=west x64-66 y174 z2; piston facing=west axis_along_first=false ScrollValue:1 (63,174,2)
- gearshift axis=z (63,174,3); creative_motor facing=north ScrollValue:-64 (63,174,4)
- casing support (64,173,3); pulse_extender facing=east ScrollValue:100 (64,174,3)
- casing (66,174,3); receiver redstone_link facing=west receiver=true (65,174,3) freq redstone/iron_door
- Plates: stone_pressure_plate (58,173,3) on polished_andesite, transmitter link facing=down (58,171,3); plate (58,173,0), link (58,171,0)
- Housing casing x63-66 y173-175 z2-5 replace air, EXCEPT keep x64-66 y174 z2 empty.

## Diesel engine unit (verified working, 1.20.1)
- Engine `createdieselgenerators:diesel_engine[facing=F]` outputs rotation on its F side, 96 RPM, tank 1000 mB (NEVER set >1000, it stalls).
- Fuel ONLY accepted from BELOW. Unit: tank `create:creative_fluid_tank{Size:1,Height:1,Uninitialized:1b,TankContent:{FluidName:"createdieselgenerators:diesel",Amount:8000}}` at y-2, `create:mechanical_pump[facing=up]` at y-1, engine at y.
- Pump powered by `create:cogwheel[axis=y]` next to it, fed from a `create:gearbox` (axis perpendicular) on the engine output side (gearbox passes power down into the cog and onward).
- Uninitialized:1b is REQUIRED on command-placed fluid tanks.

## Block IDs confirmed in this pack
- createdeco: industrial_iron_{bars,catwalk,catwalk_railing,catwalk_stairs,door,facade,hull,ladder,sheet_metal,support,support_wedge}, andesite_* same set, yellow_industrial_iron_lamp[facing,inverted,lit], *_shipping_container, *_placard
- createdieselgenerators: diesel_engine, asphalt_block/slab/stairs, *_cement, andesite_girder, distillation_tank, basin_lid

## Session 3 – Overhaul progress
- DOOR DONE (verified). All creative motors REPLACED by diesel units (verified running, tanks full):
  - Farm: engine -5 71 -2 facing west, gearbox axis z -6 71 -2, cog -6 70 -2, pump -5 70 -2, tank -5 69 -2 (piston now 96 RPM)
  - Belt: engine 0 71 3 facing south, gearbox axis x 0 71 4, shaft z 0 71 5, cog 0 70 4, pump 0 70 3, tank 0 69 3 (belt -96)
  - Belt press: engine 3 73 4 facing south, gearbox x 3 73 5, cog 3 72 5, pump 3 72 4, tank 3 71 4
  - Door: engine 3 72 15 facing north, gearbox x 3 72 14, cog 3 71 14, pump 3 71 15, tank 3 70 15
  - Engines need a starter load: place with {Tanks:[{TankContent:{FluidName:"createdieselgenerators:diesel",Amount:1000}}]}
- DONE detail pass: framed glass windows, stone brick base course, industrial iron pilasters, industrial iron sheet-metal roof + iron-bar roof railing, 4 encased-fan roof vents, taller chimneys with stone bands (campfires at 3 90 -4 and -7 94 -4), 9 yellow industrial lamps (y76) + hidden light blocks y75, catwalk x5-6 y74 z-6..9 with girder supports + ladder 6 71..74 10.
- DONE island: flattened/filled x-45..40 z-40..60 (grass y70, dirt 67-69, stone below), stone-brick sea wall + wall railing on edges.
- DONE roads (asphalt): main road x-4..0 z12..60 (dashed yellow line x-2), cross road z40..44 x-45..40 (solid yellow z42), sidewalks smooth stone, 8 street lamps, service road x9..13 z12..39, container yard x10..30 z-6..12 (6 container stacks + girder gantry), fuel depot x-22..-18 z-2..7 (two 3x3x6 Create fluid tanks + pipes), wooden dock x-5..1 z61..76.
- FARM FIX (session 4): moving frame vanished (piston stuck Offset 2, ExtensionLength 2) — likely chunk unloaded mid-move while teleporting far away. Rebuilt: remove gearshift first, clear x-8 z-7..-2 and frame row, re-place poles/piston/chassis/chest/PSI/harvesters, replant, then restore gearshift (-7 71 -2 axis x). Added `/forceload add -16 -16 15 15` so the factory chunks never unload. Verified harvest -> output chest.
- Session 5: farm broke again (piston Offset 2, frame lost; user said they broke it) -> rebuilt same way, verified full harvest. Belt line refilled (supply chest 0 73 6 with 128 ingots), all speeds OK.
- GARAGE (in progress) x-11..7 z-27..-8 north of factory. DONE: shell (brick walls, stone base, casing band y77, sheet-metal roof y78 + skylights, iron-bar rail y79, corner logs, iron pilasters), 3 bay openings on east wall (z-25..-22, -19..-16, -13..-10, y71-74) with yellow/black hazard frames + casing shutter boxes y75, framed glass windows N/W, doorway to factory 0..1 71..72 z-7, light gray floor + yellow bay lines (z-21, z-15, x-6), car lift bay1 (catwalk runways z-25/-22, girder posts x-4/4 z-26, beam y76, analog lever), service pit bay2 x-4..2 y67-69 z-18..-17 (ladder, lamp, hazard edge), floor drain -2 70 -12, hose pulley 0 76 -12, workbench x-10 z-26..-14 (toolboxes, anvil, depots w/ cogs, grindstone), item frames tool wall y74.
- GARAGE DONE: office x-10..-7 z-12..-8 (iron door -7 71 -10, desk, seat), oil tanks/tires/barrels north, girder trusses y76 x-9/x5, gantry shaft y76 z-19 + carriage -1 75 -19, yellow lamps y76 z-23/-15, asphalt apron x8..30 z-28..-7, fuel island x20..21 z-21..-13, canopy y76 x16..25 z-24..-10, sign 8 76 -18.
- Session 6: garage front hazard frame (x7, y71..75, z-27..-8) redone as diagonal stripes ((y-z)%4 in 0,1 = yellow). Workbench items at x-10 y72 were floating -> spruce cabinet counter x-10 y71 z-23..-13.
- FARM SELF-REPAIR (cause: Create bug - moving contraptions vanish on world reload/unload mid-move, piston left at Offset>0). Farm rebuilt again. Template copy of the machine at -10 40 -7 .. -4 40 0 (retracted state). Command chain at x-12 y44 z-10..-6 (facing south, repeating then chain): if no create:stationary_contraption within 9 of (-7,71,0) AND -8 71 -1 is not linear_chassis -> clear leftover harvester/chassis/chest/PSI in -10..-4 71 1..5, then clone template back to -10 71 -7. gamerule commandBlockOutput false. Chat input max 256 chars: set long commands with /data merge block.
- STATUE MOVED (session 7b): old ocean site removed (statue:removeold, water restored y35-62, dock view clear). New site north behind garage, statue faces south: statue x-26..21 y77..172 z-88..-65, pedestal platform x-30..26 z-92..-58, tier x-26..22 z-89..-61, stairs x-8..4 z-57..-60. Sacred path x-6..2 y70 z-57..-29 (red center line x-2, lantern posts x-7/3 every 4, red candles), arch at z-30. Functions now: statue:northpedestal, statue:north1, statue:north2 (old part1/part2/pedestal replaced with a "retired" message). Coordinates below are the OLD site.
- STATUE (old site, removed): user's own skin (skin cache ModrinthApp/meta/assets/skins/2b/2b4e43...), 3x scale, 96 tall, facing north at end of main road/dock. Statue x-25..22 y77..172 z85..108. Pedestal: stone-brick foundation x-30..26 z80..114 y35..71, platform y72 with red trim + lantern posts, blackstone tier x-26..22 z83..111 y73..76, stairs x-8..4 z79..82, bridge x-5..1 z77..79 from dock, glowing plaques at -14/10 75 82. Built with datapack saves/Create!/datapacks/statue (functions statue:pedestal, statue:part1, statue:part2; generator /tmp/claude-0/statue.py + ped.py). Light blocks placed outside glowing red pixels.
- KITCHEN / AUTO BAKERY DONE x-41..-25 z-2..12 (doors: east -25 z8..9, south x-34..-32 z12). Verified: wheat -> bread in chest -39 70 5.
  - Engine: tank -28 69 1, pump -28 70 1, cog -28 70 2, engine -28 71 1 (south), gearbox -28 71 2 axis x.
  - Fast path (96): gearbox -28 72 2 axis z -> gearshift -28 73 2 (unpowered) -> shafts 74..75 -> cogs y76 z2,z3,z4 -> cog line y76 z4 + column -28 72..76 5 -> millstone -28 73 4. Branch: gearbox -29 72 2 axis y -> cog -29 72 1 axis z -> cog line y71 z1 x-36..-29 (axis z) -> fans y71 z2 at x-30,-32 (wash) and -34,-36 (bake), all -96, facing south. Casing at -31/-33/-35 z2.
  - Catalysts y71 z3: waterlogged iron bars x-32..-30, lit campfires x-36..-34.
  - Slow belt (4 RPM, needed because fan processing needs ~150 ticks exposure): cog -28 70 3 -> cog -27 70 3 -> shaft -27 69 3 -> gearbox -27 68 3 -> shafts -27 68 4..5 -> gearbox -27 68 6 -> shaft -27 69 6 -> gearbox[axis=z] -27 70 6 -> RSC[axis=x]{ScrollValue:4} -28 70 6 -> large cog[axis=z] -28 71 6 -> shaft -28 71 5 -> belt start -28 71 4 (belt to -38, moves west at +4).
  - LESSONS: RSC large cog axis must differ from RSC axis. Vertical fans over belt did NOT process belt items; horizontal fan -> catalyst -> belt at belt level works. Changing RSC ScrollValue needs re-placing RSC + large cog. Never left-click the game (creative breaks blocks) - click the title bar (400,12) to refocus.
  - Input: chest -28 75 4 (wheat) -> hopper -> millstone -> hopper onto belt. Output: hopper -39 70 4 -> chest -39 70 5 (in floor).
  - Decor: FD oak cabinets + stoves (-38/-37 z11) with cooking pot/skillet, cutting boards, hood of industrial iron, upper cabinets y74, sink cauldron -40 71 7, crates/hay along z-1, 2 tables with red seats, depot display (bread/cake/cookies) x-38..-36 z6, slicer station -26 71..73 2 (decorative), glow frames flour/dough/bread on west wall, lamps y76, glowing sign -24 74 10.
- OLD NOTE: exterior/interior detail pass, then island expansion (x-45..40 z-40..60, avoid factory box x-12..8 z-8..15) and asphalt road south from door (x-4..0).

## Upgrade ideas (new mods)
- Replace creative motors with Create: Diesel Generators / Crafts & Additions power.
- Create Deco lamps, catwalks, bars for decoration.
- Slice & Dice + Farmer's Delight: wheat → flour → dough → bread line.

## Progress log
- (session 1) Plan written. Build starting.
- New world (Forge 1.20.1, beach/stone shore near spawn). OFFSET: new = old + (-60, -102, +10). New floor y70, factory x-11..7 z-7..11.
- Site prep: clear air x-13..9 y71..91 z-9..20; stone foundation y55..69 same x/z (replace air & water).
- Front yard cleared x-13..9 z21..32 (hill), path to z32.
- DONE: factory shell (floor, walls, casing band, roof, pillars, door frame, windows, ceiling shafts, lanterns, chimneys, cog, path, lamp posts).
- DONE: Machine 1 water wheel press (new: wheel -3 72 2, shaft -2 72 2, press -1 72 2, depot -1 70 2, water -3 73 1, drain -3 67..70 1, cap y73 x-4..-2 z0..2 + -3 74 1). Verified iron sheet.
- IN PROGRESS: Machine 2 farm (new coords = old + offset). Sent: barrels, lantern, casings, workbenches, poles (-8 71 -7..-3), piston (-8 71 -2), chassis (-10..-7 71 -1, -6 sticky), chest (-5 71 -1), PSI (-4 71 -1). The GAME CLOSED/CRASHED around the harvester fill command (/fill -10 71 0 -6 71 0 create:mechanical_harvester[facing=south]) — verify what exists after relaunch; check crash report.
- Relaunched; crash seemed one-off. FARM DONE & VERIFIED (output chest -4 70 -3 gets wheat/seeds/straw). NOTE 1.20.1: gearshift powered = piston EXTENDS, so extender=200, timer=600. Place light blocks BEFORE wheat. /save-all not available in singleplayer.
- (old todo, now done) farm: water rows y70 z0 & z6 (x-10..-6), farmland x-10..-6 z1..5, wheat y71, light y74, PSI south (-4 71 -2), hopper north (-4 70 -2), chest (-4 70 -3), gearshift axis x (-7 71 -2), motor west (-6 71 -2), timer north 600 (-7 71 -4), extender north 400 (-7 71 -3), covers.
- BELT LINE DONE & VERIFIED (belt x0..5 y71 z6, motor 0 71 5 ScrollValue -16, press 3 73 6 + motor 3 73 5, hopper 0 72 6 + supply chest 0 73 6, output hopper 6 70 6 -> chest 6 70 7).
- TODO: door (new z12): panel x0..2 y71..73 z12, poles x4..6 y72 z12, piston 3 72 12, gearshift 3 72 13, motor 3 72 14, casing 4 71 13 & 6 72 13, extender 4 72 13, receiver 5 72 13, plates -2 71 13 / -2 71 10 with links at y69.

## CAR "fireheart-cr01" (session 8, in progress)
- Built with datapack function statue:car (generator /tmp/claude-0/car.py). Local frame: world = (10+lx, 73+ly, -29+lz), faces +x (east), left side = north (lz0).
- Layout: rear engine (creative diesel tank 11 73 -27, pump 11 74 -27, diesel engine 11 75 -27 facing east, gearbox 12 75 -27, cog 12 74 -27, gearbox 12 73 -27, shaft 13 73 -27) -> Clockwork Command Seat 14 73 -27 facing east -> shafts 15..17 73 -27 -> gearbox 18 73 -27 axis y -> shafts 18 73 -28/-26 -> front wheels (trackwork med_simple_wheel) 18 73 -29 (facing south) and 18 73 -25 (facing north). Rear wheels 12 73 -29/-25 free-spin.
- Steering: seat left/right faces -> shafts -> speedometers 14 73 -29 / -25 -> comparators (x15) -> repeaters (x16,x17) -> front wheels (redstone steers).
- Seat NBT set: ForwardRules [{},{keys:1,op:3,value:1},{keys:2,op:3,value:-1},{},{}] (keys bitmask FORWARD1 BACKWARD2 LEFT4 RIGHT8; op MULTIPLY=3; default Left/Right rules = A/D x1).
- Extras: horn + button, hand crank on Suspension Controller (ride height), headlights/taillights, spoiler, spare tire, exhausts, plate sign, rear bench seats, steering-wheel valve handle.
- Assembled with Area Assembler (item NBT firstPosX/Y/Z preset to 9 76 -29, click on helper block 21 72 -25) -> ship renamed fireheart-cr01. One roof slab above the seat was broken to get in (sunroof).
- Input quirk: computer-use clicks rotate the camera to look straight down (+78 yaw), so any right/left click hits the block under the player's feet. Escape key currently not delivered.
- STATUS: player seated; holding W for 4 s showed no visible movement yet -> needs diagnosis (drive direction / wheel contact / key input / weight). Shipyard coords are beyond 30M so /data can't inspect ship blocks.
- CAR FIX (session 8b): Clockwork Command Seat never received key input (and seat/engine/cog/gearbox had vanished) -> replaced drive control with Drive By Wire.
  - Shipyard mapping for this ship: shipyard = (-28669957+lx, 127+ly, 12290045+lz).
  - (4,0,2) gearshift axis x (S reverses), (5,0,2) clutch axis x; comparator (5,0,3) facing south SUBTRACT with redstone block (5,0,4) behind and lever (6,0,3) as side "signal source" -> clutch powered (neutral) unless W/S pressed. Right steering repeaters removed.
  - Driver seat create:red_seat (4,1,2). DBW hub (6,1,2). Backup block (6,1,1) carries the network NBT (Name "car"; channels keyUp->comparator EAST face, keyDown->comparator+gearshift, keyLeft->FL wheel, keyRight->FR wheel, keyJump->horn). Linked controller given with {Hub:<hub shipyard pos as long>}.
  - Wheels were too high (car rested on its floor): floor changed to top slabs, chains removed, wheels swapped to trackwork:simple_wheel (radius 1.0). Car now drives ~1.5 m/s and accelerating, reverses, D turns right.
  - Controls: sit in red seat, hold Linked Controller, right-click to activate (opening chat/inventory deactivates it). W/S/A/D, Space = horn.
  - Function files: statue:carfix, cardbw, carwheels, cardiag.

### Car session 8c – spin fix
- Cause: front wheels face opposite ways (FL south, FR north) on one shared axle, so Trackwork drove them in opposite directions and the car yawed.
- Fix (`statue:cardiff`): gearshift[axis=z] at local (8,0,3) on the right front axle, kept powered by a redstone block at local (7,0,3).
- Verified: W drives +x straight (~17 blocks/1.5 s), S reverses straight, W+A turns left, W+D turns right.
- Recovery: `/vs teleport fireheart-cr01 15 75 -29` resets the car upright, facing east, in the open area.

### Car session 8d – engine sound
- Create Diesel Generators plays its engine loop at the block's shipyard position, and no engine sound reaches the player on the VS2 car.
- Replacement: tick-driven datapack sound (`car_engine_tick/sound/play/load`, plus minecraft tick/load tags).
  - Active when the player rides a create:seat while holding the Linked Controller, and the car's clutch block exists.
  - Every 10 ticks: stopsound + playsound `createdieselgenerators:engine_normal` at the player.
  - `car_rev` 0–30 rises 1/tick while the clutch is engaged (powered=false) and falls 2/tick otherwise; pitch goes from 0.75 to 1.3 in 6 steps.
  - The sound stops as soon as the player leaves the seat or puts the controller away.

### Car session 8e – custom engine sound + stray block
- The CDG engine loop sounded like a train, so it was replaced with a synthesized 4-cylinder engine loop (`/tmp/claude-0/snd/gen.py`: f0 55 Hz, 1 s, 80 ms fades).
- Resource pack `resourcepacks/FireheartCarSounds` adds the sound event `fireheart:car_engine`. The user must enable it in Options > Resource Packs.
- The datapack now uses 7 rev steps with pitch 0.5, 0.7, 0.9, 1.1, 1.35, 1.65, 2.0 and a replay interval of (1/pitch − 0.08) s.
  - rev 0–30 goes +1/tick with W/S held and −1/tick otherwise.
  - `car_engine_start` plays a start-up sound; `car_engine_off` plays a shut-off clunk.
- Stray block: the assembly helper block (world 21,72,-25) had been assembled into the ship at shipyard (-28669946,126,12290049), local (11,-1,4). Removed.

## Session 9 – City expansion (generators in /tmp/claude-0/city, helper lib.py, map scan scan.py/render.py)
Island layout: perimeter wall x-45/x40, z-40/z60; main road x-6..-2; road x9..13 (z13..40); E-W road z40..44 with sidewalks z38-39 and z45-46.
- statue:city_apartment – "Ember Heights" 6-floor brick apartment x18..34 z16..32 y70..95.
  - Entrance on the west side (z24) with a striped awning and sign.
  - Balconies on the south side, west flower boxes, ladder shaft in the NE corner, rooftop water tower, vents, antenna and a "Fireheart Motors" billboard.
- statue:city_shops – 3 shops at z49..58, doors on the north side: Diesel Diner (x3), Create Supply Co. (x15), Green Leaf Market (x27). Each has a striped awning, interior, loft and ladder. The sidewalk has lamps and birches.
- statue:city_park – Fireheart Plaza x-31..-9 z14..37: round fountain at (-20,25), cross paths, flower beds, seats, lamps, trees, and a south gate with a sign.
  - Working clock tower at (-26,18) with 4 cuckoo clocks at y88/89.
  - Driven by a buried diesel unit (engine -26 66 17 facing south) and a shaft column.
- statue:city_works – Fireheart Aggregates x-42..-14 z49..58.
  - Diesel engine -38 72 54 (east) drives a cog/millstone chain at y73 z54 (x-37..-25).
  - Input chests y75 (27 stacks each): cobble at x-36,-34,-30,-26 and gravel at x-32,-28.
  - Output chests y71. VERIFIED: gravel and flint produced at -96 RPM.
  - Flywheel at -23 76 54, chimneys with campfires, shipping containers outside.
- statue:city_yard – Container Port x-43..-16 z-38..-6: checkered concrete, fence and lamps, stacked coloured containers, yellow gantry crane with a hanging container, office hut, gate + asphalt to x-13 z-21..-18.
- statue:city_decor – lanterns on top of the perimeter wall every 6 blocks, seats along the main road (x-9).
- LESSON: diesel engines placed by command need `{Tanks:[{TankContent:{FluidName:"createdieselgenerators:diesel",Amount:1000}}]}` or they never start (the pump needs the engine running). lib.diesel() now adds it.
- A birch tree from `place feature` landed on the plaza path (-19,34); its trunk was removed.

### Session 9b – Apartment elevator (VERIFIED)
- statue:city_elevator builds a glass shaft on the Ember Heights east side: interior x35..37, z23..25, y70..98; landings through x34 at z24.
- Machine room on the roof (x30..34 z22..26 y95..100): diesel unit with engine at 32 99 24 facing east, shafts to the elevator pulley at 36 99 24 (facing=north, so axis x). The pulley runs at -96 RPM.
- Cabin was built at the roof stop (floor y94, roof y98, sea-lantern anchor under the pulley).
  - Framed glass door at 35 95..96 24, contraption controls at 37 95 24, cabin redstone contact at 35 97 23 facing west.
  - Glued with a super_glue entity summoned at 35.0 94.0 23.0, From [0,0,0] To [3,5,3], set via data merge.
- Landing contacts at 34 (y0+3) 23 facing east, with call buttons at 33 (y0+3) 23 and signs at 33 (y0+3) 24. Floors y0 = 70..94 step 4.
- Assembled by right-clicking the pulley with an empty hand (player tp'd to 36.5 100 24.5, looking down through the roof hole). Then statue:city_elevator_names set the names G/1-5/R and closed the hole with framed glass.
- Tested: a redstone pulse at the lobby and floor-3 contacts sent the cabin to each floor (contact powering=true).
- If the cabin vanishes (Create contraption reload bug): run city_elevator again, then re-click the pulley, then run city_elevator_names.

## Session 10 – Inspection, repairs, motorbike, marina, boat
- Integrity scan: `city/verify.py` builds statue:verify1/2 (execute unless block … tellraw "CHK …"). Mismatches are read from logs/latest.log.
  - Real issues fixed with statue:city_fix: diner door lintel (7/8 73 49) and panes (6/9 71-72 49), one statue pixel (-4 172 -65), and the apartment ladder.
  - The apartment ladder had popped off glass panes; it now runs at x31 z31 facing north with a roof trapdoor at 31 95 31.
- statue:checkup prints machine speeds/chest contents with tellraw.
  - Findings and fixes:
    - Farm engine had run dry: re-placed engine + pump (facing=up). Verified tank stays 1000 and the piston runs.
    - Door engine: refuelled, pump facing=up.
    - Belt supply chest was empty: refilled with 27×64 iron.
    - Bakery wheat input was empty: filled with 27×64 wheat; wheat and seeds removed from the farm output chest.
    - Gravel works inputs: refilled with statue:refill_gravel.
    - Clock-tower drive stuck at 0 RPM: rebuilt with statue:clockfix1 then clockfix2.
    - Gravel/elevator engines stalled after a data merge: fixed by re-placing the engine block.
- LESSON: don't `data merge` running diesel engines, it can stall the network. Re-place the engine block with Tanks NBT instead.
- LESSON: a linked controller must see key RELEASE before chat opens. Wait ~1s after hold_key, or a thruster stays on (the bike ran away).
- LESSON: click actions do nothing in spectator mode. Area Assembler assembly works when `/item replace entity @s weapon.mainhand with valkyrienskies:area_assembler{firstPosX..Z}` is used and the player stands on the top corner block.
- Hub binding: stand on the controller hub, right-click it holding an unbound create:linked_controller (a Hub tag gets written).
- MARINA (statue:veh_marina):
  - Stairs x4-5 from the sea wall z60 down to walkway y63 z67-68 (x2..26); fingers at x6-7, 14-15, 22-23 (z69..82) with lanterns.
  - Dockmaster hut x20-25 z61-64 with a "FIREHEART MARINA" sign; fuel tank at 26 64-65 66; buoys z84-86.
- BOAT fireheart-sea01 (statue:veh_boat): birch/white-wool hull, cabin, seats, nav lights, horn, 4× valkyrienskies:test_thruster (100 kN each) driven by the DBW network in a backup block.
  - Controls: W stern thruster facing +z, S reverse, A/D bow side thrusters, Space horn.
  - Hub long -7879610722575974274 (shipyard -28665857 126 12290048; local origin -28665858 125 12290042). Mass 66 t, floats.
  - Park command: `/vs teleport fireheart-sea01 10.5 63 75 (0 0 0)` (bow south in its slip).
- MOTORBIKE fireheart-mb01 (statue:veh_bike): 3-wide quad-style bike, 4 simple_wheels, iron ballast, W/S thrusters, A/D wheel steering via DBW, headlight/tail light, rear flag.
  - Hub long -7878484547791228801. Parked on the sidewalk by the plaza: `/vs teleport fireheart-mb01 -12 73 38.8 (0 0 0)`. The VS teleport target is the centre of mass, and the seat can land a few blocks away.
- Controllers in hotbar slots 1-3: Car (red), Boat (aqua), Motorbike (gold).


---

# PART 3 – MOD KNOWLEDGE BASE (main text, verbatim)

# Mod Knowledge Reference – "Create!" world (Create: Remastered profile)

Everything below was read directly from the 15 mod jars in your `mods` folder (block-state files, language files, ponder text, config files and compiled classes), plus your world's server configs. The full list of all 3,528 block IDs is in the appendix at the end.

## 1. Your mod list

| Mod | Version | Namespace | Blocks | What it adds |
|---|---|---|---|---|
| Create | 6.0.8 | `create` | 643 | Rotation-powered machines, contraptions, trains, logistics (Create 6 adds packages, frogports, stock tickers, factory gauges, chain conveyors) |
| Create Deco | 2.0.3 | `createdeco` | 397 | Metal decoration: bricks, sheet metal, catwalks, bars, lamps, doors, shipping containers, placards |
| Create Encased | 1.8 | `createcasing` | 507 | Shafts, cogs, gearboxes, pipes, chain drives etc. in many wood types and casing materials |
| Create Diesel Generators | 1.3.12 | `createdieselgenerators` | 64 | Diesel/huge/large engines, oil pumpjack, distillation, fuels, asphalt, cement, canisters |
| Create Crafts & Additions | 1.3.3 | `createaddition` | 23 | Electricity (FE): alternator, electric motor, wires/connectors, accumulators, tesla coil, rolling mill |
| Create Enchantment Industry | 2.5.2 | `create_enchantment_industry` | 20 | Liquid experience, blaze enchanter/forger, printer, mechanical grindstone |
| Create Big Cannons | 5.11.4 | `createbigcannons` | 139 | Built-up cannons, autocannons, shells, cannon mounts and loaders |
| Create Dragons Plus | 1.11.8 | `create_dragons_plus` | 54 | Fluid hatch, liquid dyes, dragon breath fluid |
| Steam 'n' Rails | 1.7.3 | `railways` | 1447 | Track types/gauges, conductors, semaphores, couplers, bogeys, couplers, locometal train-building blocks, smokestacks, hazard stripes |
| Farmer's Delight | 1.3.4 | `farmersdelight` | 132 | Crops, stove, cooking pot, skillet, cutting board, cabinets, crates, feasts |
| Slice & Dice | 3.6.0 | `sliceanddice` | 4 | Slicer (automated cutting board), sprinkler, fertilizer |
| Valkyrien Skies 2 | 2.4.11 | `valkyrienskies` | 6 | Physics "ships" (any block group becomes a moving physics object) |
| VS Clockwork | 0.5.6 | `vs_clockwork` | 73 | Propellers, wings, balloons, gas network, phys bearing, command seat, sensors, Gravitron |
| Trackwork | 1.2.3 | `trackwork` | 16 | Wheels, suspension/sprocket tank tracks, landing gear, car horn |
| Drive By Wire | 0.1.1 | `drivebywire` | 3 | Cable networks that carry redstone/controller inputs around a VS ship |

Also installed (no blocks): Embeddium + Oculus (performance/shaders), FerriteCore, Kotlin for Forge, Ritchie's Projectile Lib.

Important: your Create is **6.0.x**, not 0.5.x. Trackwork requires Create 6.0–6.1 and VS 2.4+, which matches.

## 2. Vehicle mods in detail

### Valkyrien Skies 2 (`valkyrienskies`)
- **What it does:** turns a group of blocks into a physics object ("ship"). The ship has mass, collides with terrain, can be pushed and falls under gravity.
- **Ways to create a ship (the "shipify" step):**
  - `valkyrienskies:area_assembler` (Area Assembler): right-click the first corner, then the second corner. Every block in the box becomes one ship. This is the most predictable method.
  - `valkyrienskies:ship_assembler` (Ship Assembler): right-click a block to assemble.
  - `valkyrienskies:ship_creator` / `ship_creator_smaller`: spawn a test ship.
  - `valkyrienskies:ship_remover`: removes a ship.
  - `valkyrienskies:connection_checker`: shows which blocks are connected.
- **`/vs` commands in this version:** `delete`, `dry`, `get-air`, `get-gravity`, `get-ship`, `remass`, `rename`, `scale`, `splitting`, `set-static <ships> true|false`, `teleport <ships> position/euler-angles/velocity/angular-velocity`, `backend`.
  - There is **no `/vs shipify` command** in this build, so assembly is always done with an item.
  - `set-static` is useful for freezing a car while working on it.
- **Your world's settings:**
  - `defaultBlockMass = 1000` (1 t per block unless the block has its own mass).
  - `defaultBlockFriction = 0.5`, `defaultBlockElasticity = 0.3`.
  - `enableWorldSplitting = false`, so breaking a block doesn't split the ship.
  - `enablePocketBuoyancy = false`, so enclosed hulls don't float from air pockets.
- **Aerodynamics:** every block has drag, and slabs give a little lift (per Clockwork's ponder text).
- **Debug blocks** (`test_chair`, `test_thruster`, `test_wing`, `test_flap`, `test_hinge`, `test_antigrav`): for testing only, not for real builds.

### Trackwork (`trackwork`) – wheels and tracks
- **Wheels:**
  - Blocks: `small_simple_wheel`, `med_simple_wheel`, `simple_wheel` (Large), `large_simple_wheel` (Gigantic).
  - Matching decorative `*_part` "Spare Tire" blocks.
  - Property: `facing` (horizontal) plus a `variant` (visual style, cycled by right-click).
- **Only works on assembled VS ships.** On normal ground a wheel is just a block.
- **Drive:**
  - A wheel is a Create kinetic block that rotates around its `facing` axis.
  - It is driven from its **inner side**: the block on the `facing` side must have a shaft pointing at it.
  - If nothing is connected there, the wheel **free-spins** (unpowered, rolls freely).
  - With `wheelRPMPassthrough = true`, rotation also passes *through* a wheel.
- **Steering (confirmed in code):**
  - Steering angle = redstone strength ÷ 15, taken from the strongest neighbouring signal on the wheel block.
  - The steering direction depends on which way that wheel faces (positive or negative axis).
  - The wheel then searches along its facing direction, up to `wheelPairDistance` (**7 blocks** in your config), for its paired wheel and copies the steering to it.
  - So two front wheels facing each other across the axle are paired automatically.
  - Powering the left front wheel turns one way; powering the right front wheel turns the other way.
- **Suspension (from the code):**
  - Every wheel, track and oleo wheel is a spring. Each physics tick it casts a ray down to the ground and pushes the vehicle up, with a limit on how far it can travel.
  - Stiffness and damping are set per vehicle: one setting shared by all its wheels and tracks, not per wheel.
  - Track Toolkit (`trackwork:track_tool_kit`), three modes, switched by right-clicking the air:
    1. Stiffness gauge: click a wheel for stiffer, sneak-click for softer (limited to 1–4).
    2. Damping gauge: click for less bouncy, sneak-click for bouncier.
    3. Power wrench: moves individual wheels forward/back (tracks sideways) to adjust wheelbase and balance.
  - Suspension Controller (`trackwork:track_level_controller`, has an `axis` property):
    - Rotation fed into it adjusts the whole vehicle's suspension while it spins; reverse the rotation to undo.
    - On the X or Z axis it tilts the body (lean left/right or nose up/down, limited to ±0.5).
    - On the Y axis it raises or lowers the ride height (0.1–1.0).
    - Faster rotation adjusts faster. This allows lowrider/hydraulic effects or levelling on slopes, and can be driven from the controller.
  - Suspension tracks (`*_suspension_track`) are the sprung road wheels of a tank tread; the `*_phys_track` sprockets drive the tread.
- **Tracks (tank treads):**
  - Blocks: `suspension_track`, `med_suspension_track`, `large_suspension_track`, plus sprocket versions `phys_track`, `med_phys_track`, `large_phys_track`.
  - They are built in a row like chain drives and powered through the sprockets.
- **Other blocks:**
  - `oleo_wheel` (landing gear): redstone on its sides steers, redstone on its front/back brakes, and a wrench switches between its two variants.
  - `track_level_controller` (Suspension Controller): input rotation tilts and adjusts suspension across the ship.
  - `horn` (Car Horn): sounds on a redstone signal; right-click sets the pitch.
- **Your config:**
  - `maxTrackRPM = 256` (1 RPM ≈ 0.104 m/s, so 256 RPM ≈ 27 m/s).
  - `enableTrackStress = false`, so the wheels don't overload the engine.

### Drive By Wire (`drivebywire`) – controls
- **Blocks:**
  - `controller_hub` (Linked Controller Hub).
  - `tweaked_controller_hub` (needs the Create Tweaked Controllers mod, which you don't have).
  - `backup_block` (saves the cable network when the ship is copied with a schematic).
- **Items:** `wire` (Cable) and `wire_cutter` (Cable Cutter).
- **Cables only work on assembled VS ships.**
- **Binding the controller:** right-click the Linked Controller Hub with Create's **Linked Controller** (`create:linked_controller`). The hub position is saved on the controller ("Controller connected!").
- **Channels:** the hub has six channels, one per controller key: `keyUp` (W), `keyDown` (S), `keyLeft` (A), `keyRight` (D), `keyJump` (Space), `keyShift` (Shift).
  - While holding a Cable, scroll to pick a channel, then click the hub and then the target block.
  - That key's redstone is then delivered to the target block.
  - Block-to-block cables carry ordinary redstone.
- **Limitation:** connections are made by hand with the Cable item; they can't be created with commands. The Backup Block exists so copies built from a schematic keep their wiring.

### VS Clockwork (`vs_clockwork`) – useful for vehicles and flying machines
- **`command_seat` (Command Seat):**
  - A seat that is also a Create split shaft. Rotation passes through it and is changed while the rider presses W/A/S/D.
  - Each key has its own list of rules, set in the seat's GUI. The operations are None, Move (distance), Rotate (angle) and Multiply.
  - Example: W = ×1 and S = ×−1 turns an engine shaft into forward/reverse drive without any redstone.
- **`phys_bearing` (Phys Bearing):** joints a *separate* ship onto a bearing. Rotation modes are Follow angle, Unlocked and Locked. Can be used for steerable axles, turrets and doors.
- **`physics_infuser` + Wanderwand:** Clockwork's own assembly method. Select an area with the wand, put the wand in the infuser, and the selection becomes a ship.
- **Gravitron:** a handheld tool that grabs, assembles and disassembles ships (max 256 mass in survival).
- **`redstone_resistor`:** passes rotation through, slowing it as redstone increases (16 RPM input → 10.67 → 5.33 → 0). Usable as a throttle.
- **Stabilisers:** `gyro` (stabilises the ship; redstone on each side tilts it) and `reactionwheel`.
- **Sensors:**
  - `distance_sensor` (Peepotron): distance/ground sensing.
  - `gyroscopic_sensor` (Spinotron): tilt sensing.
  - `impact_sensor` (Impactotron): detects collisions.
  - `alt_meter` (Altimeter): outputs redstone at a target altitude.
- **Flight:**
  - Propellers: `brass_propeller_bearing`, `juryrigged_propeller_bearing` and `copter_bearing`, with `blade_controller` to set blade angle.
  - Wings: `flap` (Wing) and `wing` (Cambered Wing, +20° angle of attack), plus the flap bearings `andesite_flap_bearing` and `smart_flap_bearing`, which tilt by redstone.
  - Balloons: `balloon_casing`, `gas_nozzle`, `ballooner`.
  - Thrust: `gas_thruster`, `afterblazer`, `sugar_rocket`.
- **Gas network:** `duct`, `pump_duct`, `valve_duct`, `gas_tank`, `air_compressor`, `coal_burner`, `gas_heater`, `gas_engine`, `steam_generator`, `exhaust`, `intake`, `extendon` (a pressure-driven piston), `hose_port`.
- **Also:**
  - `universal_shaft` (connects rotation between two points up to 10 blocks apart).
  - `spinoff_bearing`.
  - `slicker` (a slippery surface piston).
  - `delivery_cannon`/`delivery_chute` (move items between ships).
  - `combustion_engine`.
- **Decoration:** Wanderlite and Nyx stone sets, `wanderglass`, `clock`.

## 3. Car building plan (based on the above)

**Parts:**

| Part | Block(s) | Notes |
|---|---|---|
| Frame | `createdeco:*_sheet_metal`, `create:industrial_iron_block`, slabs | Keep it light (1 t per block by default) and low for a low centre of gravity |
| Wheels | 4× `trackwork:med_simple_wheel` | Front pair and rear pair, each pair facing inward, ≤ 7 blocks apart |
| Engine | `createdieselgenerators:diesel_engine` + small tank | `powered=true` (redstone) switches it off, because "Engines disabled with redstone" is on in your config |
| Drive train | `create:shaft`, `create:gearbox`, `create:gearshift` or `create:clutch` | Rear wheels driven through their inner faces |
| Drive control | Option A: `vs_clockwork:command_seat` with rules W ×1, S ×−1 (no wiring). Option B: `create:gearshift` + `create:clutch` wired to W/S with Drive By Wire | A is simpler |
| Steering | Drive By Wire hub: A → front-left wheel, D → front-right wheel | Confirmed by Trackwork's steering code |
| Driver | `vs_clockwork:command_seat` or `create:*_seat` | Rider holds the Linked Controller |
| Extras | `trackwork:horn` on Space, `createdeco` lamps for headlights, `create:item_vault` or a chest for the trunk, `trackwork:*_simple_wheel_part` as a spare tyre | |

**Build order:**
1. Build the frame with a function file.
2. Set wheel facings and shaft connections.
3. Place the hub and seat.
4. Assemble with the Area Assembler.
5. Wire the Drive By Wire cables by hand.
6. Bind the Linked Controller.
7. Test and tune: Track Toolkit stiffness around the middle and damping raised until it stops bouncing; wheelbase adjusted with the wrench mode; optionally a Suspension Controller on a Y-axis shaft to set ride height.

**Risks to test first:**
- Which rotation direction counts as "forward".
- Whether Command Seat rules are easier than wired gearshifts.
- How much steering signal strength is needed.
- Tipping at speed.
- The car surviving a save and reload.

A small test cart (frame, 4 wheels, engine, seat, hub) should come before the full car.

## 4. The other mods – key blocks at a glance
- **Create 6:**
  - Power sources: `creative_motor`, `water_wheel`/`large_water_wheel`, `windmill_bearing`, `steam_engine`, `hand_crank`, `flywheel`.
  - Transmission: `shaft`, `cogwheel`, `large_cogwheel`, `gearbox`, `gearshift`, `clutch`, `sequenced_gearshift`, `rotation_speed_controller`, `encased_chain_drive`, `adjustable_chain_gearshift`, `chain_conveyor`.
  - Processing: `mechanical_press`, `mechanical_mixer`, `millstone`, `crushing_wheel`, `encased_fan`, `mechanical_saw`, `deployer`, `mechanical_crafter`, `spout`, `item_drain`, `basin`, `blaze_burner`.
  - Logistics: `belt`, `chute`/`smart_chute`, `funnel`s, `tunnel`s, `mechanical_arm`, `weighted_ejector`, `item_vault`, plus Create 6's `packager`, `repackager`, `package_frogport`, `stock_link`, `stock_ticker`, `redstone_requester`, `factory_gauge`.
  - Contraptions: `mechanical_piston`/`sticky_mechanical_piston`, `mechanical_bearing`, `clockwork_bearing`, `rope_pulley`, `elevator_pulley`, `gantry_carriage`, `cart_assembler`, `linear_chassis`/`secondary_linear_chassis`/`radial_chassis`, `sticker`, `contraption_controls`, `mechanical_drill`/`harvester`/`plough`/`roller`.
  - Fluids: `fluid_pipe`, `mechanical_pump`, `fluid_tank`, `hose_pulley`, `portable_fluid_interface`.
  - Redstone: `redstone_link`, `pulse_repeater`, `pulse_extender`, `pulse_timer`, `analog_lever`, `nixie_tube`, `display_link`, `display_board`, `content_observer`, `stockpile_switch`.
  - Controls: `linked_controller` item, `lectern_controller`.
  - Trains: `track`, `track_station`, `track_signal`, `track_observer`, `controls`, `train_door`/`train_trapdoor`, `railway_casing`.
- **Diesel Generators:**
  - Engines: `diesel_engine`, `large_diesel_engine` (modular), `huge_diesel_engine`. Turbocharged engines run at 2× speed.
  - Oil: `pumpjack_*` (oil extraction), `distillation_tank`, `bulk_fermenter`, `burner`, `oil_barrel`, `canister`.
  - Fuels: `diesel`, `gasoline`, `biodiesel`, `ethanol`, `crude_oil`, `plant_oil`.
  - Building: `asphalt_*`, 16 colours of `*_cement`, `sheet_metal_panel`, `chip_wood_*`, `andesite_girder`.
- **Crafts & Additions:**
  - Power: `electric_motor` (FE → rotation), `alternator` (rotation → FE), `accumulator`/`modular_accumulator`, `creative_energy`.
  - Wiring: `connector`/`large_connector`/`small_light_connector` (wire nodes), `copper_wire_casing`, `redstone_relay`.
  - Machines: `tesla_coil` (charging), `rolling_mill` (plates → rods/wire), `portable_energy_interface`, `liquid_blaze_burner`, `digital_adapter`.
  - Also: `barbed_wire`.
- **Enchantment Industry:**
  - Experience: `experience` (fluid), `experience_hatch`, `experience_lantern`, `super_experience_block`.
  - Enchanting: `blaze_enchanter`/`classic_blaze_enchanter`, `blaze_forger`, `blaze_composer`, `affix_augmentor`, `gem_cutter`, `brass_bookshelf`.
  - Other machines: `printer`, `mechanical_grindstone`, `grindstone_drain`, `infuser`.
  - Also: `ender_woven_bag`.
- **Big Cannons:**
  - Cannon parts: barrels, chambers, ends and breeches (sliding, screw, quick-firing) in log, wrought iron, cast iron, bronze, steel and nethersteel; built-up cannon layers.
  - Mounts: `cannon_mount` + `yaw_controller`, `fixed_cannon_mount`, `cannon_carriage`.
  - Manufacturing: `cannon_loader` + `ram_head`/`worm_head`, `cannon_builder`, `cannon_drill`, casting moulds and molten metals.
  - Autocannon parts and `autocannon_ammo_container`.
  - Ammunition: `powder_charge`, `solid_shot`, `ap_shot`, `he_shell`, `ap_shell`, `shrapnel_shell`, `smoke_shell`, `fluid_shell`, `drop_mortar_shell`, `bag_of_grapeshot`, `big_cartridge`.
  - Also: `traffic_cone` (a handy road decoration).
- **Steam 'n' Rails:**
  - Tracks: many track materials and gauges (standard, wide, narrow, monorail).
  - Signalling and routing: `semaphore`, `track_coupler`, `track_switch`, `conductor_whistle`.
  - Train details: `smokestack_*`, `conductor_vent`, `fuel_tank`, `portable_fuel_interface`, buffers/headstocks/`link_and_pin` couplers, and `locometal_*` decoration (boilers, smokeboxes, doors, windows, ladders, vents).
  - Hazard-stripe blocks: `*hazard_stripes_diagonal_on_black`, `*hazard_stripes_chevron_on_white` and similar, in many colours. These would suit the garage bay frames.
  - Bogeys and handcar parts.
- **Farmer's Delight:**
  - Kitchen: `stove`, `cooking_pot`, `skillet`, `cutting_board`, `*_cabinet`, `wooden_basket`/`bamboo_basket` (there is no plain `basket`, which is why that block failed in the kitchen).
  - Crops: `cabbages`, `tomatoes`, `onions`, `rice`, `*_crate`, `rice_bag`, `straw_bale`, `rich_soil`, `organic_compost`.
  - Food: pies and feasts.
  - Decoration: `canvas_sign`s, `tatami`, `safety_net`, `rope`.
- **Slice & Dice:** `slicer` (cuts like a cutting board, used like a press), `sprinkler` (speeds crop growth; can spread fluids such as fertilizer), `fertilizer`, `wet_air`.
- **Create Encased:** encased versions of shafts, cogs, large cogs, gearboxes, clutches, gearshifts, chain drives and pipes in all wood types, bamboo and glass, inside andesite, brass, copper, railway and other casings. Also configurable gearboxes, a brass chain conveyor, a brass deployer and a brass depot.
- **Create Deco:**
  - Brick sets: brick, copper, zinc, industrial iron, andesite, brass and more, with slabs, stairs, walls and tiles.
  - Metal building blocks: `*_sheet_metal`, `*_catwalk` + railing + stairs, `*_bars`, `*_support`, `*_hull`, `*_window`, `*_door`/`trapdoor`, `*_ladder`, `*_mesh_fence`.
  - Lights and fittings: `*_lamp` (lit/inverted states), `*_placard`, `*_shipping_container` in 16 colours, `*_coinstack`.


---

# PART 4 – GENERATOR CODE

Recreate these files in `/tmp/claude-0/city/` in a new session.

## lib.py
```python
import os

ATTACH = ('lamp', 'torch', 'lantern', 'button', 'lever', 'sign', 'banner', 'carpet', 'flower_pot', 'potted', 'ladder',
          'door', 'trapdoor', 'rail', 'pressure_plate', 'candle', 'seat', 'redstone_wire', 'repeater', 'comparator',
          'item_frame', 'dandelion', 'poppy', 'tulip', 'allium', 'orchid', 'cornflower', 'oxeye', 'azure', 'lily',
          'grass[', 'fern', 'bush', 'rose', 'peony', 'lilac', 'sunflower', 'hopper', 'chute', 'depot', 'water',
          'lava', 'bed', 'cuckoo', 'nixie', 'display_board', 'whistle', 'valve_handle', 'hand_crank', 'decal',
          'lightning_rod', 'end_rod', 'chain', 'bell', 'belt', 'cogwheel', 'shaft', 'gearbox', 'millstone', 'fan',
          'engine', 'pump', 'fluid_tank', 'bearing', 'drill', 'press', 'mixer', 'basin', 'vault', 'funnel', 'tunnel',
          'bars', 'pane', 'fence', 'wall[', '_wall', 'light[', 'campfire', 'scaffold', 'sapling', 'kelp', 'vine')
PANE = ('_pane', 'iron_bars', 'industrial_iron_bars')


class Build:
    def __init__(self):
        self.b = {}
        self.pre = []
        self.post = []

    def set(self, x, y, z, s):
        if ':' not in s.split('[')[0].split('{')[0]:
            s = 'minecraft:' + s
        self.b[(x, y, z)] = s

    def box(self, x0, y0, z0, x1, y1, z1, s):
        for x in range(min(x0, x1), max(x0, x1) + 1):
            for y in range(min(y0, y1), max(y0, y1) + 1):
                for z in range(min(z0, z1), max(z0, z1) + 1):
                    self.set(x, y, z, s)

    def walls(self, x0, y0, z0, x1, y1, z1, s):
        for x in range(x0, x1 + 1):
            for y in range(y0, y1 + 1):
                for z in range(z0, z1 + 1):
                    if x in (x0, x1) or z in (z0, z1):
                        self.set(x, y, z, s)

    def get(self, x, y, z):
        return self.b.get((x, y, z))

    def resolve_panes(self):
        for (x, y, z), s in list(self.b.items()):
            base = s.split('[')[0]
            if '[' in s or not any(base.endswith(p) for p in PANE):
                continue
            props = []
            for d, (dx, dz) in (('north', (0, -1)), ('south', (0, 1)), ('west', (-1, 0)), ('east', (1, 0))):
                n = self.b.get((x + dx, y, z + dz))
                ok = False
                if n:
                    nb = n.split('[')[0]
                    ok = any(nb.endswith(p) for p in PANE) or not any(k in nb for k in ATTACH) and 'slab' not in nb and 'stairs' not in nb and 'air' not in nb
                props.append(f'{d}={"true" if ok else "false"}')
            self.b[(x, y, z)] = f'{base}[{",".join(props)}]'

    def commands(self):
        self.resolve_panes()
        solid, late = [], []
        for k, s in self.b.items():
            (late if any(a in s for a in ATTACH) else solid).append((k, s))
        out = list(self.pre)
        for group in (solid, late):
            group.sort(key=lambda kv: (kv[0][1], kv[0][2], kv[0][0]))
            cur = None
            for (x, y, z), s in group:
                if '{' not in s and cur and cur[1] == y and cur[2] == z and cur[3] == s and cur[4] == x - 1:
                    cur[4] = x
                    continue
                if cur:
                    out.append(self._emit(cur))
                cur = [x, y, z, s, x]
            if cur:
                out.append(self._emit(cur))
        return out + self.post

    @staticmethod
    def _emit(c):
        x, y, z, s, x1 = c
        if x1 == x:
            return f'setblock {x} {y} {z} {s}'
        return f'fill {x} {y} {z} {x1} {y} {z} {s}'


FUNC_DIR = '/mnt/user-data/outputs/statue_datapack/statue/data/statue/functions'


def write(name, lines, chunk=6000):
    os.makedirs(FUNC_DIR, exist_ok=True)
    names = []
    for i in range(0, len(lines), chunk):
        n = f'{name}{i // chunk + 1}' if len(lines) > chunk else name
        open(f'{FUNC_DIR}/{n}.mcfunction', 'w').write('\n'.join(lines[i:i + chunk]) + f'\nsay {n.upper()}_DONE\n')
        names.append(n)
    return names


def diesel(B, x, y, z, facing):
    d = {'north': (0, -1), 'south': (0, 1), 'west': (-1, 0), 'east': (1, 0)}[facing]
    axis = 'x' if facing in ('north', 'south') else 'z'
    B.set(x, y - 2, z, 'create:creative_fluid_tank{Size:1,Height:1,Uninitialized:1b,TankContent:{FluidName:"createdieselgenerators:diesel",Amount:8000}}')
    B.set(x, y - 1, z, 'create:mechanical_pump[facing=up]')
    B.set(x, y, z, f'createdieselgenerators:diesel_engine[facing={facing}]{{Tanks:[{{TankContent:{{FluidName:"createdieselgenerators:diesel",Amount:1000}}}}]}}')
    gx, gz = x + d[0], z + d[1]
    B.set(gx, y, gz, f'create:gearbox[axis={axis}]')
    B.set(gx, y - 1, gz, 'create:cogwheel[axis=y]')
    return gx, y, gz


def lamp_post(B, x, y, z, h=3, color='yellow'):
    for i in range(h):
        B.set(x, y + i, z, 'minecraft:polished_blackstone_wall')
    B.set(x, y + h, z, f'createdeco:{color}_industrial_iron_lamp[facing=up,lit=true,inverted=true]')


def sign(B, x, y, z, facing, lines, wood='dark_oak', color='white', glow=True):
    msgs = ','.join("'\"%s\"'" % l for l in (lines + ['', '', '', ''])[:4])
    B.set(x, y, z, f'minecraft:{wood}_wall_sign[facing={facing}]{{front_text:{{color:"{color}",has_glowing_text:{1 if glow else 0}b,messages:[{msgs}]}}}}')

```

## apartment.py
```python
import random
from lib import Build, write, sign, lamp_post
random.seed(3)
B = Build()
X0, X1, Z0, Z1 = 18, 34, 16, 32
G = 70
FL = 6
H = 4
TOP = G + FL * H
B.pre.append(f'fill 14 71 13 39 {TOP + 12} 37 air')
B.pre.append('fill 14 70 13 39 70 37 minecraft:grass_block')
B.box(X0 - 1, G, Z0 - 1, X1 + 1, G, Z1 + 1, 'minecraft:stone_bricks')
B.box(X0, G - 3, Z0, X1, G - 1, Z1, 'minecraft:stone')
for f in range(FL):
    y0 = G + f * H
    B.box(X0, y0, Z0, X1, y0, Z1, 'minecraft:spruce_planks' if f else 'minecraft:polished_andesite')
    for y in range(y0 + 1, y0 + H):
        for x in range(X0, X1 + 1):
            for z in range(Z0, Z1 + 1):
                edge_x = x in (X0, X1)
                edge_z = z in (Z0, Z1)
                if not (edge_x or edge_z):
                    continue
                if edge_x and edge_z:
                    B.set(x, y, z, 'create:industrial_iron_block')
                    continue
                t = (z - Z0) if edge_x else (x - X0)
                win = t % 4 in (2, 3) and y in (y0 + 1, y0 + 2)
                if f == 0 and edge_x and x == X0 and 22 <= z <= 26:
                    win = False
                if win:
                    B.set(x, y, z, 'minecraft:black_stained_glass_pane' if f == 0 else 'minecraft:glass_pane')
                elif t % 4 == 0:
                    B.set(x, y, z, 'createdeco:dusk_bricks')
                else:
                    B.set(x, y, z, 'minecraft:bricks')
    for x in range(X0, X1 + 1):
        for z in range(Z0, Z1 + 1):
            if x in (X0, X1) or z in (Z0, Z1):
                B.set(x, y0 + H, z, 'create:industrial_iron_block')
    if f:
        for x in range(X0 + 2, X1 - 1, 4):
            for z in range(Z0 + 2, Z1 - 1, 4):
                if random.random() < 0.7:
                    B.set(x, y0 + 3, z, 'minecraft:light[level=13]')
        room = random.choice(['bed', 'lounge', 'office'])
        cx, cz = X0 + 4, Z0 + 3
        if room == 'bed':
            B.set(cx, y0 + 1, cz, 'minecraft:red_bed[facing=south,part=head]')
            B.set(cx, y0 + 1, cz + 1, 'minecraft:red_bed[facing=south,part=foot]')
            B.set(cx + 2, y0 + 1, cz, 'minecraft:bookshelf')
        elif room == 'lounge':
            B.set(cx, y0 + 1, cz, 'create:red_seat')
            B.set(cx + 1, y0 + 1, cz, 'create:black_seat')
            B.set(cx, y0 + 1, cz + 2, 'minecraft:dark_oak_fence')
            B.set(cx, y0 + 2, cz + 2, 'minecraft:dark_oak_pressure_plate')
        else:
            B.set(cx, y0 + 1, cz, 'minecraft:crafting_table')
            B.set(cx + 1, y0 + 1, cz, 'minecraft:lectern[facing=south]')
            B.set(cx + 2, y0 + 1, cz, 'minecraft:bookshelf')
        B.set(X1 - 3, y0 + 1, Z1 - 2, 'minecraft:potted_fern')
        B.set(X1 - 4, y0 + 1, Z1 - 1, 'minecraft:barrel[facing=up]')
        B.set(X1 - 2, y0 + 1, Z0 + 1, 'minecraft:red_carpet')
        B.set(X1 - 3, y0 + 1, Z0 + 1, 'minecraft:red_carpet')
for f in range(1, FL + 1):
    B.set(X1 - 3, G + f * H, Z1 - 1, 'minecraft:air')
for y in range(G + 1, TOP + 1):
    B.set(X1 - 3, y, Z1 - 1, 'minecraft:ladder[facing=north]')
B.set(X1 - 3, TOP + 1, Z1 - 1, 'minecraft:spruce_trapdoor[facing=north,half=bottom,open=false]')

lobby = G
B.box(X0 + 1, lobby + 1, 21, X0 + 3, lobby + 1, 27, 'minecraft:red_carpet')
for z in (22, 26):
    B.set(X0 + 5, lobby + 1, z, 'create:red_seat')
B.set(X0 + 6, lobby + 1, 24, 'minecraft:dark_oak_fence')
B.set(X0 + 6, lobby + 2, 24, 'minecraft:dark_oak_pressure_plate')
B.box(X0 + 10, lobby + 1, 18, X0 + 14, lobby + 1, 18, 'minecraft:polished_blackstone')
B.set(X0 + 12, lobby + 2, 18, 'minecraft:bell[attachment=floor,facing=south]')
for x in range(X0 + 2, X1 - 1, 3):
    for z in range(Z0 + 2, Z1 - 1, 3):
        B.set(x, lobby + 3, z, 'minecraft:light[level=14]')
for z in (23, 24, 25):
    B.set(X0, lobby + 1, z, 'minecraft:air')
    B.set(X0, lobby + 2, z, 'minecraft:air')
B.set(X0, lobby + 3, 23, 'minecraft:bricks')
B.set(X0, lobby + 3, 25, 'minecraft:bricks')
B.set(X0, lobby + 3, 24, 'minecraft:bricks')
B.set(X0, lobby + 1, 24, 'minecraft:dark_oak_door[facing=west,half=lower,hinge=left]')
B.set(X0, lobby + 2, 24, 'minecraft:dark_oak_door[facing=west,half=upper,hinge=left]')
B.set(X0, lobby + 1, 23, 'minecraft:black_stained_glass_pane')
B.set(X0, lobby + 2, 23, 'minecraft:black_stained_glass_pane')
B.set(X0, lobby + 1, 25, 'minecraft:black_stained_glass_pane')
B.set(X0, lobby + 2, 25, 'minecraft:black_stained_glass_pane')
for z in range(21, 28):
    B.set(X0 - 1, lobby + 4, z, 'minecraft:red_terracotta' if z % 2 else 'minecraft:white_terracotta')
    B.set(X0 - 2, lobby + 4, z, ('minecraft:red_concrete' if z % 2 else 'minecraft:white_concrete'))
    B.set(X0 - 2, lobby + 3, z, 'minecraft:black_concrete' if z in (21, 27) else 'minecraft:air')
B.set(X0 - 2, lobby + 1, 21, 'minecraft:polished_blackstone_wall')
B.set(X0 - 2, lobby + 2, 21, 'minecraft:polished_blackstone_wall')
B.set(X0 - 2, lobby + 1, 27, 'minecraft:polished_blackstone_wall')
B.set(X0 - 2, lobby + 2, 27, 'minecraft:polished_blackstone_wall')
B.set(X0 - 2, lobby + 3, 21, 'minecraft:polished_blackstone_wall')
B.set(X0 - 2, lobby + 3, 27, 'minecraft:polished_blackstone_wall')
sign(B, X0 - 1, lobby + 5, 24, 'west', ['', 'EMBER HEIGHTS', 'APARTMENTS', ''], color='orange')
B.set(X0 - 1, lobby + 5, 22, 'createdeco:yellow_industrial_iron_lamp[facing=west,lit=true,inverted=true]')
B.set(X0 - 1, lobby + 5, 26, 'createdeco:yellow_industrial_iron_lamp[facing=west,lit=true,inverted=true]')
for x in range(14, X0 - 1):
    for z in range(23, 26):
        B.set(x, G, z, 'minecraft:polished_andesite' if z == 24 else 'minecraft:stone_bricks')
B.set(X0 - 1, G, 24, 'minecraft:polished_andesite')
for f in range(1, FL):
    y0 = G + f * H
    for x in range(X0 + 2, X1 - 1, 4):
        B.box(x, y0, Z1 + 1, x + 2, y0, Z1 + 2, 'minecraft:smooth_stone_slab[type=top]')
        for xx in range(x, x + 3):
            B.set(xx, y0 + 1, Z1 + 2, 'minecraft:iron_bars')
        B.set(x, y0 + 1, Z1 + 1, 'minecraft:iron_bars')
        B.set(x + 2, y0 + 1, Z1 + 1, 'minecraft:iron_bars')
        if random.random() < 0.6:
            B.set(x + 1, y0 + 1, Z1 + 1, random.choice(['minecraft:potted_red_tulip', 'minecraft:potted_poppy', 'minecraft:potted_dandelion', 'minecraft:potted_azure_bluet']))
    for z in range(Z0 + 2, Z1 - 3, 4):
        if f == 1 and 18 <= z <= 28:
            continue
        B.set(X0 - 1, y0 + 3, z, 'minecraft:spruce_trapdoor[facing=west,half=top,open=true]') if False else None
        B.set(X0 - 1, y0, z + 2, 'minecraft:polished_blackstone_slab[type=top]')
        B.set(X0 - 1, y0, z + 3, 'minecraft:polished_blackstone_slab[type=top]')
        B.set(X0 - 1, y0 + 1, z + 2, 'minecraft:flower_pot') if False else None
        B.set(X0 - 1, y0 + 1, z + 2, random.choice(['minecraft:red_tulip', 'minecraft:poppy', 'minecraft:cornflower', 'minecraft:dandelion']))
        B.set(X0 - 1, y0 + 1, z + 3, random.choice(['minecraft:red_tulip', 'minecraft:poppy', 'minecraft:cornflower', 'minecraft:oxeye_daisy']))
        B.set(X0 - 1, y0, z + 2, 'minecraft:moss_block')
        B.set(X0 - 1, y0, z + 3, 'minecraft:moss_block')
R = TOP + 1
for x in range(X0, X1 + 1):
    for z in range(Z0, Z1 + 1):
        if x in (X0, X1) or z in (Z0, Z1):
            B.set(x, R, z, 'minecraft:polished_blackstone_wall')
B.box(X0 + 1, TOP, Z0 + 1, X1 - 1, TOP, Z1 - 1, 'createdeco:industrial_iron_sheet_metal[axis=y]')
tx, tz = X0 + 5, Z0 + 5
for dx, dz in ((0, 0), (2, 0), (0, 2), (2, 2)):
    for y in range(R, R + 4):
        B.set(tx + dx, y, tz + dz, 'minecraft:spruce_fence')
B.box(tx, R + 4, tz, tx + 2, R + 4, tz + 2, 'minecraft:spruce_planks')
B.box(tx, R + 5, tz, tx + 2, R + 8, tz + 2, 'minecraft:spruce_planks')
B.box(tx + 1, R + 5, tz + 1, tx + 1, R + 7, tz + 1, 'minecraft:water')
for y in (R + 5, R + 7):
    for dx in range(3):
        for dz in range(3):
            if (dx, dz) != (1, 1):
                B.set(tx + dx, y, tz + dz, 'minecraft:stripped_spruce_log[axis=y]' if dx != 1 and dz != 1 else 'minecraft:spruce_planks')
B.box(tx, R + 9, tz, tx + 2, R + 9, tz + 2, 'minecraft:spruce_slab[type=bottom]')
B.set(tx + 1, R + 9, tz + 1, 'minecraft:spruce_planks')
B.set(tx + 1, R + 10, tz + 1, 'minecraft:lightning_rod')
sign(B, tx + 1, R + 6, tz - 1, 'north', ['', 'EMBER', 'WATER CO.', ''], wood='spruce', color='black', glow=False)
for (fx, fz) in ((X1 - 4, Z0 + 3), (X1 - 4, Z0 + 7), (X1 - 8, Z0 + 3)):
    B.set(fx, R, fz, 'create:encased_fan[facing=up]')
    B.set(fx, R - 0, fz, 'create:encased_fan[facing=up]')
    B.set(fx + 1, R, fz, 'create:andesite_casing')
B.set(X0 + 2, R, Z1 - 2, 'create:copper_valve_handle[facing=up]')
for y in range(R, R + 6):
    B.set(X1 - 2, y, Z1 - 3, 'minecraft:iron_bars')
B.set(X1 - 2, R + 6, Z1 - 3, 'minecraft:end_rod[facing=up]')
B.set(X1 - 2, R + 3, Z1 - 4, 'minecraft:redstone_lamp[lit=true]') if False else None
B.set(X1 - 2, R + 7, Z1 - 3, 'minecraft:light[level=10]')
for x in range(X0 + 1, X1, 3):
    B.set(x, R + 1, Z0, 'minecraft:light[level=8]') if False else None
for z in range(Z0 + 2, Z1 - 1, 4):
    B.set(X1 + 1, G + 12, z, 'createdeco:red_industrial_iron_lamp[facing=east,lit=true,inverted=true]')
bill_x = X1 - 3
for x in range(X0 + 9, X0 + 14):
    B.set(x, R + 1, Z0, 'minecraft:black_concrete')
    B.set(x, R + 2, Z0, 'minecraft:red_concrete')
    B.set(x, R + 3, Z0, 'minecraft:black_concrete')
sign(B, X0 + 11, R + 2, Z0 - 1, 'north', ['FIREHEART', 'MOTORS', 'CR-01 on sale!', ''], color='yellow')
for z in (14, 20, 28, 34):
    lamp_post(B, 14, G + 1, z)
for (x, z) in ((15, 17), (15, 31), (37, 15), (37, 35), (26, 35), (15, 35)):
    B.post.append(f'place feature minecraft:fancy_oak {x} {G + 1} {z}')
for x in range(20, 36, 3):
    B.set(x, G + 1, 35, random.choice(['minecraft:rose_bush[half=lower]', 'minecraft:peony[half=lower]', 'minecraft:lilac[half=lower]']))
    B.set(x, G + 2, 35, B.get(x, G + 1, 35).replace('lower', 'upper'))
lines = B.commands()
print(len(lines), write('city_apartment', lines))

```

## shops.py
```python
import random
from lib import Build, write, sign, lamp_post
random.seed(5)
B = Build()
G = 70
B.pre.append('fill 1 71 47 39 90 59 air')
B.pre.append('fill 1 70 47 39 70 59 minecraft:grass_block')
SHOPS = [
    (3, 'createdeco:dusk_bricks', 'red', ['', 'DIESEL', 'DINER', ''], 'diner'),
    (15, 'minecraft:bricks', 'yellow', ['CREATE', 'SUPPLY CO.', 'gears & shafts', ''], 'supply'),
    (27, 'createdeco:scarlet_bricks', 'lime', ['', 'GREEN LEAF', 'MARKET', ''], 'market'),
]
W, D, Z0 = 10, 10, 49
for x0, wall, col, text, kind in SHOPS:
    x1, z1 = x0 + W - 1, Z0 + D - 1
    B.box(x0, G, Z0, x1, G, z1, 'minecraft:polished_andesite')
    B.walls(x0, G + 1, Z0, x1, G + 8, z1, wall)
    for x in (x0, x1):
        for z in (Z0, z1):
            B.box(x, G + 1, z, x, G + 9, z, 'create:industrial_iron_block')
    B.box(x0, G + 5, Z0, x1, G + 5, z1, 'create:industrial_iron_block')
    B.box(x0 + 1, G + 5, Z0 + 1, x1 - 1, G + 5, z1 - 1, 'minecraft:dark_oak_planks')
    B.box(x0, G + 9, Z0, x1, G + 9, z1, 'createdeco:industrial_iron_sheet_metal[axis=y]')
    for x in range(x0, x1 + 1):
        B.set(x, G + 10, Z0, 'minecraft:polished_blackstone_wall')
        B.set(x, G + 10, z1, 'minecraft:polished_blackstone_wall')
    for z in range(Z0, z1 + 1):
        B.set(x0, G + 10, z, 'minecraft:polished_blackstone_wall')
        B.set(x1, G + 10, z, 'minecraft:polished_blackstone_wall')
    for x in range(x0 + 1, x1):
        for y in (G + 1, G + 2, G + 3):
            B.set(x, y, Z0, 'minecraft:glass_pane')
        B.set(x, G + 4, Z0, wall)
    B.set(x0 + 4, G + 1, Z0, 'minecraft:spruce_door[facing=north,half=lower,hinge=left]')
    B.set(x0 + 4, G + 2, Z0, 'minecraft:spruce_door[facing=north,half=upper,hinge=left]')
    B.set(x0 + 5, G + 1, Z0, 'minecraft:spruce_door[facing=north,half=lower,hinge=right]')
    B.set(x0 + 5, G + 2, Z0, 'minecraft:spruce_door[facing=north,half=upper,hinge=right]')
    B.set(x0 + 4, G + 3, Z0, wall)
    B.set(x0 + 5, G + 3, Z0, wall)
    for x in range(x0 + 1, x1):
        if x % 3:
            B.set(x, G + 7, Z0, 'minecraft:glass_pane')
            B.set(x, G + 6, Z0, 'minecraft:glass_pane')
    for x in range(x0, x1 + 1):
        c = col if (x - x0) % 2 == 0 else 'white'
        B.set(x, G + 4, Z0 - 1, f'minecraft:{c}_wool')
        B.set(x, G + 3, Z0 - 2, f'minecraft:{c}_carpet') if False else None
        B.set(x, G + 4, Z0 - 2, f'minecraft:{"black" if c == "white" else c}_carpet') if False else None
        B.set(x, G + 3, Z0 - 1, 'minecraft:air')
    for x in range(x0, x1 + 1):
        B.set(x, G + 4, Z0 - 2, f'minecraft:{col if (x - x0) % 2 == 0 else "white"}_carpet') if False else None
    sign(B, x0 + 4, G + 5, Z0 - 1, 'north', text[:2] + ['', ''], color='white')
    sign(B, x0 + 5, G + 5, Z0 - 1, 'north', text[2:] + ['', ''], color='white') if text[2] else None
    B.set(x0 + 2, G + 5, Z0 - 1, f'createdeco:yellow_industrial_iron_lamp[facing=north,lit=true,inverted=true]')
    B.set(x0 + 7, G + 5, Z0 - 1, f'createdeco:yellow_industrial_iron_lamp[facing=north,lit=true,inverted=true]')
    for x in range(x0 + 1, x1, 2):
        for z in range(Z0 + 2, z1, 3):
            B.set(x, G + 4, z, 'minecraft:light[level=14]')
            B.set(x, G + 8, z, 'minecraft:light[level=12]')
    B.box(x0 + 1, G + 1, z1 - 2, x1 - 1, G + 1, z1 - 2, 'minecraft:spruce_planks' if kind != 'supply' else 'create:andesite_casing')
    B.set(x0 + 1, G + 1, z1 - 1, 'minecraft:air')
    if kind == 'diner':
        for x in (x0 + 2, x0 + 7):
            B.set(x, G + 1, Z0 + 3, 'minecraft:dark_oak_fence')
            B.set(x, G + 2, Z0 + 3, 'minecraft:white_carpet')
            B.set(x - 1, G + 1, Z0 + 3, 'create:red_seat')
            B.set(x + 1, G + 1, Z0 + 3, 'create:red_seat')
        B.set(x0 + 3, G + 2, z1 - 2, 'farmersdelight:cooking_pot[facing=north]') if False else None
        B.set(x0 + 3, G + 2, z1 - 2, 'minecraft:cake')
        B.set(x0 + 6, G + 2, z1 - 2, 'minecraft:smoker[facing=north]') if False else None
        B.set(x0 + 5, G + 1, z1 - 1, 'minecraft:smoker[facing=north,lit=true]')
        B.set(x0 + 6, G + 1, z1 - 1, 'minecraft:furnace[facing=north,lit=true]')
        B.set(x0 + 4, G + 1, z1 - 1, 'minecraft:barrel[facing=north]')
        B.box(x0 + 1, G + 1, Z0 + 1, x1 - 1, G + 1, Z0 + 1, 'minecraft:air')
        for x in range(x0 + 1, x1):
            if x not in (x0 + 4, x0 + 5):
                B.set(x, G, Z0 + 1, 'minecraft:red_concrete' if x % 2 else 'minecraft:white_concrete')
    elif kind == 'supply':
        for x in range(x0 + 1, x1):
            B.set(x, G + 1, z1 - 1, 'minecraft:barrel[facing=north]')
            B.set(x, G + 2, z1 - 1, random.choice(['create:cogwheel[axis=y]', 'create:large_cogwheel[axis=y]', 'create:shaft[axis=x]', 'create:gearbox[axis=y]']))
        for x in (x0 + 2, x0 + 7):
            B.set(x, G + 1, Z0 + 4, 'create:depot')
        B.set(x0 + 4, G + 2, z1 - 2, 'create:brass_hand') if False else None
        B.set(x0 + 3, G + 2, z1 - 2, 'create:red_toolbox[facing=north]')
        B.set(x0 + 6, G + 2, z1 - 2, 'create:yellow_toolbox[facing=north]')
        B.set(x0 + 5, G + 2, z1 - 2, 'create:wrench') if False else None
        for x in range(x0 + 1, x1):
            B.set(x, G + 3, z1 - 1, 'createdeco:decal_warning[face=wall,facing=north]') if x in (x0 + 2, x0 + 7) else None
    else:
        crops = ['minecraft:melon', 'minecraft:pumpkin', 'minecraft:hay_block', 'minecraft:carved_pumpkin[facing=north]']
        for x in range(x0 + 1, x1):
            if x in (x0 + 4, x0 + 5):
                continue
            B.set(x, G + 1, Z0 + 3, 'minecraft:composter[level=8]') if False else None
            B.set(x, G + 1, Z0 + 3, random.choice(crops))
            B.set(x, G + 1, Z0 + 5, 'minecraft:barrel[facing=up]')
        B.set(x0 + 3, G + 2, z1 - 2, 'minecraft:potted_red_mushroom')
        B.set(x0 + 6, G + 2, z1 - 2, 'minecraft:potted_bamboo')
    B.set(x0 + 1, G + 1, z1 - 1, 'minecraft:ladder[facing=north]') if False else None
    for y in range(G + 1, G + 6):
        B.set(x1 - 1, y, z1 - 1, 'minecraft:ladder[facing=west]')
    B.set(x1 - 1, G + 5, z1 - 1, 'minecraft:ladder[facing=west]')
    B.set(x0 + 2, G + 6, z1 - 1, 'minecraft:red_bed[facing=north,part=foot]') if False else None
    B.set(x0 + 2, G + 6, z1 - 2, 'minecraft:red_bed[facing=south,part=foot]') if False else None
    B.set(x0 + 2, G + 6, z1 - 1, 'minecraft:blue_bed[facing=south,part=head]')
    B.set(x0 + 2, G + 6, z1 - 2, 'minecraft:blue_bed[facing=south,part=foot]')
    B.set(x0 + 5, G + 6, z1 - 1, 'minecraft:bookshelf')
    B.set(x0 + 3, G + 6, Z0 + 2, 'minecraft:potted_fern')
    B.set(x0 + 2, G + 10, Z0 + 3, 'create:encased_fan[facing=up]')
    B.set(x0 + 7, G + 10, z1 - 3, 'railways:conductor_vent[conductor_visible=false]')
for x in range(1, 40):
    B.set(x, G, 47, 'minecraft:stone_bricks')
    B.set(x, G, 48, 'minecraft:polished_andesite' if x % 4 else 'minecraft:stone_bricks')
for x in (13, 14, 25, 26):
    B.set(x, G, 49, 'minecraft:grass_block')
for x in (13, 25, 37):
    B.post.append(f'place feature minecraft:birch {x} {G + 1} 52')
for x in (2, 14, 26, 38):
    lamp_post(B, x, G + 1, 48)
for (x, z) in ((13, 49), (26, 49)):
    B.set(x, G + 1, z, 'create:red_seat') if False else None
B.set(13, G + 1, 48, 'create:black_seat') if False else None
for x in (8, 20, 32):
    B.set(x, G + 1, 47, 'minecraft:potted_cornflower') if False else None
lines = B.commands()
print(len(lines), write('city_shops', lines))

```

## park.py
```python
import random, math
from lib import Build, write, sign, lamp_post, diesel
random.seed(11)
B = Build()
G = 70
XA, XB, ZA, ZB = -31, -9, 14, 37
B.pre.append(f'fill {XA} 71 {ZA} {XB} 100 {ZB} air')
B.pre.append(f'fill {XA} 70 {ZA} {XB} 70 {ZB} minecraft:grass_block')
CX, CZ = -20, 25
for x in range(XA, XB + 1):
    for z in range(CZ - 1, CZ + 2):
        B.set(x, G, z, 'minecraft:stone_bricks' if z != CZ else 'minecraft:polished_andesite')
for z in range(ZA, ZB + 1):
    for x in range(CX - 1, CX + 2):
        B.set(x, G, z, 'minecraft:stone_bricks' if x != CX else 'minecraft:polished_andesite')
for x in range(CX - 7, CX + 8):
    for z in range(CZ - 7, CZ + 8):
        d = math.hypot(x - CX, z - CZ)
        if d <= 6.5:
            B.set(x, G, z, 'minecraft:stone_bricks' if d > 5.5 else 'minecraft:polished_andesite')
        if d <= 3.5:
            B.set(x, G, z, 'minecraft:water')
            B.set(x, G - 1, z, 'minecraft:sea_lantern' if (x + z) % 3 == 0 else 'minecraft:stone_bricks')
            B.set(x, G - 2, z, 'minecraft:stone_bricks')
        elif d <= 4.5:
            B.set(x, G, z, 'minecraft:polished_blackstone')
            B.set(x, G + 1, z, 'minecraft:polished_blackstone_slab[type=bottom]')
            B.set(x, G - 1, z, 'minecraft:stone_bricks')
B.box(CX, G - 1, CZ, CX, G + 3, CZ, 'minecraft:stone_bricks')
B.set(CX, G + 3, CZ, 'minecraft:chiseled_stone_bricks')
B.set(CX, G + 4, CZ, 'minecraft:water')
for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
    B.set(CX + dx, G + 3, CZ + dz, 'minecraft:stone_brick_stairs[facing=%s,half=top]' % {(1, 0): 'west', (-1, 0): 'east', (0, 1): 'north', (0, -1): 'south'}[(dx, dz)])
    B.set(CX + dx, G + 1, CZ + dz, 'minecraft:air')
    B.set(CX + dx, G + 2, CZ + dz, 'minecraft:air')
for a in range(8):
    ang = a * math.pi / 4
    x, z = CX + round(5.5 * math.cos(ang)), CZ + round(5.5 * math.sin(ang))
    if a % 2:
        lamp_post(B, x, G + 1, z)
    else:
        pass
seats = [(CX - 6, CZ - 3), (CX - 6, CZ + 3), (CX + 6, CZ - 3), (CX + 6, CZ + 3), (CX - 3, CZ - 6), (CX + 3, CZ - 6), (CX - 3, CZ + 6), (CX + 3, CZ + 6)]
for (x, z) in seats:
    B.set(x, G + 1, z, random.choice(['create:red_seat', 'create:black_seat', 'create:brown_seat']))
beds = [(XA + 1, ZA + 8, XA + 6, ZA + 9), (XB - 6, ZA + 1, XB - 1, ZA + 2), (XA + 1, ZB - 2, XA + 7, ZB - 1), (XB - 7, ZB - 2, XB - 1, ZB - 1), (XB - 6, ZA + 5, XB - 5, ZA + 8)]
flowers = ['minecraft:poppy', 'minecraft:red_tulip', 'minecraft:orange_tulip', 'minecraft:dandelion', 'minecraft:cornflower', 'minecraft:allium', 'minecraft:oxeye_daisy', 'minecraft:pink_tulip', 'minecraft:azure_bluet']
for x0, z0, x1, z1 in beds:
    for x in range(x0 - 1, x1 + 2):
        for z in range(z0 - 1, z1 + 2):
            if x in (x0 - 1, x1 + 1) or z in (z0 - 1, z1 + 1):
                B.set(x, G + 1, z, 'minecraft:spruce_slab[type=bottom]')
            else:
                B.set(x, G + 1, z, 'minecraft:rooted_dirt') if False else None
                B.set(x, G + 1, z, random.choice(flowers))
for (x, z) in ((XA + 3, ZA + 3), (XA + 3, ZB - 6), (XB - 3, ZB - 6), (XA + 10, ZB - 4), (XB - 12, ZA + 3), (XB - 3, CZ + 5), (XA + 8, CZ - 6), (XA + 3, CZ + 4), (XB - 10, ZB - 3)):
    B.post.append(f'place feature minecraft:{random.choice(["fancy_oak", "birch", "cherry", "oak"])} {x} {G + 1} {z}')
for z in range(ZA, ZB + 1, 5):
    if abs(z - CZ) > 2:
        B.set(CX - 2, G + 1, z, 'minecraft:azalea' if z % 2 else 'minecraft:flowering_azalea')
        B.set(CX + 2, G + 1, z, 'minecraft:flowering_azalea' if z % 2 else 'minecraft:azalea')
for x in range(XA, XB + 1, 6):
    if abs(x - CX) > 7:
        lamp_post(B, x, G + 1, CZ - 2)
        B.set(x + 2, G + 1, CZ + 2, 'create:red_seat')
for z in range(ZA + 2, ZB, 7):
    if abs(z - CZ) > 7:
        lamp_post(B, CX + 2, G + 1, z) if False else None
        lamp_post(B, CX - 2, G + 1, z + 2)
TX, TZ = -26, 18
Y = 88
B.box(TX - 3, G, TZ - 3, TX + 3, G, TZ + 3, 'minecraft:polished_andesite')
B.walls(TX - 2, G + 1, TZ - 2, TX + 2, Y + 2, TZ + 2, 'minecraft:bricks')
for x in (TX - 2, TX + 2):
    for z in (TZ - 2, TZ + 2):
        B.box(x, G + 1, z, x, Y + 3, z, 'create:industrial_iron_block')
for y in range(G + 5, Y, 5):
    B.walls(TX - 2, y, TZ - 2, TX + 2, y, TZ + 2, 'createdeco:dusk_bricks')
    for d in (-1, 1):
        B.set(TX, y + 2, TZ + 2 * d, 'minecraft:glass_pane')
        B.set(TX + 2 * d, y + 2, TZ, 'minecraft:glass_pane')
        B.set(TX, y + 3, TZ + 2 * d, 'minecraft:glass_pane')
        B.set(TX + 2 * d, y + 3, TZ, 'minecraft:glass_pane')
B.set(TX, G + 1, TZ + 2, 'minecraft:dark_oak_door[facing=south,half=lower,hinge=left]')
B.set(TX, G + 2, TZ + 2, 'minecraft:dark_oak_door[facing=south,half=upper,hinge=left]')
B.set(TX, G + 3, TZ + 3, 'createdeco:yellow_industrial_iron_lamp[facing=south,lit=true,inverted=true]')
sign(B, TX + 1, G + 3, TZ + 3, 'south', ['', 'CLOCK', 'TOWER', ''], color='yellow')
for y in range(G + 4, Y, 4):
    B.set(TX - 1, y, TZ - 1, 'minecraft:light[level=12]')
ex, ey, ez = TX, 66, TZ - 1
B.pre.append(f'fill {TX - 1} 62 {TZ - 2} {TX + 1} 69 {TZ + 1} minecraft:stone')
gx, gy, gz = diesel(B, ex, ey, ez, 'south')
for y in range(gy + 1, Y):
    B.set(gx, y, gz, 'create:shaft[axis=y]')
B.set(gx, Y, gz, 'create:gearbox[axis=x]')
B.set(gx, Y, gz - 1, 'create:shaft[axis=z]')
B.set(gx, Y, gz + 1, 'create:shaft[axis=z]')
B.set(gx, Y, gz - 2, 'create:cuckoo_clock[facing=north]')
B.set(gx, Y, gz + 2, 'create:cuckoo_clock[facing=south]')
B.set(gx, Y + 1, gz, 'create:gearbox[axis=z]')
B.set(gx - 1, Y + 1, gz, 'create:shaft[axis=x]')
B.set(gx + 1, Y + 1, gz, 'create:shaft[axis=x]')
B.set(gx - 2, Y + 1, gz, 'create:cuckoo_clock[facing=west]')
B.set(gx + 2, Y + 1, gz, 'create:cuckoo_clock[facing=east]')
B.box(TX - 3, Y + 3, TZ - 3, TX + 3, Y + 3, TZ + 3, 'minecraft:polished_blackstone_bricks')
for i, s in enumerate(range(3, 0, -1)):
    B.box(TX - s + 1, Y + 4 + i, TZ - s + 1, TX + s - 1, Y + 4 + i, TZ + s - 1, 'minecraft:red_nether_bricks')
B.set(TX, Y + 7, TZ, 'minecraft:bell[attachment=floor,facing=south]') if False else None
B.set(TX, Y + 7, TZ, 'minecraft:lightning_rod')
B.set(TX, Y + 2, TZ, 'minecraft:bell[attachment=ceiling,facing=south]')
for d in ((-3, 0), (3, 0), (0, -3), (0, 3)):
    B.set(TX + d[0], Y + 4, TZ + d[1], 'createdeco:red_industrial_iron_lamp[facing=up,lit=true,inverted=true]')
sign(B, CX, G + 1, CZ + 7, 'south', ['FIREHEART', 'PLAZA', 'est. 2026', ''], color='yellow') if False else None
B.set(CX + 2, G + 1, ZB, 'minecraft:polished_blackstone_wall')
B.set(CX - 2, G + 1, ZB, 'minecraft:polished_blackstone_wall')
B.set(CX + 2, G + 2, ZB, 'minecraft:polished_blackstone_wall')
B.set(CX - 2, G + 2, ZB, 'minecraft:polished_blackstone_wall')
B.box(CX - 2, G + 3, ZB, CX + 2, G + 3, ZB, 'minecraft:polished_blackstone_bricks')
sign(B, CX, G + 3, ZB + 1, 'south', ['', 'FIREHEART', 'PLAZA', ''], color='orange')
B.set(CX - 2, G + 4, ZB, 'createdeco:yellow_industrial_iron_lamp[facing=up,lit=true,inverted=true]')
B.set(CX + 2, G + 4, ZB, 'createdeco:yellow_industrial_iron_lamp[facing=up,lit=true,inverted=true]')
for x in range(XA, XB + 1):
    if abs(x - CX) > 2 and x % 2 == 0:
        B.set(x, G + 1, ZB, 'minecraft:oak_leaves[persistent=true]')
lines = B.commands()
print(len(lines), write('city_park', lines))

```

## works.py
```python
import random
from lib import Build, write, sign, lamp_post, diesel
random.seed(21)
B = Build()
G = 70
X0, X1, Z0, Z1 = -42, -14, 49, 58
B.pre.append('fill -44 71 47 -9 95 59 air')
B.pre.append('fill -44 70 47 -9 70 59 minecraft:grass_block')
B.pre.append('fill -44 66 47 -9 69 59 minecraft:stone replace #minecraft:dirt')
B.box(X0, G, Z0, X1, G, Z1, 'minecraft:polished_andesite')
HT = 11
B.walls(X0, G + 1, Z0, X1, G + HT, Z1, 'minecraft:bricks')
for x in range(X0, X1 + 1, 4):
    for z in (Z0, Z1):
        B.box(x, G + 1, z, x, G + HT + 1, z, 'create:industrial_iron_block')
for z in (Z0, Z1):
    B.box(X1, G + 1, z, X1, G + HT + 1, z, 'create:industrial_iron_block')
for z in range(Z0, Z1 + 1, 3):
    B.box(X0, G + 1, z, X0, G + HT + 1, z, 'create:industrial_iron_block')
    B.box(X1, G + 1, z, X1, G + HT + 1, z, 'create:industrial_iron_block')
B.walls(X0, G + 1, Z0, X1, G + 1, Z1, 'minecraft:stone_bricks')
B.walls(X0, G + 6, Z0, X1, G + 6, Z1, 'create:andesite_casing')
for x in range(X0 + 1, X1):
    if (x - X0) % 4:
        for z in (Z0, Z1):
            for y in (G + 3, G + 4, G + 8, G + 9):
                B.set(x, y, z, 'create:framed_glass')
B.box(X0, G + HT + 1, Z0, X1, G + HT + 1, Z1, 'createdeco:industrial_iron_sheet_metal[axis=y]')
for x in range(X0 + 2, X1 - 1, 3):
    B.set(x, G + HT + 1, 53, 'create:framed_glass')
    B.set(x, G + HT + 1, 54, 'create:framed_glass')
for x in range(X0, X1 + 1):
    B.set(x, G + HT + 2, Z0, 'createdeco:industrial_iron_bars')
    B.set(x, G + HT + 2, Z1, 'createdeco:industrial_iron_bars')
DX = -28
for y in range(G + 1, G + 4):
    for x in (DX - 1, DX, DX + 1):
        B.set(x, y, Z0, 'minecraft:air')
B.box(DX - 2, G + 4, Z0 - 1, DX + 2, G + 4, Z0 - 1, 'minecraft:yellow_concrete')
B.set(DX - 2, G + 4, Z0 - 1, 'minecraft:black_concrete')
B.set(DX, G + 4, Z0 - 1, 'minecraft:black_concrete')
B.set(DX + 2, G + 4, Z0 - 1, 'minecraft:black_concrete')
for x in (DX - 2, DX + 2):
    for y in range(G + 1, G + 4):
        B.set(x, y, Z0 - 1, 'minecraft:yellow_concrete' if y % 2 else 'minecraft:black_concrete')
sign(B, DX, G + 5, Z0 - 1, 'north', ['FIREHEART', 'AGGREGATES', 'gravel & flint', ''], color='yellow') if False else None
B.box(DX - 3, G + 6, Z0 - 1, DX + 3, G + 7, Z0 - 1, 'minecraft:black_concrete')
sign(B, DX - 1, G + 7, Z0 - 2, 'north', ['', 'FIREHEART', '', ''], color='yellow')
sign(B, DX + 1, G + 7, Z0 - 2, 'north', ['', 'AGGREGATES', '', ''], color='yellow')
sign(B, DX, G + 6, Z0 - 2, 'north', ['', 'gravel & flint', '', ''], color='white')
for x in (DX - 3, DX + 3):
    B.set(x, G + 5, Z0 - 1, 'createdeco:yellow_industrial_iron_lamp[facing=north,lit=true,inverted=true]')
for x in range(DX - 1, DX + 2):
    for z in range(Z0 - 2, Z0):
        B.set(x, G, z, 'minecraft:polished_andesite')
    B.set(x, G, 47, 'minecraft:polished_andesite')
    B.set(x, G, 48, 'minecraft:polished_andesite')
ZL = 54
Y = 73
ex = -38
gx, gy, gz = diesel(B, ex, 72, ZL, 'east')
B.set(gx, 73, gz, 'create:cogwheel[axis=y]')
B.set(ex - 1, 72, ZL, 'create:fluid_pipe') if False else None
items = {'cobble': 'minecraft:cobblestone', 'gravel': 'minecraft:gravel'}
order = ['cobble', 'cobble', 'gravel', 'cobble', 'gravel', 'cobble']
x = gx + 1
labels = []
for kind in order:
    B.set(x, Y, ZL, 'create:millstone')
    B.set(x, Y + 1, ZL, 'minecraft:hopper[facing=down]')
    inv = ','.join('{Slot:%db,id:"%s",Count:64b}' % (i, items[kind]) for i in range(27))
    B.set(x, Y + 2, ZL, 'minecraft:chest[facing=north]{Items:[%s]}' % inv)
    B.set(x, Y - 1, ZL, 'minecraft:hopper[facing=down]')
    B.set(x, Y - 2, ZL, 'minecraft:chest[facing=north]')
    None and sign(B, x, Y + 2, ZL - 1, 'north', ['INPUT', 'cobblestone' if kind == 'cobble' else 'gravel', '', ''], color='white', glow=False)
    None and sign(B, x, Y - 2, ZL - 1, 'north', ['OUTPUT', 'gravel' if kind == 'cobble' else 'flint', '', ''], color='white', glow=False)
    B.set(x + 1, Y, ZL, 'create:cogwheel[axis=y]')
    B.set(x + 1, Y - 1, ZL, 'create:andesite_casing')
    B.set(x + 1, Y - 2, ZL, 'create:andesite_casing')
    x += 2
last = x - 1
B.set(last, Y, ZL, 'create:andesite_casing') if False else None
B.set(last, Y + 1, ZL, 'create:shaft[axis=y]')
B.set(last, Y + 2, ZL, 'create:speedometer[facing=north,axis_along_first=false]') if False else None
B.set(last, Y + 2, ZL, 'create:shaft[axis=y]')
B.set(last, Y + 3, ZL, 'create:flywheel[axis=y]') if False else None
B.set(last, Y + 3, ZL, 'create:gearbox[axis=z]')
B.set(last + 1, Y + 3, ZL, 'create:shaft[axis=x]')
B.set(last + 2, Y + 3, ZL, 'create:flywheel[axis=x]')
B.set(last + 3, Y + 3, ZL, 'create:shaft[axis=x]')
B.set(last + 4, Y + 3, ZL, 'create:speedometer[facing=north,axis_along_first=true]') if False else None
for xx in range(gx - 3, last + 5):
    for z in (ZL - 2, ZL + 2):
        B.set(xx, G, z, 'minecraft:yellow_concrete' if xx % 2 else 'minecraft:black_concrete')
for xx in range(gx - 3, last + 4):
    B.set(xx, G + 7, ZL - 2, 'createdeco:industrial_iron_catwalk[bottom=true]') if False else None
for xx in range(X0 + 1, X1):
    B.set(xx, G + 6, ZL + 3, 'minecraft:smooth_stone_slab[type=top]')
    B.set(xx, G + 7, ZL + 2, 'minecraft:iron_bars')
B.set(X1 - 1, G + 1, Z1 - 1, 'minecraft:air')
for y in range(G + 1, G + 7):
    B.set(X1 - 1, y, ZL + 3, 'minecraft:ladder[facing=north]') if False else None
for y in range(G + 1, G + 8):
    B.set(X1 - 1, y, Z1 - 1, 'minecraft:ladder[facing=west]')
for xx in range(X0 + 2, X1 - 1, 3):
    for z in (Z0 + 2, Z1 - 2):
        B.set(xx, G + HT - 1, z, 'minecraft:light[level=15]')
    B.set(xx, G + 3, Z0 + 2, 'minecraft:light[level=13]')
for i, (xx, zz) in enumerate(((X0 + 2, Z1 - 2), (X0 + 4, Z1 - 2), (X0 + 2, Z1 - 4))):
    B.set(xx, G + 1, zz, random.choice(['createdeco:red_shipping_container[axis=x,large=false]', 'minecraft:barrel[facing=up]', 'create:item_vault[axis=x,large=false]']))
B.set(X0 + 3, G + 1, Z0 + 2, 'minecraft:anvil[facing=north]')
B.set(X0 + 4, G + 1, Z0 + 2, 'create:red_toolbox[facing=south]')
B.set(X0 + 5, G + 1, Z0 + 2, 'minecraft:crafting_table')
for xx in (X0 + 3, X1 - 3):
    B.box(xx, G + HT + 2, 55, xx + 1, G + HT + 9, 56, 'minecraft:bricks')
    B.box(xx, G + HT + 6, 55, xx + 1, G + HT + 6, 56, 'minecraft:stone_bricks')
    B.set(xx, G + HT + 10, 55, 'minecraft:campfire[lit=true,signal_fire=true]')
    B.set(xx + 1, G + HT + 10, 56, 'minecraft:campfire[lit=true,signal_fire=true]')
    B.set(xx + 1, G + HT + 10, 55, 'minecraft:bricks')
    B.set(xx, G + HT + 10, 56, 'minecraft:bricks')
for (xx, zz, ax) in ((-12, 52, 'z'), (-12, 55, 'z'), (-43, 52, 'z')):
    B.set(xx, G + 1, zz, f'createdeco:blue_shipping_container[axis={ax},large=false]')
    B.set(xx, G + 2, zz, f'createdeco:orange_shipping_container[axis={ax},large=false]')
for xx in (-40, -16):
    lamp_post(B, xx, G + 1, 47)
lines = B.commands()
print(len(lines), write('city_works', lines))

```

## yard.py
```python
import random
from lib import Build, write, sign, lamp_post
random.seed(8)
B = Build()
G = 70
XA, XB, ZA, ZB = -43, -16, -38, -6
B.pre.append(f'fill {XA} 71 {ZA} {XB} 95 {ZB} air')
for x in range(XA, XB + 1):
    for z in range(ZA, ZB + 1):
        B.set(x, G, z, 'minecraft:gray_concrete' if (x // 4 + z // 4) % 2 else 'minecraft:light_gray_concrete')
for x in range(XA, XB + 1):
    for z in (ZA, ZB):
        B.set(x, G + 1, z, 'createdeco:industrial_iron_mesh_fence') if False else None
        B.set(x, G + 1, z, 'minecraft:iron_bars')
        B.set(x, G + 2, z, 'minecraft:iron_bars')
for z in range(ZA, ZB + 1):
    for x in (XA, XB):
        B.set(x, G + 1, z, 'minecraft:iron_bars')
        B.set(x, G + 2, z, 'minecraft:iron_bars')
for x in range(XA, XB + 1, 6):
    for z in (ZA, ZB):
        B.set(x, G + 1, z, 'create:industrial_iron_block')
        B.set(x, G + 2, z, 'create:industrial_iron_block')
        B.set(x, G + 3, z, 'createdeco:yellow_industrial_iron_lamp[facing=up,lit=true,inverted=true]')
for z in range(ZB - 3, ZB + 1):
    pass
for z in (-21, -20, -19, -18):
    B.set(XB, G + 1, z, 'minecraft:air')
    B.set(XB, G + 2, z, 'minecraft:air')
    B.set(XB, G, z, 'minecraft:yellow_concrete' if z % 2 else 'minecraft:black_concrete')
for x in range(XB + 1, -12):
    for z in (-21, -20, -19, -18):
        B.set(x, G, z, 'createdieselgenerators:asphalt_block')
cols = ['red', 'blue', 'green', 'orange', 'yellow', 'white', 'gray', 'cyan', 'purple', 'lime']
for row, z in enumerate(range(ZA + 3, ZB - 3, 4)):
    for x in range(XA + 3, XB - 8, 4):
        if random.random() < 0.15:
            continue
        h = random.randint(1, 3)
        for y in range(h):
            c = random.choice(cols)
            for dx in range(3):
                for dz in range(2):
                    B.set(x + dx, G + 1 + y, z + dz, f'createdeco:{c}_shipping_container[axis=x,large=false]')
GX0, GX1 = XA + 1, XB - 7
for z in (ZA + 1, ZB - 1):
    for x in (GX0, GX1):
        B.box(x, G + 1, z, x, G + 12, z, 'minecraft:yellow_concrete')
        B.set(x, G + 13, z, 'create:industrial_iron_block')
    B.box(GX0, G + 13, z, GX1, G + 13, z, 'minecraft:yellow_concrete')
    for x in range(GX0, GX1 + 1, 2):
        B.set(x, G + 13, z, 'minecraft:black_concrete')
CZ = (ZA + ZB) // 2
for z in range(ZA + 1, ZB):
    B.set(-30, G + 14, z, 'minecraft:yellow_concrete')
    B.set(-29, G + 14, z, 'minecraft:yellow_concrete')
    B.set(-30, G + 13, z, 'minecraft:black_concrete') if z % 4 == 0 else None
B.box(-31, G + 15, CZ - 1, -28, G + 17, CZ + 1, 'minecraft:yellow_concrete')
B.box(-30, G + 16, CZ - 2, -29, G + 16, CZ - 2, 'minecraft:black_stained_glass')
for y in range(G + 9, G + 14):
    B.set(-30, y, CZ, 'minecraft:chain[axis=y]')
    B.set(-29, y, CZ, 'minecraft:chain[axis=y]')
B.box(-31, G + 8, CZ - 1, -28, G + 8, CZ + 1, 'create:industrial_iron_block')
for dx in range(3):
    for dz in range(2):
        B.set(-31 + dx, G + 7, CZ + dz, 'createdeco:red_shipping_container[axis=x,large=false]')
B.set(-28, G + 18, CZ, 'createdeco:red_industrial_iron_lamp[facing=up,lit=true,inverted=true]')
OX, OZ = XB - 6, ZB - 7
B.walls(OX, G + 1, OZ, OX + 4, G + 4, OZ + 4, 'minecraft:white_concrete')
B.box(OX, G + 5, OZ, OX + 4, G + 5, OZ + 4, 'createdeco:industrial_iron_sheet_metal[axis=y]')
for y in (G + 2, G + 3):
    B.set(OX + 2, y, OZ, 'minecraft:glass_pane')
    B.set(OX, y, OZ + 2, 'minecraft:glass_pane')
B.set(OX + 4, G + 1, OZ + 2, 'minecraft:iron_door[facing=east,half=lower,hinge=left]')
B.set(OX + 4, G + 2, OZ + 2, 'minecraft:iron_door[facing=east,half=upper,hinge=left]')
B.set(OX + 5, G + 1, OZ + 3, 'minecraft:stone_button[face=floor,facing=east]') if False else None
B.set(OX + 1, G + 1, OZ + 1, 'minecraft:lectern[facing=east]')
B.set(OX + 1, G + 1, OZ + 3, 'create:black_seat')
B.set(OX + 2, G + 4, OZ + 2, 'minecraft:light[level=13]')
sign(B, OX + 5, G + 4, OZ + 2, 'east', ['', 'PORT YARD', 'office', ''], color='black', glow=False)
sign(B, XB + 1, G + 3, -22, 'east', ['FIREHEART', 'CONTAINER', 'PORT', ''], color='yellow') if False else None
B.set(XB, G + 3, -22, 'create:industrial_iron_block')
B.set(XB, G + 3, -17, 'create:industrial_iron_block')
B.box(XB, G + 4, -22, XB, G + 4, -17, 'minecraft:black_concrete')
sign(B, XB + 1, G + 4, -20, 'east', ['', 'CONTAINER', 'PORT', ''], color='yellow')
sign(B, XB + 1, G + 4, -19, 'east', ['', 'EST.', '2026', ''], color='yellow')
lines = B.commands()
print(len(lines), write('city_yard', lines))

```

## elevator.py
```python
from lib import Build, write, sign, diesel
B = Build()
FLOORS = [70, 74, 78, 82, 86, 90, 94]
NAMES = [('G', 'Lobby'), ('1', 'Floor 1'), ('2', 'Floor 2'), ('3', 'Floor 3'), ('4', 'Floor 4'), ('5', 'Floor 5'), ('R', 'Roof')]
B.pre.append('fill 35 70 22 38 101 26 air')
B.pre.append('fill 30 95 22 34 101 26 air')
for y in range(70, 100):
    for (x, z) in ((38, 22), (38, 26)):
        B.set(x, y, z, 'create:industrial_iron_block')
    for x in (35, 36, 37):
        B.set(x, y, 22, 'create:framed_glass')
        B.set(x, y, 26, 'create:framed_glass')
    for z in (23, 24, 25):
        B.set(38, y, z, 'create:framed_glass')
for y in FLOORS[1:]:
    for x in (35, 36, 37):
        B.set(x, y - 1, 22, 'create:industrial_iron_block')
        B.set(x, y - 1, 26, 'create:industrial_iron_block')
    for z in (23, 24, 25):
        B.set(38, y - 1, z, 'create:industrial_iron_block')
B.box(35, 69, 22, 38, 69, 26, 'create:industrial_iron_block')
B.box(35, 70, 23, 37, 70, 25, 'minecraft:air')
for i, y0 in enumerate(FLOORS):
    B.set(34, y0 + 1, 24, 'minecraft:air')
    B.set(34, y0 + 2, 24, 'minecraft:air')
    B.set(34, y0 + 3, 24, 'create:industrial_iron_block')
    B.set(34, y0 + 3, 23, 'create:redstone_contact[facing=east,powered=false]')
    B.set(33, y0 + 3, 23, 'minecraft:polished_blackstone_button[face=wall,facing=west,powered=false]')
    sign(B, 33, y0 + 3, 24, 'west', ['ELEVATOR', NAMES[i][1], 'press button', 'to call'], color='white')
    B.set(33, y0, 24, 'minecraft:polished_blackstone')
B.walls(30, 95, 22, 34, 99, 26, 'minecraft:bricks')
for (x, z) in ((30, 22), (30, 26), (34, 22), (34, 26)):
    B.box(x, 95, z, x, 99, z, 'create:industrial_iron_block')
B.box(31, 95, 23, 33, 99, 25, 'minecraft:air')
for z in (23, 24, 25):
    for y in (95, 96, 97, 98, 99):
        B.set(34, y, z, 'minecraft:air')
B.box(34, 96, 23, 34, 98, 25, 'create:industrial_iron_block')
B.set(34, 95, 23, 'create:industrial_iron_block')
B.set(34, 95, 25, 'create:industrial_iron_block')
B.set(34, 95, 24, 'minecraft:air')
B.set(34, 96, 24, 'minecraft:air')
B.set(34, 97, 23, 'create:redstone_contact[facing=east,powered=false]')
B.set(34, 99, 23, 'create:industrial_iron_block')
B.set(34, 99, 25, 'create:industrial_iron_block')
B.set(30, 95, 24, 'minecraft:air')
B.set(30, 96, 24, 'minecraft:air')
for (x, z) in ((32, 22), (32, 26)):
    B.set(x, 97, z, 'create:framed_glass')
    B.set(x, 98, z, 'create:framed_glass')
B.box(30, 100, 22, 38, 100, 26, 'createdeco:industrial_iron_sheet_metal[axis=y]')
B.set(36, 100, 24, 'minecraft:air')
for x in (30, 38):
    B.set(x, 101, 22, 'createdeco:yellow_industrial_iron_lamp[facing=up,lit=true,inverted=true]')
    B.set(x, 101, 26, 'createdeco:yellow_industrial_iron_lamp[facing=up,lit=true,inverted=true]')
sign(B, 29, 97, 24, 'west', ['', 'ELEVATOR', 'MACHINE ROOM', ''], color='yellow')
diesel(B, 32, 99, 24, 'east')
B.set(34, 99, 24, 'create:shaft[axis=x]')
B.set(35, 99, 24, 'create:shaft[axis=x]')
B.set(36, 99, 24, 'create:elevator_pulley[facing=north]')
B.set(37, 99, 24, 'create:industrial_iron_block')
B.set(31, 95, 23, 'minecraft:light[level=12]')
B.set(31, 98, 25, 'create:speedometer[facing=up,axis_along_first=false]') if False else None
B.box(35, 94, 23, 37, 94, 25, 'minecraft:smooth_quartz')
B.set(36, 94, 24, 'minecraft:black_concrete')
B.box(35, 98, 23, 37, 98, 25, 'minecraft:white_concrete')
B.set(36, 98, 24, 'minecraft:sea_lantern')
for z in (23, 25):
    B.set(35, 95, z, 'create:framed_glass_pane')
    B.set(35, 96, z, 'create:framed_glass_pane')
B.set(35, 97, 25, 'create:framed_glass_pane')
B.set(35, 97, 23, 'create:redstone_contact[facing=west,powered=false]')
B.set(35, 97, 24, 'create:framed_glass')
B.set(35, 95, 24, 'create:framed_glass_door[facing=east,half=lower,hinge=left,open=false,visible=true]')
B.set(35, 96, 24, 'create:framed_glass_door[facing=east,half=upper,hinge=left,open=false,visible=true]')
B.set(37, 95, 24, 'create:contraption_controls[facing=west,open=false,virtual=false]')
B.set(37, 95, 23, 'minecraft:polished_blackstone_slab[type=bottom]')
B.set(37, 95, 25, 'minecraft:polished_blackstone_slab[type=bottom]')
B.post.append('kill @e[type=create:super_glue,x=34,y=68,z=21,dx=5,dy=33,dz=6]')
B.post.append('summon create:super_glue 35.0 94.0 23.0')
B.post.append('execute positioned 35.0 94.0 23.0 run data merge entity @e[type=create:super_glue,limit=1,sort=nearest] {From:[0.0d,0.0d,0.0d],To:[3.0d,5.0d,3.0d]}')
lines = B.commands()
print(len(lines), write('city_elevator', lines))
nm = []
for i, y0 in enumerate(FLOORS):
    nm.append('data merge block 34 %d 23 {ShortName:"%s",LongName:"%s"}' % (y0 + 3, NAMES[i][0], NAMES[i][1]))
nm.append('setblock 36 100 24 create:framed_glass')
print(write('city_elevator_names', nm))

```

## vehicles.py
```python
from lib import Build, write, sign, lamp_post

def L(x, y, z):
    v = ((x & 0x3FFFFFF) << 38) | ((z & 0x3FFFFFF) << 12) | (y & 0xFFF)
    return v - (1 << 64) if v >= 1 << 63 else v

UP = 1

def network(name, backup, chans):
    rel = lambda p: L(p[0] - backup[0], p[1] - backup[1], p[2] - backup[2])
    parts = []
    for k, v in chans.items():
        arr = []
        for s, t, d in v:
            arr += [rel(s), rel(t), d]
        parts.append(f'{k}:[L;' + ','.join(f'{a}L' for a in arr) + ']')
    return '{WireNetwork:{Name:"%s",BackupOffset:0L,Network:{%s:{%s}}}}' % (name, name, ','.join(parts))

def bike():
    B = Build()
    O = (32, 71, -2)
    P = lambda x, y, z: (O[0] + x, O[1] + y, O[2] + z)
    def s(x, y, z, b): B.set(*P(x, y, z), b)
    B.pre.append(f'fill {O[0]-1} 71 {O[2]-1} {O[0]+6} 76 {O[2]+3} air')
    W = 'trackwork:simple_wheel'
    s(0, 0, 0, W + '[facing=south]'); s(0, 0, 2, W + '[facing=north]'); s(0, 0, 1, 'minecraft:polished_blackstone_slab[type=top]')
    s(1, 0, 1, 'valkyrienskies:test_thruster[facing=west,powered=false]')
    s(2, 0, 1, 'minecraft:iron_block')
    H, BK = (3, 1, 1), (3, 0, 1)
    FL, FR, TW, TS = (4, 0, 0), (4, 0, 2), (1, 1, 1), (1, 0, 1)
    net = network('bike', BK, {'keyUp': [(H, TW, UP)], 'keyDown': [(H, TS, UP)], 'keyLeft': [(H, FL, UP)], 'keyRight': [(H, FR, UP)]})
    s(*BK, 'drivebywire:backup_block[facing=north]' + net)
    s(4, 0, 0, W + '[facing=south]'); s(4, 0, 2, W + '[facing=north]'); s(4, 0, 1, 'minecraft:polished_blackstone_slab[type=top]')
    s(1, 1, 1, 'valkyrienskies:test_thruster[facing=east,powered=false]')
    s(0, 1, 1, 'createdeco:red_industrial_iron_lamp[facing=west,lit=true,inverted=true]')
    s(2, 1, 1, 'create:black_seat')
    s(*H, 'drivebywire:controller_hub')
    s(4, 1, 1, 'minecraft:red_wool')
    s(5, 1, 1, 'createdeco:yellow_industrial_iron_lamp[facing=east,lit=true,inverted=true]')
    s(4, 2, 1, 'minecraft:black_concrete')
    s(4, 2, 0, 'minecraft:lever[face=wall,facing=north,powered=false]')
    s(4, 2, 2, 'minecraft:lever[face=wall,facing=south,powered=false]')
    s(0, 1, 0, 'minecraft:dark_oak_fence'); s(0, 2, 0, 'minecraft:dark_oak_fence'); s(0, 3, 0, 'minecraft:red_wool')
    return B, P(5, 0, 2), P(0, 3, 0), P(*H)

def boat():
    B = Build()
    O = (8, 64, 70)
    P = lambda x, y, z: (O[0] + x, O[1] + y, O[2] + z)
    def s(x, y, z, b): B.set(*P(x, y, z), b)
    B.pre.append(f'fill {O[0]} {O[1]} {O[2]} {O[0]+4} {O[1]+5} {O[2]+10} air')
    rows = {1: (1, 3), 9: (1, 3), 10: (2, 2)}
    shape = {}
    for z in range(1, 11):
        a, b = rows.get(z, (0, 4))
        shape[z] = (a, b)
        for x in range(a, b + 1):
            s(x, 0, z, 'minecraft:birch_planks')
    for z in range(1, 11):
        a, b = shape[z]
        for x in range(a, b + 1):
            edge = x in (a, b) or z in (1, 10) or (z == 9)
            if edge:
                s(x, 1, z, 'minecraft:light_blue_wool' if x in (0, 4) and 3 <= z <= 7 else 'minecraft:white_wool')
                s(x, 2, z, 'minecraft:white_wool')
            else:
                s(x, 1, z, 'minecraft:birch_slab[type=bottom]')
    s(2, 2, 1, 'minecraft:birch_slab[type=bottom]')
    for x in (1, 2, 3):
        s(x, 2, 8, 'minecraft:white_wool')
        s(x, 2, 7, 'minecraft:white_wool')
        s(x, 3, 7, 'minecraft:light_blue_stained_glass_pane')
    s(2, 2, 9, 'trackwork:horn[facing=south]')
    s(1, 3, 4, 'minecraft:birch_fence'); s(3, 3, 4, 'minecraft:birch_fence')
    for x in (1, 2, 3):
        for z in (4, 5, 6, 7):
            s(x, 4, z, 'minecraft:birch_slab[type=bottom]')
    s(2, 3, 5, 'minecraft:light[level=12]')
    s(1, 3, 8, 'createdeco:red_industrial_iron_lamp[facing=up,lit=true,inverted=true]')
    s(3, 3, 8, 'createdeco:green_industrial_iron_lamp[facing=up,lit=true,inverted=true]')
    for x in range(0, 5):
        s(x, 1, 0, 'minecraft:birch_slab[type=top]')
    s(1, 0, 0, 'valkyrienskies:test_thruster[facing=south,powered=false]')
    s(3, 0, 0, 'valkyrienskies:test_thruster[facing=north,powered=false]')
    s(1, 0, 9, 'valkyrienskies:test_thruster[facing=east,powered=false]')
    s(3, 0, 9, 'valkyrienskies:test_thruster[facing=west,powered=false]')
    s(0, 1, 0, 'minecraft:birch_fence'); s(0, 2, 0, 'minecraft:birch_fence'); s(0, 3, 0, 'minecraft:birch_fence'); s(0, 4, 0, 'minecraft:birch_fence')
    s(0, 5, 0, 'minecraft:blue_wool')
    s(2, 1, 5, 'create:white_seat')
    s(1, 1, 3, 'create:light_blue_seat'); s(3, 1, 3, 'create:light_blue_seat')
    H, BK = (1, 1, 6), (3, 1, 6)
    net = network('boat', BK, {'keyUp': [(H, (1, 0, 0), UP)], 'keyDown': [(H, (3, 0, 0), UP)], 'keyLeft': [(H, (1, 0, 9), UP)], 'keyRight': [(H, (3, 0, 9), UP)], 'keyJump': [(H, (2, 2, 9), UP)]})
    s(*H, 'drivebywire:controller_hub')
    s(*BK, 'drivebywire:backup_block[facing=north]' + net)
    s(2, 2, 6, 'create:copper_valve_handle[facing=north]')
    sign(B, *P(2, 2, 0), 'north', ['', 'FIREHEART', 'SEA-01', ''], wood='birch', color='blue', glow=False)
    return B, P(4, 0, 10), P(0, 5, 0), P(*H)

def marina():
    B = Build()
    Y = 63
    for x in (4, 5):
        B.set(x, 71, 60, 'minecraft:air')
        B.set(x, 70, 60, 'minecraft:stone_bricks')
        for i in range(6):
            z, y = 61 + i, 69 - i
            B.set(x, y, z, 'minecraft:stone_brick_stairs[facing=north]')
            B.box(x, 55, z, x, y - 1, z, 'minecraft:stone_bricks')
    for z in range(61, 67):
        B.set(3, 69 - (z - 61) + 1, z, 'minecraft:stone_brick_wall')
        B.set(6, 69 - (z - 61) + 1, z, 'minecraft:stone_brick_wall')
    B.box(2, Y, 67, 26, Y, 68, 'minecraft:spruce_planks')
    for x in (4, 5):
        B.set(x, Y, 67, 'minecraft:stone_bricks')
    for fx in (6, 14, 22):
        B.box(fx, Y, 69, fx + 1, Y, 82, 'minecraft:spruce_planks')
        for z in range(70, 83, 4):
            B.box(fx, 55, z, fx, Y - 1, z, 'minecraft:stripped_spruce_log[axis=y]')
            B.set(fx + 1, Y + 1, z, 'minecraft:spruce_fence')
            B.set(fx + 1, Y + 2, z, 'minecraft:lantern[hanging=false]')
        B.set(fx, Y + 1, 82, 'minecraft:spruce_fence'); B.set(fx + 1, Y + 1, 82, 'minecraft:spruce_fence')
    for x in range(2, 27, 4):
        B.box(x, 55, 68, x, Y - 1, 68, 'minecraft:stripped_spruce_log[axis=y]')
        B.set(x, Y + 1, 67, 'minecraft:spruce_fence') if x not in (4,) else None
    for x in (2, 10, 18, 26):
        lamp_post(B, x, Y + 1, 68)
    B.box(18, Y, 61, 26, Y, 66, 'minecraft:spruce_planks')
    B.walls(20, Y + 1, 61, 25, Y + 4, 64, 'minecraft:spruce_planks')
    for x in (20, 25):
        for z in (61, 64):
            B.box(x, Y + 1, z, x, Y + 4, z, 'minecraft:stripped_spruce_log[axis=y]')
    for x in (21, 22, 23, 24):
        B.set(x, Y + 2, 64, 'minecraft:glass_pane')
        B.set(x, Y + 3, 64, 'minecraft:glass_pane')
    B.set(22, Y + 1, 64, 'minecraft:spruce_door[facing=south,half=lower,hinge=left]')
    B.set(22, Y + 2, 64, 'minecraft:spruce_door[facing=south,half=upper,hinge=left]')
    B.box(19, Y + 5, 60, 26, Y + 5, 65, 'minecraft:dark_oak_slab[type=bottom]')
    B.set(21, Y + 1, 62, 'minecraft:lectern[facing=south]')
    B.set(24, Y + 1, 62, 'minecraft:barrel[facing=up]')
    B.set(23, Y + 1, 62, 'create:blue_seat')
    B.set(22, Y + 3, 62, 'minecraft:light[level=13]')
    sign(B, 23, Y + 4, 65, 'south', ['', 'FIREHEART', 'MARINA', ''], wood='spruce', color='light_blue')
    B.set(26, Y + 1, 66, 'create:fluid_tank[bottom=true,top=false,shape=window]')
    B.set(26, Y + 2, 66, 'create:fluid_tank[bottom=false,top=true,shape=window]')
    B.set(25, Y + 1, 66, 'minecraft:yellow_concrete')
    sign(B, 25, Y + 1, 67, 'south', ['FUEL', 'DIESEL', '', ''], wood='spruce', color='black', glow=False)
    for (x, z) in ((10, 84), (18, 84), (4, 86), (26, 86)):
        B.set(x, 62, z, 'minecraft:red_wool' if x % 4 else 'minecraft:white_wool')
        B.set(x, 63, z, 'minecraft:lantern[hanging=false]')
    return B

bB, bFirst, bClick, bHub = bike()
oB, oFirst, oClick, oHub = boat()
mB = marina()
write('veh_bike', bB.commands())
write('veh_marina', mB.commands())
write('veh_boat', oB.commands())
open('/tmp/claude-0/city/veh_info.txt', 'w').write(f'bike first {bFirst} click {bClick} hub {bHub}\nboat first {oFirst} click {oClick} hub {oHub}\n')
print(open('/tmp/claude-0/city/veh_info.txt').read())

```

## verify.py
```python
import re, os
from lib import write
F = '/mnt/user-data/outputs/statue_datapack/statue/data/statue/functions/'
order = ['northpedestal', 'north1', 'north2', 'city_apartment', 'city_shops', 'city_park', 'city_works', 'city_yard', 'city_decor', 'city_elevator']
SKIP = ('air', 'water', 'light', 'lava', 'fire', 'bed', 'door', 'poppy', 'tulip', 'dandelion', 'cornflower', 'allium', 'daisy', 'bluet', 'azalea', 'bush', 'peony', 'lilac', 'leaves', 'sign')
W = {}
pat = re.compile(r'^(setblock|fill) (-?\d+) (-?\d+) (-?\d+)(?: (-?\d+) (-?\d+) (-?\d+))? ([a-z0-9_:]+)(.*)$')
for n in order:
    for line in open(F + n + '.mcfunction'):
        m = pat.match(line.strip())
        if not m:
            continue
        g = m.groups()
        bid = g[7] if ':' in g[7] else 'minecraft:' + g[7]
        rest = g[8]
        if g[0] == 'fill' and ('replace' in rest or 'hollow' in rest or 'outline' in rest):
            continue
        x0, y0, z0 = int(g[1]), int(g[2]), int(g[3])
        x1, y1, z1 = (int(g[4]), int(g[5]), int(g[6])) if g[4] else (x0, y0, z0)
        vol = (abs(x1 - x0) + 1) * (abs(y1 - y0) + 1) * (abs(z1 - z0) + 1)
        if vol > 20000:
            continue
        for x in range(min(x0, x1), max(x0, x1) + 1):
            for y in range(min(y0, y1), max(y0, y1) + 1):
                for z in range(min(z0, z1), max(z0, z1) + 1):
                    W[(x, y, z)] = bid
for x in range(35, 38):
    for y in range(69, 100):
        for z in range(23, 26):
            W.pop((x, y, z), None)
for k in [k for k in W if k[1] < 60 and not (-35 <= k[0] <= 30 and -95 <= k[2] <= -55)]:
    pass
lines = []
for (x, y, z), b in sorted(W.items()):
    if any(s in b.split(':')[1] for s in SKIP):
        continue
    lines.append(f'execute unless block {x} {y} {z} {b} run tellraw @a "CHK {x} {y} {z} {b}"')
print(len(lines))
for f in os.listdir(F):
    if f.startswith('verify'):
        os.remove(F + f)
print(write('verify', lines, chunk=40000))

```

## scan.py
```python
import zlib, struct, io, pickle, numpy as np, nbtlib
R = "/mnt/user-data/uploads/ModrinthApp/profiles/Create_ Remastered/saves/Create!/region/"
X0, X1, Z0, Z1 = -160, 160, -160, 160
W = X1 - X0; H = Z1 - Z0
top = np.full((H, W), -999, dtype=np.int16)
topid = np.empty((H, W), dtype=object)
ground = np.full((H, W), -999, dtype=np.int16)
SKIP = {'minecraft:air', 'minecraft:cave_air', 'minecraft:light', 'minecraft:void_air'}
NATURAL = ('leaves', 'log', 'grass', 'fern', 'flower', 'dandelion', 'poppy', 'snow', 'vine', 'bush', 'tulip', 'orchid', 'allium', 'bluet', 'daisy', 'cornflower', 'lily', 'mushroom', 'sugar_cane', 'kelp', 'seagrass', 'dirt', 'stone', 'sand', 'gravel', 'water', 'podzol', 'clay', 'granite', 'diorite', 'andesite', 'coal_ore', 'iron_ore', 'copper_ore', 'deepslate', 'tuff', 'moss', 'azalea', 'pumpkin', 'berry', 'lava', 'mud')
def decode(data, n, bits):
    out = []
    mask = (1 << bits) - 1
    per = 64 // bits
    for l in data:
        v = int(l) & ((1 << 64) - 1)
        for i in range(per):
            out.append((v >> (i * bits)) & mask)
            if len(out) == n: return out
    return out
for rx, rz in [(-1, -1), (-1, 0), (0, -1), (0, 0)]:
    f = open(f"{R}r.{rx}.{rz}.mca", 'rb').read()
    for i in range(1024):
        off = int.from_bytes(f[i*4:i*4+3], 'big'); sc = f[i*4+3]
        if off == 0: continue
        p = off * 4096
        ln = int.from_bytes(f[p:p+4], 'big'); comp = f[p+4]
        raw = f[p+5:p+4+ln]
        if comp != 2: continue
        nb = nbtlib.File.parse(io.BytesIO(zlib.decompress(raw)))
        cx = int(nb['xPos']); cz = int(nb['zPos'])
        bx, bz = cx*16, cz*16
        if bx+16 <= X0 or bx >= X1 or bz+16 <= Z0 or bz >= Z1: continue
        secs = sorted(nb['sections'], key=lambda s: -int(s['Y']))
        for s in secs:
            if 'block_states' not in s: continue
            bs = s['block_states']; pal = [str(e['Name']) for e in bs['palette']]
            if all(p in SKIP for p in pal): continue
            if 'data' in bs:
                bits = max(4, (len(pal)-1).bit_length())
                idx = np.array(decode(bs['data'], 4096, bits)).reshape(16, 16, 16)
            else:
                idx = np.zeros((16, 16, 16), int)
            sy = int(s['Y'])*16
            for ly in range(15, -1, -1):
                for lz in range(16):
                    for lx in range(16):
                        wx, wz = bx+lx, bz+lz
                        if not (X0 <= wx < X1 and Z0 <= wz < Z1): continue
                        name = pal[idx[ly, lz, lx]]
                        if name in SKIP: continue
                        gx, gz = wx-X0, wz-Z0
                        if top[gz, gx] == -999:
                            top[gz, gx] = sy+ly; topid[gz, gx] = name
                        if ground[gz, gx] == -999 and not any(k in name for k in ('leaves', 'log', 'grass', 'fern', 'flower', 'vine', 'bush')) :
                            ground[gz, gx] = sy+ly
pickle.dump((X0, Z0, top, topid, ground), open('/tmp/claude-0/city/map.pkl', 'wb'))
print('done')

```

## render.py
```python
import pickle, numpy as np
from PIL import Image, ImageDraw
X0, Z0, top, topid, ground = pickle.load(open('/tmp/claude-0/city/map.pkl', 'rb'))
H, W = top.shape
NAT = ('leaves', 'log', 'grass', 'fern', 'flower', 'dandelion', 'poppy', 'snow', 'vine', 'bush', 'tulip', 'orchid', 'allium', 'bluet', 'daisy', 'cornflower', 'lily', 'mushroom', 'sugar_cane', 'kelp', 'seagrass', 'dirt', 'stone', 'sand', 'gravel', 'water', 'podzol', 'clay', 'granite', 'diorite', 'andesite', '_ore', 'deepslate', 'tuff', 'moss', 'azalea', 'pumpkin', 'berry', 'lava', 'mud')
img = np.zeros((H, W, 3), np.uint8)
built = np.zeros((H, W), bool)
for z in range(H):
    for x in range(W):
        n = topid[z, x] or ''
        y = top[z, x]
        shade = int(np.clip((y - 50) * 3, -60, 60))
        if 'water' in n or 'kelp' in n or 'seagrass' in n: c = (40, 80, 200)
        elif 'leaves' in n or 'log' in n or 'azalea' in n: c = (20, 90, 30)
        elif any(k in n for k in NAT): c = (90, 160, 60)
        else:
            built[z, x] = True
            c = (200, 60, 60) if 'red' in n else (230, 200, 60) if 'yellow' in n else (150, 150, 160)
        img[z, x] = [max(0, min(255, v + shade)) for v in c]
S = 4
im = Image.fromarray(img).resize((W*S, H*S), Image.NEAREST)
d = ImageDraw.Draw(im)
for v in range(-160, 161, 20):
    d.line([((v-X0)*S, 0), ((v-X0)*S, H*S)], fill=(255, 255, 255) if v == 0 else (0, 0, 0), width=1)
    d.line([(0, (v-Z0)*S), (W*S, (v-Z0)*S)], fill=(255, 255, 255) if v == 0 else (0, 0, 0), width=1)
    d.text(((v-X0)*S+2, 2), str(v), fill=(255, 255, 255))
    d.text((2, (v-Z0)*S+2), str(v), fill=(255, 255, 255))
im.save('/tmp/claude-0/city/map.png')
np.save('/tmp/claude-0/city/built.npy', built)
print(built.sum())

```

## dump.py
```python
import zlib, io, sys, nbtlib, numpy as np
R = "/mnt/user-data/uploads/ModrinthApp/profiles/Create_ Remastered/saves/Create!/region/r.-1.0.mca"
X0, X1, Y0, Y1, Z0, Z1 = map(int, sys.argv[1:7])
f = open(R, 'rb').read()
W = {}
def decode(data, n, bits):
    out = []; mask = (1 << bits) - 1; per = 64 // bits
    for l in data:
        v = int(l) & ((1 << 64) - 1)
        for i in range(per):
            out.append((v >> (i * bits)) & mask)
            if len(out) == n: return out
    return out
BE = {}
for i in range(1024):
    off = int.from_bytes(f[i*4:i*4+3], 'big')
    if not off: continue
    p = off * 4096; ln = int.from_bytes(f[p:p+4], 'big')
    nb = nbtlib.File.parse(io.BytesIO(zlib.decompress(f[p+5:p+4+ln])))
    cx, cz = int(nb['xPos']) * 16, int(nb['zPos']) * 16
    if cx + 16 <= X0 or cx > X1 or cz + 16 <= Z0 or cz > Z1: continue
    for be in nb.get('block_entities', []):
        x, y, z = int(be['x']), int(be['y']), int(be['z'])
        if X0 <= x <= X1 and Y0 <= y <= Y1 and Z0 <= z <= Z1:
            BE[(x, y, z)] = str(be.get('id'))
    for s in nb['sections']:
        sy = int(s['Y']) * 16
        if sy + 16 <= Y0 or sy > Y1 or 'block_states' not in s: continue
        bs = s['block_states']
        pal = []
        for e in bs['palette']:
            n = str(e['Name']).replace('minecraft:', '')
            if 'Properties' in e:
                n += '[' + ','.join(f'{k}={v}' for k, v in e['Properties'].items()) + ']'
            pal.append(n)
        idx = decode(bs['data'], 4096, max(4, (len(pal) - 1).bit_length())) if 'data' in bs else [0] * 4096
        for k, v in enumerate(idx):
            y = sy + k // 256; z = cz + (k // 16) % 16; x = cx + k % 16
            if X0 <= x <= X1 and Y0 <= y <= Y1 and Z0 <= z <= Z1:
                W[(x, y, z)] = pal[v]
for y in range(Y0, Y1 + 1):
    print('== y', y)
    for z in range(Z0, Z1 + 1):
        row = []
        for x in range(X0, X1 + 1):
            b = W.get((x, y, z), '?')
            if b != 'air':
                row.append(f'{x},{z}:{b}')
        if row: print('  ' + ' | '.join(row))
print(BE)

```

## car.py (original car layout)
```python
import os

OX, OY, OZ = 10, 73, -29
B = {}


def put(lx, ly, lz, block):
    B[(lx, ly, lz)] = block


def box(x0, x1, y0, y1, z0, z1, block):
    for x in range(x0, x1 + 1):
        for y in range(y0, y1 + 1):
            for z in range(z0, z1 + 1):
                put(x, y, z, block)


BODY = 'minecraft:black_concrete'
PAINT = 'minecraft:red_concrete'
TRIM = 'minecraft:polished_blackstone'
SLAB_B = 'minecraft:polished_blackstone_slab[type=bottom]'
SLAB_T = 'minecraft:polished_blackstone_slab[type=top]'
RSLAB = 'minecraft:red_nether_brick_slab[type=bottom]'
FLOOR = 'create:industrial_iron_block'
WHEEL = 'trackwork:med_simple_wheel'
GLASS_Z = 'minecraft:black_stained_glass_pane[north=true,south=true]'
GLASS_X = 'minecraft:black_stained_glass_pane[east=true,west=true]'

box(1, 9, -1, -1, 0, 4, FLOOR)
for x in (2, 8):
    for z in (0, 4):
        B.pop((x, -1, z))
put(-1, -1, 0, 'minecraft:chain[axis=x]')
put(10, -1, 4, 'minecraft:chain[axis=x]')
put(10, -1, 0, 'minecraft:chain[axis=x]')
put(-1, -1, 4, 'minecraft:chain[axis=x]')

box(0, 0, 0, 0, 0, 4, TRIM)
put(-1, 0, 2, 'minecraft:crimson_wall_sign[facing=west]{front_text:{color:"red",has_glowing_text:1b,messages:[\'""\',\'"FIREHEART"\',\'"CR-01"\',\'""\']}}')
put(0, 1, 0, 'createdeco:red_industrial_iron_lamp[facing=west,lit=true,inverted=true]')
put(0, 1, 4, 'createdeco:red_industrial_iron_lamp[facing=west,lit=true,inverted=true]')
put(0, 1, 1, 'minecraft:lightning_rod[facing=west]')
put(0, 1, 3, 'minecraft:lightning_rod[facing=west]')
put(0, 1, 2, BODY)
put(0, 2, 0, 'minecraft:polished_blackstone_wall')
put(0, 2, 4, 'minecraft:polished_blackstone_wall')
put(0, 2, 2, 'trackwork:med_simple_wheel_part[axis=x]')
for z in range(5):
    put(0, 3, z, SLAB_B)
    put(-1, 3, z, RSLAB)

put(1, 0, 0, BODY)
put(1, 0, 1, BODY)
put(1, 0, 2, 'create:creative_fluid_tank{Size:1,Height:1,Uninitialized:1b,TankContent:{FluidName:"createdieselgenerators:diesel",Amount:8000}}')
put(1, 0, 3, BODY)
put(1, 0, 4, BODY)
put(1, 1, 0, PAINT)
put(1, 1, 1, 'railways:conductor_vent')
put(1, 1, 2, 'create:mechanical_pump[facing=up]')
put(1, 1, 3, 'railways:conductor_vent')
put(1, 1, 4, PAINT)
put(1, 2, 0, SLAB_B)
put(1, 2, 4, SLAB_B)
put(1, 2, 2, 'createdieselgenerators:diesel_engine[facing=east]{Tanks:[{TankContent:{FluidName:"createdieselgenerators:diesel",Amount:1000}}]}')
put(1, 3, 2, 'createdeco:industrial_iron_bars[east=true,west=true]')

put(2, 0, 0, WHEEL + '[facing=south]')
put(2, 0, 4, WHEEL + '[facing=north]')
put(2, 0, 1, BODY)
put(2, 0, 3, BODY)
put(2, 0, 2, 'create:gearbox[axis=z]')
put(2, 1, 0, PAINT)
put(2, 1, 4, PAINT)
put(2, 1, 1, SLAB_T)
put(2, 1, 3, SLAB_T)
put(2, 1, 2, 'create:cogwheel[axis=y]')
put(2, 2, 2, 'create:gearbox[axis=z]')
put(2, 2, 0, SLAB_B)
put(2, 2, 4, SLAB_B)
put(2, 3, 2, 'createdeco:industrial_iron_bars[east=true,west=true]')

for z in (0, 1, 3, 4):
    put(3, 0, z, BODY)
put(3, 0, 2, 'create:shaft[axis=x]')
put(3, 1, 0, PAINT)
put(3, 1, 4, PAINT)
put(3, 1, 1, 'create:black_seat')
put(3, 1, 3, 'create:red_seat')
put(3, 2, 0, BODY)
put(3, 2, 4, BODY)
for z in (1, 2, 3):
    put(3, 2, z, GLASS_Z)

put(4, 0, 0, 'create:speedometer[facing=up,axis_along_first=false]')
put(4, 0, 1, 'create:shaft[axis=z]')
put(4, 0, 2, 'vs_clockwork:command_seat[facing=east]')
put(4, 0, 3, 'create:shaft[axis=z]')
put(4, 0, 4, 'create:speedometer[facing=up,axis_along_first=false]')
put(4, 1, 4, PAINT)
put(4, 2, 4, GLASS_X)

put(5, 0, 0, 'minecraft:comparator[facing=west]')
put(5, 0, 1, 'trackwork:track_level_controller[axis=y]')
put(5, 0, 2, 'create:shaft[axis=x]')
put(5, 0, 3, BODY)
put(5, 0, 4, 'minecraft:comparator[facing=west]')
put(5, 1, 0, PAINT)
put(5, 1, 4, PAINT)
put(5, 1, 1, 'create:hand_crank[facing=up]')
put(5, 1, 2, 'create:copper_valve_handle[facing=west]')
put(5, 1, 3, 'minecraft:polished_blackstone_button[face=wall,facing=west]')
put(5, 2, 0, GLASS_X)
put(5, 2, 4, GLASS_X)

put(6, 0, 0, 'minecraft:repeater[facing=west,delay=1]')
put(6, 0, 1, BODY)
put(6, 0, 2, 'create:shaft[axis=x]')
put(6, 0, 3, BODY)
put(6, 0, 4, 'minecraft:repeater[facing=west,delay=1]')
put(6, 1, 0, PAINT)
put(6, 1, 4, PAINT)
put(6, 1, 1, TRIM)
put(6, 1, 2, TRIM)
put(6, 1, 3, 'trackwork:horn[facing=west]')
put(6, 2, 0, BODY)
put(6, 2, 4, BODY)
for z in (1, 2, 3):
    put(6, 2, z, GLASS_Z)

for x in range(3, 7):
    for z in range(5):
        put(x, 3, z, SLAB_B)
for z in range(5):
    put(4, 3, z, 'minecraft:red_nether_brick_slab[type=bottom]')

put(7, 0, 0, 'minecraft:repeater[facing=west,delay=1]')
put(7, 0, 1, BODY)
put(7, 0, 2, 'create:shaft[axis=x]')
put(7, 0, 3, BODY)
put(7, 0, 4, 'minecraft:repeater[facing=west,delay=1]')
for z in range(5):
    put(7, 1, z, PAINT)

put(8, 0, 0, WHEEL + '[facing=south]')
put(8, 0, 4, WHEEL + '[facing=north]')
put(8, 0, 1, 'create:shaft[axis=z]')
put(8, 0, 2, 'create:gearbox[axis=y]')
put(8, 0, 3, 'create:shaft[axis=z]')
for z in range(5):
    put(8, 1, z, PAINT)
put(8, 2, 1, SLAB_B)
put(8, 2, 3, SLAB_B)

for z in range(5):
    put(9, 0, z, BODY)
put(9, 1, 0, 'createdeco:yellow_industrial_iron_lamp[facing=east,lit=true,inverted=true]')
put(9, 1, 4, 'createdeco:yellow_industrial_iron_lamp[facing=east,lit=true,inverted=true]')
for z in (1, 2, 3):
    put(9, 1, z, PAINT)

for z in range(5):
    put(10, 0, z, SLAB_T)
for z in (1, 2, 3):
    put(10, 1, z, 'createdeco:industrial_iron_bars[north=true,south=true]')


def W(lx, ly, lz):
    return OX + lx, OY + ly, OZ + lz


lines = ['fill 8 72 -31 21 78 -25 minecraft:air']
first = []
later = []
for (lx, ly, lz), b in sorted(B.items(), key=lambda kv: (kv[0][1], kv[0][0], kv[0][2])):
    x, y, z = W(lx, ly, lz)
    cmd = f'setblock {x} {y} {z} {b}'
    if any(s in b for s in ('comparator', 'repeater', 'button', 'wall_sign', 'lamp', 'bars', 'pane', 'lightning_rod', 'hand_crank', 'valve_handle')):
        later.append(cmd)
    else:
        first.append(cmd)
lines += first + later
d = '/tmp/claude-0/dp3/statue/data/statue/functions'
os.makedirs(d, exist_ok=True)
open(f'{d}/car.mcfunction', 'w').write('\n'.join(lines) + '\n')
xs = [W(*k)[0] for k in B]; ys = [W(*k)[1] for k in B]; zs = [W(*k)[2] for k in B]
print(len(B), min(xs), max(xs), min(ys), max(ys), min(zs), max(zs))
print('corner1', W(-1, 3, 0), 'corner2', W(10, -1, 4), 'seat', W(4, 0, 2))

```

## dbwnet.py (car Drive By Wire network)
```python
def L(x,y,z):
    v=((x & 0x3FFFFFF) << 38) | ((z & 0x3FFFFFF) << 12) | (y & 0xFFF)
    if v >= 1<<63: v -= 1<<64
    return v
B=(6,1,1)
def rel(p): return L(p[0]-B[0],p[1]-B[1],p[2]-B[2])
H=(6,1,2); C=(5,0,3); G=(4,0,2); FL=(8,0,0); FR=(8,0,4); HORN=(6,1,3)
UP,EAST=1,5
ch={'keyUp':[(H,C,EAST)],'keyDown':[(H,C,EAST),(H,G,UP)],'keyLeft':[(H,FL,UP)],'keyRight':[(H,FR,UP)],'keyJump':[(H,HORN,UP)]}
parts=[]
for k,v in ch.items():
    arr=[]
    for s,t,d in v: arr+= [rel(s),rel(t),d]
    parts.append(f'{k}:[L;'+','.join(f'{a}L' for a in arr)+']')
net='{Name:"car",BackupOffset:0L,Network:{car:{'+','.join(parts)+'}}}'
S=lambda p:(-28669957+p[0],127+p[1],12290045+p[2])
hub=S(H); hubl=L(*hub)
cmds=[
f'setblock {" ".join(map(str,S((4,0,2))))} create:gearshift[axis=x]',
f'setblock {" ".join(map(str,S((5,0,2))))} create:clutch[axis=x]',
f'setblock {" ".join(map(str,S((4,1,2))))} create:red_seat',
f'setblock {" ".join(map(str,S((6,0,4))))} minecraft:black_concrete',
f'setblock {" ".join(map(str,S((7,0,4))))} minecraft:black_concrete',
f'setblock {" ".join(map(str,S((5,0,4))))} minecraft:redstone_block',
f'setblock {" ".join(map(str,S((6,0,3))))} minecraft:lever[face=floor,facing=north,powered=false]',
f'setblock {" ".join(map(str,S((5,0,3))))} minecraft:comparator[facing=south,mode=subtract]',
f'setblock {" ".join(map(str,S(H)))} drivebywire:controller_hub',
f'setblock {" ".join(map(str,S(B)))} drivebywire:backup_block[facing=north]{{WireNetwork:{net}}}',
f'give @s create:linked_controller{{Hub:{hubl}L}}',
'say CAR_DBW_DONE',
]
open('/mnt/user-data/outputs/statue_datapack/statue/data/statue/functions/cardbw.mcfunction','w').write('\n'.join(cmds)+'\n')
print('\n'.join(cmds))

```

## snd/gen.py (engine sound synth)
```python
import numpy as np, soundfile as sf
from scipy.signal import butter, sosfilt
sr = 44100
L = 1.0
f0 = 55.0
n = int(sr * L)
t = np.arange(n) / sr
rng = np.random.default_rng(7)

phase = 2 * np.pi * f0 * t
pulse = np.zeros(n)
for k in range(1, 40):
    pulse += np.sin(k * phase) / k ** 1.15 * (1.0 if k % 2 == 0 else 0.75)
half = 0.55 * np.sin(0.5 * phase) + 0.25 * np.sin(1.5 * phase + 0.6)
cyl = 1.0 + 0.18 * np.sin(0.25 * phase) + 0.1 * np.sin(0.5 * phase + 1.3)
tone = (pulse + half) * cyl

noise = rng.standard_normal(n * 3)
noise = sosfilt(butter(4, [150, 1800], 'bandpass', fs=sr, output='sos'), noise)[n:2 * n]
noise *= (0.6 + 0.4 * np.clip(np.sin(phase), 0, None))

sig = tone / np.abs(tone).max() + 0.35 * noise / np.abs(noise).max()
sig = np.tanh(1.8 * sig)
sig = sosfilt(butter(2, 2500, 'lowpass', fs=sr, output='sos'), np.tile(sig, 3))[n:2 * n]
sig = sosfilt(butter(2, 25, 'highpass', fs=sr, output='sos'), np.tile(sig, 3))[n:2 * n]
fade = int(0.08 * sr)
env = np.ones(n)
env[:fade] = np.sin(np.linspace(0, np.pi / 2, fade)) ** 2
env[-fade:] = env[:fade][::-1]
sig = sig * env
sig = 0.9 * sig / np.abs(sig).max()
sf.write('engine.wav', sig.astype(np.float32), sr)

```

## Car engine sound functions (datapack)

### car_engine_load.mcfunction
```mcfunction
scoreboard objectives add car_timer dummy
scoreboard objectives add car_rev dummy
scoreboard objectives add car_gear dummy
scoreboard objectives add car_newgear dummy
```

### car_engine_tick.mcfunction
```mcfunction
tag @a remove car_driver
tag @a[nbt={RootVehicle:{Entity:{id:"create:seat"}},SelectedItem:{id:"create:linked_controller"}}] add car_driver
execute as @a[tag=car_engine_on,tag=!car_driver] run function statue:car_engine_off
execute as @a[tag=car_driver] at @s if block -28669952 127 12290047 create:clutch run function statue:car_engine_sound
```

### car_engine_sound.mcfunction
```mcfunction
execute unless entity @s[tag=car_engine_on] run function statue:car_engine_start
execute if block -28669952 127 12290047 create:clutch[powered=false] if score @s car_rev matches ..29 run scoreboard players add @s car_rev 1
execute if block -28669952 127 12290047 create:clutch[powered=true] if score @s car_rev matches 1.. run scoreboard players remove @s car_rev 1
scoreboard players set @s car_newgear 0
execute if score @s car_rev matches 5..9 run scoreboard players set @s car_newgear 1
execute if score @s car_rev matches 10..14 run scoreboard players set @s car_newgear 2
execute if score @s car_rev matches 15..19 run scoreboard players set @s car_newgear 3
execute if score @s car_rev matches 20..24 run scoreboard players set @s car_newgear 4
execute if score @s car_rev matches 25..29 run scoreboard players set @s car_newgear 5
execute if score @s car_rev matches 30.. run scoreboard players set @s car_newgear 6
execute unless score @s car_newgear = @s car_gear run function statue:car_engine_shift
scoreboard players remove @s car_timer 1
execute if score @s car_timer matches ..0 run function statue:car_engine_play
```

### car_engine_start.mcfunction
```mcfunction
tag @s add car_engine_on
scoreboard players set @s car_rev 0
scoreboard players set @s car_gear -1
scoreboard players set @s car_timer 0
playsound minecraft:block.piston.extend block @s ~ ~ ~ 0.8 0.5
playsound minecraft:entity.iron_golem.repair block @s ~ ~ ~ 0.7 0.6
```

### car_engine_shift.mcfunction
```mcfunction
stopsound @s * fireheart:car_engine
scoreboard players operation @s car_gear = @s car_newgear
scoreboard players set @s car_timer 0
```

### car_engine_play.mcfunction
```mcfunction
execute if score @s car_gear matches 0 run playsound fireheart:car_engine block @s ~ ~ ~ 0.7 0.5
execute if score @s car_gear matches 0 run scoreboard players set @s car_timer 38
execute if score @s car_gear matches 1 run playsound fireheart:car_engine block @s ~ ~ ~ 0.8 0.7
execute if score @s car_gear matches 1 run scoreboard players set @s car_timer 27
execute if score @s car_gear matches 2 run playsound fireheart:car_engine block @s ~ ~ ~ 0.85 0.9
execute if score @s car_gear matches 2 run scoreboard players set @s car_timer 20
execute if score @s car_gear matches 3 run playsound fireheart:car_engine block @s ~ ~ ~ 0.9 1.1
execute if score @s car_gear matches 3 run scoreboard players set @s car_timer 16
execute if score @s car_gear matches 4 run playsound fireheart:car_engine block @s ~ ~ ~ 0.95 1.35
execute if score @s car_gear matches 4 run scoreboard players set @s car_timer 13
execute if score @s car_gear matches 5 run playsound fireheart:car_engine block @s ~ ~ ~ 1.0 1.65
execute if score @s car_gear matches 5 run scoreboard players set @s car_timer 10
execute if score @s car_gear matches 6 run playsound fireheart:car_engine block @s ~ ~ ~ 1.0 2.0
execute if score @s car_gear matches 6 run scoreboard players set @s car_timer 8
```

### car_engine_off.mcfunction
```mcfunction
stopsound @s * fireheart:car_engine
stopsound @s * createdieselgenerators:engine_normal
playsound minecraft:block.iron_trapdoor.close block @s ~ ~ ~ 0.6 0.6
tag @s remove car_engine_on
scoreboard players set @s car_rev 0
scoreboard players set @s car_gear -1
```

### cardbw.mcfunction
```mcfunction
setblock -28669953 127 12290047 create:gearshift[axis=x]
setblock -28669952 127 12290047 create:clutch[axis=x]
setblock -28669953 128 12290047 create:red_seat
setblock -28669951 127 12290049 minecraft:black_concrete
setblock -28669950 127 12290049 minecraft:black_concrete
setblock -28669952 127 12290049 minecraft:redstone_block
setblock -28669951 127 12290048 minecraft:lever[face=floor,facing=north,powered=false]
setblock -28669952 127 12290048 minecraft:comparator[facing=south,mode=subtract]
setblock -28669951 128 12290047 drivebywire:controller_hub
setblock -28669951 128 12290046 drivebywire:backup_block[facing=north]{WireNetwork:{Name:"car",BackupOffset:0L,Network:{car:{keyUp:[L;4096L,-274877894657L,5L],keyDown:[L;4096L,-274877894657L,5L,4096L,-549755805697L,1L],keyLeft:[L;4096L,824633720831L,1L],keyRight:[L;4096L,549755830271L,1L],keyJump:[L;4096L,8192L,1L]}}}}
give @s create:linked_controller{Hub:-7880736072727007104L}
say CAR_DBW_DONE
```

### carwheels.mcfunction
```mcfunction
fill -28669956 126 12290045 -28669948 126 12290049 minecraft:polished_blackstone_slab[type=top] replace create:industrial_iron_block
setblock -28669958 126 12290045 air
setblock -28669947 126 12290049 air
setblock -28669947 126 12290045 air
setblock -28669958 126 12290049 air
setblock -28669955 127 12290045 trackwork:simple_wheel[facing=south]
setblock -28669955 127 12290049 trackwork:simple_wheel[facing=north]
setblock -28669949 127 12290045 trackwork:simple_wheel[facing=south]
setblock -28669949 127 12290049 trackwork:simple_wheel[facing=north]
say CAR_WHEELS_DONE
```

### cardiff.mcfunction
```mcfunction
setblock -28669949 127 12290048 create:gearshift[axis=z]
setblock -28669950 127 12290048 minecraft:redstone_block
say CAR_DIFF_FIX_DONE
```

### doghouse.mcfunction
```mcfunction
fill -41 70 31 -36 70 32 minecraft:grass_block
fill -39 70 33 -37 70 33 minecraft:grass_block
fill -41 71 31 -36 72 33 minecraft:air replace #minecraft:replaceable
setblock -42 71 30 minecraft:spruce_fence[north=false,south=true,west=false,east=true]
setblock -41 71 30 minecraft:spruce_fence[north=false,south=false,west=true,east=true]
setblock -40 71 30 minecraft:spruce_fence[north=false,south=false,west=true,east=true]
setblock -39 71 30 minecraft:spruce_fence[north=false,south=false,west=true,east=true]
setblock -38 71 30 minecraft:spruce_fence[north=false,south=false,west=true,east=true]
setblock -37 71 30 minecraft:spruce_fence[north=false,south=false,west=true,east=true]
setblock -36 71 30 minecraft:spruce_fence[north=false,south=false,west=true,east=true]
setblock -35 71 30 minecraft:spruce_fence[north=false,south=true,west=true,east=false]
setblock -42 71 31 minecraft:spruce_fence[north=true,south=true,west=false,east=false]
setblock -35 71 31 minecraft:spruce_fence[north=true,south=true,west=false,east=false]
setblock -35 71 32 minecraft:spruce_fence[north=true,south=true,west=false,east=false]
setblock -42 71 33 minecraft:spruce_fence[north=true,south=false,west=false,east=true]
setblock -35 71 33 minecraft:spruce_fence[north=true,south=false,west=true,east=false]
setblock -41 71 33 minecraft:spruce_fence[north=false,south=false,west=true,east=true]
setblock -40 71 33 minecraft:spruce_fence[north=false,south=false,west=true,east=false]
setblock -36 71 33 minecraft:spruce_fence[north=false,south=false,west=false,east=true]
setblock -42 71 32 minecraft:spruce_fence_gate[facing=west,open=false,in_wall=false]
setblock -42 72 30 minecraft:lantern[hanging=false]
setblock -35 72 30 minecraft:lantern[hanging=false]
setblock -38 71 34 minecraft:air
setblock -38 72 34 minecraft:air
setblock -38 71 33 minecraft:brown_carpet
setblock -40 71 31 minecraft:red_carpet
setblock -39 71 31 minecraft:red_carpet
setblock -37 71 31 minecraft:bone_block[axis=x]
setblock -36 71 31 minecraft:hay_block[axis=y]
setblock -36 71 32 minecraft:water_cauldron[level=3]
setblock -40 73 33 minecraft:spruce_stairs[facing=east,half=bottom]
setblock -39 74 33 minecraft:spruce_stairs[facing=east,half=bottom]
setblock -38 74 33 minecraft:spruce_slab[type=double]
setblock -37 74 33 minecraft:spruce_stairs[facing=west,half=bottom]
setblock -36 73 33 minecraft:spruce_stairs[facing=west,half=bottom]
fill -38 75 34 -38 75 37 minecraft:spruce_slab[type=bottom]
setblock -40 72 37 minecraft:air
setblock -36 72 37 minecraft:air
setblock -40 71 37 minecraft:barrel[facing=up]
setblock -36 71 37 minecraft:water_cauldron[level=3]
setblock -38 71 38 minecraft:red_carpet
setblock -41 71 36 minecraft:dark_oak_fence
setblock -41 72 36 minecraft:lantern[hanging=false]
setblock -35 71 36 minecraft:dark_oak_fence
setblock -35 72 36 minecraft:lantern[hanging=false]
setblock -40 71 35 minecraft:moss_carpet
setblock -40 71 36 minecraft:moss_carpet
setblock -36 71 35 minecraft:moss_carpet
setblock -36 71 36 minecraft:moss_carpet
setblock -40 71 35 minecraft:flowering_azalea
setblock -36 71 35 minecraft:flowering_azalea
fill -39 70 34 -37 70 37 minecraft:stone_bricks replace minecraft:dirt
fill -39 70 35 -37 70 36 minecraft:stone_bricks replace minecraft:grass_block
say DOGHOUSE_DONE
```

### checkup.mcfunction
```mcfunction
tellraw @a ["CHECK WaterWheel speed = ",{"nbt":"Speed","block":"-3 72 2"}]
tellraw @a ["CHECK Press(waterwheel) speed = ",{"nbt":"Speed","block":"-1 72 2"}]
tellraw @a ["CHECK Press depot = ",{"nbt":"HeldItem","block":"-1 70 2"}]
tellraw @a ["CHECK Farm engine = ",{"nbt":"Speed","block":"-5 71 -2"}]
tellraw @a ["CHECK Farm gearshift = ",{"nbt":"Speed","block":"-7 71 -2"}]
tellraw @a ["CHECK Farm piston = ",{"nbt":"Speed","block":"-8 71 -2"}]
tellraw @a ["CHECK Farm piston offset = ",{"nbt":"Offset","block":"-8 71 -2"}]
tellraw @a ["CHECK Farm output chest = ",{"nbt":"Items","block":"-4 70 -3"}]
tellraw @a ["CHECK Belt engine = ",{"nbt":"Speed","block":"0 71 3"}]
tellraw @a ["CHECK Belt = ",{"nbt":"Speed","block":"0 71 6"}]
tellraw @a ["CHECK Belt supply chest = ",{"nbt":"Items","block":"0 73 6"}]
tellraw @a ["CHECK Belt output chest = ",{"nbt":"Items","block":"6 70 7"}]
tellraw @a ["CHECK Belt press engine = ",{"nbt":"Speed","block":"3 73 4"}]
tellraw @a ["CHECK Belt press = ",{"nbt":"Speed","block":"3 73 6"}]
tellraw @a ["CHECK Door engine = ",{"nbt":"Speed","block":"3 72 15"}]
tellraw @a ["CHECK Door piston = ",{"nbt":"Speed","block":"3 72 12"}]
tellraw @a ["CHECK Bakery engine = ",{"nbt":"Speed","block":"-28 71 1"}]
tellraw @a ["CHECK Bakery millstone = ",{"nbt":"Speed","block":"-28 73 4"}]
tellraw @a ["CHECK Bakery belt = ",{"nbt":"Speed","block":"-28 71 4"}]
tellraw @a ["CHECK Bakery wheat chest = ",{"nbt":"Items","block":"-28 75 4"}]
tellraw @a ["CHECK Bakery bread chest = ",{"nbt":"Items","block":"-39 70 5"}]
tellraw @a ["CHECK Gravel engine = ",{"nbt":"Speed","block":"-38 72 54"}]
tellraw @a ["CHECK Gravel millstone = ",{"nbt":"Speed","block":"-26 73 54"}]
tellraw @a ["CHECK Gravel out1 = ",{"nbt":"Items","block":"-36 71 54"}]
tellraw @a ["CHECK Gravel in1 = ",{"nbt":"Items","block":"-36 75 54"}]
tellraw @a ["CHECK Clock engine = ",{"nbt":"Speed","block":"-26 66 17"}]
tellraw @a ["CHECK Clock N = ",{"nbt":"Speed","block":"-26 88 16"}]
tellraw @a ["CHECK Clock E = ",{"nbt":"Speed","block":"-24 89 18"}]
tellraw @a ["CHECK Elevator engine = ",{"nbt":"Speed","block":"32 99 24"}]
tellraw @a ["CHECK Elevator pulley running = ",{"nbt":"Running","block":"36 99 24"}]
tellraw @a ["CHECK Repair cmd1 = ",{"nbt":"Command","block":"-12 44 -10"}]
execute unless entity @e[type=create:stationary_contraption,x=-12,y=66,z=-8,dx=10,dy=10,dz=12] run tellraw @a "CHECK farm contraption MISSING"
execute if entity @e[type=create:stationary_contraption,x=-12,y=66,z=-8,dx=10,dy=10,dz=12] run tellraw @a "CHECK farm contraption present"
say CHECKUP_DONE
```

### maint.mcfunction
```mcfunction
data merge block -5 71 -2 {Tanks:[{TankContent:{FluidName:"createdieselgenerators:diesel",Amount:1000}}]}
data merge block 0 73 6 {Items:[{Slot:0b,id:"minecraft:iron_ingot",Count:64b},{Slot:1b,id:"minecraft:iron_ingot",Count:64b},{Slot:2b,id:"minecraft:iron_ingot",Count:64b},{Slot:3b,id:"minecraft:iron_ingot",Count:64b},{Slot:4b,id:"minecraft:iron_ingot",Count:64b},{Slot:5b,id:"minecraft:iron_ingot",Count:64b},{Slot:6b,id:"minecraft:iron_ingot",Count:64b},{Slot:7b,id:"minecraft:iron_ingot",Count:64b},{Slot:8b,id:"minecraft:iron_ingot",Count:64b},{Slot:9b,id:"minecraft:iron_ingot",Count:64b},{Slot:10b,id:"minecraft:iron_ingot",Count:64b},{Slot:11b,id:"minecraft:iron_ingot",Count:64b},{Slot:12b,id:"minecraft:iron_ingot",Count:64b},{Slot:13b,id:"minecraft:iron_ingot",Count:64b},{Slot:14b,id:"minecraft:iron_ingot",Count:64b},{Slot:15b,id:"minecraft:iron_ingot",Count:64b},{Slot:16b,id:"minecraft:iron_ingot",Count:64b},{Slot:17b,id:"minecraft:iron_ingot",Count:64b},{Slot:18b,id:"minecraft:iron_ingot",Count:64b},{Slot:19b,id:"minecraft:iron_ingot",Count:64b},{Slot:20b,id:"minecraft:iron_ingot",Count:64b},{Slot:21b,id:"minecraft:iron_ingot",Count:64b},{Slot:22b,id:"minecraft:iron_ingot",Count:64b},{Slot:23b,id:"minecraft:iron_ingot",Count:64b},{Slot:24b,id:"minecraft:iron_ingot",Count:64b},{Slot:25b,id:"minecraft:iron_ingot",Count:64b},{Slot:26b,id:"minecraft:iron_ingot",Count:64b}]}
data merge block -28 75 4 {Items:[{Slot:0b,id:"minecraft:wheat",Count:64b},{Slot:1b,id:"minecraft:wheat",Count:64b},{Slot:2b,id:"minecraft:wheat",Count:64b},{Slot:3b,id:"minecraft:wheat",Count:64b},{Slot:4b,id:"minecraft:wheat",Count:64b},{Slot:5b,id:"minecraft:wheat",Count:64b},{Slot:6b,id:"minecraft:wheat",Count:64b},{Slot:7b,id:"minecraft:wheat",Count:64b},{Slot:8b,id:"minecraft:wheat",Count:64b},{Slot:9b,id:"minecraft:wheat",Count:64b},{Slot:10b,id:"minecraft:wheat",Count:64b},{Slot:11b,id:"minecraft:wheat",Count:64b},{Slot:12b,id:"minecraft:wheat",Count:64b},{Slot:13b,id:"minecraft:wheat",Count:64b},{Slot:14b,id:"minecraft:wheat",Count:64b},{Slot:15b,id:"minecraft:wheat",Count:64b},{Slot:16b,id:"minecraft:wheat",Count:64b},{Slot:17b,id:"minecraft:wheat",Count:64b},{Slot:18b,id:"minecraft:wheat",Count:64b},{Slot:19b,id:"minecraft:wheat",Count:64b},{Slot:20b,id:"minecraft:wheat",Count:64b},{Slot:21b,id:"minecraft:wheat",Count:64b},{Slot:22b,id:"minecraft:wheat",Count:64b},{Slot:23b,id:"minecraft:wheat",Count:64b},{Slot:24b,id:"minecraft:wheat",Count:64b},{Slot:25b,id:"minecraft:wheat",Count:64b},{Slot:26b,id:"minecraft:wheat",Count:64b}]}
data remove block -4 70 -3 Items[{id:"minecraft:wheat"}]
data remove block -4 70 -3 Items[{id:"minecraft:wheat_seeds"}]
execute if block 0 71 3 createdieselgenerators:diesel_engine run data merge block 0 71 3 {Tanks:[{TankContent:{FluidName:"createdieselgenerators:diesel",Amount:1000}}]}
execute if block 3 73 4 createdieselgenerators:diesel_engine run data merge block 3 73 4 {Tanks:[{TankContent:{FluidName:"createdieselgenerators:diesel",Amount:1000}}]}
execute if block 3 72 15 createdieselgenerators:diesel_engine run data merge block 3 72 15 {Tanks:[{TankContent:{FluidName:"createdieselgenerators:diesel",Amount:1000}}]}
execute if block -28 71 1 createdieselgenerators:diesel_engine run data merge block -28 71 1 {Tanks:[{TankContent:{FluidName:"createdieselgenerators:diesel",Amount:1000}}]}
execute if block -38 72 54 createdieselgenerators:diesel_engine run data merge block -38 72 54 {Tanks:[{TankContent:{FluidName:"createdieselgenerators:diesel",Amount:1000}}]}
execute if block -26 66 17 createdieselgenerators:diesel_engine run data merge block -26 66 17 {Tanks:[{TankContent:{FluidName:"createdieselgenerators:diesel",Amount:1000}}]}
execute if block 32 99 24 createdieselgenerators:diesel_engine run data merge block 32 99 24 {Tanks:[{TankContent:{FluidName:"createdieselgenerators:diesel",Amount:1000}}]}
say MAINT_DONE
```

### FireheartCarSounds/assets/fireheart/sounds.json
```json
{
  "car_engine": {
    "subtitle": "Car engine",
    "sounds": [
      {"name": "fireheart:car_engine", "attenuation_distance": 24}
    ]
  }
}
```

---

# PART 5 – EVERY BLOCK ID IN THE MODPACK (appendix, verbatim)

# Appendix: every block ID in your mods

Generated from the blockstate and language files inside each mod jar. Format: `id` (English name) [block-state properties]. Colour/wood/stone variants are listed in full.

## create  (643 blocks, jar: create-1.20.1-6.0.8)

`acacia_window` (Acacia Window); `acacia_window_pane` (Acacia Window Pane) [north, south, west, east]; `adjustable_chain_gearshift` (Adjustable Chain Gearshift) [axis, axis_along_first, part, powered]; `analog_lever` (Analog Lever) [face, facing]; `andesite_alloy_block` (Block of Andesite Alloy); `andesite_bars` (Andesite Bars) [east, north, south, west]; `andesite_belt_funnel` (Andesite Belt Funnel) [facing, powered, shape, waterlogged]; `andesite_casing` (Andesite Casing); `andesite_door` (Andesite Door) [facing, half, hinge, open, visible]; `andesite_encased_cogwheel` (Andesite Encased Cogwheel) [axis, bottom_shaft, top_shaft]; `andesite_encased_large_cogwheel` (Andesite Encased Large Cogwheel) [axis, bottom_shaft, top_shaft]; `andesite_encased_shaft` (Andesite Encased Shaft) [axis]; `andesite_funnel` (Andesite Funnel) [extracting, facing, powered, waterlogged]; `andesite_ladder` (Andesite Ladder) [facing, waterlogged]; `andesite_pillar` (Andesite Pillar) [axis]; `andesite_scaffolding` (Andesite Scaffolding) [bottom]; `andesite_table_cloth` (Andesite Table Cover); `andesite_tunnel` (Andesite Tunnel) [axis, shape]; `asurine` (Asurine); `asurine_pillar` (Asurine Pillar) [axis]; `bamboo_window` (Bamboo Window); `bamboo_window_pane` (Bamboo Window Pane) [north, south, west, east]; `basin` (Basin) [facing]; `belt` (Belt) [casing, facing, part, slope, waterlogged]; `birch_window` (Birch Window); `birch_window_pane` (Birch Window Pane) [north, south, west, east]; `black_nixie_tube` (Black Nixie Tube) [double_face, facing, waterlogged]; `black_postbox` (Black Postbox) [facing, open, waterlogged]; `black_sail` (Black Sail) [facing]; `black_seat` (Black Seat); `black_table_cloth` (Black Table Cloth); `black_toolbox` (Black Toolbox) [facing, waterlogged]; `black_valve_handle` (Black Valve Handle) [facing, waterlogged]; `blaze_burner` (Blaze Burner); `blue_nixie_tube` (Blue Nixie Tube) [double_face, facing, waterlogged]; `blue_postbox` (Blue Postbox) [facing, open, waterlogged]; `blue_sail` (Blue Sail) [facing]; `blue_seat` (Blue Seat); `blue_table_cloth` (Blue Table Cloth); `blue_toolbox` (Blue Toolbox) [facing, waterlogged]; `blue_valve_handle` (Blue Valve Handle) [facing, waterlogged]; `bound_cardboard_block` (Bound Block of Cardboard) [axis]; `brass_bars` (Brass Bars) [east, north, south, west]; `brass_belt_funnel` (Brass Belt Funnel) [facing, powered, shape, waterlogged]; `brass_block` (Block of Brass); `brass_casing` (Brass Casing); `brass_door` (Brass Door) [facing, half, hinge, open, visible]; `brass_encased_cogwheel` (Brass Encased Cogwheel) [axis, bottom_shaft, top_shaft]; `brass_encased_large_cogwheel` (Brass Encased Large Cogwheel) [axis, bottom_shaft, top_shaft]; `brass_encased_shaft` (Brass Encased Shaft) [axis]; `brass_funnel` (Brass Funnel) [extracting, facing, powered, waterlogged]; `brass_ladder` (Brass Ladder) [facing, waterlogged]; `brass_scaffolding` (Brass Scaffolding) [bottom]; `brass_table_cloth` (Brass Table Cover); `brass_tunnel` (Brass Tunnel) [axis, shape]; `brown_nixie_tube` (Brown Nixie Tube) [double_face, facing, waterlogged]; `brown_postbox` (Brown Postbox) [facing, open, waterlogged]; `brown_sail` (Brown Sail) [facing]; `brown_seat` (Brown Seat); `brown_table_cloth` (Brown Table Cloth); `brown_toolbox` (Brown Toolbox) [facing, waterlogged]; `brown_valve_handle` (Brown Valve Handle) [facing, waterlogged]; `calcite_pillar` (Calcite Pillar) [axis]; `cardboard_block` (Block of Cardboard) [axis]; `cart_assembler` (Cart Assembler) [backwards, powered, rail_type, shape, waterlogged]; `chain_conveyor` (Chain Conveyor); `cherry_window` (Cherry Window); `cherry_window_pane` (Cherry Window Pane) [north, south, west, east]; `chocolate` (Chocolate); `chute` (Chute) [facing, shape, waterlogged]; `clipboard` (Clipboard) [face, facing, waterlogged, written] — Keeps your notes and makes you look more professional.; `clockwork_bearing` (Clockwork Bearing) [facing]; `clutch` (Clutch) [axis, powered]; `cogwheel` (Cogwheel) [axis]; `content_observer` (Smart Observer) [facing, powered, target]; `contraption_controls` (Contraption Controls) [facing, open, virtual, waterlogged]; `controller_rail` (Controller Rail) [backwards, shape, waterlogged] — A uni-directional powered rail with variable speed, controlled by the signal strength supplied to it.; `controls` (Train Controls) [facing, open, virtual, waterlogged]; `copper_backtank` (Copper Backtank) [facing, waterlogged]; `copper_bars` (Copper Bars) [east, north, south, west]; `copper_casing` (Copper Casing); `copper_door` (Copper Door) [facing, half, hinge, open, visible]; `copper_ladder` (Copper Ladder) [facing, waterlogged]; `copper_scaffolding` (Copper Scaffolding) [bottom]; `copper_shingle_slab` (Copper Shingle Slab) [type]; `copper_shingle_stairs` (Copper Shingle Stairs) [facing, half, shape]; `copper_shingles` (Copper Shingles); `copper_table_cloth` (Copper Table Cover); `copper_tile_slab` (Copper Tile Slab) [type]; `copper_tile_stairs` (Copper Tile Stairs) [facing, half, shape]; `copper_tiles` (Copper Tiles); `copper_valve_handle` (Copper Valve Handle) [facing, waterlogged]; `copycat_bars` (Copycat Bars) [facing]; `copycat_base` (Copycat Base); `copycat_panel` (Copycat Panel) — Converts any full block into a decorative panel. Also accepts Bars and Trapdoors.; `copycat_step` (Copycat Step) — Converts any full block into a decorative step.; `creative_crate` (Creative Crate) [facing] — This Storage Container allows infinite replication of items.; `creative_fluid_tank` (Creative Fluid Tank) [bottom, shape, top]; `creative_motor` (Creative Motor) [facing]; `crimsite` (Crimsite); `crimsite_pillar` (Crimsite Pillar) [axis]; `crimson_window` (Crimson Window); `crimson_window_pane` (Crimson Window Pane) [north, south, west, east]; `crushing_wheel` (Crushing Wheel) [axis]; `crushing_wheel_controller` (Crushing Wheel Controller) [valid]; `cuckoo_clock` (Cuckoo Clock) [facing] — Fine craftsmanship for decorating a space and keeping track of time.; `cut_andesite` (Cut Andesite); `cut_andesite_brick_slab` (Cut Andesite Brick Slab) [type]; `cut_andesite_brick_stairs` (Cut Andesite Brick Stairs) [facing, half, shape]; `cut_andesite_brick_wall` (Cut Andesite Brick Wall) [up, east, north, south, west]; `cut_andesite_bricks` (Cut Andesite Bricks); `cut_andesite_slab` (Cut Andesite Slab) [type]; `cut_andesite_stairs` (Cut Andesite Stairs) [facing, half, shape]; `cut_andesite_wall` (Cut Andesite Wall) [up, east, north, south, west]; `cut_asurine` (Cut Asurine); `cut_asurine_brick_slab` (Cut Asurine Brick Slab) [type]; `cut_asurine_brick_stairs` (Cut Asurine Brick Stairs) [facing, half, shape]; `cut_asurine_brick_wall` (Cut Asurine Brick Wall) [up, east, north, south, west]; `cut_asurine_bricks` (Cut Asurine Bricks); `cut_asurine_slab` (Cut Asurine Slab) [type]; `cut_asurine_stairs` (Cut Asurine Stairs) [facing, half, shape]; `cut_asurine_wall` (Cut Asurine Wall) [up, east, north, south, west]; `cut_calcite` (Cut Calcite); `cut_calcite_brick_slab` (Cut Calcite Brick Slab) [type]; `cut_calcite_brick_stairs` (Cut Calcite Brick Stairs) [facing, half, shape]; `cut_calcite_brick_wall` (Cut Calcite Brick Wall) [up, east, north, south, west]; `cut_calcite_bricks` (Cut Calcite Bricks); `cut_calcite_slab` (Cut Calcite Slab) [type]; `cut_calcite_stairs` (Cut Calcite Stairs) [facing, half, shape]; `cut_calcite_wall` (Cut Calcite Wall) [up, east, north, south, west]; `cut_crimsite` (Cut Crimsite); `cut_crimsite_brick_slab` (Cut Crimsite Brick Slab) [type]; `cut_crimsite_brick_stairs` (Cut Crimsite Brick Stairs) [facing, half, shape]; `cut_crimsite_brick_wall` (Cut Crimsite Brick Wall) [up, east, north, south, west]; `cut_crimsite_bricks` (Cut Crimsite Bricks); `cut_crimsite_slab` (Cut Crimsite Slab) [type]; `cut_crimsite_stairs` (Cut Crimsite Stairs) [facing, half, shape]; `cut_crimsite_wall` (Cut Crimsite Wall) [up, east, north, south, west]; `cut_deepslate` (Cut Deepslate); `cut_deepslate_brick_slab` (Cut Deepslate Brick Slab) [type]; `cut_deepslate_brick_stairs` (Cut Deepslate Brick Stairs) [facing, half, shape]; `cut_deepslate_brick_wall` (Cut Deepslate Brick Wall) [up, east, north, south, west]; `cut_deepslate_bricks` (Cut Deepslate Bricks); `cut_deepslate_slab` (Cut Deepslate Slab) [type]; `cut_deepslate_stairs` (Cut Deepslate Stairs) [facing, half, shape]; `cut_deepslate_wall` (Cut Deepslate Wall) [up, east, north, south, west]; `cut_diorite` (Cut Diorite); `cut_diorite_brick_slab` (Cut Diorite Brick Slab) [type]; `cut_diorite_brick_stairs` (Cut Diorite Brick Stairs) [facing, half, shape]; `cut_diorite_brick_wall` (Cut Diorite Brick Wall) [up, east, north, south, west]; `cut_diorite_bricks` (Cut Diorite Bricks); `cut_diorite_slab` (Cut Diorite Slab) [type]; `cut_diorite_stairs` (Cut Diorite Stairs) [facing, half, shape]; `cut_diorite_wall` (Cut Diorite Wall) [up, east, north, south, west]; `cut_dripstone` (Cut Dripstone); `cut_dripstone_brick_slab` (Cut Dripstone Brick Slab) [type]; `cut_dripstone_brick_stairs` (Cut Dripstone Brick Stairs) [facing, half, shape]; `cut_dripstone_brick_wall` (Cut Dripstone Brick Wall) [up, east, north, south, west]; `cut_dripstone_bricks` (Cut Dripstone Bricks); `cut_dripstone_slab` (Cut Dripstone Slab) [type]; `cut_dripstone_stairs` (Cut Dripstone Stairs) [facing, half, shape]; `cut_dripstone_wall` (Cut Dripstone Wall) [up, east, north, south, west]; `cut_granite` (Cut Granite); `cut_granite_brick_slab` (Cut Granite Brick Slab) [type]; `cut_granite_brick_stairs` (Cut Granite Brick Stairs) [facing, half, shape]; `cut_granite_brick_wall` (Cut Granite Brick Wall) [up, east, north, south, west]; `cut_granite_bricks` (Cut Granite Bricks); `cut_granite_slab` (Cut Granite Slab) [type]; `cut_granite_stairs` (Cut Granite Stairs) [facing, half, shape]; `cut_granite_wall` (Cut Granite Wall) [up, east, north, south, west]; `cut_limestone` (Cut Limestone); `cut_limestone_brick_slab` (Cut Limestone Brick Slab) [type]; `cut_limestone_brick_stairs` (Cut Limestone Brick Stairs) [facing, half, shape]; `cut_limestone_brick_wall` (Cut Limestone Brick Wall) [up, east, north, south, west]; `cut_limestone_bricks` (Cut Limestone Bricks); `cut_limestone_slab` (Cut Limestone Slab) [type]; `cut_limestone_stairs` (Cut Limestone Stairs) [facing, half, shape]; `cut_limestone_wall` (Cut Limestone Wall) [up, east, north, south, west]; `cut_ochrum` (Cut Ochrum); `cut_ochrum_brick_slab` (Cut Ochrum Brick Slab) [type]; `cut_ochrum_brick_stairs` (Cut Ochrum Brick Stairs) [facing, half, shape]; `cut_ochrum_brick_wall` (Cut Ochrum Brick Wall) [up, east, north, south, west]; `cut_ochrum_bricks` (Cut Ochrum Bricks); `cut_ochrum_slab` (Cut Ochrum Slab) [type]; `cut_ochrum_stairs` (Cut Ochrum Stairs) [facing, half, shape]; `cut_ochrum_wall` (Cut Ochrum Wall) [up, east, north, south, west]; `cut_scorchia` (Cut Scorchia); `cut_scorchia_brick_slab` (Cut Scorchia Brick Slab) [type]; `cut_scorchia_brick_stairs` (Cut Scorchia Brick Stairs) [facing, half, shape]; `cut_scorchia_brick_wall` (Cut Scorchia Brick Wall) [up, east, north, south, west]; `cut_scorchia_bricks` (Cut Scorchia Bricks); `cut_scorchia_slab` (Cut Scorchia Slab) [type]; `cut_scorchia_stairs` (Cut Scorchia Stairs) [facing, half, shape]; `cut_scorchia_wall` (Cut Scorchia Wall) [up, east, north, south, west]; `cut_scoria` (Cut Scoria); `cut_scoria_brick_slab` (Cut Scoria Brick Slab) [type]; `cut_scoria_brick_stairs` (Cut Scoria Brick Stairs) [facing, half, shape]; `cut_scoria_brick_wall` (Cut Scoria Brick Wall) [up, east, north, south, west]; `cut_scoria_bricks` (Cut Scoria Bricks); `cut_scoria_slab` (Cut Scoria Slab) [type]; `cut_scoria_stairs` (Cut Scoria Stairs) [facing, half, shape]; `cut_scoria_wall` (Cut Scoria Wall) [up, east, north, south, west]; `cut_tuff` (Cut Tuff); `cut_tuff_brick_slab` (Cut Tuff Brick Slab) [type]; `cut_tuff_brick_stairs` (Cut Tuff Brick Stairs) [facing, half, shape]; `cut_tuff_brick_wall` (Cut Tuff Brick Wall) [up, east, north, south, west]; `cut_tuff_bricks` (Cut Tuff Bricks); `cut_tuff_slab` (Cut Tuff Slab) [type]; `cut_tuff_stairs` (Cut Tuff Stairs) [facing, half, shape]; `cut_tuff_wall` (Cut Tuff Wall) [up, east, north, south, west]; `cut_veridium` (Cut Veridium); `cut_veridium_brick_slab` (Cut Veridium Brick Slab) [type]; `cut_veridium_brick_stairs` (Cut Veridium Brick Stairs) [facing, half, shape]; `cut_veridium_brick_wall` (Cut Veridium Brick Wall) [up, east, north, south, west]; `cut_veridium_bricks` (Cut Veridium Bricks); `cut_veridium_slab` (Cut Veridium Slab) [type]; `cut_veridium_stairs` (Cut Veridium Stairs) [facing, half, shape]; `cut_veridium_wall` (Cut Veridium Wall) [up, east, north, south, west]; `cyan_nixie_tube` (Cyan Nixie Tube) [double_face, facing, waterlogged]; `cyan_postbox` (Cyan Postbox) [facing, open, waterlogged]; `cyan_sail` (Cyan Sail) [facing]; `cyan_seat` (Cyan Seat); `cyan_table_cloth` (Cyan Table Cloth); `cyan_toolbox` (Cyan Toolbox) [facing, waterlogged]; `cyan_valve_handle` (Cyan Valve Handle) [facing, waterlogged]; `dark_oak_window` (Dark Oak Window); `dark_oak_window_pane` (Dark Oak Window Pane) [north, south, west, east]; `deepslate_pillar` (Deepslate Pillar) [axis]; `deepslate_zinc_ore` (Deepslate Zinc Ore); `deployer` (Deployer) [axis_along_first, facing]; `depot` (Depot); `desk_bell` (Desk Bell) [facing, powered, waterlogged] — Ring for Service! Emits a redstone signal when activated.; `diorite_pillar` (Diorite Pillar) [axis]; `display_board` (Display Board) [down, facing, up, waterlogged]; `display_link` (Display Link) [facing, powered]; `dripstone_pillar` (Dripstone Pillar) [axis]; `elevator_contact` (Elevator Contact) [calling, facing, powered, powering]; `elevator_pulley` (Elevator Pulley) [facing]; `encased_chain_drive` (Encased Chain Drive) [axis, axis_along_first, part]; `encased_fan` (Encased Fan) [facing]; `encased_fluid_pipe` (Encased Fluid Pipe) [down, up, north, south, west, east]; `experience_block` (Block of Experience); `exposed_copper_shingle_slab` (Exposed Copper Shingle Slab) [type]; `exposed_copper_shingle_stairs` (Exposed Copper Shingle Stairs) [facing, half, shape]; `exposed_copper_shingles` (Exposed Copper Shingles); `exposed_copper_tile_slab` (Exposed Copper Tile Slab) [type]; `exposed_copper_tile_stairs` (Exposed Copper Tile Stairs) [facing, half, shape]; `exposed_copper_tiles` (Exposed Copper Tiles); `factory_gauge` (Factory Gauge) [face, facing, powered, waterlogged]; `fake_track` (Track Marker for Maps); `fluid_pipe` (Fluid Pipe) [down, north, south, up, east, west]; `fluid_tank` (Fluid Tank) [bottom, shape, top]; `fluid_valve` (Fluid Valve) [axis_along_first, enabled, facing, waterlogged]; `flywheel` (Flywheel) [axis] — Embellish your Machines with this imposing Wheel of Brass.; `framed_glass` (Framed Glass); `framed_glass_door` (Framed Glass Door) [facing, half, hinge, open, visible]; `framed_glass_pane` (Framed Glass Pane) [north, south, west, east]; `framed_glass_trapdoor` (Framed Glass Trapdoor) [facing, half, open]; `gantry_carriage` (Gantry Carriage) [axis_along_first, facing]; `gantry_shaft` (Gantry Shaft) [facing, part, powered]; `gearbox` (Gearbox) [axis]; `gearshift` (Gearshift) [axis, powered]; `glass_fluid_pipe` (Glass Fluid Pipe) [alt, axis]; `granite_pillar` (Granite Pillar) [axis]; `gray_nixie_tube` (Gray Nixie Tube) [double_face, facing, waterlogged]; `gray_postbox` (Gray Postbox) [facing, open, waterlogged]; `gray_sail` (Gray Sail) [facing]; `gray_seat` (Gray Seat); `gray_table_cloth` (Gray Table Cloth); `gray_toolbox` (Gray Toolbox) [facing, waterlogged]; `gray_valve_handle` (Gray Valve Handle) [facing, waterlogged]; `green_nixie_tube` (Green Nixie Tube) [double_face, facing, waterlogged]; `green_postbox` (Green Postbox) [facing, open, waterlogged]; `green_sail` (Green Sail) [facing]; `green_seat` (Green Seat); `green_table_cloth` (Green Table Cloth); `green_toolbox` (Green Toolbox) [facing, waterlogged]; `green_valve_handle` (Green Valve Handle) [facing, waterlogged]; `hand_crank` (Hand Crank) [facing, waterlogged]; `haunted_bell` (Haunted Bell) [attachment, facing, powered] — A Cursed Bell haunted by lost souls of the Nether.; `honey` (Honey); `horizontal_framed_glass` (Horizontal Framed Glass); `horizontal_framed_glass_pane` (Horizontal Framed Glass Pane) [north, south, west, east]; `hose_pulley` (Hose Pulley) [facing]; `industrial_iron_block` (Block of Industrial Iron); `industrial_iron_window` (Industrial Iron Window); `industrial_iron_window_pane` (Industrial Iron Window Pane) [north, south, west, east]; `item_drain` (Item Drain); `item_hatch` (Item Hatch) [facing, open, waterlogged] — Quickly solves your inventory clutter and makes it someone else's problem.; `item_vault` (Item Vault) [axis, large]; `jungle_window` (Jungle Window); `jungle_window_pane` (Jungle Window Pane) [north, south, west, east]; `large_bogey` (Large Bogey) [axis, waterlogged]; `large_cogwheel` (Large Cogwheel) [axis]; `large_water_wheel` (Large Water Wheel) [axis, extension]; `layered_andesite` (Layered Andesite); `layered_asurine` (Layered Asurine); `layered_calcite` (Layered Calcite); `layered_crimsite` (Layered Crimsite); `layered_deepslate` (Layered Deepslate); `layered_diorite` (Layered Diorite); `layered_dripstone` (Layered Dripstone); `layered_granite` (Layered Granite); `layered_limestone` (Layered Limestone); `layered_ochrum` (Layered Ochrum); `layered_scorchia` (Layered Scorchia); `layered_scoria` (Layered Scoria); `layered_tuff` (Layered Tuff); `layered_veridium` (Layered Veridium); `lectern_controller` (Lectern Controller) [facing, has_book, powered]; `light_blue_nixie_tube` (Light Blue Nixie Tube) [double_face, facing, waterlogged]; `light_blue_postbox` (Light Blue Postbox) [facing, open, waterlogged]; `light_blue_sail` (Light Blue Sail) [facing]; `light_blue_seat` (Light Blue Seat); `light_blue_table_cloth` (Light Blue Table Cloth); `light_blue_toolbox` (Light Blue Toolbox) [facing, waterlogged]; `light_blue_valve_handle` (Light Blue Valve Handle) [facing, waterlogged]; `light_gray_nixie_tube` (Light Gray Nixie Tube) [double_face, facing, waterlogged]; `light_gray_postbox` (Light Gray Postbox) [facing, open, waterlogged]; `light_gray_sail` (Light Gray Sail) [facing]; `light_gray_seat` (Light Gray Seat); `light_gray_table_cloth` (Light Gray Table Cloth); `light_gray_toolbox` (Light Gray Toolbox) [facing, waterlogged]; `light_gray_valve_handle` (Light Gray Valve Handle) [facing, waterlogged]; `lime_nixie_tube` (Lime Nixie Tube) [double_face, facing, waterlogged]; `lime_postbox` (Lime Postbox) [facing, open, waterlogged]; `lime_sail` (Lime Sail) [facing]; `lime_seat` (Lime Seat); `lime_table_cloth` (Lime Table Cloth); `lime_toolbox` (Lime Toolbox) [facing, waterlogged]; `lime_valve_handle` (Lime Valve Handle) [facing, waterlogged]; `limestone` (Limestone); `limestone_pillar` (Limestone Pillar) [axis]; `linear_chassis` (Linear Chassis) [axis, sticky_bottom, sticky_top]; `lit_blaze_burner` (Lit Blaze Burner) [flame_type]; `magenta_nixie_tube` (Magenta Nixie Tube) [double_face, facing, waterlogged]; `magenta_postbox` (Magenta Postbox) [facing, open, waterlogged]; `magenta_sail` (Magenta Sail) [facing]; `magenta_seat` (Magenta Seat); `magenta_table_cloth` (Magenta Table Cloth); `magenta_toolbox` (Magenta Toolbox) [facing, waterlogged]; `magenta_valve_handle` (Magenta Valve Handle) [facing, waterlogged]; `mangrove_window` (Mangrove Window); `mangrove_window_pane` (Mangrove Window Pane) [north, south, west, east]; `mechanical_arm` (Mechanical Arm) [ceiling]; `mechanical_bearing` (Mechanical Bearing) [facing]; `mechanical_crafter` (Mechanical Crafter) [facing, pointing]; `mechanical_drill` (Mechanical Drill) [facing, waterlogged]; `mechanical_harvester` (Mechanical Harvester) [facing, waterlogged]; `mechanical_mixer` (Mechanical Mixer); `mechanical_piston` (Mechanical Piston) [axis_along_first, facing, state]; `mechanical_piston_head` (Mechanical Piston Head) [facing, type]; `mechanical_plough` (Mechanical Plough) [facing, waterlogged]; `mechanical_press` (Mechanical Press) [facing]; `mechanical_pump` (Mechanical Pump) [facing]; `mechanical_roller` (Mechanical Roller) [facing, waterlogged]; `mechanical_saw` (Mechanical Saw) [axis_along_first, facing, flipped]; `metal_bracket` (Metal Bracket) [axis_along_first, facing, type] — Decorate your Shafts, Cogwheels and Pipes with an industrial and sturdy bit of reinforcement.; `metal_girder` (Metal Girder) [x, z, top, bottom]; `metal_girder_encased_shaft` (Metal Girder Encased Shaft) [axis, top, bottom]; `millstone` (Millstone); `minecart_anchor` (Minecart Anchor); `mysterious_cuckoo_clock` (Cuckoo Clock) [facing]; `netherite_backtank` (Netherite Backtank) [facing, waterlogged]; `nixie_tube` (Nixie Tube) [double_face, facing, waterlogged]; `nozzle` (Nozzle) [facing] — Attach to the front of an Encased Fan to distribute its effect on Entities in all directions.; `oak_window` (Oak Window); `oak_window_pane` (Oak Window Pane) [north, south, west, east]; `ochrum` (Ochrum); `ochrum_pillar` (Ochrum Pillar) [axis]; `orange_postbox` (Orange Postbox) [facing, open, waterlogged]; `orange_sail` (Orange Sail) [facing]; `orange_seat` (Orange Seat); `orange_table_cloth` (Orange Table Cloth); `orange_toolbox` (Orange Toolbox) [facing, waterlogged]; `orange_valve_handle` (Orange Valve Handle) [facing, waterlogged]; `ornate_iron_window` (Ornate Iron Window); `ornate_iron_window_pane` (Ornate Iron Window Pane) [north, south, west, east]; `oxidized_copper_shingle_slab` (Oxidized Copper Shingle Slab) [type]; `oxidized_copper_shingle_stairs` (Oxidized Copper Shingle Stairs) [facing, half, shape]; `oxidized_copper_shingles` (Oxidized Copper Shingles); `oxidized_copper_tile_slab` (Oxidized Copper Tile Slab) [type]; `oxidized_copper_tile_stairs` (Oxidized Copper Tile Stairs) [facing, half, shape]; `oxidized_copper_tiles` (Oxidized Copper Tiles); `package_frogport` (Package Frogport); `packager` (Packager) [facing, linked, powered]; `peculiar_bell` (Peculiar Bell) [attachment, facing, powered] — A decorative Brass Bell. Placing it right above open Soul Fire may have side-effects...; `pink_nixie_tube` (Pink Nixie Tube) [double_face, facing, waterlogged]; `pink_postbox` (Pink Postbox) [facing, open, waterlogged]; `pink_sail` (Pink Sail) [facing]; `pink_seat` (Pink Seat); `pink_table_cloth` (Pink Table Cloth); `pink_toolbox` (Pink Toolbox) [facing, waterlogged]; `pink_valve_handle` (Pink Valve Handle) [facing, waterlogged]; `piston_extension_pole` (Piston Extension Pole) [facing]; `placard` (Placard) [face, facing, powered, waterlogged] — Frame your items in brass using this fancy wall panel. Safe for contraptions!; `polished_cut_andesite` (Polished Cut Andesite); `polished_cut_andesite_slab` (Polished Cut Andesite Slab) [type]; `polished_cut_andesite_stairs` (Polished Cut Andesite Stairs) [facing, half, shape]; `polished_cut_andesite_wall` (Polished Cut Andesite Wall) [up, east, north, south, west]; `polished_cut_asurine` (Polished Cut Asurine); `polished_cut_asurine_slab` (Polished Cut Asurine Slab) [type]; `polished_cut_asurine_stairs` (Polished Cut Asurine Stairs) [facing, half, shape]; `polished_cut_asurine_wall` (Polished Cut Asurine Wall) [up, east, north, south, west]; `polished_cut_calcite` (Polished Cut Calcite); `polished_cut_calcite_slab` (Polished Cut Calcite Slab) [type]; `polished_cut_calcite_stairs` (Polished Cut Calcite Stairs) [facing, half, shape]; `polished_cut_calcite_wall` (Polished Cut Calcite Wall) [up, east, north, south, west]; `polished_cut_crimsite` (Polished Cut Crimsite); `polished_cut_crimsite_slab` (Polished Cut Crimsite Slab) [type]; `polished_cut_crimsite_stairs` (Polished Cut Crimsite Stairs) [facing, half, shape]; `polished_cut_crimsite_wall` (Polished Cut Crimsite Wall) [up, east, north, south, west]; `polished_cut_deepslate` (Polished Cut Deepslate); `polished_cut_deepslate_slab` (Polished Cut Deepslate Slab) [type]; `polished_cut_deepslate_stairs` (Polished Cut Deepslate Stairs) [facing, half, shape]; `polished_cut_deepslate_wall` (Polished Cut Deepslate Wall) [up, east, north, south, west]; `polished_cut_diorite` (Polished Cut Diorite); `polished_cut_diorite_slab` (Polished Cut Diorite Slab) [type]; `polished_cut_diorite_stairs` (Polished Cut Diorite Stairs) [facing, half, shape]; `polished_cut_diorite_wall` (Polished Cut Diorite Wall) [up, east, north, south, west]; `polished_cut_dripstone` (Polished Cut Dripstone); `polished_cut_dripstone_slab` (Polished Cut Dripstone Slab) [type]; `polished_cut_dripstone_stairs` (Polished Cut Dripstone Stairs) [facing, half, shape]; `polished_cut_dripstone_wall` (Polished Cut Dripstone Wall) [up, east, north, south, west]; `polished_cut_granite` (Polished Cut Granite); `polished_cut_granite_slab` (Polished Cut Granite Slab) [type]; `polished_cut_granite_stairs` (Polished Cut Granite Stairs) [facing, half, shape]; `polished_cut_granite_wall` (Polished Cut Granite Wall) [up, east, north, south, west]; `polished_cut_limestone` (Polished Cut Limestone); `polished_cut_limestone_slab` (Polished Cut Limestone Slab) [type]; `polished_cut_limestone_stairs` (Polished Cut Limestone Stairs) [facing, half, shape]; `polished_cut_limestone_wall` (Polished Cut Limestone Wall) [up, east, north, south, west]; `polished_cut_ochrum` (Polished Cut Ochrum); `polished_cut_ochrum_slab` (Polished Cut Ochrum Slab) [type]; `polished_cut_ochrum_stairs` (Polished Cut Ochrum Stairs) [facing, half, shape]; `polished_cut_ochrum_wall` (Polished Cut Ochrum Wall) [up, east, north, south, west]; `polished_cut_scorchia` (Polished Cut Scorchia); `polished_cut_scorchia_slab` (Polished Cut Scorchia Slab) [type]; `polished_cut_scorchia_stairs` (Polished Cut Scorchia Stairs) [facing, half, shape]; `polished_cut_scorchia_wall` (Polished Cut Scorchia Wall) [up, east, north, south, west]; `polished_cut_scoria` (Polished Cut Scoria); `polished_cut_scoria_slab` (Polished Cut Scoria Slab) [type]; `polished_cut_scoria_stairs` (Polished Cut Scoria Stairs) [facing, half, shape]; `polished_cut_scoria_wall` (Polished Cut Scoria Wall) [up, east, north, south, west]; `polished_cut_tuff` (Polished Cut Tuff); `polished_cut_tuff_slab` (Polished Cut Tuff Slab) [type]; `polished_cut_tuff_stairs` (Polished Cut Tuff Stairs) [facing, half, shape]; `polished_cut_tuff_wall` (Polished Cut Tuff Wall) [up, east, north, south, west]; `polished_cut_veridium` (Polished Cut Veridium); `polished_cut_veridium_slab` (Polished Cut Veridium Slab) [type]; `polished_cut_veridium_stairs` (Polished Cut Veridium Stairs) [facing, half, shape]; `polished_cut_veridium_wall` (Polished Cut Veridium Wall) [up, east, north, south, west]; `portable_fluid_interface` (Portable Fluid Interface) [facing]; `portable_storage_interface` (Portable Storage Interface) [facing]; `powered_latch` (Powered Latch) [facing, powered, powered_side, powering]; `powered_shaft` (Powered Shaft) [axis]; `powered_toggle_latch` (Powered Toggle Latch) [facing, powered, powering]; `pulley_magnet` (Pulley Magnet); `pulse_extender` (Pulse Extender) [facing, inverted, powered, powering]; `pulse_repeater` (Pulse Repeater) [facing, inverted, powered, powering]; `pulse_timer` (Pulse Timer) [facing, inverted, powered, powering]; `purple_nixie_tube` (Purple Nixie Tube) [double_face, facing, waterlogged]; `purple_postbox` (Purple Postbox) [facing, open, waterlogged]; `purple_sail` (Purple Sail) [facing]; `purple_seat` (Purple Seat); `purple_table_cloth` (Purple Table Cloth); `purple_toolbox` (Purple Toolbox) [facing, waterlogged]; `purple_valve_handle` (Purple Valve Handle) [facing, waterlogged]; `radial_chassis` (Radial Chassis) [axis, sticky_south, sticky_west, sticky_north, sticky_east]; `railway_casing` (Train Casing); `raw_zinc_block` (Block of Raw Zinc); `red_nixie_tube` (Red Nixie Tube) [double_face, facing, waterlogged]; `red_postbox` (Red Postbox) [facing, open, waterlogged]; `red_sail` (Red Sail) [facing]; `red_seat` (Red Seat); `red_table_cloth` (Red Table Cloth); `red_toolbox` (Red Toolbox) [facing, waterlogged]; `red_valve_handle` (Red Valve Handle) [facing, waterlogged]; `redstone_contact` (Redstone Contact) [facing, powered]; `redstone_link` (Redstone Link) [facing, powered, receiver]; `redstone_requester` (Redstone Requester) [axis, powered]; `refined_radiance_casing` (Radiant Casing); `repackager` (Re-Packager) [facing, powered]; `rope` (Rope); `rope_pulley` (Rope Pulley) [axis]; `rose_quartz_block` (Block of Rose Quartz) [axis]; `rose_quartz_lamp` (Rose Quartz Lamp) [activate, powered, powering]; `rose_quartz_tiles` (Rose Quartz Tiles); `rotation_speed_controller` (Rotation Speed Controller) [axis]; `sail_frame` (Windmill Sail Frame) [facing]; `schematic_table` (Schematic Table) [facing] — Writes saved Schematics onto an Empty Schematic.; `schematicannon` (Schematicannon) — Shoots blocks to recreate a deployed Schematic in the World. Uses items from adjacent Inventories and Gunpowder as fuel.; `scorchia` (Scorchia); `scorchia_pillar` (Scorchia Pillar) [axis]; `scoria` (Scoria); `scoria_pillar` (Scoria Pillar) [axis]; `secondary_linear_chassis` (Secondary Linear Chassis) [axis, sticky_bottom, sticky_top]; `sequenced_gearshift` (Sequenced Gearshift) [axis, state, vertical]; `shadow_steel_casing` (Shadow Casing); `shaft` (Shaft) [axis]; `small_andesite_brick_slab` (Small Andesite Brick Slab) [type]; `small_andesite_brick_stairs` (Small Andesite Brick Stairs) [facing, half, shape]; `small_andesite_brick_wall` (Small Andesite Brick Wall) [up, east, north, south, west]; `small_andesite_bricks` (Small Andesite Bricks); `small_asurine_brick_slab` (Small Asurine Brick Slab) [type]; `small_asurine_brick_stairs` (Small Asurine Brick Stairs) [facing, half, shape]; `small_asurine_brick_wall` (Small Asurine Brick Wall) [up, east, north, south, west]; `small_asurine_bricks` (Small Asurine Bricks); `small_bogey` (Small Bogey) [axis, waterlogged]; `small_calcite_brick_slab` (Small Calcite Brick Slab) [type]; `small_calcite_brick_stairs` (Small Calcite Brick Stairs) [facing, half, shape]; `small_calcite_brick_wall` (Small Calcite Brick Wall) [up, east, north, south, west]; `small_calcite_bricks` (Small Calcite Bricks); `small_crimsite_brick_slab` (Small Crimsite Brick Slab) [type]; `small_crimsite_brick_stairs` (Small Crimsite Brick Stairs) [facing, half, shape]; `small_crimsite_brick_wall` (Small Crimsite Brick Wall) [up, east, north, south, west]; `small_crimsite_bricks` (Small Crimsite Bricks); `small_deepslate_brick_slab` (Small Deepslate Brick Slab) [type]; `small_deepslate_brick_stairs` (Small Deepslate Brick Stairs) [facing, half, shape]; `small_deepslate_brick_wall` (Small Deepslate Brick Wall) [up, east, north, south, west]; `small_deepslate_bricks` (Small Deepslate Bricks); `small_diorite_brick_slab` (Small Diorite Brick Slab) [type]; `small_diorite_brick_stairs` (Small Diorite Brick Stairs) [facing, half, shape]; `small_diorite_brick_wall` (Small Diorite Brick Wall) [up, east, north, south, west]; `small_diorite_bricks` (Small Diorite Bricks); `small_dripstone_brick_slab` (Small Dripstone Brick Slab) [type]; `small_dripstone_brick_stairs` (Small Dripstone Brick Stairs) [facing, half, shape]; `small_dripstone_brick_wall` (Small Dripstone Brick Wall) [up, east, north, south, west]; `small_dripstone_bricks` (Small Dripstone Bricks); `small_granite_brick_slab` (Small Granite Brick Slab) [type]; `small_granite_brick_stairs` (Small Granite Brick Stairs) [facing, half, shape]; `small_granite_brick_wall` (Small Granite Brick Wall) [up, east, north, south, west]; `small_granite_bricks` (Small Granite Bricks); `small_limestone_brick_slab` (Small Limestone Brick Slab) [type]; `small_limestone_brick_stairs` (Small Limestone Brick Stairs) [facing, half, shape]; `small_limestone_brick_wall` (Small Limestone Brick Wall) [up, east, north, south, west]; `small_limestone_bricks` (Small Limestone Bricks); `small_ochrum_brick_slab` (Small Ochrum Brick Slab) [type]; `small_ochrum_brick_stairs` (Small Ochrum Brick Stairs) [facing, half, shape]; `small_ochrum_brick_wall` (Small Ochrum Brick Wall) [up, east, north, south, west]; `small_ochrum_bricks` (Small Ochrum Bricks); `small_rose_quartz_tiles` (Small Rose Quartz Tiles); `small_scorchia_brick_slab` (Small Scorchia Brick Slab) [type]; `small_scorchia_brick_stairs` (Small Scorchia Brick Stairs) [facing, half, shape]; `small_scorchia_brick_wall` (Small Scorchia Brick Wall) [up, east, north, south, west]; `small_scorchia_bricks` (Small Scorchia Bricks); `small_scoria_brick_slab` (Small Scoria Brick Slab) [type]; `small_scoria_brick_stairs` (Small Scoria Brick Stairs) [facing, half, shape]; `small_scoria_brick_wall` (Small Scoria Brick Wall) [up, east, north, south, west]; `small_scoria_bricks` (Small Scoria Bricks); `small_tuff_brick_slab` (Small Tuff Brick Slab) [type]; `small_tuff_brick_stairs` (Small Tuff Brick Stairs) [facing, half, shape]; `small_tuff_brick_wall` (Small Tuff Brick Wall) [up, east, north, south, west]; `small_tuff_bricks` (Small Tuff Bricks); `small_veridium_brick_slab` (Small Veridium Brick Slab) [type]; `small_veridium_brick_stairs` (Small Veridium Brick Stairs) [facing, half, shape]; `small_veridium_brick_wall` (Small Veridium Brick Wall) [up, east, north, south, west]; `small_veridium_bricks` (Small Veridium Bricks); `smart_chute` (Smart Chute) [powered]; `smart_fluid_pipe` (Smart Fluid Pipe) [face, facing, waterlogged]; `speedometer` (Speedometer) [axis_along_first, facing]; `spout` (Spout); `spruce_window` (Spruce Window); `spruce_window_pane` (Spruce Window Pane) [north, south, west, east]; `steam_engine` (Steam Engine) [face, facing, waterlogged]; `steam_whistle` (Steam Whistle) [facing, powered, size, wall]; `steam_whistle_extension` (Steam Whistle Extension) [shape, size]; `sticker` (Sticker) [extended, facing, powered]; `sticky_mechanical_piston` (Sticky Mechanical Piston) [axis_along_first, facing, state]; `stock_link` (Stock Link) [face, facing, powered, waterlogged]; `stock_ticker` (Stock Ticker) [facing]; `stockpile_switch` (Threshold Switch) [facing, level, target]; `stressometer` (Stressometer) [axis_along_first, facing]; `tiled_glass` (Tiled Glass); `tiled_glass_pane` (Tiled Glass Pane) [north, south, west, east]; `track` (Train Track) [shape, turn, waterlogged]; `track_observer` (Train Observer) [powered]; `track_signal` (Train Signal) [powered, type]; `track_station` (Train Station); `train_door` (Train Door) [facing, half, hinge, open, visible]; `train_trapdoor` (Train Trapdoor) [facing, half, open]; `tuff_pillar` (Tuff Pillar) [axis]; `turntable` (Turntable) — Uses Rotational Force to create Motion Sickness.; `veridium` (Veridium); `veridium_pillar` (Veridium Pillar) [axis]; `vertical_framed_glass` (Vertical Framed Glass); `vertical_framed_glass_pane` (Vertical Framed Glass Pane) [north, south, west, east]; `warped_window` (Warped Window); `warped_window_pane` (Warped Window Pane) [north, south, west, east]; `water_wheel` (Water Wheel) [facing]; `water_wheel_structure` (Large Water Wheel); `waxed_copper_shingle_slab` (Waxed Copper Shingle Slab) [type]; `waxed_copper_shingle_stairs` (Waxed Copper Shingle Stairs) [facing, half, shape]; `waxed_copper_shingles` (Waxed Copper Shingles); `waxed_copper_tile_slab` (Waxed Copper Tile Slab) [type]; `waxed_copper_tile_stairs` (Waxed Copper Tile Stairs) [facing, half, shape]; `waxed_copper_tiles` (Waxed Copper Tiles); `waxed_exposed_copper_shingle_slab` (Waxed Exposed Copper Shingle Slab) [type]; `waxed_exposed_copper_shingle_stairs` (Waxed Exposed Copper Shingle Stairs) [facing, half, shape]; `waxed_exposed_copper_shingles` (Waxed Exposed Copper Shingles); `waxed_exposed_copper_tile_slab` (Waxed Exposed Copper Tile Slab) [type]; `waxed_exposed_copper_tile_stairs` (Waxed Exposed Copper Tile Stairs) [facing, half, shape]; `waxed_exposed_copper_tiles` (Waxed Exposed Copper Tiles); `waxed_oxidized_copper_shingle_slab` (Waxed Oxidized Copper Shingle Slab) [type]; `waxed_oxidized_copper_shingle_stairs` (Waxed Oxidized Copper Shingle Stairs) [facing, half, shape]; `waxed_oxidized_copper_shingles` (Waxed Oxidized Copper Shingles); `waxed_oxidized_copper_tile_slab` (Waxed Oxidized Copper Tile Slab) [type]; `waxed_oxidized_copper_tile_stairs` (Waxed Oxidized Copper Tile Stairs) [facing, half, shape]; `waxed_oxidized_copper_tiles` (Waxed Oxidized Copper Tiles); `waxed_weathered_copper_shingle_slab` (Waxed Weathered Copper Shingle Slab) [type]; `waxed_weathered_copper_shingle_stairs` (Waxed Weathered Copper Shingle Stairs) [facing, half, shape]; `waxed_weathered_copper_shingles` (Waxed Weathered Copper Shingles); `waxed_weathered_copper_tile_slab` (Waxed Weathered Copper Tile Slab) [type]; `waxed_weathered_copper_tile_stairs` (Waxed Weathered Copper Tile Stairs) [facing, half, shape]; `waxed_weathered_copper_tiles` (Waxed Weathered Copper Tiles); `weathered_copper_shingle_slab` (Weathered Copper Shingle Slab) [type]; `weathered_copper_shingle_stairs` (Weathered Copper Shingle Stairs) [facing, half, shape]; `weathered_copper_shingles` (Weathered Copper Shingles); `weathered_copper_tile_slab` (Weathered Copper Tile Slab) [type]; `weathered_copper_tile_stairs` (Weathered Copper Tile Stairs) [facing, half, shape]; `weathered_copper_tiles` (Weathered Copper Tiles); `weathered_iron_block` (Block of Weathered Iron); `weathered_iron_window` (Weathered Iron Window); `weathered_iron_window_pane` (Weathered Iron Window Pane) [north, south, west, east]; `weighted_ejector` (Weighted Ejector) [facing, waterlogged]; `white_nixie_tube` (White Nixie Tube) [double_face, facing, waterlogged]; `white_postbox` (White Postbox) [facing, open, waterlogged]; `white_sail` (Windmill Sail) [facing]; `white_seat` (White Seat); `white_table_cloth` (White Table Cloth); `white_toolbox` (White Toolbox) [facing, waterlogged]; `white_valve_handle` (White Valve Handle) [facing, waterlogged]; `windmill_bearing` (Windmill Bearing) [facing]; `wooden_bracket` (Wooden Bracket) [axis_along_first, facing, type] — Decorate your Shafts, Cogwheels and Pipes with a cozy and wooden bit of reinforcement.; `yellow_nixie_tube` (Yellow Nixie Tube) [double_face, facing, waterlogged]; `yellow_postbox` (Yellow Postbox) [facing, open, waterlogged]; `yellow_sail` (Yellow Sail) [facing]; `yellow_seat` (Yellow Seat); `yellow_table_cloth` (Yellow Table Cloth); `yellow_toolbox` (Yellow Toolbox) [facing, waterlogged]; `yellow_valve_handle` (Yellow Valve Handle) [facing, waterlogged]; `zinc_block` (Block of Zinc); `zinc_ore` (Zinc Ore)

## createdeco  (397 blocks, jar: createdeco-2.0.3-1.20.1-forge)

`andesite_bars` (Andesite Bars) [east, north, south, west]; `andesite_bars_overlay` (Andesite Bars Overlay) [east, north, south, west]; `andesite_catwalk` (Andesite Catwalk) [bottom]; `andesite_catwalk_railing` (Andesite Catwalk Railing) [north, south, east, west]; `andesite_catwalk_stairs` (Andesite Catwalk Stairs) [facing, railing_left, railing_right]; `andesite_door` (Andesite Door) [facing, half, hinge, open]; `andesite_facade` (Andesite Facade) [north, south, east, west, up, down]; `andesite_hull` (Andesite Train Hull) [facing]; `andesite_mesh_fence` (Andesite Mesh Fence) [east, north, south, west, up]; `andesite_sheet_metal` (Andesite Sheet Metal) [axis]; `andesite_support` (Andesite Support) [facing, waterlogged]; `andesite_support_wedge` (Andesite Support Wedge) [facing, orientation, waterlogged]; `andesite_trapdoor` (Andesite Trapdoor) [facing, half, open]; `andesite_window` (Andesite Window); `andesite_window_pane` (Andesite Window Pane) [north, south, west, east]; `black_placard` (Black Placard) [face, facing, powered, waterlogged]; `black_shipping_container` (Black Shipping Container) [axis, large]; `blue_andesite_lamp` (Blue Andesite Cage Lamp) [facing, inverted, lit, waterlogged]; `blue_brass_lamp` (Blue Brass Cage Lamp) [facing, inverted, lit, waterlogged]; `blue_brick_slab` (Blue Brick Slab) [type]; `blue_brick_stairs` (Blue Brick Stairs) [facing, half, shape]; `blue_brick_wall` (Blue Brick Wall) [up, east, north, south, west]; `blue_bricks` (Blue Bricks); `blue_copper_lamp` (Blue Copper Cage Lamp) [facing, inverted, lit, waterlogged]; `blue_industrial_iron_lamp` (Blue Industrial Iron Cage Lamp) [facing, inverted, lit, waterlogged]; `blue_iron_lamp` (Blue Iron Cage Lamp) [facing, inverted, lit, waterlogged]; `blue_placard` (Blue Placard) [face, facing, powered, waterlogged]; `blue_shipping_container` (Blue Shipping Container) [axis, large]; `blue_zinc_lamp` (Blue Zinc Cage Lamp) [facing, inverted, lit, waterlogged]; `brass_bars` (Brass Bars) [east, north, south, west]; `brass_bars_overlay` (Brass Bars Overlay) [east, north, south, west]; `brass_catwalk` (Brass Catwalk) [bottom]; `brass_catwalk_railing` (Brass Catwalk Railing) [north, south, east, west]; `brass_catwalk_stairs` (Brass Catwalk Stairs) [facing, railing_left, railing_right]; `brass_coinstack` (BrassCoin Stack Block) [layers, waterlogged]; `brass_door` (Brass Door) [facing, half, hinge, open]; `brass_facade` (Brass Facade) [north, south, east, west, up, down]; `brass_hull` (Brass Train Hull) [facing]; `brass_mesh_fence` (Brass Mesh Fence) [east, north, south, west, up]; `brass_sheet_metal` (Brass Sheet Metal) [axis]; `brass_support` (Brass Support) [facing, waterlogged]; `brass_support_wedge` (Brass Support Wedge) [facing, orientation, waterlogged]; `brass_trapdoor` (Brass Trapdoor) [facing, half, open]; `brass_window` (Brass Window); `brass_window_pane` (Brass Window Pane) [north, south, west, east]; `brown_placard` (Brown Placard) [face, facing, powered, waterlogged]; `brown_shipping_container` (Brown Shipping Container) [axis, large]; `copper_bars` (Copper Bars) [east, north, south, west]; `copper_bars_overlay` (Copper Bars Overlay) [east, north, south, west]; `copper_catwalk` (Copper Catwalk) [bottom]; `copper_catwalk_railing` (Copper Catwalk Railing) [north, south, east, west]; `copper_catwalk_stairs` (Copper Catwalk Stairs) [facing, railing_left, railing_right]; `copper_coinstack` (CopperCoin Stack Block) [layers, waterlogged]; `copper_door` (Copper Door) [facing, half, hinge, open]; `copper_facade` (Copper Facade) [north, south, east, west, up, down]; `copper_hull` (Copper Train Hull) [facing]; `copper_mesh_fence` (Copper Mesh Fence) [east, north, south, west, up]; `copper_sheet_metal` (Copper Sheet Metal) [axis]; `copper_support` (Copper Support) [facing, waterlogged]; `copper_support_wedge` (Copper Support Wedge) [facing, orientation, waterlogged]; `copper_trapdoor` (Copper Trapdoor) [facing, half, open]; `copper_window` (Copper Window); `copper_window_pane` (Copper Window Pane) [north, south, west, east]; `corner_blue_brick_slab` (Corner Blue Brick Slab) [type]; `corner_blue_brick_stairs` (Corner Blue Brick Stairs) [facing, half, shape]; `corner_blue_brick_wall` (Corner Blue Brick Wall) [up, east, north, south, west]; `corner_blue_bricks` (Corner Blue Bricks); `corner_dean_brick_slab` (Corner Dean Brick Slab) [type]; `corner_dean_brick_stairs` (Corner Dean Brick Stairs) [facing, half, shape]; `corner_dean_brick_wall` (Corner Dean Brick Wall) [up, east, north, south, west]; `corner_dean_bricks` (Corner Dean Bricks); `corner_dusk_brick_slab` (Corner Dusk Brick Slab) [type]; `corner_dusk_brick_stairs` (Corner Dusk Brick Stairs) [facing, half, shape]; `corner_dusk_brick_wall` (Corner Dusk Brick Wall) [up, east, north, south, west]; `corner_dusk_bricks` (Corner Dusk Bricks); `corner_pearl_brick_slab` (Corner Pearl Brick Slab) [type]; `corner_pearl_brick_stairs` (Corner Pearl Brick Stairs) [facing, half, shape]; `corner_pearl_brick_wall` (Corner Pearl Brick Wall) [up, east, north, south, west]; `corner_pearl_bricks` (Corner Pearl Bricks); `corner_red_brick_slab` (Corner Red Brick Slab) [type]; `corner_red_brick_stairs` (Corner Red Brick Stairs) [facing, half, shape]; `corner_red_brick_wall` (Corner Red Brick Wall) [up, east, north, south, west]; `corner_red_bricks` (Corner Red Bricks); `corner_scarlet_brick_slab` (Corner Scarlet Brick Slab) [type]; `corner_scarlet_brick_stairs` (Corner Scarlet Brick Stairs) [facing, half, shape]; `corner_scarlet_brick_wall` (Corner Scarlet Brick Wall) [up, east, north, south, west]; `corner_scarlet_bricks` (Corner Scarlet Bricks); `corner_umber_brick_slab` (Corner Umber Brick Slab) [type]; `corner_umber_brick_stairs` (Corner Umber Brick Stairs) [facing, half, shape]; `corner_umber_brick_wall` (Corner Umber Brick Wall) [up, east, north, south, west]; `corner_umber_bricks` (Corner Umber Bricks); `corner_verdant_brick_slab` (Corner Verdant Brick Slab) [type]; `corner_verdant_brick_stairs` (Corner Verdant Brick Stairs) [facing, half, shape]; `corner_verdant_brick_wall` (Corner Verdant Brick Wall) [up, east, north, south, west]; `corner_verdant_bricks` (Corner Verdant Bricks); `cracked_blue_brick_slab` (Cracked Blue Brick Slab) [type]; `cracked_blue_brick_stairs` (Cracked Blue Brick Stairs) [facing, half, shape]; `cracked_blue_brick_wall` (Cracked Blue Brick Wall) [up, east, north, south, west]; `cracked_blue_bricks` (Cracked Blue Bricks); `cracked_dean_brick_slab` (Cracked Dean Brick Slab) [type]; `cracked_dean_brick_stairs` (Cracked Dean Brick Stairs) [facing, half, shape]; `cracked_dean_brick_wall` (Cracked Dean Brick Wall) [up, east, north, south, west]; `cracked_dean_bricks` (Cracked Dean Bricks); `cracked_dusk_brick_slab` (Cracked Dusk Brick Slab) [type]; `cracked_dusk_brick_stairs` (Cracked Dusk Brick Stairs) [facing, half, shape]; `cracked_dusk_brick_wall` (Cracked Dusk Brick Wall) [up, east, north, south, west]; `cracked_dusk_bricks` (Cracked Dusk Bricks); `cracked_pearl_brick_slab` (Cracked Pearl Brick Slab) [type]; `cracked_pearl_brick_stairs` (Cracked Pearl Brick Stairs) [facing, half, shape]; `cracked_pearl_brick_wall` (Cracked Pearl Brick Wall) [up, east, north, south, west]; `cracked_pearl_bricks` (Cracked Pearl Bricks); `cracked_red_brick_slab` (Cracked Red Brick Slab) [type]; `cracked_red_brick_stairs` (Cracked Red Brick Stairs) [facing, half, shape]; `cracked_red_brick_wall` (Cracked Red Brick Wall) [up, east, north, south, west]; `cracked_red_bricks` (Cracked Red Bricks); `cracked_scarlet_brick_slab` (Cracked Scarlet Brick Slab) [type]; `cracked_scarlet_brick_stairs` (Cracked Scarlet Brick Stairs) [facing, half, shape]; `cracked_scarlet_brick_wall` (Cracked Scarlet Brick Wall) [up, east, north, south, west]; `cracked_scarlet_bricks` (Cracked Scarlet Bricks); `cracked_umber_brick_slab` (Cracked Umber Brick Slab) [type]; `cracked_umber_brick_stairs` (Cracked Umber Brick Stairs) [facing, half, shape]; `cracked_umber_brick_wall` (Cracked Umber Brick Wall) [up, east, north, south, west]; `cracked_umber_bricks` (Cracked Umber Bricks); `cracked_verdant_brick_slab` (Cracked Verdant Brick Slab) [type]; `cracked_verdant_brick_stairs` (Cracked Verdant Brick Stairs) [facing, half, shape]; `cracked_verdant_brick_wall` (Cracked Verdant Brick Wall) [up, east, north, south, west]; `cracked_verdant_bricks` (Cracked Verdant Bricks); `cyan_placard` (Cyan Placard) [face, facing, powered, waterlogged]; `cyan_shipping_container` (Cyan Shipping Container) [axis, large]; `dean_brick_slab` (Dean Brick Slab) [type]; `dean_brick_stairs` (Dean Brick Stairs) [facing, half, shape]; `dean_brick_wall` (Dean Brick Wall) [up, east, north, south, west]; `dean_bricks` (Dean Bricks); `decal_creeper` (Creeper Decal) [face, facing]; `decal_cross` (Cross Decal) [face, facing]; `decal_down` (Down Arrow Decal) [face, facing]; `decal_down_left` (Down Left Arrow Decal) [face, facing]; `decal_down_right` (Down Right Arrow Decal) [face, facing]; `decal_electrical` (Electrical Decal) [face, facing]; `decal_fire` (Fire Decal) [face, facing]; `decal_fire_diamond` (Fire Diamond Decal) [face, facing]; `decal_flow` (Flow Decal) [face, facing]; `decal_fluid` (Fluid Decal) [face, facing]; `decal_ice` (Ice Decal) [face, facing]; `decal_left` (Left Arrow Decal) [face, facing]; `decal_no_entry` (No Entry Decal) [face, facing]; `decal_radioactive` (Radioactive Decal) [face, facing]; `decal_right` (Right Arrow Decal) [face, facing]; `decal_skull` (Skull Decal) [face, facing]; `decal_top_left` (Up Left Arrow Decal) [face, facing]; `decal_top_right` (Up Right Arrow Decal) [face, facing]; `decal_up` (Up Arrow Decal) [face, facing]; `decal_warning` (Warning Decal) [face, facing]; `dusk_brick_slab` (Dusk Brick Slab) [type]; `dusk_brick_stairs` (Dusk Brick Stairs) [facing, half, shape]; `dusk_brick_wall` (Dusk Brick Wall) [up, east, north, south, west]; `dusk_bricks` (Dusk Bricks); `gold_coinstack` (GoldCoin Stack Block) [layers, waterlogged]; `gray_placard` (Gray Placard) [face, facing, powered, waterlogged]; `gray_shipping_container` (Gray Shipping Container) [axis, large]; `green_andesite_lamp` (Green Andesite Cage Lamp) [facing, inverted, lit, waterlogged]; `green_brass_lamp` (Green Brass Cage Lamp) [facing, inverted, lit, waterlogged]; `green_copper_lamp` (Green Copper Cage Lamp) [facing, inverted, lit, waterlogged]; `green_industrial_iron_lamp` (Green Industrial Iron Cage Lamp) [facing, inverted, lit, waterlogged]; `green_iron_lamp` (Green Iron Cage Lamp) [facing, inverted, lit, waterlogged]; `green_placard` (Green Placard) [face, facing, powered, waterlogged]; `green_shipping_container` (Green Shipping Container) [axis, large]; `green_zinc_lamp` (Green Zinc Cage Lamp) [facing, inverted, lit, waterlogged]; `industrial_iron_bars` (Industrial Iron Bars) [east, north, south, west]; `industrial_iron_bars_overlay` (Industrial Iron Bars Overlay) [east, north, south, west]; `industrial_iron_catwalk` (Industrial Iron Catwalk) [bottom]; `industrial_iron_catwalk_railing` (Industrial Iron Catwalk Railing) [north, south, east, west]; `industrial_iron_catwalk_stairs` (Industrial Iron Catwalk Stairs) [facing, railing_left, railing_right]; `industrial_iron_coinstack` (Industrial IronCoin Stack Block) [layers, waterlogged]; `industrial_iron_door` (Industrial Iron Door) [facing, half, hinge, open]; `industrial_iron_facade` (Industrial Iron Facade) [north, south, east, west, up, down]; `industrial_iron_hull` (Industrial Iron Train Hull) [facing]; `industrial_iron_ladder` (Industrial Iron Ladder) [facing, waterlogged]; `industrial_iron_mesh_fence` (Industrial Iron Mesh Fence) [east, north, south, west, up]; `industrial_iron_sheet_metal` (Industrial Iron Sheet Metal) [axis]; `industrial_iron_support` (Industrial Iron Support) [facing, waterlogged]; `industrial_iron_support_wedge` (Industrial Iron Support Wedge) [facing, orientation, waterlogged]; `industrial_iron_trapdoor` (Industrial Iron Trapdoor) [facing, half, open]; `industrial_iron_window` (Industrial Iron Window); `industrial_iron_window_pane` (Industrial Iron Window Pane) [north, south, west, east]; `iron_bars_overlay` (Iron Bars Overlay) [east, north, south, west]; `iron_catwalk` (Iron Catwalk) [bottom]; `iron_catwalk_railing` (Iron Catwalk Railing) [north, south, east, west]; `iron_catwalk_stairs` (Iron Catwalk Stairs) [facing, railing_left, railing_right]; `iron_coinstack` (IronCoin Stack Block) [layers, waterlogged]; `iron_facade` (Iron Facade) [north, south, east, west, up, down]; `iron_hull` (Iron Train Hull) [facing]; `iron_ladder` (Iron Ladder) [facing, waterlogged]; `iron_mesh_fence` (Iron Mesh Fence) [east, north, south, west, up]; `iron_sheet_metal` (Iron Sheet Metal) [axis]; `iron_support` (Iron Support) [facing, waterlogged]; `iron_support_wedge` (Iron Support Wedge) [facing, orientation, waterlogged]; `iron_window` (Iron Window); `iron_window_pane` (Iron Window Pane) [north, south, west, east]; `light_blue_placard` (Light Blue Placard) [face, facing, powered, waterlogged]; `light_blue_shipping_container` (Light blue Shipping Container) [axis, large]; `light_gray_placard` (Light Gray Placard) [face, facing, powered, waterlogged]; `light_gray_shipping_container` (Light gray Shipping Container) [axis, large]; `lime_placard` (Lime Placard) [face, facing, powered, waterlogged]; `lime_shipping_container` (Lime Shipping Container) [axis, large]; `locked_andesite_door` (Locked Andesite Door) [facing, half, hinge, open]; `locked_brass_door` (Locked Brass Door) [facing, half, hinge, open]; `locked_copper_door` (Locked Copper Door) [facing, half, hinge, open]; `locked_industrial_iron_door` (Locked Industrial Iron Door) [facing, half, hinge, open]; `locked_zinc_door` (Locked Zinc Door) [facing, half, hinge, open]; `long_blue_brick_slab` (Long Blue Brick Slab) [type]; `long_blue_brick_stairs` (Long Blue Brick Stairs) [facing, half, shape]; `long_blue_brick_wall` (Long Blue Brick Wall) [up, east, north, south, west]; `long_blue_bricks` (Long Blue Bricks); `long_dean_brick_slab` (Long Dean Brick Slab) [type]; `long_dean_brick_stairs` (Long Dean Brick Stairs) [facing, half, shape]; `long_dean_brick_wall` (Long Dean Brick Wall) [up, east, north, south, west]; `long_dean_bricks` (Long Dean Bricks); `long_dusk_brick_slab` (Long Dusk Brick Slab) [type]; `long_dusk_brick_stairs` (Long Dusk Brick Stairs) [facing, half, shape]; `long_dusk_brick_wall` (Long Dusk Brick Wall) [up, east, north, south, west]; `long_dusk_bricks` (Long Dusk Bricks); `long_pearl_brick_slab` (Long Pearl Brick Slab) [type]; `long_pearl_brick_stairs` (Long Pearl Brick Stairs) [facing, half, shape]; `long_pearl_brick_wall` (Long Pearl Brick Wall) [up, east, north, south, west]; `long_pearl_bricks` (Long Pearl Bricks); `long_red_brick_slab` (Long Red Brick Slab) [type]; `long_red_brick_stairs` (Long Red Brick Stairs) [facing, half, shape]; `long_red_brick_wall` (Long Red Brick Wall) [up, east, north, south, west]; `long_red_bricks` (Long Red Bricks); `long_scarlet_brick_slab` (Long Scarlet Brick Slab) [type]; `long_scarlet_brick_stairs` (Long Scarlet Brick Stairs) [facing, half, shape]; `long_scarlet_brick_wall` (Long Scarlet Brick Wall) [up, east, north, south, west]; `long_scarlet_bricks` (Long Scarlet Bricks); `long_umber_brick_slab` (Long Umber Brick Slab) [type]; `long_umber_brick_stairs` (Long Umber Brick Stairs) [facing, half, shape]; `long_umber_brick_wall` (Long Umber Brick Wall) [up, east, north, south, west]; `long_umber_bricks` (Long Umber Bricks); `long_verdant_brick_slab` (Long Verdant Brick Slab) [type]; `long_verdant_brick_stairs` (Long Verdant Brick Stairs) [facing, half, shape]; `long_verdant_brick_wall` (Long Verdant Brick Wall) [up, east, north, south, west]; `long_verdant_bricks` (Long Verdant Bricks); `magenta_placard` (Magenta Placard) [face, facing, powered, waterlogged]; `magenta_shipping_container` (Magenta Shipping Container) [axis, large]; `mossy_blue_brick_slab` (Mossy Blue Brick Slab) [type]; `mossy_blue_brick_stairs` (Mossy Blue Brick Stairs) [facing, half, shape]; `mossy_blue_brick_wall` (Mossy Blue Brick Wall) [up, east, north, south, west]; `mossy_blue_bricks` (Mossy Blue Bricks); `mossy_dean_brick_slab` (Mossy Dean Brick Slab) [type]; `mossy_dean_brick_stairs` (Mossy Dean Brick Stairs) [facing, half, shape]; `mossy_dean_brick_wall` (Mossy Dean Brick Wall) [up, east, north, south, west]; `mossy_dean_bricks` (Mossy Dean Bricks); `mossy_dusk_brick_slab` (Mossy Dusk Brick Slab) [type]; `mossy_dusk_brick_stairs` (Mossy Dusk Brick Stairs) [facing, half, shape]; `mossy_dusk_brick_wall` (Mossy Dusk Brick Wall) [up, east, north, south, west]; `mossy_dusk_bricks` (Mossy Dusk Bricks); `mossy_pearl_brick_slab` (Mossy Pearl Brick Slab) [type]; `mossy_pearl_brick_stairs` (Mossy Pearl Brick Stairs) [facing, half, shape]; `mossy_pearl_brick_wall` (Mossy Pearl Brick Wall) [up, east, north, south, west]; `mossy_pearl_bricks` (Mossy Pearl Bricks); `mossy_red_brick_slab` (Mossy Red Brick Slab) [type]; `mossy_red_brick_stairs` (Mossy Red Brick Stairs) [facing, half, shape]; `mossy_red_brick_wall` (Mossy Red Brick Wall) [up, east, north, south, west]; `mossy_red_bricks` (Mossy Red Bricks); `mossy_scarlet_brick_slab` (Mossy Scarlet Brick Slab) [type]; `mossy_scarlet_brick_stairs` (Mossy Scarlet Brick Stairs) [facing, half, shape]; `mossy_scarlet_brick_wall` (Mossy Scarlet Brick Wall) [up, east, north, south, west]; `mossy_scarlet_bricks` (Mossy Scarlet Bricks); `mossy_umber_brick_slab` (Mossy Umber Brick Slab) [type]; `mossy_umber_brick_stairs` (Mossy Umber Brick Stairs) [facing, half, shape]; `mossy_umber_brick_wall` (Mossy Umber Brick Wall) [up, east, north, south, west]; `mossy_umber_bricks` (Mossy Umber Bricks); `mossy_verdant_brick_slab` (Mossy Verdant Brick Slab) [type]; `mossy_verdant_brick_stairs` (Mossy Verdant Brick Stairs) [facing, half, shape]; `mossy_verdant_brick_wall` (Mossy Verdant Brick Wall) [up, east, north, south, west]; `mossy_verdant_bricks` (Mossy Verdant Bricks); `netherite_coinstack` (NetheriteCoin Stack Block) [layers, waterlogged]; `orange_placard` (Orange Placard) [face, facing, powered, waterlogged]; `orange_shipping_container` (Orange Shipping Container) [axis, large]; `pearl_brick_slab` (Pearl Brick Slab) [type]; `pearl_brick_stairs` (Pearl Brick Stairs) [facing, half, shape]; `pearl_brick_wall` (Pearl Brick Wall) [up, east, north, south, west]; `pearl_bricks` (Pearl Bricks); `pink_placard` (Pink Placard) [face, facing, powered, waterlogged]; `pink_shipping_container` (Pink Shipping Container) [axis, large]; `purple_placard` (Purple Placard) [face, facing, powered, waterlogged]; `purple_shipping_container` (Purple Shipping Container) [axis, large]; `red_andesite_lamp` (Red Andesite Cage Lamp) [facing, inverted, lit, waterlogged]; `red_brass_lamp` (Red Brass Cage Lamp) [facing, inverted, lit, waterlogged]; `red_copper_lamp` (Red Copper Cage Lamp) [facing, inverted, lit, waterlogged]; `red_industrial_iron_lamp` (Red Industrial Iron Cage Lamp) [facing, inverted, lit, waterlogged]; `red_iron_lamp` (Red Iron Cage Lamp) [facing, inverted, lit, waterlogged]; `red_placard` (Red Placard) [face, facing, powered, waterlogged]; `red_shipping_container` (Red Shipping Container) [axis, large]; `red_zinc_lamp` (Red Zinc Cage Lamp) [facing, inverted, lit, waterlogged]; `scarlet_brick_slab` (Scarlet Brick Slab) [type]; `scarlet_brick_stairs` (Scarlet Brick Stairs) [facing, half, shape]; `scarlet_brick_wall` (Scarlet Brick Wall) [up, east, north, south, west]; `scarlet_bricks` (Scarlet Bricks); `short_blue_brick_slab` (Short Blue Brick Slab) [type]; `short_blue_brick_stairs` (Short Blue Brick Stairs) [facing, half, shape]; `short_blue_brick_wall` (Short Blue Brick Wall) [up, east, north, south, west]; `short_blue_bricks` (Short Blue Bricks); `short_dean_brick_slab` (Short Dean Brick Slab) [type]; `short_dean_brick_stairs` (Short Dean Brick Stairs) [facing, half, shape]; `short_dean_brick_wall` (Short Dean Brick Wall) [up, east, north, south, west]; `short_dean_bricks` (Short Dean Bricks); `short_dusk_brick_slab` (Short Dusk Brick Slab) [type]; `short_dusk_brick_stairs` (Short Dusk Brick Stairs) [facing, half, shape]; `short_dusk_brick_wall` (Short Dusk Brick Wall) [up, east, north, south, west]; `short_dusk_bricks` (Short Dusk Bricks); `short_pearl_brick_slab` (Short Pearl Brick Slab) [type]; `short_pearl_brick_stairs` (Short Pearl Brick Stairs) [facing, half, shape]; `short_pearl_brick_wall` (Short Pearl Brick Wall) [up, east, north, south, west]; `short_pearl_bricks` (Short Pearl Bricks); `short_red_brick_slab` (Short Red Brick Slab) [type]; `short_red_brick_stairs` (Short Red Brick Stairs) [facing, half, shape]; `short_red_brick_wall` (Short Red Brick Wall) [up, east, north, south, west]; `short_red_bricks` (Short Red Bricks); `short_scarlet_brick_slab` (Short Scarlet Brick Slab) [type]; `short_scarlet_brick_stairs` (Short Scarlet Brick Stairs) [facing, half, shape]; `short_scarlet_brick_wall` (Short Scarlet Brick Wall) [up, east, north, south, west]; `short_scarlet_bricks` (Short Scarlet Bricks); `short_umber_brick_slab` (Short Umber Brick Slab) [type]; `short_umber_brick_stairs` (Short Umber Brick Stairs) [facing, half, shape]; `short_umber_brick_wall` (Short Umber Brick Wall) [up, east, north, south, west]; `short_umber_bricks` (Short Umber Bricks); `short_verdant_brick_slab` (Short Verdant Brick Slab) [type]; `short_verdant_brick_stairs` (Short Verdant Brick Stairs) [facing, half, shape]; `short_verdant_brick_wall` (Short Verdant Brick Wall) [up, east, north, south, west]; `short_verdant_bricks` (Short Verdant Bricks); `tiled_blue_brick_slab` (Tiled Blue Brick Slab) [type]; `tiled_blue_brick_stairs` (Tiled Blue Brick Stairs) [facing, half, shape]; `tiled_blue_brick_wall` (Tiled Blue Brick Wall) [up, east, north, south, west]; `tiled_blue_bricks` (Tiled Blue Bricks); `tiled_dean_brick_slab` (Tiled Dean Brick Slab) [type]; `tiled_dean_brick_stairs` (Tiled Dean Brick Stairs) [facing, half, shape]; `tiled_dean_brick_wall` (Tiled Dean Brick Wall) [up, east, north, south, west]; `tiled_dean_bricks` (Tiled Dean Bricks); `tiled_dusk_brick_slab` (Tiled Dusk Brick Slab) [type]; `tiled_dusk_brick_stairs` (Tiled Dusk Brick Stairs) [facing, half, shape]; `tiled_dusk_brick_wall` (Tiled Dusk Brick Wall) [up, east, north, south, west]; `tiled_dusk_bricks` (Tiled Dusk Bricks); `tiled_pearl_brick_slab` (Tiled Pearl Brick Slab) [type]; `tiled_pearl_brick_stairs` (Tiled Pearl Brick Stairs) [facing, half, shape]; `tiled_pearl_brick_wall` (Tiled Pearl Brick Wall) [up, east, north, south, west]; `tiled_pearl_bricks` (Tiled Pearl Bricks); `tiled_red_brick_slab` (Tiled Red Brick Slab) [type]; `tiled_red_brick_stairs` (Tiled Red Brick Stairs) [facing, half, shape]; `tiled_red_brick_wall` (Tiled Red Brick Wall) [up, east, north, south, west]; `tiled_red_bricks` (Tiled Red Bricks); `tiled_scarlet_brick_slab` (Tiled Scarlet Brick Slab) [type]; `tiled_scarlet_brick_stairs` (Tiled Scarlet Brick Stairs) [facing, half, shape]; `tiled_scarlet_brick_wall` (Tiled Scarlet Brick Wall) [up, east, north, south, west]; `tiled_scarlet_bricks` (Tiled Scarlet Bricks); `tiled_umber_brick_slab` (Tiled Umber Brick Slab) [type]; `tiled_umber_brick_stairs` (Tiled Umber Brick Stairs) [facing, half, shape]; `tiled_umber_brick_wall` (Tiled Umber Brick Wall) [up, east, north, south, west]; `tiled_umber_bricks` (Tiled Umber Bricks); `tiled_verdant_brick_slab` (Tiled Verdant Brick Slab) [type]; `tiled_verdant_brick_stairs` (Tiled Verdant Brick Stairs) [facing, half, shape]; `tiled_verdant_brick_wall` (Tiled Verdant Brick Wall) [up, east, north, south, west]; `tiled_verdant_bricks` (Tiled Verdant Bricks); `umber_brick_slab` (Umber Brick Slab) [type]; `umber_brick_stairs` (Umber Brick Stairs) [facing, half, shape]; `umber_brick_wall` (Umber Brick Wall) [up, east, north, south, west]; `umber_bricks` (Umber Bricks); `verdant_brick_slab` (Verdant Brick Slab) [type]; `verdant_brick_stairs` (Verdant Brick Stairs) [facing, half, shape]; `verdant_brick_wall` (Verdant Brick Wall) [up, east, north, south, west]; `verdant_bricks` (Verdant Bricks); `white_shipping_container` (White Shipping Container) [axis, large]; `yellow_andesite_lamp` (Yellow Andesite Cage Lamp) [facing, inverted, lit, waterlogged]; `yellow_brass_lamp` (Yellow Brass Cage Lamp) [facing, inverted, lit, waterlogged]; `yellow_copper_lamp` (Yellow Copper Cage Lamp) [facing, inverted, lit, waterlogged]; `yellow_industrial_iron_lamp` (Yellow Industrial Iron Cage Lamp) [facing, inverted, lit, waterlogged]; `yellow_iron_lamp` (Yellow Iron Cage Lamp) [facing, inverted, lit, waterlogged]; `yellow_placard` (Yellow Placard) [face, facing, powered, waterlogged]; `yellow_shipping_container` (Yellow Shipping Container) [axis, large]; `yellow_zinc_lamp` (Yellow Zinc Cage Lamp) [facing, inverted, lit, waterlogged]; `zinc_bars` (Zinc Bars) [east, north, south, west]; `zinc_bars_overlay` (Zinc Bars Overlay) [east, north, south, west]; `zinc_catwalk` (Zinc Catwalk) [bottom]; `zinc_catwalk_railing` (Zinc Catwalk Railing) [north, south, east, west]; `zinc_catwalk_stairs` (Zinc Catwalk Stairs) [facing, railing_left, railing_right]; `zinc_coinstack` (ZincCoin Stack Block) [layers, waterlogged]; `zinc_door` (Zinc Door) [facing, half, hinge, open]; `zinc_facade` (Zinc Facade) [north, south, east, west, up, down]; `zinc_hull` (Zinc Train Hull) [facing]; `zinc_ladder` (Zinc Ladder) [facing, waterlogged]; `zinc_mesh_fence` (Zinc Mesh Fence) [east, north, south, west, up]; `zinc_sheet_metal` (Zinc Sheet Metal) [axis]; `zinc_support` (Zinc Support) [facing, waterlogged]; `zinc_support_wedge` (Zinc Support Wedge) [facing, orientation, waterlogged]; `zinc_trapdoor` (Zinc Trapdoor) [facing, half, open]; `zinc_window` (Zinc Window); `zinc_window_pane` (Zinc Window Pane) [north, south, west, east]

## createcasing  (507 blocks, jar: Create_Encased-1.20.1-1.8-ht2)

`acacia_cogwheel` (Acacia Cogwheel) [axis]; `acacia_large_cogwheel` (Acacia Large Cogwheel) [axis]; `acacia_shaft` (Acacia Shaft) [axis]; `andesite_configurable_gearbox` (Andesite Configurable Gearbox) [down, up, north, south, west, east]; `andesite_encased_acacia_cogwheel` (Andesite Encased Acacia Cogwheel) [axis, bottom_shaft, top_shaft]; `andesite_encased_acacia_large_cogwheel` (Andesite Encased Acacia Large Cogwheel) [axis, bottom_shaft, top_shaft]; `andesite_encased_acacia_shaft` (Andesite Encased Acacia Shaft) [axis]; `andesite_encased_bamboo_cogwheel` (Andesite Encased Bamboo Cogwheel) [axis, bottom_shaft, top_shaft]; `andesite_encased_bamboo_large_cogwheel` (Andesite Encased Bamboo Large Cogwheel) [axis, bottom_shaft, top_shaft]; `andesite_encased_bamboo_shaft` (Andesite Encased Bamboo Shaft) [axis]; `andesite_encased_birch_cogwheel` (Andesite Encased Birch Cogwheel) [axis, bottom_shaft, top_shaft]; `andesite_encased_birch_large_cogwheel` (Andesite Encased Birch Large Cogwheel) [axis, bottom_shaft, top_shaft]; `andesite_encased_birch_shaft` (Andesite Encased Birch Shaft) [axis]; `andesite_encased_cherry_cogwheel` (Andesite Encased Cherry Cogwheel) [axis, bottom_shaft, top_shaft]; `andesite_encased_cherry_large_cogwheel` (Andesite Encased Cherry Large Cogwheel) [axis, bottom_shaft, top_shaft]; `andesite_encased_cherry_shaft` (Andesite Encased Cherry Shaft) [axis]; `andesite_encased_crimson_cogwheel` (Andesite Encased Crimson Cogwheel) [axis, bottom_shaft, top_shaft]; `andesite_encased_crimson_large_cogwheel` (Andesite Encased Crimson Large Cogwheel) [axis, bottom_shaft, top_shaft]; `andesite_encased_crimson_shaft` (Andesite Encased Crimson Shaft) [axis]; `andesite_encased_dark_oak_cogwheel` (Andesite Encased Dark Oak Cogwheel) [axis, bottom_shaft, top_shaft]; `andesite_encased_dark_oak_large_cogwheel` (Andesite Encased Dark Oak Large Cogwheel) [axis, bottom_shaft, top_shaft]; `andesite_encased_dark_oak_shaft` (Andesite Encased Dark Oak Shaft) [axis]; `andesite_encased_fluid_pipe` [down, up, north, south, west, east]; `andesite_encased_glass_shaft` (Andesite Encased Glass Shaft) [axis]; `andesite_encased_jungle_cogwheel` (Andesite Encased Jungle Cogwheel) [axis, bottom_shaft, top_shaft]; `andesite_encased_jungle_large_cogwheel` (Andesite Encased Jungle Large Cogwheel) [axis, bottom_shaft, top_shaft]; `andesite_encased_jungle_shaft` (Andesite Encased Jungle Shaft) [axis]; `andesite_encased_mangrove_cogwheel` (Andesite Encased Mangrove Cogwheel) [axis, bottom_shaft, top_shaft]; `andesite_encased_mangrove_large_cogwheel` (Andesite Encased Mangrove Large Cogwheel) [axis, bottom_shaft, top_shaft]; `andesite_encased_mangrove_shaft` (Andesite Encased Mangrove Shaft) [axis]; `andesite_encased_mldeg_shaft` (Andesite Encased Mldeg Shaft) [axis]; `andesite_encased_oak_cogwheel` (Andesite Encased Oak Cogwheel) [axis, bottom_shaft, top_shaft]; `andesite_encased_oak_large_cogwheel` (Andesite Encased Oak Large Cogwheel) [axis, bottom_shaft, top_shaft]; `andesite_encased_oak_shaft` (Andesite Encased Oak Shaft) [axis]; `andesite_encased_spruce_shaft` (Andesite Encased Spruce Shaft) [axis]; `andesite_encased_warped_cogwheel` (Andesite Encased Warped Cogwheel) [axis, bottom_shaft, top_shaft]; `andesite_encased_warped_large_cogwheel` (Andesite Encased Warped Large Cogwheel) [axis, bottom_shaft, top_shaft]; `andesite_encased_warped_shaft` (Andesite Encased Warped Shaft) [axis]; `bamboo_cogwheel` (Bamboo Cogwheel) [axis]; `bamboo_large_cogwheel` (Bamboo Large Cogwheel) [axis]; `bamboo_shaft` (Bamboo Shaft) [axis]; `birch_cogwheel` (Birch Cogwheel) [axis]; `birch_large_cogwheel` (Birch Large Cogwheel) [axis]; `birch_shaft` (Birch Shaft) [axis]; `brass_adjustable_chain_gearshift` (Brass Adjustable Chain Gearshift) [axis, axis_along_first, part, powered]; `brass_chain_conveyor` (Brass Chain Conveyor); `brass_clutch` (Brass Clutch) [axis, powered]; `brass_configurable_gearbox` (Brass Configurable Gearbox) [down, up, north, south, west, east]; `brass_deployer` (Brass Deployer) [axis_along_first, facing]; `brass_depot` (Brass Depot); `brass_encased_acacia_cogwheel` (Brass Encased Acacia Cogwheel) [axis, bottom_shaft, top_shaft]; `brass_encased_acacia_large_cogwheel` (Brass Encased Acacia Large Cogwheel) [axis, bottom_shaft, top_shaft]; `brass_encased_acacia_shaft` (Brass Encased Acacia Shaft) [axis]; `brass_encased_bamboo_cogwheel` (Brass Encased Bamboo Cogwheel) [axis, bottom_shaft, top_shaft]; `brass_encased_bamboo_large_cogwheel` (Brass Encased Bamboo Large Cogwheel) [axis, bottom_shaft, top_shaft]; `brass_encased_bamboo_shaft` (Brass Encased Bamboo Shaft) [axis]; `brass_encased_birch_cogwheel` (Brass Encased Birch Cogwheel) [axis, bottom_shaft, top_shaft]; `brass_encased_birch_large_cogwheel` (Brass Encased Birch Large Cogwheel) [axis, bottom_shaft, top_shaft]; `brass_encased_birch_shaft` (Brass Encased Birch Shaft) [axis]; `brass_encased_chain_drive` (Brass Encased Chain Drive) [axis, axis_along_first, part]; `brass_encased_cherry_cogwheel` (Brass Encased Cherry Cogwheel) [axis, bottom_shaft, top_shaft]; `brass_encased_cherry_large_cogwheel` (Brass Encased Cherry Large Cogwheel) [axis, bottom_shaft, top_shaft]; `brass_encased_cherry_shaft` (Brass Encased Cherry Shaft) [axis]; `brass_encased_crimson_cogwheel` (Brass Encased Crimson Cogwheel) [axis, bottom_shaft, top_shaft]; `brass_encased_crimson_large_cogwheel` (Brass Encased Crimson Large Cogwheel) [axis, bottom_shaft, top_shaft]; `brass_encased_crimson_shaft` (Brass Encased Crimson Shaft) [axis]; `brass_encased_dark_oak_cogwheel` (Brass Encased Dark Oak Cogwheel) [axis, bottom_shaft, top_shaft]; `brass_encased_dark_oak_large_cogwheel` (Brass Encased Dark Oak Large Cogwheel) [axis, bottom_shaft, top_shaft]; `brass_encased_dark_oak_shaft` (Brass Encased Dark Oak Shaft) [axis]; `brass_encased_fan` (Brass Encased Fan) [facing]; `brass_encased_fluid_pipe` [down, up, north, south, west, east]; `brass_encased_glass_shaft` (Brass Encased Glass Shaft) [axis]; `brass_encased_jungle_cogwheel` (Brass Encased Jungle Cogwheel) [axis, bottom_shaft, top_shaft]; `brass_encased_jungle_large_cogwheel` (Brass Encased Jungle Large Cogwheel) [axis, bottom_shaft, top_shaft]; `brass_encased_jungle_shaft` (Brass Encased Jungle Shaft) [axis]; `brass_encased_mangrove_cogwheel` (Brass Encased Mangrove Cogwheel) [axis, bottom_shaft, top_shaft]; `brass_encased_mangrove_large_cogwheel` (Brass Encased Mangrove Large Cogwheel) [axis, bottom_shaft, top_shaft]; `brass_encased_mangrove_shaft` (Brass Encased Mangrove Shaft) [axis]; `brass_encased_mldeg_shaft` (Brass Encased Mldeg Shaft) [axis]; `brass_encased_oak_cogwheel` (Brass Encased Oak Cogwheel) [axis, bottom_shaft, top_shaft]; `brass_encased_oak_large_cogwheel` (Brass Encased Oak Large Cogwheel) [axis, bottom_shaft, top_shaft]; `brass_encased_oak_shaft` (Brass Encased Oak Shaft) [axis]; `brass_encased_spruce_shaft` (Brass Encased Spruce Shaft) [axis]; `brass_encased_warped_cogwheel` (Brass Encased Warped Cogwheel) [axis, bottom_shaft, top_shaft]; `brass_encased_warped_large_cogwheel` (Brass Encased Warped Large Cogwheel) [axis, bottom_shaft, top_shaft]; `brass_encased_warped_shaft` (Brass Encased Warped Shaft) [axis]; `brass_gearbox` (Brass Gearbox) [axis]; `brass_gearshift` (Brass Gearshift) [axis, powered]; `brass_mechanical_drill` (Brass Mechanical Drill) [facing, waterlogged]; `brass_mechanical_harvester` (Brass Mechanical Harvester) [facing, waterlogged]; `brass_mechanical_plough` (Brass Mechanical Plough) [facing, waterlogged]; `brass_mechanical_roller` (Brass Mechanical Roller) [facing, waterlogged]; `brass_mechanical_saw` (Brass Mechanical Saw) [axis_along_first, facing, flipped]; `brass_mixer` (Brass Mixer); `brass_portable_storage_interface` (Brass Portable Storage Interface) [facing]; `brass_press` (Brass Press) [facing]; `brass_shaft` (Brass Shaft) [axis] — Stop when the stress is upper than the configured stress. Can be restarted with redstone signal. It doesn't restart automatically; `cherry_cogwheel` (Cherry Cogwheel) [axis]; `cherry_large_cogwheel` (Cherry Large Cogwheel) [axis]; `cherry_shaft` (Cherry Shaft) [axis]; `copper_adjustable_chain_gearshift` (Copper Adjustable Chain Gearshift) [axis, axis_along_first, part, powered]; `copper_chain_conveyor` (Copper Chain Conveyor); `copper_clutch` (Copper Clutch) [axis, powered]; `copper_configurable_gearbox` (Copper Configurable Gearbox) [down, up, north, south, west, east]; `copper_deployer` (Copper Deployer) [axis_along_first, facing]; `copper_depot` (Copper Depot); `copper_encased_acacia_cogwheel` (Copper Encased Acacia Cogwheel) [axis, bottom_shaft, top_shaft]; `copper_encased_acacia_large_cogwheel` (Copper Encased Acacia Large Cogwheel) [axis, bottom_shaft, top_shaft]; `copper_encased_acacia_shaft` (Copper Encased Acacia Shaft) [axis]; `copper_encased_bamboo_cogwheel` (Copper Encased Bamboo Cogwheel) [axis, bottom_shaft, top_shaft]; `copper_encased_bamboo_large_cogwheel` (Copper Encased Bamboo Large Cogwheel) [axis, bottom_shaft, top_shaft]; `copper_encased_bamboo_shaft` (Copper Encased Bamboo Shaft) [axis]; `copper_encased_birch_cogwheel` (Copper Encased Birch Cogwheel) [axis, bottom_shaft, top_shaft]; `copper_encased_birch_large_cogwheel` (Copper Encased Birch Large Cogwheel) [axis, bottom_shaft, top_shaft]; `copper_encased_birch_shaft` (Copper Encased Birch Shaft) [axis]; `copper_encased_chain_drive` (Copper Encased Chain Drive) [axis, axis_along_first, part]; `copper_encased_cherry_cogwheel` (Copper Encased Cherry Cogwheel) [axis, bottom_shaft, top_shaft]; `copper_encased_cherry_large_cogwheel` (Copper Encased Cherry Large Cogwheel) [axis, bottom_shaft, top_shaft]; `copper_encased_cherry_shaft` (Copper Encased Cherry Shaft) [axis]; `copper_encased_cogwheel` (Copper Encased Cogwheel) [axis, bottom_shaft, top_shaft]; `copper_encased_crimson_cogwheel` (Copper Encased Crimson Cogwheel) [axis, bottom_shaft, top_shaft]; `copper_encased_crimson_large_cogwheel` (Copper Encased Crimson Large Cogwheel) [axis, bottom_shaft, top_shaft]; `copper_encased_crimson_shaft` (Copper Encased Crimson Shaft) [axis]; `copper_encased_dark_oak_cogwheel` (Copper Encased Dark Oak Cogwheel) [axis, bottom_shaft, top_shaft]; `copper_encased_dark_oak_large_cogwheel` (Copper Encased Dark Oak Large Cogwheel) [axis, bottom_shaft, top_shaft]; `copper_encased_dark_oak_shaft` (Copper Encased Dark Oak Shaft) [axis]; `copper_encased_fan` (Copper Encased Fan) [facing]; `copper_encased_glass_shaft` (Copper Encased Glass Shaft) [axis]; `copper_encased_jungle_cogwheel` (Copper Encased Jungle Cogwheel) [axis, bottom_shaft, top_shaft]; `copper_encased_jungle_large_cogwheel` (Copper Encased Jungle Large Cogwheel) [axis, bottom_shaft, top_shaft]; `copper_encased_jungle_shaft` (Copper Encased Jungle Shaft) [axis]; `copper_encased_large_cogwheel` (Copper Encased Large Cogwheel) [axis, bottom_shaft, top_shaft]; `copper_encased_mangrove_cogwheel` (Copper Encased Mangrove Cogwheel) [axis, bottom_shaft, top_shaft]; `copper_encased_mangrove_large_cogwheel` (Copper Encased Mangrove Large Cogwheel) [axis, bottom_shaft, top_shaft]; `copper_encased_mangrove_shaft` (Copper Encased Mangrove Shaft) [axis]; `copper_encased_mldeg_shaft` (Copper Encased Mldeg Shaft) [axis]; `copper_encased_oak_cogwheel` (Copper Encased Oak Cogwheel) [axis, bottom_shaft, top_shaft]; `copper_encased_oak_large_cogwheel` (Copper Encased Oak Large Cogwheel) [axis, bottom_shaft, top_shaft]; `copper_encased_oak_shaft` (Copper Encased Oak Shaft) [axis]; `copper_encased_shaft` (Copper Encased Shaft) [axis]; `copper_encased_spruce_shaft` (Copper Encased Spruce Shaft) [axis]; `copper_encased_warped_cogwheel` (Copper Encased Warped Cogwheel) [axis, bottom_shaft, top_shaft]; `copper_encased_warped_large_cogwheel` (Copper Encased Warped Large Cogwheel) [axis, bottom_shaft, top_shaft]; `copper_encased_warped_shaft` (Copper Encased Warped Shaft) [axis]; `copper_gearbox` (Copper Gearbox) [axis]; `copper_gearshift` (Copper Gearshift) [axis, powered]; `copper_mechanical_drill` (Copper Mechanical Drill) [facing, waterlogged]; `copper_mechanical_harvester` (Copper Mechanical Harvester) [facing, waterlogged]; `copper_mechanical_plough` (Copper Mechanical Plough) [facing, waterlogged]; `copper_mechanical_roller` (Copper Mechanical Roller) [facing, waterlogged]; `copper_mechanical_saw` (Copper Mechanical Saw) [axis_along_first, facing, flipped]; `copper_mixer` (Copper Mixer); `copper_portable_storage_interface` (Copper Portable Storage Interface) [facing]; `copper_press` (Copper Press) [facing]; `creative_adjustable_chain_gearshift` (Creative Adjustable Chain Gearshift) [axis, axis_along_first, part, powered]; `creative_casing` (Creative Casing); `creative_chain_conveyor` (Creative Chain Conveyor); `creative_clutch` (Creative Clutch) [axis, powered]; `creative_cogwheel` (Creative Cogwheel) [axis]; `creative_configurable_gearbox` (Creative Configurable Gearbox) [down, up, north, south, west, east]; `creative_deployer` (Creative Deployer) [axis_along_first, facing]; `creative_depot` (Creative Depot); `creative_encased_acacia_cogwheel` (Creative Encased Acacia Cogwheel) [axis, bottom_shaft, top_shaft]; `creative_encased_acacia_large_cogwheel` (Creative Encased Acacia Large Cogwheel) [axis, bottom_shaft, top_shaft]; `creative_encased_acacia_shaft` (Creative Encased Acacia Shaft) [axis]; `creative_encased_bamboo_cogwheel` (Creative Encased Bamboo Cogwheel) [axis, bottom_shaft, top_shaft]; `creative_encased_bamboo_large_cogwheel` (Creative Encased Bamboo Large Cogwheel) [axis, bottom_shaft, top_shaft]; `creative_encased_bamboo_shaft` (Creative Encased Bamboo Shaft) [axis]; `creative_encased_birch_cogwheel` (Creative Encased Birch Cogwheel) [axis, bottom_shaft, top_shaft]; `creative_encased_birch_large_cogwheel` (Creative Encased Birch Large Cogwheel) [axis, bottom_shaft, top_shaft]; `creative_encased_birch_shaft` (Creative Encased Birch Shaft) [axis]; `creative_encased_chain_drive` (Creative Encased Chain Drive) [axis, axis_along_first, part]; `creative_encased_cherry_cogwheel` (Creative Encased Cherry Cogwheel) [axis, bottom_shaft, top_shaft]; `creative_encased_cherry_large_cogwheel` (Creative Encased Cherry Large Cogwheel) [axis, bottom_shaft, top_shaft]; `creative_encased_cherry_shaft` (Creative Encased Cherry Shaft) [axis]; `creative_encased_cogwheel` (Creative Encased Cogwheel) [axis, bottom_shaft, top_shaft]; `creative_encased_crimson_cogwheel` (Creative Encased Crimson Cogwheel) [axis, bottom_shaft, top_shaft]; `creative_encased_crimson_large_cogwheel` (Creative Encased Crimson Large Cogwheel) [axis, bottom_shaft, top_shaft]; `creative_encased_crimson_shaft` (Creative Encased Crimson Shaft) [axis]; `creative_encased_dark_oak_cogwheel` (Creative Encased Dark Oak Cogwheel) [axis, bottom_shaft, top_shaft]; `creative_encased_dark_oak_large_cogwheel` (Creative Encased Dark Oak Large Cogwheel) [axis, bottom_shaft, top_shaft]; `creative_encased_dark_oak_shaft` (Creative Encased Dark Oak Shaft) [axis]; `creative_encased_fan` (Creative Encased Fan) [facing]; `creative_encased_fluid_pipe` [down, up, north, south, west, east]; `creative_encased_glass_shaft` (Creative Encased Glass Shaft) [axis]; `creative_encased_jungle_cogwheel` (Creative Encased Jungle Cogwheel) [axis, bottom_shaft, top_shaft]; `creative_encased_jungle_large_cogwheel` (Creative Encased Jungle Large Cogwheel) [axis, bottom_shaft, top_shaft]; `creative_encased_jungle_shaft` (Creative Encased Jungle Shaft) [axis]; `creative_encased_large_cogwheel` (Creative Encased Large Cogwheel) [axis, bottom_shaft, top_shaft]; `creative_encased_mangrove_cogwheel` (Creative Encased Mangrove Cogwheel) [axis, bottom_shaft, top_shaft]; `creative_encased_mangrove_large_cogwheel` (Creative Encased Mangrove Large Cogwheel) [axis, bottom_shaft, top_shaft]; `creative_encased_mangrove_shaft` (Creative Encased Mangrove Shaft) [axis]; `creative_encased_mldeg_shaft` (Creative Encased Mldeg Shaft) [axis]; `creative_encased_oak_cogwheel` (Creative Encased Oak Cogwheel) [axis, bottom_shaft, top_shaft]; `creative_encased_oak_large_cogwheel` (Creative Encased Oak Large Cogwheel) [axis, bottom_shaft, top_shaft]; `creative_encased_oak_shaft` (Creative Encased Oak Shaft) [axis]; `creative_encased_shaft` (Creative Encased Shaft) [axis]; `creative_encased_spruce_shaft` (Creative Encased Spruce Shaft) [axis]; `creative_encased_warped_cogwheel` (Creative Encased Warped Cogwheel) [axis, bottom_shaft, top_shaft]; `creative_encased_warped_large_cogwheel` (Creative Encased Warped Large Cogwheel) [axis, bottom_shaft, top_shaft]; `creative_encased_warped_shaft` (Creative Encased Warped Shaft) [axis]; `creative_gearbox` (Creative Gearbox) [axis]; `creative_gearshift` (Creative Gearshift) [axis, powered]; `creative_mechanical_drill` (Creative Mechanical Drill) [facing, waterlogged]; `creative_mechanical_harvester` (Creative Mechanical Harvester) [facing, waterlogged]; `creative_mechanical_plough` (Creative Mechanical Plough) [facing, waterlogged]; `creative_mechanical_roller` (Creative Mechanical Roller) [facing, waterlogged]; `creative_mechanical_saw` (Creative Mechanical Saw) [axis_along_first, facing, flipped]; `creative_mixer` (Creative Mixer); `creative_portable_storage_interface` (Creative Portable Storage Interface) [facing]; `creative_press` (Creative Press) [facing]; `crimson_cogwheel` (Crimson Cogwheel) [axis]; `crimson_large_cogwheel` (Crimson Large Cogwheel) [axis]; `crimson_shaft` (Crimson Shaft) [axis]; `dark_oak_cogwheel` (Dark Oak Cogwheel) [axis]; `dark_oak_large_cogwheel` (Dark Oak Large Cogwheel) [axis]; `dark_oak_shaft` (Dark Oak Shaft) [axis]; `glass_shaft` (Glass Shaft) [axis]; `industrial_iron_adjustable_chain_gearshift` (Industrial Iron Adjustable Chain Gearshift) [axis, axis_along_first, part, powered]; `industrial_iron_chain_conveyor` (Industrial Iron Chain Conveyor); `industrial_iron_clutch` (Industrial Iron Clutch) [axis, powered]; `industrial_iron_configurable_gearbox` (Industrial Iron Configurable Gearbox) [down, up, north, south, west, east]; `industrial_iron_deployer` (Industrial Iron Deployer) [axis_along_first, facing]; `industrial_iron_depot` (Industrial Iron Depot); `industrial_iron_encased_acacia_cogwheel` (Industrial Iron Encased Acacia Cogwheel) [axis, bottom_shaft, top_shaft]; `industrial_iron_encased_acacia_large_cogwheel` (Industrial Iron Encased Acacia Large Cogwheel) [axis, bottom_shaft, top_shaft]; `industrial_iron_encased_acacia_shaft` (Industrial Iron Encased Acacia Shaft) [axis]; `industrial_iron_encased_bamboo_cogwheel` (Industrial Iron Encased Bamboo Cogwheel) [axis, bottom_shaft, top_shaft]; `industrial_iron_encased_bamboo_large_cogwheel` (Industrial Iron Encased Bamboo Large Cogwheel) [axis, bottom_shaft, top_shaft]; `industrial_iron_encased_bamboo_shaft` (Industrial Iron Encased Bamboo Shaft) [axis]; `industrial_iron_encased_birch_cogwheel` (Industrial Iron Encased Birch Cogwheel) [axis, bottom_shaft, top_shaft]; `industrial_iron_encased_birch_large_cogwheel` (Industrial Iron Encased Birch Large Cogwheel) [axis, bottom_shaft, top_shaft]; `industrial_iron_encased_birch_shaft` (Industrial Iron Encased Birch Shaft) [axis]; `industrial_iron_encased_chain_drive` (Industrial Iron Encased Chain Drive) [axis, axis_along_first, part]; `industrial_iron_encased_cherry_cogwheel` (Industrial Iron Encased Cherry Cogwheel) [axis, bottom_shaft, top_shaft]; `industrial_iron_encased_cherry_large_cogwheel` (Industrial Iron Encased Cherry Large Cogwheel) [axis, bottom_shaft, top_shaft]; `industrial_iron_encased_cherry_shaft` (Industrial Iron Encased Cherry Shaft) [axis]; `industrial_iron_encased_cogwheel` (Industrial Iron Encased Cogwheel) [axis, bottom_shaft, top_shaft]; `industrial_iron_encased_crimson_cogwheel` (Industrial Iron Encased Crimson Cogwheel) [axis, bottom_shaft, top_shaft]; `industrial_iron_encased_crimson_large_cogwheel` (Industrial Iron Encased Crimson Large Cogwheel) [axis, bottom_shaft, top_shaft]; `industrial_iron_encased_crimson_shaft` (Industrial Iron Encased Crimson Shaft) [axis]; `industrial_iron_encased_dark_oak_cogwheel` (Industrial Iron Encased Dark Oak Cogwheel) [axis, bottom_shaft, top_shaft]; `industrial_iron_encased_dark_oak_large_cogwheel` (Industrial Iron Encased Dark Oak Large Cogwheel) [axis, bottom_shaft, top_shaft]; `industrial_iron_encased_dark_oak_shaft` (Industrial Iron Encased Dark Oak Shaft) [axis]; `industrial_iron_encased_fan` (Industrial Iron Encased Fan) [facing]; `industrial_iron_encased_fluid_pipe` [down, up, north, south, west, east]; `industrial_iron_encased_glass_shaft` (Industrial Iron Encased Glass Shaft) [axis]; `industrial_iron_encased_jungle_cogwheel` (Industrial Iron Encased Jungle Cogwheel) [axis, bottom_shaft, top_shaft]; `industrial_iron_encased_jungle_large_cogwheel` (Industrial Iron Encased Jungle Large Cogwheel) [axis, bottom_shaft, top_shaft]; `industrial_iron_encased_jungle_shaft` (Industrial Iron Encased Jungle Shaft) [axis]; `industrial_iron_encased_large_cogwheel` (Industrial Iron Encased Large Cogwheel) [axis, bottom_shaft, top_shaft]; `industrial_iron_encased_mangrove_cogwheel` (Industrial Iron Encased Mangrove Cogwheel) [axis, bottom_shaft, top_shaft]; `industrial_iron_encased_mangrove_large_cogwheel` (Industrial Iron Encased Mangrove Large Cogwheel) [axis, bottom_shaft, top_shaft]; `industrial_iron_encased_mangrove_shaft` (Industrial Iron Encased Mangrove Shaft) [axis]; `industrial_iron_encased_mldeg_shaft` (Industrial Iron Encased Mldeg Shaft) [axis]; `industrial_iron_encased_oak_cogwheel` (Industrial Iron Encased Oak Cogwheel) [axis, bottom_shaft, top_shaft]; `industrial_iron_encased_oak_large_cogwheel` (Industrial Iron Encased Oak Large Cogwheel) [axis, bottom_shaft, top_shaft]; `industrial_iron_encased_oak_shaft` (Industrial Iron Encased Oak Shaft) [axis]; `industrial_iron_encased_shaft` (Industrial Iron Encased Shaft) [axis]; `industrial_iron_encased_spruce_shaft` (Industrial Iron Encased Spruce Shaft) [axis]; `industrial_iron_encased_warped_cogwheel` (Industrial Iron Encased Warped Cogwheel) [axis, bottom_shaft, top_shaft]; `industrial_iron_encased_warped_large_cogwheel` (Industrial Iron Encased Warped Large Cogwheel) [axis, bottom_shaft, top_shaft]; `industrial_iron_encased_warped_shaft` (Industrial Iron Encased Warped Shaft) [axis]; `industrial_iron_gearbox` (Industrial Iron Gearbox) [axis]; `industrial_iron_gearshift` (Industrial Iron Gearshift) [axis, powered]; `industrial_iron_mechanical_drill` (Industrial Iron Mechanical Drill) [facing, waterlogged]; `industrial_iron_mechanical_harvester` (Industrial Iron Mechanical Harvester) [facing, waterlogged]; `industrial_iron_mechanical_plough` (Industrial Iron Mechanical Plough) [facing, waterlogged]; `industrial_iron_mechanical_roller` (Industrial Iron Mechanical Roller) [facing, waterlogged]; `industrial_iron_mechanical_saw` (Industrial Iron Mechanical Saw) [axis_along_first, facing, flipped]; `industrial_iron_mixer` (Industrial Iron Mixer); `industrial_iron_portable_storage_interface` (Industrial Iron Portable Storage Interface) [facing]; `industrial_iron_press` (Industrial Iron Press) [facing]; `jungle_cogwheel` (Jungle Cogwheel) [axis]; `jungle_large_cogwheel` (Jungle Large Cogwheel) [axis]; `jungle_shaft` (Jungle Shaft) [axis]; `mangrove_cogwheel` (Mangrove Cogwheel) [axis]; `mangrove_large_cogwheel` (Mangrove Large Cogwheel) [axis]; `mangrove_shaft` (Mangrove Shaft) [axis]; `mldeg_shaft` (MLDEG Shaft) [axis]; `oak_cogwheel` (Oak Cogwheel) [axis]; `oak_large_cogwheel` (Oak Large Cogwheel) [axis]; `oak_shaft` (Oak Shaft) [axis]; `railway_adjustable_chain_gearshift` (Train Adjustable Chain Gearshift) [axis, axis_along_first, part, powered]; `railway_chain_conveyor` (Train Chain Conveyor); `railway_clutch` (Train Clutch) [axis, powered]; `railway_configurable_gearbox` (Train Configurable Gearbox) [down, up, north, south, west, east]; `railway_deployer` (Train Deployer) [axis_along_first, facing]; `railway_depot` (Train Depot); `railway_encased_acacia_cogwheel` (Railway Encased Acacia Cogwheel) [axis, bottom_shaft, top_shaft]; `railway_encased_acacia_large_cogwheel` (Railway Encased Acacia Large Cogwheel) [axis, bottom_shaft, top_shaft]; `railway_encased_acacia_shaft` (Train Encased Acacia Shaft) [axis]; `railway_encased_bamboo_cogwheel` (Railway Encased Bamboo Cogwheel) [axis, bottom_shaft, top_shaft]; `railway_encased_bamboo_large_cogwheel` (Railway Encased Bamboo Large Cogwheel) [axis, bottom_shaft, top_shaft]; `railway_encased_bamboo_shaft` (Train Encased Bamboo Shaft) [axis]; `railway_encased_birch_cogwheel` (Railway Encased Birch Cogwheel) [axis, bottom_shaft, top_shaft]; `railway_encased_birch_large_cogwheel` (Railway Encased Birch Large Cogwheel) [axis, bottom_shaft, top_shaft]; `railway_encased_birch_shaft` (Train Encased Birch Shaft) [axis]; `railway_encased_chain_drive` (Train Encased Chain Drive) [axis, axis_along_first, part]; `railway_encased_cherry_cogwheel` (Railway Encased Cherry Cogwheel) [axis, bottom_shaft, top_shaft]; `railway_encased_cherry_large_cogwheel` (Railway Encased Cherry Large Cogwheel) [axis, bottom_shaft, top_shaft]; `railway_encased_cherry_shaft` (Train Encased Cherry Shaft) [axis]; `railway_encased_cogwheel` (Train Encased Cogwheel) [axis, bottom_shaft, top_shaft]; `railway_encased_crimson_cogwheel` (Railway Encased Crimson Cogwheel) [axis, bottom_shaft, top_shaft]; `railway_encased_crimson_large_cogwheel` (Railway Encased Crimson Large Cogwheel) [axis, bottom_shaft, top_shaft]; `railway_encased_crimson_shaft` (Train Encased Crimson Shaft) [axis]; `railway_encased_dark_oak_cogwheel` (Railway Encased Dark Oak Cogwheel) [axis, bottom_shaft, top_shaft]; `railway_encased_dark_oak_large_cogwheel` (Railway Encased Dark Oak Large Cogwheel) [axis, bottom_shaft, top_shaft]; `railway_encased_dark_oak_shaft` (Train Encased Dark Oak Shaft) [axis]; `railway_encased_fan` (Train Encased Fan) [facing]; `railway_encased_fluid_pipe` [down, up, north, south, west, east]; `railway_encased_glass_shaft` (Train Encased Glass Shaft) [axis]; `railway_encased_jungle_cogwheel` (Railway Encased Jungle Cogwheel) [axis, bottom_shaft, top_shaft]; `railway_encased_jungle_large_cogwheel` (Railway Encased Jungle Large Cogwheel) [axis, bottom_shaft, top_shaft]; `railway_encased_jungle_shaft` (Train Encased Jungle Shaft) [axis]; `railway_encased_large_cogwheel` (Train Encased Large Cogwheel) [axis, bottom_shaft, top_shaft]; `railway_encased_mangrove_cogwheel` (Railway Encased Mangrove Cogwheel) [axis, bottom_shaft, top_shaft]; `railway_encased_mangrove_large_cogwheel` (Railway Encased Mangrove Large Cogwheel) [axis, bottom_shaft, top_shaft]; `railway_encased_mangrove_shaft` (Train Encased Mangrove Shaft) [axis]; `railway_encased_mldeg_shaft` (Train Encased Mldeg Shaft) [axis]; `railway_encased_oak_cogwheel` (Railway Encased Oak Cogwheel) [axis, bottom_shaft, top_shaft]; `railway_encased_oak_large_cogwheel` (Railway Encased Oak Large Cogwheel) [axis, bottom_shaft, top_shaft]; `railway_encased_oak_shaft` (Train Encased Oak Shaft) [axis]; `railway_encased_shaft` (Train Encased Shaft) [axis]; `railway_encased_spruce_shaft` (Train Encased Spruce Shaft) [axis]; `railway_encased_warped_cogwheel` (Railway Encased Warped Cogwheel) [axis, bottom_shaft, top_shaft]; `railway_encased_warped_large_cogwheel` (Railway Encased Warped Large Cogwheel) [axis, bottom_shaft, top_shaft]; `railway_encased_warped_shaft` (Train Encased Warped Shaft) [axis]; `railway_gearbox` (Train Gearbox) [axis]; `railway_gearshift` (Train Gearshift) [axis, powered]; `railway_mechanical_drill` (Train Mechanical Drill) [facing, waterlogged]; `railway_mechanical_harvester` (Train Mechanical Harvester) [facing, waterlogged]; `railway_mechanical_plough` (Train Mechanical Plough) [facing, waterlogged]; `railway_mechanical_roller` (Train Mechanical Roller) [facing, waterlogged]; `railway_mechanical_saw` (Train Mechanical Saw) [axis_along_first, facing, flipped]; `railway_mixer` (Train Mixer); `railway_portable_storage_interface` (Train Portable Storage Interface) [facing]; `railway_press` (Train Press) [facing]; `refined_radiance_adjustable_chain_gearshift` (Refined Radiance Adjustable Chain Gearshift) [axis, axis_along_first, part, powered]; `refined_radiance_chain_conveyor` (Refined Radiance Chain Conveyor); `refined_radiance_clutch` (Refined Radiance Clutch) [axis, powered]; `refined_radiance_configurable_gearbox` (Refined Radiance Configurable Gearbox) [down, up, north, south, west, east]; `refined_radiance_deployer` (Refined Radiance Deployer) [axis_along_first, facing]; `refined_radiance_depot` (Refined Radiance Depot); `refined_radiance_encased_acacia_cogwheel` (Refined Radiance Encased Acacia Cogwheel) [axis, bottom_shaft, top_shaft]; `refined_radiance_encased_acacia_large_cogwheel` (Refined Radiance Encased Acacia Large Cogwheel) [axis, bottom_shaft, top_shaft]; `refined_radiance_encased_acacia_shaft` (Refined Radiance Encased Acacia Shaft) [axis]; `refined_radiance_encased_bamboo_cogwheel` (Refined Radiance Encased Bamboo Cogwheel) [axis, bottom_shaft, top_shaft]; `refined_radiance_encased_bamboo_large_cogwheel` (Refined Radiance Encased Bamboo Large Cogwheel) [axis, bottom_shaft, top_shaft]; `refined_radiance_encased_bamboo_shaft` (Refined Radiance Encased Bamboo Shaft) [axis]; `refined_radiance_encased_birch_cogwheel` (Refined Radiance Encased Birch Cogwheel) [axis, bottom_shaft, top_shaft]; `refined_radiance_encased_birch_large_cogwheel` (Refined Radiance Encased Birch Large Cogwheel) [axis, bottom_shaft, top_shaft]; `refined_radiance_encased_birch_shaft` (Refined Radiance Encased Birch Shaft) [axis]; `refined_radiance_encased_chain_drive` (Refined Radiance Encased Chain Drive) [axis, axis_along_first, part]; `refined_radiance_encased_cherry_cogwheel` (Refined Radiance Encased Cherry Cogwheel) [axis, bottom_shaft, top_shaft]; `refined_radiance_encased_cherry_large_cogwheel` (Refined Radiance Encased Cherry Large Cogwheel) [axis, bottom_shaft, top_shaft]; `refined_radiance_encased_cherry_shaft` (Refined Radiance Encased Cherry Shaft) [axis]; `refined_radiance_encased_cogwheel` (Refined Radiance Encased Cogwheel) [axis, bottom_shaft, top_shaft]; `refined_radiance_encased_crimson_cogwheel` (Refined Radiance Encased Crimson Cogwheel) [axis, bottom_shaft, top_shaft]; `refined_radiance_encased_crimson_large_cogwheel` (Refined Radiance Encased Crimson Large Cogwheel) [axis, bottom_shaft, top_shaft]; `refined_radiance_encased_crimson_shaft` (Refined Radiance Encased Crimson Shaft) [axis]; `refined_radiance_encased_dark_oak_cogwheel` (Refined Radiance Encased Dark Oak Cogwheel) [axis, bottom_shaft, top_shaft]; `refined_radiance_encased_dark_oak_large_cogwheel` (Refined Radiance Encased Dark Oak Large Cogwheel) [axis, bottom_shaft, top_shaft]; `refined_radiance_encased_dark_oak_shaft` (Refined Radiance Encased Dark Oak Shaft) [axis]; `refined_radiance_encased_fan` (Refined Radiance Encased Fan) [facing]; `refined_radiance_encased_fluid_pipe` (Refined Radiance Encased Fluid Pipe) [down, up, north, south, west, east]; `refined_radiance_encased_glass_shaft` (Refined Radiance Encased Glass Shaft) [axis]; `refined_radiance_encased_jungle_cogwheel` (Refined Radiance Encased Jungle Cogwheel) [axis, bottom_shaft, top_shaft]; `refined_radiance_encased_jungle_large_cogwheel` (Refined Radiance Encased Jungle Large Cogwheel) [axis, bottom_shaft, top_shaft]; `refined_radiance_encased_jungle_shaft` (Refined Radiance Encased Jungle Shaft) [axis]; `refined_radiance_encased_large_cogwheel` (Refined Radiance Encased Large Cogwheel) [axis, bottom_shaft, top_shaft]; `refined_radiance_encased_mangrove_cogwheel` (Refined Radiance Encased Mangrove Cogwheel) [axis, bottom_shaft, top_shaft]; `refined_radiance_encased_mangrove_large_cogwheel` (Refined Radiance Encased Mangrove Large Cogwheel) [axis, bottom_shaft, top_shaft]; `refined_radiance_encased_mangrove_shaft` (Refined Radiance Encased Mangrove Shaft) [axis]; `refined_radiance_encased_mldeg_shaft` (Refined Radiance Encased Mldeg Shaft) [axis]; `refined_radiance_encased_oak_cogwheel` (Refined Radiance Encased Oak Cogwheel) [axis, bottom_shaft, top_shaft]; `refined_radiance_encased_oak_large_cogwheel` (Refined Radiance Encased Oak Large Cogwheel) [axis, bottom_shaft, top_shaft]; `refined_radiance_encased_oak_shaft` (Refined Radiance Encased Oak Shaft) [axis]; `refined_radiance_encased_shaft` (Refined Radiance Encased Shaft) [axis]; `refined_radiance_encased_spruce_shaft` (Refined Radiance Encased Spruce Shaft) [axis]; `refined_radiance_encased_warped_cogwheel` (Refined Radiance Encased Warped Cogwheel) [axis, bottom_shaft, top_shaft]; `refined_radiance_encased_warped_large_cogwheel` (Refined Radiance Encased Warped Large Cogwheel) [axis, bottom_shaft, top_shaft]; `refined_radiance_encased_warped_shaft` (Refined Radiance Encased Warped Shaft) [axis]; `refined_radiance_gearbox` (Refined Radiance Gearbox) [axis]; `refined_radiance_gearshift` (Refined Radiance Gearshift) [axis, powered]; `refined_radiance_mechanical_drill` (Refined Radiance Mechanical Drill) [facing, waterlogged]; `refined_radiance_mechanical_harvester` (Refined Radiance Mechanical Harvester) [facing, waterlogged]; `refined_radiance_mechanical_plough` (Refined Radiance Mechanical Plough) [facing, waterlogged]; `refined_radiance_mechanical_roller` (Refined Radiance Mechanical Roller) [facing, waterlogged]; `refined_radiance_mechanical_saw` (Refined Radiance Mechanical Saw) [axis_along_first, facing, flipped]; `refined_radiance_mixer` (Refined Radiance Mixer); `refined_radiance_portable_storage_interface` (Refined Radiance Portable Storage Interface) [facing]; `refined_radiance_press` (Refined Radiance Press) [facing]; `shadow_steel_adjustable_chain_gearshift` (Shadow Steel Adjustable Chain Gearshift) [axis, axis_along_first, part, powered]; `shadow_steel_chain_conveyor` (Shadow Steel Chain Conveyor); `shadow_steel_clutch` (Shadow Steel Clutch) [axis, powered]; `shadow_steel_configurable_gearbox` (Shadow Steel Configurable Gearbox) [down, up, north, south, west, east]; `shadow_steel_deployer` (Shadow Steel Deployer) [axis_along_first, facing]; `shadow_steel_depot` (Shadow Steel Depot); `shadow_steel_encased_acacia_cogwheel` (Shadow Steel Encased Acacia Cogwheel) [axis, bottom_shaft, top_shaft]; `shadow_steel_encased_acacia_large_cogwheel` (Shadow Steel Encased Acacia Large Cogwheel) [axis, bottom_shaft, top_shaft]; `shadow_steel_encased_acacia_shaft` (Shadow Steel Encased Acacia Shaft) [axis]; `shadow_steel_encased_bamboo_cogwheel` (Shadow Steel Encased Bamboo Cogwheel) [axis, bottom_shaft, top_shaft]; `shadow_steel_encased_bamboo_large_cogwheel` (Shadow Steel Encased Bamboo Large Cogwheel) [axis, bottom_shaft, top_shaft]; `shadow_steel_encased_bamboo_shaft` (Shadow Steel Encased Bamboo Shaft) [axis]; `shadow_steel_encased_birch_cogwheel` (Shadow Steel Encased Birch Cogwheel) [axis, bottom_shaft, top_shaft]; `shadow_steel_encased_birch_large_cogwheel` (Shadow Steel Encased Birch Large Cogwheel) [axis, bottom_shaft, top_shaft]; `shadow_steel_encased_birch_shaft` (Shadow Steel Encased Birch Shaft) [axis]; `shadow_steel_encased_chain_drive` (Shadow Steel Encased Chain Drive) [axis, axis_along_first, part]; `shadow_steel_encased_cherry_cogwheel` (Shadow Steel Encased Cherry Cogwheel) [axis, bottom_shaft, top_shaft]; `shadow_steel_encased_cherry_large_cogwheel` (Shadow Steel Encased Cherry Large Cogwheel) [axis, bottom_shaft, top_shaft]; `shadow_steel_encased_cherry_shaft` (Shadow Steel Encased Cherry Shaft) [axis]; `shadow_steel_encased_cogwheel` (Shadow Steel Encased Cogwheel) [axis, bottom_shaft, top_shaft]; `shadow_steel_encased_crimson_cogwheel` (Shadow Steel Encased Crimson Cogwheel) [axis, bottom_shaft, top_shaft]; `shadow_steel_encased_crimson_large_cogwheel` (Shadow Steel Encased Crimson Large Cogwheel) [axis, bottom_shaft, top_shaft]; `shadow_steel_encased_crimson_shaft` (Shadow Steel Encased Crimson Shaft) [axis]; `shadow_steel_encased_dark_oak_cogwheel` (Shadow Steel Encased Dark Oak Cogwheel) [axis, bottom_shaft, top_shaft]; `shadow_steel_encased_dark_oak_large_cogwheel` (Shadow Steel Encased Dark Oak Large Cogwheel) [axis, bottom_shaft, top_shaft]; `shadow_steel_encased_dark_oak_shaft` (Shadow Steel Encased Dark Oak Shaft) [axis]; `shadow_steel_encased_fan` (Shadow Steel Encased Fan) [facing]; `shadow_steel_encased_fluid_pipe` (Shadow Steel Encased Fluid Pipe) [down, up, north, south, west, east]; `shadow_steel_encased_glass_shaft` (Shadow Steel Encased Glass Shaft) [axis]; `shadow_steel_encased_jungle_cogwheel` (Shadow Steel Encased Jungle Cogwheel) [axis, bottom_shaft, top_shaft]; `shadow_steel_encased_jungle_large_cogwheel` (Shadow Steel Encased Jungle Large Cogwheel) [axis, bottom_shaft, top_shaft]; `shadow_steel_encased_jungle_shaft` (Shadow Steel Encased Jungle Shaft) [axis]; `shadow_steel_encased_large_cogwheel` (Shadow Steel Encased Large Cogwheel) [axis, bottom_shaft, top_shaft]; `shadow_steel_encased_mangrove_cogwheel` (Shadow Steel Encased Mangrove Cogwheel) [axis, bottom_shaft, top_shaft]; `shadow_steel_encased_mangrove_large_cogwheel` (Shadow Steel Encased Mangrove Large Cogwheel) [axis, bottom_shaft, top_shaft]; `shadow_steel_encased_mangrove_shaft` (Shadow Steel Encased Mangrove Shaft) [axis]; `shadow_steel_encased_mldeg_shaft` (Shadow Steel Encased Mldeg Shaft) [axis]; `shadow_steel_encased_oak_cogwheel` (Shadow Steel Encased Oak Cogwheel) [axis, bottom_shaft, top_shaft]; `shadow_steel_encased_oak_large_cogwheel` (Shadow Steel Encased Oak Large Cogwheel) [axis, bottom_shaft, top_shaft]; `shadow_steel_encased_oak_shaft` (Shadow Steel Encased Oak Shaft) [axis]; `shadow_steel_encased_shaft` (Shadow Steel Encased Shaft) [axis]; `shadow_steel_encased_spruce_shaft` (Shadow Steel Encased Spruce Shaft) [axis]; `shadow_steel_encased_warped_cogwheel` (Shadow Steel Encased Warped Cogwheel) [axis, bottom_shaft, top_shaft]; `shadow_steel_encased_warped_large_cogwheel` (Shadow Steel Encased Warped Large Cogwheel) [axis, bottom_shaft, top_shaft]; `shadow_steel_encased_warped_shaft` (Shadow Steel Encased Warped Shaft) [axis]; `shadow_steel_gearbox` (Shadow Steel Gearbox) [axis]; `shadow_steel_gearshift` (Shadow Steel Gearshift) [axis, powered]; `shadow_steel_mechanical_drill` (Shadow Steel Mechanical Drill) [facing, waterlogged]; `shadow_steel_mechanical_harvester` (Shadow Steel Mechanical Harvester) [facing, waterlogged]; `shadow_steel_mechanical_plough` (Shadow Steel Mechanical Plough) [facing, waterlogged]; `shadow_steel_mechanical_roller` (Shadow Steel Mechanical Roller) [facing, waterlogged]; `shadow_steel_mechanical_saw` (Shadow Steel Mechanical Saw) [axis_along_first, facing, flipped]; `shadow_steel_mixer` (Shadow Steel Mixer); `shadow_steel_portable_storage_interface` (Shadow Steel Portable Storage Interface) [facing]; `shadow_steel_press` (Shadow Steel Press) [facing]; `spruce_shaft` (Spruce Shaft) [axis]; `warped_cogwheel` (Warped Cogwheel) [axis]; `warped_large_cogwheel` (Warped Large Cogwheel) [axis]; `warped_shaft` (Warped Shaft) [axis]; `weathered_iron_adjustable_chain_gearshift` (Weathered Iron Adjustable Chain Gearshift) [axis, axis_along_first, part, powered]; `weathered_iron_chain_conveyor` (Weathered Iron Chain Conveyor); `weathered_iron_clutch` (Weathered Iron Clutch) [axis, powered]; `weathered_iron_configurable_gearbox` (Weathered Iron Configurable Gearbox) [down, up, north, south, west, east]; `weathered_iron_deployer` (Weathered Iron Deployer) [axis_along_first, facing]; `weathered_iron_depot` (Weathered Iron Depot); `weathered_iron_encased_acacia_cogwheel` (Weathered Iron Encased Acacia Cogwheel) [axis, bottom_shaft, top_shaft]; `weathered_iron_encased_acacia_large_cogwheel` (Weathered Iron Encased Acacia Large Cogwheel) [axis, bottom_shaft, top_shaft]; `weathered_iron_encased_acacia_shaft` (Weathered Iron Encased Acacia Shaft) [axis]; `weathered_iron_encased_bamboo_cogwheel` (Weathered Iron Encased Bamboo Cogwheel) [axis, bottom_shaft, top_shaft]; `weathered_iron_encased_bamboo_large_cogwheel` (Weathered Iron Encased Bamboo Large Cogwheel) [axis, bottom_shaft, top_shaft]; `weathered_iron_encased_bamboo_shaft` (Weathered Iron Encased Bamboo Shaft) [axis]; `weathered_iron_encased_birch_cogwheel` (Weathered Iron Encased Birch Cogwheel) [axis, bottom_shaft, top_shaft]; `weathered_iron_encased_birch_large_cogwheel` (Weathered Iron Encased Birch Large Cogwheel) [axis, bottom_shaft, top_shaft]; `weathered_iron_encased_birch_shaft` (Weathered Iron Encased Birch Shaft) [axis]; `weathered_iron_encased_chain_drive` (Weathered Iron Encased Chain Drive) [axis, axis_along_first, part]; `weathered_iron_encased_cherry_cogwheel` (Weathered Iron Encased Cherry Cogwheel) [axis, bottom_shaft, top_shaft]; `weathered_iron_encased_cherry_large_cogwheel` (Weathered Iron Encased Cherry Large Cogwheel) [axis, bottom_shaft, top_shaft]; `weathered_iron_encased_cherry_shaft` (Weathered Iron Encased Cherry Shaft) [axis]; `weathered_iron_encased_cogwheel` (Weathered Iron Encased Cogwheel) [axis, bottom_shaft, top_shaft]; `weathered_iron_encased_crimson_cogwheel` (Weathered Iron Encased Crimson Cogwheel) [axis, bottom_shaft, top_shaft]; `weathered_iron_encased_crimson_large_cogwheel` (Weathered Iron Encased Crimson Large Cogwheel) [axis, bottom_shaft, top_shaft]; `weathered_iron_encased_crimson_shaft` (Weathered Iron Encased Crimson Shaft) [axis]; `weathered_iron_encased_dark_oak_cogwheel` (Weathered Iron Encased Dark Oak Cogwheel) [axis, bottom_shaft, top_shaft]; `weathered_iron_encased_dark_oak_large_cogwheel` (Weathered Iron Encased Dark Oak Large Cogwheel) [axis, bottom_shaft, top_shaft]; `weathered_iron_encased_dark_oak_shaft` (Weathered Iron Encased Dark Oak Shaft) [axis]; `weathered_iron_encased_fan` (Weathered Iron Encased Fan) [facing]; `weathered_iron_encased_fluid_pipe` (Weathered Iron Encased Fluid Pipe) [down, up, north, south, west, east]; `weathered_iron_encased_glass_shaft` (Weathered Iron Encased Glass Shaft) [axis]; `weathered_iron_encased_jungle_cogwheel` (Weathered Iron Encased Jungle Cogwheel) [axis, bottom_shaft, top_shaft]; `weathered_iron_encased_jungle_large_cogwheel` (Weathered Iron Encased Jungle Large Cogwheel) [axis, bottom_shaft, top_shaft]; `weathered_iron_encased_jungle_shaft` (Weathered Iron Encased Jungle Shaft) [axis]; `weathered_iron_encased_large_cogwheel` (Weathered Iron Encased Large Cogwheel) [axis, bottom_shaft, top_shaft]; `weathered_iron_encased_mangrove_cogwheel` (Weathered Iron Encased Mangrove Cogwheel) [axis, bottom_shaft, top_shaft]; `weathered_iron_encased_mangrove_large_cogwheel` (Weathered Iron Encased Mangrove Large Cogwheel) [axis, bottom_shaft, top_shaft]; `weathered_iron_encased_mangrove_shaft` (Weathered Iron Encased Mangrove Shaft) [axis]; `weathered_iron_encased_mldeg_shaft` (Weathered Iron Encased Mldeg Shaft) [axis]; `weathered_iron_encased_oak_cogwheel` (Weathered Iron Encased Oak Cogwheel) [axis, bottom_shaft, top_shaft]; `weathered_iron_encased_oak_large_cogwheel` (Weathered Iron Encased Oak Large Cogwheel) [axis, bottom_shaft, top_shaft]; `weathered_iron_encased_oak_shaft` (Weathered Iron Encased Oak Shaft) [axis]; `weathered_iron_encased_shaft` (Weathered Iron Encased Shaft) [axis]; `weathered_iron_encased_spruce_shaft` (Weathered Iron Encased Spruce Shaft) [axis]; `weathered_iron_encased_warped_cogwheel` (Weathered Iron Encased Warped Cogwheel) [axis, bottom_shaft, top_shaft]; `weathered_iron_encased_warped_large_cogwheel` (Weathered Iron Encased Warped Large Cogwheel) [axis, bottom_shaft, top_shaft]; `weathered_iron_encased_warped_shaft` (Weathered Iron Encased Warped Shaft) [axis]; `weathered_iron_gearbox` (Weathered Iron Gearbox) [axis]; `weathered_iron_gearshift` (Weathered Iron Gearshift) [axis, powered]; `weathered_iron_mechanical_drill` (Weathered Iron Mechanical Drill) [facing, waterlogged]; `weathered_iron_mechanical_harvester` (Weathered Iron Mechanical Harvester) [facing, waterlogged]; `weathered_iron_mechanical_plough` (Weathered Iron Mechanical Plough) [facing, waterlogged]; `weathered_iron_mechanical_roller` (Weathered Iron Mechanical Roller) [facing, waterlogged]; `weathered_iron_mechanical_saw` (Weathered Iron Mechanical Saw) [axis_along_first, facing, flipped]; `weathered_iron_mixer` (Weathered Iron Mixer); `weathered_iron_portable_storage_interface` (Weathered Iron Portable Storage Interface) [facing]; `weathered_iron_press` (Weathered Iron Press) [facing]

## createdieselgenerators  (64 blocks, jar: createdieselgenerators-1.20.1-1.3.12)

`andesite_girder` (Andesite Girder) [x, z, top, bottom]; `andesite_girder_encased_shaft` (Andesite Girder Encased Shaft) [axis, top, bottom]; `asphalt_block` (Asphalt Block); `asphalt_slab` (Asphalt Slab) [type]; `asphalt_stairs` (Asphalt Stairs) [facing, half, shape]; `basin_lid` (Basin Lid) [facing, on_a_basin, open, powered, waterlogged]; `biodiesel` (Biodiesel); `black_cement` (Black Cement); `black_concrete_encased_fluid_pipe` (Black Concrete Encased Fluid Pipe) [north, down, up, east, west, south]; `blue_cement` (Blue Cement); `blue_concrete_encased_fluid_pipe` (Blue Concrete Encased Fluid Pipe) [north, down, up, east, west, south]; `brown_cement` (Brown Cement); `brown_concrete_encased_fluid_pipe` (Brown Concrete Encased Fluid Pipe) [north, down, up, east, west, south]; `bulk_fermenter` (Bulk Fermenter); `burner` (Burner) [axis, blaze, lit]; `canister` (Canister) [enchanted, facing, waterlogged]; `chemical_turret` (Chemical Turret); `chip_wood_beam` (Chip Wood Beam) [axis]; `chip_wood_block` (Chip Wood Block) [axis]; `chip_wood_slab` (Chip Wood Slab) [type]; `chip_wood_stairs` (Chip Wood Stairs) [facing, half, shape]; `crude_oil` (Crude Oil); `cyan_cement` (Cyan Cement); `cyan_concrete_encased_fluid_pipe` (Cyan Concrete Encased Fluid Pipe) [north, down, up, east, west, south]; `diesel` (Diesel); `diesel_engine` (Diesel Engine) [facing, powered, waterlogged]; `distillation_tank` (Distillation Tank) [bottom, shape, top]; `ethanol` (Ethanol); `gasoline` (Gasoline); `gray_cement` (Gray Cement); `gray_concrete_encased_fluid_pipe` (Gray Concrete Encased Fluid Pipe) [north, down, up, east, west, south]; `green_cement` (Green Cement); `green_concrete_encased_fluid_pipe` (Green Concrete Encased Fluid Pipe) [north, down, up, east, west, south]; `huge_diesel_engine` (Huge Diesel Engine) [facing, powered]; `large_diesel_engine` (Modular Diesel Engine) [facing, pipe, powered]; `light_blue_cement` (Light Blue Cement); `light_blue_concrete_encased_fluid_pipe` (Light Blue Concrete Encased Fluid Pipe) [north, down, up, east, west, south]; `light_gray_cement` (Light Gray Cement); `light_gray_concrete_encased_fluid_pipe` (Light Gray Concrete Encased Fluid Pipe) [north, down, up, east, west, south]; `lime_cement` (Lime Cement); `lime_concrete_encased_fluid_pipe` (Lime Concrete Encased Fluid Pipe) [north, down, up, east, west, south]; `magenta_cement` (Magenta Cement); `magenta_concrete_encased_fluid_pipe` (Magenta Concrete Encased Fluid Pipe) [north, down, up, east, west, south]; `oil_barrel` (Oil Barrel) [axis, color]; `orange_cement` (Orange Cement); `orange_concrete_encased_fluid_pipe` (Orange Concrete Encased Fluid Pipe) [north, down, up, east, west, south]; `pink_cement` (Pink Cement); `pink_concrete_encased_fluid_pipe` (Pink Concrete Encased Fluid Pipe) [north, down, up, east, west, south]; `plant_oil` (Plant Oil); `powered_engine_shaft` (Powered Engine Shaft) [axis]; `pumpjack_bearing` (Pumpjack Bearing) [facing]; `pumpjack_bearing_b` (Pumpjack Bearing B) [facing]; `pumpjack_crank` (Pumpjack Crank) [facing]; `pumpjack_head` (Pumpjack Head) [facing, waterlogged]; `pumpjack_hole` (Pumpjack Hole) [north, east, west, south]; `purple_cement` (Purple Cement); `purple_concrete_encased_fluid_pipe` (Purple Concrete Encased Fluid Pipe) [north, down, up, east, west, south]; `red_cement` (Red Cement); `red_concrete_encased_fluid_pipe` (Red Concrete Encased Fluid Pipe) [north, down, up, east, west, south]; `sheet_metal_panel` (Sheet Metal Panel) [facing, roll, waterlogged]; `white_cement` (White Cement); `white_concrete_encased_fluid_pipe` (White Concrete Encased Fluid Pipe) [north, down, up, east, west, south]; `yellow_cement` (Yellow Cement); `yellow_concrete_encased_fluid_pipe` (Yellow Concrete Encased Fluid Pipe) [north, down, up, east, west, south]

## createaddition  (23 blocks, jar: createaddition-1.20.1-1.3.3)

`accumulator` (Accumulator) [facing] — Accumulates electric charge. Deprecated and subject to removal from future releases.; `alternator` (Alternator) [facing]; `barbed_wire` (Barbed Wire) [vertical, facing] — Inflicts damage when walked through.; `bioethanol` (Bioethanol); `biomass_pellet_block` (Biomass Pallet); `chocolate_cake` (Chocolate Cake) [bites]; `connector` (Small Connector) [AND] — Connects Forge Energy compatible blocks with wires to transfer ⚡.; `connector_old` [facing, mode]; `copper_wire_casing`; `creative_energy` (Creative Generator) — Generates an infinite supply of ⚡.; `digital_adapter` (Digital Adapter) — A Computercraft Peripheral which allows computers to interface with Display Boards, Speedometers, Stressometers, Rope Pulley, Hose Pulley, Elevator Pulley, Mechanical Piston, Mechanical Bearing, and Rotational Speed Controllers.; `electric_motor` (Electric Motor) [facing]; `electrum_block` (Electrum Block); `honey_cake` (Honey Cake) [bites]; `large_connector` (Large Connector) [AND] — Connects Forge Energy compatible blocks with wires to transfer ⚡.; `liquid_blaze_burner` (Blaze Burner with Straw); `modular_accumulator` (Accumulator) [bottom, top]; `portable_energy_interface` (Portable Energy Interface) [facing]; `redstone_relay` (Redstone Relay) [vertical, facing, powered] — Allows ⚡ to pass from the input connector to the output connector when receiving a redstone signal.; `rolling_mill` (Rolling Mill) [facing]; `seed_oil` (Seed Oil); `small_light_connector` (Small Connector With Light) [AND] — Connects Forge Energy compatible blocks with wires to transfer ⚡, provides light when receiving power.; `tesla_coil` (Tesla Coil) [facing, powered]

## create_enchantment_industry  (20 blocks, jar: create-enchantment-industry-2.5.2)

`affix_augmentor` (Affix Augmentor) [facing]; `apotheotic_essence` (Apotheotic Essence); `blaze_composer` (Blaze Composer) [blaze, facing]; `blaze_enchanter` (Blaze Enchanter) [blaze, facing]; `blaze_forger` (Blaze Forger) [blaze, facing]; `brass_bookshelf` (Brass Bookshelf) [facing]; `classic_blaze_enchanter` (Classic Blaze Enchanter) [blaze, facing]; `creative_bookshelf` (Creative Bookshelf) [facing]; `crystal_essence` (Crystal Essence); `ender_woven_bag` (Ender Woven Bag) [facing, powered]; `experience` (Liquid Experience); `experience_hatch` (Experience Hatch) [facing, waterlogged]; `experience_lantern` (Experience Lantern) [facing, light]; `gem_cutter` (Gem Cutter) [facing]; `grindstone_drain` (Grindstone Drain) [facing]; `infused_dragon_breath` (Infused Dragon's Breath); `infuser` (Infuser) [facing]; `mechanical_grindstone` (Mechanical Grindstone) [axis]; `printer` (Printer) [facing]; `super_experience_block` (Block of Super Experience)

## createbigcannons  (139 blocks, jar: createbigcannons-5.11.4-mc.1.20.1-forge)

`ap_shell` (Armor Piercing (AP) Shell) [facing, waterlogged] — Can effectively pierce through blocks, detonating inside protected structures.; `ap_shot` (Armor Piercing (AP) Shot) [facing, waterlogged] — Can effectively pierce through blocks. Good against armored targets. Cannot be fuzed and detonated.; `autocannon_ammo_container` (Autocannon Ammo Container) [axis, state]; `autocannon_barrel_cast_mould` (Autocannon Barrel Cast Mould) [sand]; `autocannon_breech_cast_mould` (Autocannon Breech Cast Mould) [sand]; `autocannon_recoil_spring_cast_mould` (Autocannon Recoil Spring Cast Mould) [sand]; `bag_of_grapeshot` (Bag of Grapeshot) [facing, waterlogged] — Contains grapeshot, which can damage soft blocks such as wood.; `basin_foundry_lid` (Basin Foundry Lid); `big_cartridge` (Big Cartridge) [damp, facing, filled] — Compact big cannon propellant that can be filled with various levels of more powerful propellant. Can only be used once in a shot, but can be refilled afterwards.; `bronze_autocannon_barrel` (Bronze Autocannon Barrel) [assembled, end, facing]; `bronze_autocannon_breech` (Bronze Autocannon Breech) [facing, handle]; `bronze_autocannon_recoil_spring` (Bronze Autocannon Recoil Spring) [facing]; `bronze_block` (Block of Bronze); `bronze_cannon_barrel` (Bronze Cannon Barrel) [facing]; `bronze_cannon_chamber` (Bronze Cannon Chamber) [facing]; `bronze_cannon_end` (Bronze Cannon End) [facing]; `bronze_quickfiring_breech` (Bronze Quick-Firing Breech) [axis_along_first, facing]; `bronze_sliding_breech` (Bronze Sliding Breech) [axis_along_first, facing]; `built_up_cannon` (Built Up Cannon) [facing]; `built_up_nethersteel_cannon_barrel` (Built-Up Nethersteel Cannon Barrel) [facing]; `built_up_nethersteel_cannon_chamber` (Built-Up Nethersteel Cannon Chamber) [facing]; `built_up_steel_cannon_barrel` (Built-Up Steel Cannon Barrel) [facing]; `built_up_steel_cannon_chamber` (Built-Up Steel Cannon Chamber) [facing]; `cannon_builder` (Cannon Builder) [axis_along_first, facing, powered, state]; `cannon_builder_head` (Cannon Builder Head) [attached, facing]; `cannon_carriage` (Cannon Carriage) [facing, saddled] — Mobile cannon mount. Can be moved around, but cannot be automated like the Cannon Mount.; `cannon_cast` (Cannon Cast); `cannon_drill` (Cannon Drill) [axis_along_first, facing, state]; `cannon_drill_bit` (Cannon Drill Bit) [facing, waterlogged]; `cannon_end_cast_mould` (Cannon End Cast Mould) [sand]; `cannon_loader` (Cannon Loader) [axis_along_first, facing, moving]; `cannon_mount` (Cannon Mount) [assembly_powered, facing, fire_powered, vertical_direction]; `cannon_mount_extension` (Cannon Mount Extension) [facing]; `cast_iron_autocannon_barrel` (Cast Iron Autocannon Barrel) [assembled, end, facing]; `cast_iron_autocannon_breech` (Cast Iron Autocannon Breech) [facing, handle]; `cast_iron_autocannon_recoil_spring` (Cast Iron Autocannon Recoil Spring) [facing]; `cast_iron_block` (Block of Cast Iron); `cast_iron_cannon_barrel` (Cast Iron Cannon Barrel) [facing]; `cast_iron_cannon_chamber` (Cast Iron Cannon Chamber) [facing]; `cast_iron_cannon_end` (Cast Iron Cannon End) [facing]; `cast_iron_quickfiring_breech` (Cast Iron Quick-Firing Breech) [axis_along_first, facing]; `cast_iron_sliding_breech` (Cast Iron Sliding Breech) [axis_along_first, facing]; `casting_sand` (Casting Sand); `creative_autocannon_ammo_container` (Creative Autocannon Ammo Container) [axis, state]; `drop_mortar_shell` (Drop Mortar Shell) [facing, waterlogged] — Light anti-entity explosive shell that deals a bit of structural damage. Fired by dropping into a drop mortar-type big cannon, although it can also be conventionally fired.; `finished_cannon_cast` (Finished Cannon Cast); `fixed_cannon_mount` (Fixed Cannon Mount) [assembly_powered, facing, fire_powered, rotation]; `fluid_shell` (Fluid Shell) [facing, waterlogged] — Spreads the contained fluid all over the targeted area, with different effects depending on the fluid.; `he_shell` (High Explosive (HE) Shell) [facing, waterlogged] — Delivers explosive force to the battlefield.; `incomplete_bronze_autocannon_breech` (Incomplete Bronze Autocannon Breech) [facing]; `incomplete_bronze_autocannon_recoil_spring` (Incomplete Bronze Autocannon Recoil Spring) [facing]; `incomplete_bronze_sliding_breech` (Incomplete Bronze Sliding Breech) [axis_along_first, facing, stage]; `incomplete_cast_iron_autocannon_breech` (Incomplete Cast Iron Autocannon Breech) [facing]; `incomplete_cast_iron_autocannon_recoil_spring` (Incomplete Cast Iron Autocannon Recoil Spring) [facing]; `incomplete_cast_iron_sliding_breech` (Incomplete Cast Iron Sliding Breech) [axis_along_first, facing, stage]; `incomplete_nethersteel_screw_breech` (Incomplete Nethersteel Screw Breech) [facing, stage]; `incomplete_steel_autocannon_breech` (Incomplete Steel Autocannon Breech) [facing]; `incomplete_steel_autocannon_recoil_spring` (Incomplete Steel Autocannon Recoil Spring) [facing]; `incomplete_steel_screw_breech` (Incomplete Steel Screw Breech) [facing, stage]; `incomplete_steel_sliding_breech` (Incomplete Steel Sliding Breech) [axis_along_first, facing, stage]; `large_cast_mould` (Large Cast Mould) [sand]; `large_nethersteel_cannon_layer` (Large Nethersteel Cannon Layer) [facing]; `large_steel_cannon_layer` (Large Steel Cannon Layer) [facing]; `log_cannon_chamber` (Log Cannon Chamber) [facing]; `log_cannon_end` (Log Cannon End) [facing]; `medium_cast_mould` (Medium Cast Mould) [sand]; `medium_nethersteel_cannon_layer` (Medium Nethersteel Cannon Layer) [facing]; `medium_steel_cannon_layer` (Medium Steel Cannon Layer) [facing]; `molten_bronze` (Molten Bronze); `molten_cast_iron` (Molten Cast Iron); `molten_nethersteel` (Molten Nethersteel); `molten_steel` (Molten Steel); `mortar_stone` (Mortar Stone) [facing, waterlogged] — Powerful stone that explodes on impact. Flies further than other projectiles. Good for attacking walls and fortifications. Cannot be fuzed and detonated. Will break if the propellant is too strong.; `mortar_stone_projectile` (Mortar Stone Projectile); `nethersteel_block` (Block of Nethersteel); `nethersteel_cannon_barrel` (Nethersteel Cannon Barrel) [facing]; `nethersteel_cannon_chamber` (Nethersteel Cannon Chamber) [facing]; `nethersteel_screw_breech` (Nethersteel Screw Breech) [facing, open]; `powder_charge` (Powder Charge) [axis, damp] — Standard big cannon propellant.; `ram_head` (Ram Head) [facing, waterlogged]; `screw_breech_cast_mould` (Screw Breech Cast Mould) [sand]; `shrapnel_shell` (Shrapnel Shell) [facing, waterlogged] — Peppers the battlefield with shrapnel bullets when detonated.; `sliding_breech_cast_mould` (Sliding Breech Cast Mould) [sand]; `small_cast_mould` (Small Cast Mould) [sand]; `small_nethersteel_cannon_layer` (Small Nethersteel Cannon Layer) [facing]; `small_steel_cannon_layer` (Small Steel Cannon Layer) [facing]; `smoke_shell` (Smoke Shell) [facing, waterlogged] — Covers the battlefield with a smoke cloud that obscures vision.; `solid_shot` (Solid Shot) [facing, waterlogged] — High penetrating force. Best suited for soft targets such as wooden structures and thin walls. Cannot be fuzed and detonated.; `steel_autocannon_barrel` (Steel Autocannon Barrel) [assembled, end, facing]; `steel_autocannon_breech` (Steel Autocannon Breech) [facing, handle]; `steel_autocannon_recoil_spring` (Steel Autocannon Recoil Spring) [facing]; `steel_block` (Block of Steel); `steel_cannon_barrel` (Steel Cannon Barrel) [facing]; `steel_cannon_chamber` (Steel Cannon Chamber) [facing]; `steel_quickfiring_breech` (Steel Quick-Firing Breech) [axis_along_first, facing]; `steel_screw_breech` (Steel Screw Breech) [facing, open]; `steel_sliding_breech` (Steel Sliding Breech) [axis_along_first, facing]; `thick_nethersteel_cannon_chamber` (Thick Nethersteel Cannon Chamber) [facing]; `thick_steel_cannon_chamber` (Thick Steel Cannon Chamber) [facing]; `traffic_cone` (Traffic Cone) [facing, waterlogged]; `unbored_bronze_autocannon_barrel` (Unbored Bronze Autocannon Barrel) [facing]; `unbored_bronze_autocannon_breech` (Unbored Bronze Autocannon Breech) [facing]; `unbored_bronze_autocannon_recoil_spring` (Unbored Bronze Autocannon Recoil Spring) [facing]; `unbored_bronze_cannon_barrel` (Unbored Bronze Cannon Barrel) [facing]; `unbored_bronze_cannon_chamber` (Unbored Bronze Cannon Chamber) [facing]; `unbored_bronze_sliding_breech` (Unbored Bronze Sliding Breech) [axis_along_first, facing]; `unbored_cast_iron_autocannon_barrel` (Unbored Cast Iron Autocannon Barrel) [facing]; `unbored_cast_iron_autocannon_breech` (Unbored Cast Iron Autocannon Breech) [facing]; `unbored_cast_iron_autocannon_recoil_spring` (Unbored Cast Iron Autocannon Recoil Spring) [facing]; `unbored_cast_iron_cannon_barrel` (Unbored Cast Iron Cannon Barrel) [facing]; `unbored_cast_iron_cannon_chamber` (Unbored Cast Iron Cannon Chamber) [facing]; `unbored_cast_iron_sliding_breech` (Unbored Cast Iron Sliding Breech) [axis_along_first, facing]; `unbored_large_nethersteel_cannon_layer` (Unbored Large Nethersteel Cannon Layer) [facing]; `unbored_large_steel_cannon_layer` (Unbored Large Steel Cannon Layer) [facing]; `unbored_medium_nethersteel_cannon_layer` (Unbored Medium Nethersteel Cannon Layer) [facing]; `unbored_medium_steel_cannon_layer` (Unbored Medium Steel Cannon Layer) [facing]; `unbored_nethersteel_screw_breech` (Unbored Nethersteel Screw Breech) [facing]; `unbored_small_nethersteel_cannon_layer` (Unbored Small Nethersteel Cannon Layer) [facing]; `unbored_small_steel_cannon_layer` (Unbored Small Steel Cannon Layer) [facing]; `unbored_steel_autocannon_barrel` (Unbored Steel Autocannon Barrel) [facing]; `unbored_steel_autocannon_breech` (Unbored Steel Autocannon Breech) [facing]; `unbored_steel_autocannon_recoil_spring` (Unbored Steel Autocannon Recoil Spring) [facing]; `unbored_steel_screw_breech` (Unbored Steel Screw Breech) [facing]; `unbored_steel_sliding_breech` (Unbored Steel Sliding Breech) [axis_along_first, facing]; `unbored_very_large_nethersteel_cannon_layer` (Unbored Very Large Nethersteel Cannon Layer) [facing]; `unbored_very_large_steel_cannon_layer` (Unbored Very Large Steel Cannon Layer) [facing]; `unbored_very_small_nethersteel_cannon_layer` (Unbored Very Small Nethersteel Cannon Layer) [facing]; `unbored_very_small_steel_cannon_layer` (Unbored Very Small Steel Cannon Layer) [facing]; `very_large_cast_mould` (Very Large Cast Mould) [sand]; `very_large_nethersteel_cannon_layer` (Very Large Nethersteel Cannon Layer) [facing]; `very_large_steel_cannon_layer` (Very Large Steel Cannon Layer) [facing]; `very_small_cast_mould` (Very Small Cast Mould) [sand]; `very_small_nethersteel_cannon_layer` (Very Small Nethersteel Cannon Layer) [facing]; `very_small_steel_cannon_layer` (Very Small Steel Cannon Layer) [facing]; `worm_head` (Worm Head) [facing, waterlogged]; `wrought_iron_cannon_chamber` (Wrought Iron Cannon Chamber) [facing]; `wrought_iron_cannon_end` (Wrought Iron Cannon End) [facing]; `wrought_iron_drop_mortar_end` (Wrought Iron Drop Mortar End) [facing]; `yaw_controller` (Yaw Controller)

## create_dragons_plus  (54 blocks, jar: CreateDragonsPlus-1.11.8)

`arts_and_crafts_bleached_dye` (Arts And Crafts Bleached Dye); `black_dye` (Black Dye); `blue_dye` (Blue Dye); `brown_dye` (Brown Dye); `cyan_dye` (Cyan Dye); `dragon_breath` (Dragon's Breath); `dragon_breath_cauldron` (Dragon's Breath Cauldron) [level]; `dye_depot_amber_dye` (Dye Depot Amber Dye); `dye_depot_aqua_dye` (Dye Depot Aqua Dye); `dye_depot_beige_dye` (Dye Depot Beige Dye); `dye_depot_coral_dye` (Dye Depot Coral Dye); `dye_depot_forest_dye` (Dye Depot Forest Dye); `dye_depot_ginger_dye` (Dye Depot Ginger Dye); `dye_depot_indigo_dye` (Dye Depot Indigo Dye); `dye_depot_maroon_dye` (Dye Depot Maroon Dye); `dye_depot_mint_dye` (Dye Depot Mint Dye); `dye_depot_navy_dye` (Dye Depot Navy Dye); `dye_depot_olive_dye` (Dye Depot Olive Dye); `dye_depot_rose_dye` (Dye Depot Rose Dye); `dye_depot_slate_dye` (Dye Depot Slate Dye); `dye_depot_tan_dye` (Dye Depot Tan Dye); `dye_depot_teal_dye` (Dye Depot Teal Dye); `dye_depot_verdant_dye` (Dye Depot Verdant Dye); `dyenamics_amber_dye` (Dyenamics Amber Dye); `dyenamics_aquamarine_dye` (Dyenamics Aquamarine Dye); `dyenamics_bubblegum_dye` (Dyenamics Bubblegum Dye); `dyenamics_cherenkov_dye` (Dyenamics Cherenkov Dye); `dyenamics_conifer_dye` (Dyenamics Conifer Dye); `dyenamics_fluorescent_dye` (Dyenamics Fluorescent Dye); `dyenamics_honey_dye` (Dyenamics Honey Dye); `dyenamics_icy_blue_dye` (Dyenamics Icy Blue Dye); `dyenamics_lavender_dye` (Dyenamics Lavender Dye); `dyenamics_maroon_dye` (Dyenamics Maroon Dye); `dyenamics_mint_dye` (Dyenamics Mint Dye); `dyenamics_navy_dye` (Dyenamics Navy Dye); `dyenamics_peach_dye` (Dyenamics Peach Dye); `dyenamics_persimmon_dye` (Dyenamics Persimmon Dye); `dyenamics_rose_dye` (Dyenamics Rose Dye); `dyenamics_spring_green_dye` (Dyenamics Spring Green Dye); `dyenamics_ultramarine_dye` (Dyenamics Ultramarine Dye); `dyenamics_wine_dye` (Dyenamics Wine Dye); `fluid_hatch` (Fluid Hatch) [facing, waterlogged] — Easily makes your tank interactable with fluid containers.; `gray_dye` (Gray Dye); `green_dye` (Green Dye); `light_blue_dye` (Light Blue Dye); `light_gray_dye` (Light Gray Dye); `lime_dye` (Lime Dye); `magenta_dye` (Magenta Dye); `orange_dye` (Orange Dye); `pink_dye` (Pink Dye); `purple_dye` (Purple Dye); `red_dye` (Red Dye); `white_dye` (White Dye); `yellow_dye` (Yellow Dye)

## railways  (1447 blocks, jar: Steam_Rails-1.7.3_forge-mc1.20.1)

`big_buffer` (Big Buffer) [facing]; `black_brass_wrapped_locometal` (Black Brass Wrapped Locometal); `black_brass_wrapped_locometal_boiler` (Black Brass Wrapped Locometal Boiler) [axis, raised, style]; `black_copper_wrapped_locometal` (Black Copper Wrapped Locometal); `black_copper_wrapped_locometal_boiler` (Black Copper Wrapped Locometal Boiler) [axis, raised, style]; `black_copper_wrapped_locometal_smokebox` (Black Copper Wrapped Locometal Smokebox) [facing]; `black_flat_riveted_locometal` (Flat Black Riveted Locometal); `black_flat_slashed_locometal` (Flat Black Slashed Locometal); `black_folding_locometal_door` (Black Folding Locometal Door) [facing, half, hinge, open, visible, windowed]; `black_four_pane_locometal_window` (Black Four Pane Locometal Window) [axis]; `black_hazard_stripes_chevron_on_black` (Black on Black Chevron) [facing]; `black_hazard_stripes_chevron_on_white` (Black on White Chevron) [facing]; `black_hazard_stripes_diagonal_on_black` (Black on Black Hazard Stripes) [facing]; `black_hazard_stripes_diagonal_on_white` (Black on White Hazard Stripes) [facing]; `black_hinged_locometal_door` (Black Hinged Locometal Door) [facing, half, hinge, open, windowed]; `black_iron_wrapped_locometal` (Black Iron Wrapped Locometal); `black_iron_wrapped_locometal_boiler` (Black Iron Wrapped Locometal Boiler) [axis, raised, style]; `black_iron_wrapped_locometal_smokebox` (Black Iron Wrapped Locometal Smokebox) [facing]; `black_locometal_boiler` (Black Locometal Boiler) [axis, raised, style]; `black_locometal_end_ladder` (Black Locometal End Ladder) [facing, waterlogged]; `black_locometal_flywheel` (Black Locometal Flywheel) [axis]; `black_locometal_pillar` (Black Locometal Pillar) [axis]; `black_locometal_rung_ladder` (Black Locometal Rung Ladder) [facing, waterlogged]; `black_locometal_smokebox` (Black Locometal Smokebox) [facing]; `black_locometal_trapdoor` (Black Locometal Trapdoor) [facing, half, open, windowed]; `black_locometal_vent` (Black Locometal Vent); `black_plated_locometal` (Plated Black Locometal); `black_riveted_locometal` (Black Riveted Locometal); `black_round_pane_locometal_window` (Black Round Pane Locometal Window) [axis]; `black_single_pane_locometal_window` (Black Single Pane Locometal Window) [axis]; `black_slashed_locometal` (Black Slashed Locometal); `black_sliding_locometal_door` (Black Sliding Locometal Door) [facing, half, hinge, open, visible, windowed]; `black_two_pane_locometal_window` (Black Two Pane Locometal Window) [axis]; `black_wrapped_locometal_smokebox` (Black Brass Wrapped Locometal Smokebox) [facing]; `blue_brass_wrapped_locometal` (Blue Brass Wrapped Locometal); `blue_brass_wrapped_locometal_boiler` (Blue Brass Wrapped Locometal Boiler) [axis, raised, style]; `blue_copper_wrapped_locometal` (Blue Copper Wrapped Locometal); `blue_copper_wrapped_locometal_boiler` (Blue Copper Wrapped Locometal Boiler) [axis, raised, style]; `blue_copper_wrapped_locometal_smokebox` (Blue Copper Wrapped Locometal Smokebox) [facing]; `blue_flat_riveted_locometal` (Flat Blue Riveted Locometal); `blue_flat_slashed_locometal` (Flat Blue Slashed Locometal); `blue_folding_locometal_door` (Blue Folding Locometal Door) [facing, half, hinge, open, visible, windowed]; `blue_four_pane_locometal_window` (Blue Four Pane Locometal Window) [axis]; `blue_hazard_stripes_chevron_on_black` (Blue on Black Chevron) [facing]; `blue_hazard_stripes_chevron_on_white` (Blue on White Chevron) [facing]; `blue_hazard_stripes_diagonal_on_black` (Blue on Black Hazard Stripes) [facing]; `blue_hazard_stripes_diagonal_on_white` (Blue on White Hazard Stripes) [facing]; `blue_hinged_locometal_door` (Blue Hinged Locometal Door) [facing, half, hinge, open, windowed]; `blue_iron_wrapped_locometal` (Blue Iron Wrapped Locometal); `blue_iron_wrapped_locometal_boiler` (Blue Iron Wrapped Locometal Boiler) [axis, raised, style]; `blue_iron_wrapped_locometal_smokebox` (Blue Iron Wrapped Locometal Smokebox) [facing]; `blue_locometal_boiler` (Blue Locometal Boiler) [axis, raised, style]; `blue_locometal_end_ladder` (Blue Locometal End Ladder) [facing, waterlogged]; `blue_locometal_flywheel` (Blue Locometal Flywheel) [axis]; `blue_locometal_pillar` (Blue Locometal Pillar) [axis]; `blue_locometal_rung_ladder` (Blue Locometal Rung Ladder) [facing, waterlogged]; `blue_locometal_smokebox` (Blue Locometal Smokebox) [facing]; `blue_locometal_trapdoor` (Blue Locometal Trapdoor) [facing, half, open, windowed]; `blue_locometal_vent` (Blue Locometal Vent); `blue_plated_locometal` (Plated Blue Locometal); `blue_riveted_locometal` (Blue Riveted Locometal); `blue_round_pane_locometal_window` (Blue Round Pane Locometal Window) [axis]; `blue_single_pane_locometal_window` (Blue Single Pane Locometal Window) [axis]; `blue_slashed_locometal` (Blue Slashed Locometal); `blue_sliding_locometal_door` (Blue Sliding Locometal Door) [facing, half, hinge, open, visible, windowed]; `blue_two_pane_locometal_window` (Blue Two Pane Locometal Window) [axis]; `blue_wrapped_locometal_smokebox` (Blue Brass Wrapped Locometal Smokebox) [facing]; `brass_wrapped_locometal` (Brass Wrapped Locometal); `brass_wrapped_locometal_boiler` (Brass Wrapped Locometal Boiler) [axis, raised, style]; `brown_brass_wrapped_locometal` (Brown Brass Wrapped Locometal); `brown_brass_wrapped_locometal_boiler` (Brown Brass Wrapped Locometal Boiler) [axis, raised, style]; `brown_copper_wrapped_locometal` (Brown Copper Wrapped Locometal); `brown_copper_wrapped_locometal_boiler` (Brown Copper Wrapped Locometal Boiler) [axis, raised, style]; `brown_copper_wrapped_locometal_smokebox` (Brown Copper Wrapped Locometal Smokebox) [facing]; `brown_flat_riveted_locometal` (Flat Brown Riveted Locometal); `brown_flat_slashed_locometal` (Flat Brown Slashed Locometal); `brown_folding_locometal_door` (Brown Folding Locometal Door) [facing, half, hinge, open, visible, windowed]; `brown_four_pane_locometal_window` (Brown Four Pane Locometal Window) [axis]; `brown_hazard_stripes_chevron_on_black` (Brown on Black Chevron) [facing]; `brown_hazard_stripes_chevron_on_white` (Brown on White Chevron) [facing]; `brown_hazard_stripes_diagonal_on_black` (Brown on Black Hazard Stripes) [facing]; `brown_hazard_stripes_diagonal_on_white` (Brown on White Hazard Stripes) [facing]; `brown_hinged_locometal_door` (Brown Hinged Locometal Door) [facing, half, hinge, open, windowed]; `brown_iron_wrapped_locometal` (Brown Iron Wrapped Locometal); `brown_iron_wrapped_locometal_boiler` (Brown Iron Wrapped Locometal Boiler) [axis, raised, style]; `brown_iron_wrapped_locometal_smokebox` (Brown Iron Wrapped Locometal Smokebox) [facing]; `brown_locometal_boiler` (Brown Locometal Boiler) [axis, raised, style]; `brown_locometal_end_ladder` (Brown Locometal End Ladder) [facing, waterlogged]; `brown_locometal_flywheel` (Brown Locometal Flywheel) [axis]; `brown_locometal_pillar` (Brown Locometal Pillar) [axis]; `brown_locometal_rung_ladder` (Brown Locometal Rung Ladder) [facing, waterlogged]; `brown_locometal_smokebox` (Brown Locometal Smokebox) [facing]; `brown_locometal_trapdoor` (Brown Locometal Trapdoor) [facing, half, open, windowed]; `brown_locometal_vent` (Brown Locometal Vent); `brown_plated_locometal` (Plated Brown Locometal); `brown_riveted_locometal` (Brown Riveted Locometal); `brown_round_pane_locometal_window` (Brown Round Pane Locometal Window) [axis]; `brown_single_pane_locometal_window` (Brown Single Pane Locometal Window) [axis]; `brown_slashed_locometal` (Brown Slashed Locometal); `brown_sliding_locometal_door` (Brown Sliding Locometal Door) [facing, half, hinge, open, visible, windowed]; `brown_two_pane_locometal_window` (Brown Two Pane Locometal Window) [axis]; `brown_wrapped_locometal_smokebox` (Brown Brass Wrapped Locometal Smokebox) [facing]; `buffer` (Track Buffer) [diagonal, facing, style] — Can be dyed by clicking with dye.; `buffer_mono` (Monorail Track Buffer) [diagonal, facing, style, upside_down]; `buffer_narrow` (Narrow Track Buffer) [diagonal, facing, style]; `buffer_wide` (Wide Track Buffer) [diagonal, facing]; `casing_collision` (Track Casing Collision Block); `chartreuse_brass_wrapped_locometal` (Chartreuse Brass Wrapped Locometal); `chartreuse_brass_wrapped_locometal_boiler` (Chartreuse Brass Wrapped Locometal Boiler) [axis, raised, style]; `chartreuse_copper_wrapped_locometal` (Chartreuse Copper Wrapped Locometal); `chartreuse_copper_wrapped_locometal_boiler` (Chartreuse Copper Wrapped Locometal Boiler) [axis, raised, style]; `chartreuse_copper_wrapped_locometal_smokebox` (Chartreuse Copper Wrapped Locometal Smokebox) [facing]; `chartreuse_flat_riveted_locometal` (Flat Chartreuse Riveted Locometal); `chartreuse_flat_slashed_locometal` (Flat Chartreuse Slashed Locometal); `chartreuse_folding_locometal_door` (Chartreuse Folding Locometal Door) [facing, half, hinge, open, visible, windowed]; `chartreuse_four_pane_locometal_window` (Chartreuse Four Pane Locometal Window) [axis]; `chartreuse_hazard_stripes_chevron_on_black` (Chartreuse on Black Chevron) [facing]; `chartreuse_hazard_stripes_chevron_on_white` (Chartreuse on White Chevron) [facing]; `chartreuse_hazard_stripes_diagonal_on_black` (Chartreuse on Black Hazard Stripes) [facing]; `chartreuse_hazard_stripes_diagonal_on_white` (Chartreuse on White Hazard Stripes) [facing]; `chartreuse_hinged_locometal_door` (Chartreuse Hinged Locometal Door) [facing, half, hinge, open, windowed]; `chartreuse_iron_wrapped_locometal` (Chartreuse Iron Wrapped Locometal); `chartreuse_iron_wrapped_locometal_boiler` (Chartreuse Iron Wrapped Locometal Boiler) [axis, raised, style]; `chartreuse_iron_wrapped_locometal_smokebox` (Chartreuse Iron Wrapped Locometal Smokebox) [facing]; `chartreuse_locometal_boiler` (Chartreuse Locometal Boiler) [axis, raised, style]; `chartreuse_locometal_end_ladder` (Chartreuse Locometal End Ladder) [facing, waterlogged]; `chartreuse_locometal_flywheel` (Chartreuse Locometal Flywheel) [axis]; `chartreuse_locometal_pillar` (Chartreuse Locometal Pillar) [axis]; `chartreuse_locometal_rung_ladder` (Chartreuse Locometal Rung Ladder) [facing, waterlogged]; `chartreuse_locometal_smokebox` (Chartreuse Locometal Smokebox) [facing]; `chartreuse_locometal_trapdoor` (Chartreuse Locometal Trapdoor) [facing, half, open, windowed]; `chartreuse_locometal_vent` (Chartreuse Locometal Vent); `chartreuse_plated_locometal` (Plated Chartreuse Locometal); `chartreuse_riveted_locometal` (Chartreuse Riveted Locometal); `chartreuse_round_pane_locometal_window` (Chartreuse Round Pane Locometal Window) [axis]; `chartreuse_single_pane_locometal_window` (Chartreuse Single Pane Locometal Window) [axis]; `chartreuse_slashed_locometal` (Chartreuse Slashed Locometal); `chartreuse_sliding_locometal_door` (Chartreuse Sliding Locometal Door) [facing, half, hinge, open, visible, windowed]; `chartreuse_two_pane_locometal_window` (Chartreuse Two Pane Locometal Window) [axis]; `chartreuse_wrapped_locometal_smokebox` (Chartreuse Brass Wrapped Locometal Smokebox) [facing]; `conductor_vent` (Vent Block) [conductor_visible]; `conductor_whistle` (Conductor Whistle) — Blow the whistle and the train will arrive shortly! The bound Conductor will drive to the selected destination ignoring its schedule.; `copper_wrapped_locometal` (Copper Wrapped Locometal); `copper_wrapped_locometal_boiler` (Copper Wrapped Locometal Boiler) [axis, raised, style]; `copper_wrapped_locometal_smokebox` (Copper Wrapped Locometal Smokebox) [facing]; `copycat_headstock` (Copycat Headstock) [facing, style, upside_down]; `copycat_headstock_bars` (Copycat Headstock Bars) [facing, upside_down]; `cyan_brass_wrapped_locometal` (Cyan Brass Wrapped Locometal); `cyan_brass_wrapped_locometal_boiler` (Cyan Brass Wrapped Locometal Boiler) [axis, raised, style]; `cyan_copper_wrapped_locometal` (Cyan Copper Wrapped Locometal); `cyan_copper_wrapped_locometal_boiler` (Cyan Copper Wrapped Locometal Boiler) [axis, raised, style]; `cyan_copper_wrapped_locometal_smokebox` (Cyan Copper Wrapped Locometal Smokebox) [facing]; `cyan_flat_riveted_locometal` (Flat Cyan Riveted Locometal); `cyan_flat_slashed_locometal` (Flat Cyan Slashed Locometal); `cyan_folding_locometal_door` (Cyan Folding Locometal Door) [facing, half, hinge, open, visible, windowed]; `cyan_four_pane_locometal_window` (Cyan Four Pane Locometal Window) [axis]; `cyan_hazard_stripes_chevron_on_black` (Cyan on Black Chevron) [facing]; `cyan_hazard_stripes_chevron_on_white` (Cyan on White Chevron) [facing]; `cyan_hazard_stripes_diagonal_on_black` (Cyan on Black Hazard Stripes) [facing]; `cyan_hazard_stripes_diagonal_on_white` (Cyan on White Hazard Stripes) [facing]; `cyan_hinged_locometal_door` (Cyan Hinged Locometal Door) [facing, half, hinge, open, windowed]; `cyan_iron_wrapped_locometal` (Cyan Iron Wrapped Locometal); `cyan_iron_wrapped_locometal_boiler` (Cyan Iron Wrapped Locometal Boiler) [axis, raised, style]; `cyan_iron_wrapped_locometal_smokebox` (Cyan Iron Wrapped Locometal Smokebox) [facing]; `cyan_locometal_boiler` (Cyan Locometal Boiler) [axis, raised, style]; `cyan_locometal_end_ladder` (Cyan Locometal End Ladder) [facing, waterlogged]; `cyan_locometal_flywheel` (Cyan Locometal Flywheel) [axis]; `cyan_locometal_pillar` (Cyan Locometal Pillar) [axis]; `cyan_locometal_rung_ladder` (Cyan Locometal Rung Ladder) [facing, waterlogged]; `cyan_locometal_smokebox` (Cyan Locometal Smokebox) [facing]; `cyan_locometal_trapdoor` (Cyan Locometal Trapdoor) [facing, half, open, windowed]; `cyan_locometal_vent` (Cyan Locometal Vent); `cyan_plated_locometal` (Plated Cyan Locometal); `cyan_riveted_locometal` (Cyan Riveted Locometal); `cyan_round_pane_locometal_window` (Cyan Round Pane Locometal Window) [axis]; `cyan_single_pane_locometal_window` (Cyan Single Pane Locometal Window) [axis]; `cyan_slashed_locometal` (Cyan Slashed Locometal); `cyan_sliding_locometal_door` (Cyan Sliding Locometal Door) [facing, half, hinge, open, visible, windowed]; `cyan_two_pane_locometal_window` (Cyan Two Pane Locometal Window) [axis]; `cyan_wrapped_locometal_smokebox` (Cyan Brass Wrapped Locometal Smokebox) [facing]; `diorite_brass_wrapped_locometal` (Diorite Brass Wrapped Locometal); `diorite_brass_wrapped_locometal_boiler` (Diorite Brass Wrapped Locometal Boiler) [axis, raised, style]; `diorite_copper_wrapped_locometal` (Diorite Copper Wrapped Locometal); `diorite_copper_wrapped_locometal_boiler` (Diorite Copper Wrapped Locometal Boiler) [axis, raised, style]; `diorite_copper_wrapped_locometal_smokebox` (Diorite Copper Wrapped Locometal Smokebox) [facing]; `diorite_flat_riveted_locometal` (Flat Diorite Riveted Locometal); `diorite_flat_slashed_locometal` (Flat Diorite Slashed Locometal); `diorite_folding_locometal_door` (Diorite Folding Locometal Door) [facing, half, hinge, open, visible, windowed]; `diorite_four_pane_locometal_window` (Diorite Four Pane Locometal Window) [axis]; `diorite_hazard_stripes_chevron_on_black` (Diorite on Black Chevron) [facing]; `diorite_hazard_stripes_chevron_on_white` (Diorite on White Chevron) [facing]; `diorite_hazard_stripes_diagonal_on_black` (Diorite on Black Hazard Stripes) [facing]; `diorite_hazard_stripes_diagonal_on_white` (Diorite on White Hazard Stripes) [facing]; `diorite_hinged_locometal_door` (Diorite Hinged Locometal Door) [facing, half, hinge, open, windowed]; `diorite_iron_wrapped_locometal` (Diorite Iron Wrapped Locometal); `diorite_iron_wrapped_locometal_boiler` (Diorite Iron Wrapped Locometal Boiler) [axis, raised, style]; `diorite_iron_wrapped_locometal_smokebox` (Diorite Iron Wrapped Locometal Smokebox) [facing]; `diorite_locometal_boiler` (Diorite Locometal Boiler) [axis, raised, style]; `diorite_locometal_end_ladder` (Diorite Locometal End Ladder) [facing, waterlogged]; `diorite_locometal_flywheel` (Diorite Locometal Flywheel) [axis]; `diorite_locometal_pillar` (Diorite Locometal Pillar) [axis]; `diorite_locometal_rung_ladder` (Diorite Locometal Rung Ladder) [facing, waterlogged]; `diorite_locometal_smokebox` (Diorite Locometal Smokebox) [facing]; `diorite_locometal_trapdoor` (Diorite Locometal Trapdoor) [facing, half, open, windowed]; `diorite_locometal_vent` (Diorite Locometal Vent); `diorite_plated_locometal` (Plated Diorite Locometal); `diorite_riveted_locometal` (Diorite Riveted Locometal); `diorite_round_pane_locometal_window` (Diorite Round Pane Locometal Window) [axis]; `diorite_single_pane_locometal_window` (Diorite Single Pane Locometal Window) [axis]; `diorite_slashed_locometal` (Diorite Slashed Locometal); `diorite_sliding_locometal_door` (Diorite Sliding Locometal Door) [facing, half, hinge, open, visible, windowed]; `diorite_two_pane_locometal_window` (Diorite Two Pane Locometal Window) [axis]; `diorite_wrapped_locometal_smokebox` (Diorite Brass Wrapped Locometal Smokebox) [facing]; `doubleaxle_bogey` (Double Axle Bogey) [axis, waterlogged]; `dripstone_brass_wrapped_locometal` (Dripstone Brass Wrapped Locometal); `dripstone_brass_wrapped_locometal_boiler` (Dripstone Brass Wrapped Locometal Boiler) [axis, raised, style]; `dripstone_copper_wrapped_locometal` (Dripstone Copper Wrapped Locometal); `dripstone_copper_wrapped_locometal_boiler` (Dripstone Copper Wrapped Locometal Boiler) [axis, raised, style]; `dripstone_copper_wrapped_locometal_smokebox` (Dripstone Copper Wrapped Locometal Smokebox) [facing]; `dripstone_flat_riveted_locometal` (Flat Dripstone Riveted Locometal); `dripstone_flat_slashed_locometal` (Flat Dripstone Slashed Locometal); `dripstone_folding_locometal_door` (Dripstone Folding Locometal Door) [facing, half, hinge, open, visible, windowed]; `dripstone_four_pane_locometal_window` (Dripstone Four Pane Locometal Window) [axis]; `dripstone_hazard_stripes_chevron_on_black` (Dripstone on Black Chevron) [facing]; `dripstone_hazard_stripes_chevron_on_white` (Dripstone on White Chevron) [facing]; `dripstone_hazard_stripes_diagonal_on_black` (Dripstone on Black Hazard Stripes) [facing]; `dripstone_hazard_stripes_diagonal_on_white` (Dripstone on White Hazard Stripes) [facing]; `dripstone_hinged_locometal_door` (Dripstone Hinged Locometal Door) [facing, half, hinge, open, windowed]; `dripstone_iron_wrapped_locometal` (Dripstone Iron Wrapped Locometal); `dripstone_iron_wrapped_locometal_boiler` (Dripstone Iron Wrapped Locometal Boiler) [axis, raised, style]; `dripstone_iron_wrapped_locometal_smokebox` (Dripstone Iron Wrapped Locometal Smokebox) [facing]; `dripstone_locometal_boiler` (Dripstone Locometal Boiler) [axis, raised, style]; `dripstone_locometal_end_ladder` (Dripstone Locometal End Ladder) [facing, waterlogged]; `dripstone_locometal_flywheel` (Dripstone Locometal Flywheel) [axis]; `dripstone_locometal_pillar` (Dripstone Locometal Pillar) [axis]; `dripstone_locometal_rung_ladder` (Dripstone Locometal Rung Ladder) [facing, waterlogged]; `dripstone_locometal_smokebox` (Dripstone Locometal Smokebox) [facing]; `dripstone_locometal_trapdoor` (Dripstone Locometal Trapdoor) [facing, half, open, windowed]; `dripstone_locometal_vent` (Dripstone Locometal Vent); `dripstone_plated_locometal` (Plated Dripstone Locometal); `dripstone_riveted_locometal` (Dripstone Riveted Locometal); `dripstone_round_pane_locometal_window` (Dripstone Round Pane Locometal Window) [axis]; `dripstone_single_pane_locometal_window` (Dripstone Single Pane Locometal Window) [axis]; `dripstone_slashed_locometal` (Dripstone Slashed Locometal); `dripstone_sliding_locometal_door` (Dripstone Sliding Locometal Door) [facing, half, hinge, open, visible, windowed]; `dripstone_two_pane_locometal_window` (Dripstone Two Pane Locometal Window) [axis]; `dripstone_wrapped_locometal_smokebox` (Dripstone Brass Wrapped Locometal Smokebox) [facing]; `flat_riveted_locometal` (Flat Riveted Locometal); `flat_slashed_locometal` (Flat Slashed Locometal); `folding_locometal_door` (Folding Locometal Door) [facing, half, hinge, open, visible, windowed]; `four_pane_locometal_window` (Four Pane Locometal Window) [axis]; `fuel_tank` (Fuel Tank) [bottom, shape, top]; `generic_crossing` (Generic Crossing); `granite_brass_wrapped_locometal` (Granite Brass Wrapped Locometal); `granite_brass_wrapped_locometal_boiler` (Granite Brass Wrapped Locometal Boiler) [axis, raised, style]; `granite_copper_wrapped_locometal` (Granite Copper Wrapped Locometal); `granite_copper_wrapped_locometal_boiler` (Granite Copper Wrapped Locometal Boiler) [axis, raised, style]; `granite_copper_wrapped_locometal_smokebox` (Granite Copper Wrapped Locometal Smokebox) [facing]; `granite_flat_riveted_locometal` (Flat Granite Riveted Locometal); `granite_flat_slashed_locometal` (Flat Granite Slashed Locometal); `granite_folding_locometal_door` (Granite Folding Locometal Door) [facing, half, hinge, open, visible, windowed]; `granite_four_pane_locometal_window` (Granite Four Pane Locometal Window) [axis]; `granite_hazard_stripes_chevron_on_black` (Granite on Black Chevron) [facing]; `granite_hazard_stripes_chevron_on_white` (Granite on White Chevron) [facing]; `granite_hazard_stripes_diagonal_on_black` (Granite on Black Hazard Stripes) [facing]; `granite_hazard_stripes_diagonal_on_white` (Granite on White Hazard Stripes) [facing]; `granite_hinged_locometal_door` (Granite Hinged Locometal Door) [facing, half, hinge, open, windowed]; `granite_iron_wrapped_locometal` (Granite Iron Wrapped Locometal); `granite_iron_wrapped_locometal_boiler` (Granite Iron Wrapped Locometal Boiler) [axis, raised, style]; `granite_iron_wrapped_locometal_smokebox` (Granite Iron Wrapped Locometal Smokebox) [facing]; `granite_locometal_boiler` (Granite Locometal Boiler) [axis, raised, style]; `granite_locometal_end_ladder` (Granite Locometal End Ladder) [facing, waterlogged]; `granite_locometal_flywheel` (Granite Locometal Flywheel) [axis]; `granite_locometal_pillar` (Granite Locometal Pillar) [axis]; `granite_locometal_rung_ladder` (Granite Locometal Rung Ladder) [facing, waterlogged]; `granite_locometal_smokebox` (Granite Locometal Smokebox) [facing]; `granite_locometal_trapdoor` (Granite Locometal Trapdoor) [facing, half, open, windowed]; `granite_locometal_vent` (Granite Locometal Vent); `granite_plated_locometal` (Plated Granite Locometal); `granite_riveted_locometal` (Granite Riveted Locometal); `granite_round_pane_locometal_window` (Granite Round Pane Locometal Window) [axis]; `granite_single_pane_locometal_window` (Granite Single Pane Locometal Window) [axis]; `granite_slashed_locometal` (Granite Slashed Locometal); `granite_sliding_locometal_door` (Granite Sliding Locometal Door) [facing, half, hinge, open, visible, windowed]; `granite_two_pane_locometal_window` (Granite Two Pane Locometal Window) [axis]; `granite_wrapped_locometal_smokebox` (Granite Brass Wrapped Locometal Smokebox) [facing]; `gray_brass_wrapped_locometal` (Gray Brass Wrapped Locometal); `gray_brass_wrapped_locometal_boiler` (Gray Brass Wrapped Locometal Boiler) [axis, raised, style]; `gray_copper_wrapped_locometal` (Gray Copper Wrapped Locometal); `gray_copper_wrapped_locometal_boiler` (Gray Copper Wrapped Locometal Boiler) [axis, raised, style]; `gray_copper_wrapped_locometal_smokebox` (Gray Copper Wrapped Locometal Smokebox) [facing]; `gray_flat_riveted_locometal` (Flat Gray Riveted Locometal); `gray_flat_slashed_locometal` (Flat Gray Slashed Locometal); `gray_folding_locometal_door` (Gray Folding Locometal Door) [facing, half, hinge, open, visible, windowed]; `gray_four_pane_locometal_window` (Gray Four Pane Locometal Window) [axis]; `gray_hazard_stripes_chevron_on_black` (Gray on Black Chevron) [facing]; `gray_hazard_stripes_chevron_on_white` (Gray on White Chevron) [facing]; `gray_hazard_stripes_diagonal_on_black` (Gray on Black Hazard Stripes) [facing]; `gray_hazard_stripes_diagonal_on_white` (Gray on White Hazard Stripes) [facing]; `gray_hinged_locometal_door` (Gray Hinged Locometal Door) [facing, half, hinge, open, windowed]; `gray_iron_wrapped_locometal` (Gray Iron Wrapped Locometal); `gray_iron_wrapped_locometal_boiler` (Gray Iron Wrapped Locometal Boiler) [axis, raised, style]; `gray_iron_wrapped_locometal_smokebox` (Gray Iron Wrapped Locometal Smokebox) [facing]; `gray_locometal_boiler` (Gray Locometal Boiler) [axis, raised, style]; `gray_locometal_end_ladder` (Gray Locometal End Ladder) [facing, waterlogged]; `gray_locometal_flywheel` (Gray Locometal Flywheel) [axis]; `gray_locometal_pillar` (Gray Locometal Pillar) [axis]; `gray_locometal_rung_ladder` (Gray Locometal Rung Ladder) [facing, waterlogged]; `gray_locometal_smokebox` (Gray Locometal Smokebox) [facing]; `gray_locometal_trapdoor` (Gray Locometal Trapdoor) [facing, half, open, windowed]; `gray_locometal_vent` (Gray Locometal Vent); `gray_plated_locometal` (Plated Gray Locometal); `gray_riveted_locometal` (Gray Riveted Locometal); `gray_round_pane_locometal_window` (Gray Round Pane Locometal Window) [axis]; `gray_single_pane_locometal_window` (Gray Single Pane Locometal Window) [axis]; `gray_slashed_locometal` (Gray Slashed Locometal); `gray_sliding_locometal_door` (Gray Sliding Locometal Door) [facing, half, hinge, open, visible, windowed]; `gray_two_pane_locometal_window` (Gray Two Pane Locometal Window) [axis]; `gray_wrapped_locometal_smokebox` (Gray Brass Wrapped Locometal Smokebox) [facing]; `green_brass_wrapped_locometal` (Green Brass Wrapped Locometal); `green_brass_wrapped_locometal_boiler` (Green Brass Wrapped Locometal Boiler) [axis, raised, style]; `green_copper_wrapped_locometal` (Green Copper Wrapped Locometal); `green_copper_wrapped_locometal_boiler` (Green Copper Wrapped Locometal Boiler) [axis, raised, style]; `green_copper_wrapped_locometal_smokebox` (Green Copper Wrapped Locometal Smokebox) [facing]; `green_flat_riveted_locometal` (Flat Green Riveted Locometal); `green_flat_slashed_locometal` (Flat Green Slashed Locometal); `green_folding_locometal_door` (Green Folding Locometal Door) [facing, half, hinge, open, visible, windowed]; `green_four_pane_locometal_window` (Green Four Pane Locometal Window) [axis]; `green_hazard_stripes_chevron_on_black` (Green on Black Chevron) [facing]; `green_hazard_stripes_chevron_on_white` (Green on White Chevron) [facing]; `green_hazard_stripes_diagonal_on_black` (Green on Black Hazard Stripes) [facing]; `green_hazard_stripes_diagonal_on_white` (Green on White Hazard Stripes) [facing]; `green_hinged_locometal_door` (Green Hinged Locometal Door) [facing, half, hinge, open, windowed]; `green_iron_wrapped_locometal` (Green Iron Wrapped Locometal); `green_iron_wrapped_locometal_boiler` (Green Iron Wrapped Locometal Boiler) [axis, raised, style]; `green_iron_wrapped_locometal_smokebox` (Green Iron Wrapped Locometal Smokebox) [facing]; `green_locometal_boiler` (Green Locometal Boiler) [axis, raised, style]; `green_locometal_end_ladder` (Green Locometal End Ladder) [facing, waterlogged]; `green_locometal_flywheel` (Green Locometal Flywheel) [axis]; `green_locometal_pillar` (Green Locometal Pillar) [axis]; `green_locometal_rung_ladder` (Green Locometal Rung Ladder) [facing, waterlogged]; `green_locometal_smokebox` (Green Locometal Smokebox) [facing]; `green_locometal_trapdoor` (Green Locometal Trapdoor) [facing, half, open, windowed]; `green_locometal_vent` (Green Locometal Vent); `green_plated_locometal` (Plated Green Locometal); `green_riveted_locometal` (Green Riveted Locometal); `green_round_pane_locometal_window` (Green Round Pane Locometal Window) [axis]; `green_single_pane_locometal_window` (Green Single Pane Locometal Window) [axis]; `green_slashed_locometal` (Green Slashed Locometal); `green_sliding_locometal_door` (Green Sliding Locometal Door) [facing, half, hinge, open, visible, windowed]; `green_two_pane_locometal_window` (Green Two Pane Locometal Window) [axis]; `green_wrapped_locometal_smokebox` (Green Brass Wrapped Locometal Smokebox) [facing]; `handcar` (Handcar) [axis, waterlogged] — Use a wrench to pick up.; `hazard_stripes_chevron_on_black` (Locometal on Black Chevron) [facing]; `hazard_stripes_chevron_on_white` (Locometal on White Chevron) [facing]; `hazard_stripes_diagonal_on_black` (Locometal on Black Hazard Stripes) [facing]; `hazard_stripes_diagonal_on_white` (Locometal on White Hazard Stripes) [facing]; `headstock` (Headstock) [facing, style, upside_down] — Press and hold the Open Cycle Menu key (default ALT) to select variants of this block. Can be dyed by clicking with dye.; `hinged_locometal_door` (Hinged Locometal Door) [facing, half, hinge, open, windowed]; `invisible_bogey` (Invisible Bogey) [axis, waterlogged]; `invisible_mono_bogey` (Invisible Mono Bogey) [axis, upside_down, waterlogged]; `iron_wrapped_locometal` (Iron Wrapped Locometal); `iron_wrapped_locometal_boiler` (Iron Wrapped Locometal Boiler) [axis, raised, style]; `iron_wrapped_locometal_smokebox` (Iron Wrapped Locometal Smokebox) [facing]; `large_create_styled_0_10_0` (Large Create Styled 0-10-0) [axis, waterlogged]; `large_create_styled_0_12_0` (Large Create Styled 0-12-0) [axis, waterlogged]; `large_create_styled_0_4_0` (Large Create Styled 0-4-0) [axis, waterlogged]; `large_create_styled_0_6_0` (Large Create Styled 0-6-0) [axis, waterlogged]; `large_create_styled_0_8_0` (Large Create Styled 0-8-0) [axis, waterlogged]; `large_platform_doubleaxle_bogey` (Large Platform Double Axle Bogey) [axis, waterlogged]; `light_blue_brass_wrapped_locometal` (Light Blue Brass Wrapped Locometal); `light_blue_brass_wrapped_locometal_boiler` (Light Blue Brass Wrapped Locometal Boiler) [axis, raised, style]; `light_blue_copper_wrapped_locometal` (Light Blue Copper Wrapped Locometal); `light_blue_copper_wrapped_locometal_boiler` (Light Blue Copper Wrapped Locometal Boiler) [axis, raised, style]; `light_blue_copper_wrapped_locometal_smokebox` (Light Blue Copper Wrapped Locometal Smokebox) [facing]; `light_blue_flat_riveted_locometal` (Flat Light Blue Riveted Locometal); `light_blue_flat_slashed_locometal` (Flat Light Blue Slashed Locometal); `light_blue_folding_locometal_door` (Light Blue Folding Locometal Door) [facing, half, hinge, open, visible, windowed]; `light_blue_four_pane_locometal_window` (Light Blue Four Pane Locometal Window) [axis]; `light_blue_hazard_stripes_chevron_on_black` (Light Blue on Black Chevron) [facing]; `light_blue_hazard_stripes_chevron_on_white` (Light Blue on White Chevron) [facing]; `light_blue_hazard_stripes_diagonal_on_black` (Light Blue on Black Hazard Stripes) [facing]; `light_blue_hazard_stripes_diagonal_on_white` (Light Blue on White Hazard Stripes) [facing]; `light_blue_hinged_locometal_door` (Light Blue Hinged Locometal Door) [facing, half, hinge, open, windowed]; `light_blue_iron_wrapped_locometal` (Light Blue Iron Wrapped Locometal); `light_blue_iron_wrapped_locometal_boiler` (Light Blue Iron Wrapped Locometal Boiler) [axis, raised, style]; `light_blue_iron_wrapped_locometal_smokebox` (Light Blue Iron Wrapped Locometal Smokebox) [facing]; `light_blue_locometal_boiler` (Light Blue Locometal Boiler) [axis, raised, style]; `light_blue_locometal_end_ladder` (Light Blue Locometal End Ladder) [facing, waterlogged]; `light_blue_locometal_flywheel` (Light Blue Locometal Flywheel) [axis]; `light_blue_locometal_pillar` (Light Blue Locometal Pillar) [axis]; `light_blue_locometal_rung_ladder` (Light Blue Locometal Rung Ladder) [facing, waterlogged]; `light_blue_locometal_smokebox` (Light Blue Locometal Smokebox) [facing]; `light_blue_locometal_trapdoor` (Light Blue Locometal Trapdoor) [facing, half, open, windowed]; `light_blue_locometal_vent` (Light Blue Locometal Vent); `light_blue_plated_locometal` (Plated Light Blue Locometal); `light_blue_riveted_locometal` (Light Blue Riveted Locometal); `light_blue_round_pane_locometal_window` (Light Blue Round Pane Locometal Window) [axis]; `light_blue_single_pane_locometal_window` (Light Blue Single Pane Locometal Window) [axis]; `light_blue_slashed_locometal` (Light Blue Slashed Locometal); `light_blue_sliding_locometal_door` (Light Blue Sliding Locometal Door) [facing, half, hinge, open, visible, windowed]; `light_blue_two_pane_locometal_window` (Light Blue Two Pane Locometal Window) [axis]; `light_blue_wrapped_locometal_smokebox` (Light Blue Brass Wrapped Locometal Smokebox) [facing]; `light_gray_brass_wrapped_locometal` (Light Gray Brass Wrapped Locometal); `light_gray_brass_wrapped_locometal_boiler` (Light Gray Brass Wrapped Locometal Boiler) [axis, raised, style]; `light_gray_copper_wrapped_locometal` (Light Gray Copper Wrapped Locometal); `light_gray_copper_wrapped_locometal_boiler` (Light Gray Copper Wrapped Locometal Boiler) [axis, raised, style]; `light_gray_copper_wrapped_locometal_smokebox` (Light Gray Copper Wrapped Locometal Smokebox) [facing]; `light_gray_flat_riveted_locometal` (Flat Light Gray Riveted Locometal); `light_gray_flat_slashed_locometal` (Flat Light Gray Slashed Locometal); `light_gray_folding_locometal_door` (Light Gray Folding Locometal Door) [facing, half, hinge, open, visible, windowed]; `light_gray_four_pane_locometal_window` (Light Gray Four Pane Locometal Window) [axis]; `light_gray_hazard_stripes_chevron_on_black` (Light Gray on Black Chevron) [facing]; `light_gray_hazard_stripes_chevron_on_white` (Light Gray on White Chevron) [facing]; `light_gray_hazard_stripes_diagonal_on_black` (Light Gray on Black Hazard Stripes) [facing]; `light_gray_hazard_stripes_diagonal_on_white` (Light Gray on White Hazard Stripes) [facing]; `light_gray_hinged_locometal_door` (Light Gray Hinged Locometal Door) [facing, half, hinge, open, windowed]; `light_gray_iron_wrapped_locometal` (Light Gray Iron Wrapped Locometal); `light_gray_iron_wrapped_locometal_boiler` (Light Gray Iron Wrapped Locometal Boiler) [axis, raised, style]; `light_gray_iron_wrapped_locometal_smokebox` (Light Gray Iron Wrapped Locometal Smokebox) [facing]; `light_gray_locometal_boiler` (Light Gray Locometal Boiler) [axis, raised, style]; `light_gray_locometal_end_ladder` (Light Gray Locometal End Ladder) [facing, waterlogged]; `light_gray_locometal_flywheel` (Light Gray Locometal Flywheel) [axis]; `light_gray_locometal_pillar` (Light Gray Locometal Pillar) [axis]; `light_gray_locometal_rung_ladder` (Light Gray Locometal Rung Ladder) [facing, waterlogged]; `light_gray_locometal_smokebox` (Light Gray Locometal Smokebox) [facing]; `light_gray_locometal_trapdoor` (Light Gray Locometal Trapdoor) [facing, half, open, windowed]; `light_gray_locometal_vent` (Light Gray Locometal Vent); `light_gray_plated_locometal` (Plated Light Gray Locometal); `light_gray_riveted_locometal` (Light Gray Riveted Locometal); `light_gray_round_pane_locometal_window` (Light Gray Round Pane Locometal Window) [axis]; `light_gray_single_pane_locometal_window` (Light Gray Single Pane Locometal Window) [axis]; `light_gray_slashed_locometal` (Light Gray Slashed Locometal); `light_gray_sliding_locometal_door` (Light Gray Sliding Locometal Door) [facing, half, hinge, open, visible, windowed]; `light_gray_two_pane_locometal_window` (Light Gray Two Pane Locometal Window) [axis]; `light_gray_wrapped_locometal_smokebox` (Light Gray Brass Wrapped Locometal Smokebox) [facing]; `lime_brass_wrapped_locometal` (Lime Brass Wrapped Locometal); `lime_brass_wrapped_locometal_boiler` (Lime Brass Wrapped Locometal Boiler) [axis, raised, style]; `lime_copper_wrapped_locometal` (Lime Copper Wrapped Locometal); `lime_copper_wrapped_locometal_boiler` (Lime Copper Wrapped Locometal Boiler) [axis, raised, style]; `lime_copper_wrapped_locometal_smokebox` (Lime Copper Wrapped Locometal Smokebox) [facing]; `lime_flat_riveted_locometal` (Flat Lime Riveted Locometal); `lime_flat_slashed_locometal` (Flat Lime Slashed Locometal); `lime_folding_locometal_door` (Lime Folding Locometal Door) [facing, half, hinge, open, visible, windowed]; `lime_four_pane_locometal_window` (Lime Four Pane Locometal Window) [axis]; `lime_hazard_stripes_chevron_on_black` (Lime on Black Chevron) [facing]; `lime_hazard_stripes_chevron_on_white` (Lime on White Chevron) [facing]; `lime_hazard_stripes_diagonal_on_black` (Lime on Black Hazard Stripes) [facing]; `lime_hazard_stripes_diagonal_on_white` (Lime on White Hazard Stripes) [facing]; `lime_hinged_locometal_door` (Lime Hinged Locometal Door) [facing, half, hinge, open, windowed]; `lime_iron_wrapped_locometal` (Lime Iron Wrapped Locometal); `lime_iron_wrapped_locometal_boiler` (Lime Iron Wrapped Locometal Boiler) [axis, raised, style]; `lime_iron_wrapped_locometal_smokebox` (Lime Iron Wrapped Locometal Smokebox) [facing]; `lime_locometal_boiler` (Lime Locometal Boiler) [axis, raised, style]; `lime_locometal_end_ladder` (Lime Locometal End Ladder) [facing, waterlogged]; `lime_locometal_flywheel` (Lime Locometal Flywheel) [axis]; `lime_locometal_pillar` (Lime Locometal Pillar) [axis]; `lime_locometal_rung_ladder` (Lime Locometal Rung Ladder) [facing, waterlogged]; `lime_locometal_smokebox` (Lime Locometal Smokebox) [facing]; `lime_locometal_trapdoor` (Lime Locometal Trapdoor) [facing, half, open, windowed]; `lime_locometal_vent` (Lime Locometal Vent); `lime_plated_locometal` (Plated Lime Locometal); `lime_riveted_locometal` (Lime Riveted Locometal); `lime_round_pane_locometal_window` (Lime Round Pane Locometal Window) [axis]; `lime_single_pane_locometal_window` (Lime Single Pane Locometal Window) [axis]; `lime_slashed_locometal` (Lime Slashed Locometal); `lime_sliding_locometal_door` (Lime Sliding Locometal Door) [facing, half, hinge, open, visible, windowed]; `lime_two_pane_locometal_window` (Lime Two Pane Locometal Window) [axis]; `lime_wrapped_locometal_smokebox` (Lime Brass Wrapped Locometal Smokebox) [facing]; `limestone_brass_wrapped_locometal` (Limestone Brass Wrapped Locometal); `limestone_brass_wrapped_locometal_boiler` (Limestone Brass Wrapped Locometal Boiler) [axis, raised, style]; `limestone_copper_wrapped_locometal` (Limestone Copper Wrapped Locometal); `limestone_copper_wrapped_locometal_boiler` (Limestone Copper Wrapped Locometal Boiler) [axis, raised, style]; `limestone_copper_wrapped_locometal_smokebox` (Limestone Copper Wrapped Locometal Smokebox) [facing]; `limestone_flat_riveted_locometal` (Flat Limestone Riveted Locometal); `limestone_flat_slashed_locometal` (Flat Limestone Slashed Locometal); `limestone_folding_locometal_door` (Limestone Folding Locometal Door) [facing, half, hinge, open, visible, windowed]; `limestone_four_pane_locometal_window` (Limestone Four Pane Locometal Window) [axis]; `limestone_hazard_stripes_chevron_on_black` (Limestone on Black Chevron) [facing]; `limestone_hazard_stripes_chevron_on_white` (Limestone on White Chevron) [facing]; `limestone_hazard_stripes_diagonal_on_black` (Limestone on Black Hazard Stripes) [facing]; `limestone_hazard_stripes_diagonal_on_white` (Limestone on White Hazard Stripes) [facing]; `limestone_hinged_locometal_door` (Limestone Hinged Locometal Door) [facing, half, hinge, open, windowed]; `limestone_iron_wrapped_locometal` (Limestone Iron Wrapped Locometal); `limestone_iron_wrapped_locometal_boiler` (Limestone Iron Wrapped Locometal Boiler) [axis, raised, style]; `limestone_iron_wrapped_locometal_smokebox` (Limestone Iron Wrapped Locometal Smokebox) [facing]; `limestone_locometal_boiler` (Limestone Locometal Boiler) [axis, raised, style]; `limestone_locometal_end_ladder` (Limestone Locometal End Ladder) [facing, waterlogged]; `limestone_locometal_flywheel` (Limestone Locometal Flywheel) [axis]; `limestone_locometal_pillar` (Limestone Locometal Pillar) [axis]; `limestone_locometal_rung_ladder` (Limestone Locometal Rung Ladder) [facing, waterlogged]; `limestone_locometal_smokebox` (Limestone Locometal Smokebox) [facing]; `limestone_locometal_trapdoor` (Limestone Locometal Trapdoor) [facing, half, open, windowed]; `limestone_locometal_vent` (Limestone Locometal Vent); `limestone_plated_locometal` (Plated Limestone Locometal); `limestone_riveted_locometal` (Limestone Riveted Locometal); `limestone_round_pane_locometal_window` (Limestone Round Pane Locometal Window) [axis]; `limestone_single_pane_locometal_window` (Limestone Single Pane Locometal Window) [axis]; `limestone_slashed_locometal` (Limestone Slashed Locometal); `limestone_sliding_locometal_door` (Limestone Sliding Locometal Door) [facing, half, hinge, open, visible, windowed]; `limestone_two_pane_locometal_window` (Limestone Two Pane Locometal Window) [axis]; `limestone_wrapped_locometal_smokebox` (Limestone Brass Wrapped Locometal Smokebox) [facing]; `link_and_pin` (Deco Coupler) [facing, style]; `locometal_boiler` (Locometal Boiler) [axis, raised, style]; `locometal_end_ladder` (Locometal End Ladder) [facing, waterlogged]; `locometal_flywheel` (Locometal Flywheel) [axis]; `locometal_pillar` (Locometal Pillar) [axis]; `locometal_rung_ladder` (Locometal Rung Ladder) [facing, waterlogged]; `locometal_smokebox` (Locometal Smokebox) [facing]; `locometal_trapdoor` (Locometal Trapdoor) [facing, half, open, windowed]; `locometal_vent` (Locometal Vent); `magenta_brass_wrapped_locometal` (Magenta Brass Wrapped Locometal); `magenta_brass_wrapped_locometal_boiler` (Magenta Brass Wrapped Locometal Boiler) [axis, raised, style]; `magenta_copper_wrapped_locometal` (Magenta Copper Wrapped Locometal); `magenta_copper_wrapped_locometal_boiler` (Magenta Copper Wrapped Locometal Boiler) [axis, raised, style]; `magenta_copper_wrapped_locometal_smokebox` (Magenta Copper Wrapped Locometal Smokebox) [facing]; `magenta_flat_riveted_locometal` (Flat Magenta Riveted Locometal); `magenta_flat_slashed_locometal` (Flat Magenta Slashed Locometal); `magenta_folding_locometal_door` (Magenta Folding Locometal Door) [facing, half, hinge, open, visible, windowed]; `magenta_four_pane_locometal_window` (Magenta Four Pane Locometal Window) [axis]; `magenta_hazard_stripes_chevron_on_black` (Magenta on Black Chevron) [facing]; `magenta_hazard_stripes_chevron_on_white` (Magenta on White Chevron) [facing]; `magenta_hazard_stripes_diagonal_on_black` (Magenta on Black Hazard Stripes) [facing]; `magenta_hazard_stripes_diagonal_on_white` (Magenta on White Hazard Stripes) [facing]; `magenta_hinged_locometal_door` (Magenta Hinged Locometal Door) [facing, half, hinge, open, windowed]; `magenta_iron_wrapped_locometal` (Magenta Iron Wrapped Locometal); `magenta_iron_wrapped_locometal_boiler` (Magenta Iron Wrapped Locometal Boiler) [axis, raised, style]; `magenta_iron_wrapped_locometal_smokebox` (Magenta Iron Wrapped Locometal Smokebox) [facing]; `magenta_locometal_boiler` (Magenta Locometal Boiler) [axis, raised, style]; `magenta_locometal_end_ladder` (Magenta Locometal End Ladder) [facing, waterlogged]; `magenta_locometal_flywheel` (Magenta Locometal Flywheel) [axis]; `magenta_locometal_pillar` (Magenta Locometal Pillar) [axis]; `magenta_locometal_rung_ladder` (Magenta Locometal Rung Ladder) [facing, waterlogged]; `magenta_locometal_smokebox` (Magenta Locometal Smokebox) [facing]; `magenta_locometal_trapdoor` (Magenta Locometal Trapdoor) [facing, half, open, windowed]; `magenta_locometal_vent` (Magenta Locometal Vent); `magenta_plated_locometal` (Plated Magenta Locometal); `magenta_riveted_locometal` (Magenta Riveted Locometal); `magenta_round_pane_locometal_window` (Magenta Round Pane Locometal Window) [axis]; `magenta_single_pane_locometal_window` (Magenta Single Pane Locometal Window) [axis]; `magenta_slashed_locometal` (Magenta Slashed Locometal); `magenta_sliding_locometal_door` (Magenta Sliding Locometal Door) [facing, half, hinge, open, visible, windowed]; `magenta_two_pane_locometal_window` (Magenta Two Pane Locometal Window) [axis]; `magenta_wrapped_locometal_smokebox` (Magenta Brass Wrapped Locometal Smokebox) [facing]; `maroon_brass_wrapped_locometal` (Maroon Brass Wrapped Locometal); `maroon_brass_wrapped_locometal_boiler` (Maroon Brass Wrapped Locometal Boiler) [axis, raised, style]; `maroon_copper_wrapped_locometal` (Maroon Copper Wrapped Locometal); `maroon_copper_wrapped_locometal_boiler` (Maroon Copper Wrapped Locometal Boiler) [axis, raised, style]; `maroon_copper_wrapped_locometal_smokebox` (Maroon Copper Wrapped Locometal Smokebox) [facing]; `maroon_flat_riveted_locometal` (Flat Maroon Riveted Locometal); `maroon_flat_slashed_locometal` (Flat Maroon Slashed Locometal); `maroon_folding_locometal_door` (Maroon Folding Locometal Door) [facing, half, hinge, open, visible, windowed]; `maroon_four_pane_locometal_window` (Maroon Four Pane Locometal Window) [axis]; `maroon_hazard_stripes_chevron_on_black` (Maroon on Black Chevron) [facing]; `maroon_hazard_stripes_chevron_on_white` (Maroon on White Chevron) [facing]; `maroon_hazard_stripes_diagonal_on_black` (Maroon on Black Hazard Stripes) [facing]; `maroon_hazard_stripes_diagonal_on_white` (Maroon on White Hazard Stripes) [facing]; `maroon_hinged_locometal_door` (Maroon Hinged Locometal Door) [facing, half, hinge, open, windowed]; `maroon_iron_wrapped_locometal` (Maroon Iron Wrapped Locometal); `maroon_iron_wrapped_locometal_boiler` (Maroon Iron Wrapped Locometal Boiler) [axis, raised, style]; `maroon_iron_wrapped_locometal_smokebox` (Maroon Iron Wrapped Locometal Smokebox) [facing]; `maroon_locometal_boiler` (Maroon Locometal Boiler) [axis, raised, style]; `maroon_locometal_end_ladder` (Maroon Locometal End Ladder) [facing, waterlogged]; `maroon_locometal_flywheel` (Maroon Locometal Flywheel) [axis]; `maroon_locometal_pillar` (Maroon Locometal Pillar) [axis]; `maroon_locometal_rung_ladder` (Maroon Locometal Rung Ladder) [facing, waterlogged]; `maroon_locometal_smokebox` (Maroon Locometal Smokebox) [facing]; `maroon_locometal_trapdoor` (Maroon Locometal Trapdoor) [facing, half, open, windowed]; `maroon_locometal_vent` (Maroon Locometal Vent); `maroon_plated_locometal` (Plated Maroon Locometal); `maroon_riveted_locometal` (Maroon Riveted Locometal); `maroon_round_pane_locometal_window` (Maroon Round Pane Locometal Window) [axis]; `maroon_single_pane_locometal_window` (Maroon Single Pane Locometal Window) [axis]; `maroon_slashed_locometal` (Maroon Slashed Locometal); `maroon_sliding_locometal_door` (Maroon Sliding Locometal Door) [facing, half, hinge, open, visible, windowed]; `maroon_two_pane_locometal_window` (Maroon Two Pane Locometal Window) [axis]; `maroon_wrapped_locometal_smokebox` (Maroon Brass Wrapped Locometal Smokebox) [facing]; `medium_2_0_2_trailing` (Medium 2-0-2 Trailing Bogey) [axis, waterlogged]; `medium_4_0_4_trailing` (Medium 4-0-4 Trailing Bogey) [axis, waterlogged]; `medium_bogey` (Medium Bogey) [axis, waterlogged]; `medium_quadruple_wheel` (Medium Quadruple Wheel Bogey) [axis, waterlogged]; `medium_quintuple_wheel` (Medium Quintuple Wheel Bogey) [axis, waterlogged]; `medium_triple_wheel` (Medium Triple Wheel Bogey) [axis, waterlogged]; `mono_bogey` (Monorail Bogey) [axis, upside_down, waterlogged]; `narrow_double_scotch_bogey` (Narrow Gauge Double Scotch Yoke Bogey) [axis, waterlogged]; `narrow_scotch_bogey` (Narrow Gauge Scotch Yoke Bogey) [axis, waterlogged]; `narrow_small_bogey` (Narrow Gauge Small Bogey) [axis, waterlogged]; `ochrum_brass_wrapped_locometal` (Ochrum Brass Wrapped Locometal); `ochrum_brass_wrapped_locometal_boiler` (Ochrum Brass Wrapped Locometal Boiler) [axis, raised, style]; `ochrum_copper_wrapped_locometal` (Ochrum Copper Wrapped Locometal); `ochrum_copper_wrapped_locometal_boiler` (Ochrum Copper Wrapped Locometal Boiler) [axis, raised, style]; `ochrum_copper_wrapped_locometal_smokebox` (Ochrum Copper Wrapped Locometal Smokebox) [facing]; `ochrum_flat_riveted_locometal` (Flat Ochrum Riveted Locometal); `ochrum_flat_slashed_locometal` (Flat Ochrum Slashed Locometal); `ochrum_folding_locometal_door` (Ochrum Folding Locometal Door) [facing, half, hinge, open, visible, windowed]; `ochrum_four_pane_locometal_window` (Ochrum Four Pane Locometal Window) [axis]; `ochrum_hazard_stripes_chevron_on_black` (Ochrum on Black Chevron) [facing]; `ochrum_hazard_stripes_chevron_on_white` (Ochrum on White Chevron) [facing]; `ochrum_hazard_stripes_diagonal_on_black` (Ochrum on Black Hazard Stripes) [facing]; `ochrum_hazard_stripes_diagonal_on_white` (Ochrum on White Hazard Stripes) [facing]; `ochrum_hinged_locometal_door` (Ochrum Hinged Locometal Door) [facing, half, hinge, open, windowed]; `ochrum_iron_wrapped_locometal` (Ochrum Iron Wrapped Locometal); `ochrum_iron_wrapped_locometal_boiler` (Ochrum Iron Wrapped Locometal Boiler) [axis, raised, style]; `ochrum_iron_wrapped_locometal_smokebox` (Ochrum Iron Wrapped Locometal Smokebox) [facing]; `ochrum_locometal_boiler` (Ochrum Locometal Boiler) [axis, raised, style]; `ochrum_locometal_end_ladder` (Ochrum Locometal End Ladder) [facing, waterlogged]; `ochrum_locometal_flywheel` (Ochrum Locometal Flywheel) [axis]; `ochrum_locometal_pillar` (Ochrum Locometal Pillar) [axis]; `ochrum_locometal_rung_ladder` (Ochrum Locometal Rung Ladder) [facing, waterlogged]; `ochrum_locometal_smokebox` (Ochrum Locometal Smokebox) [facing]; `ochrum_locometal_trapdoor` (Ochrum Locometal Trapdoor) [facing, half, open, windowed]; `ochrum_locometal_vent` (Ochrum Locometal Vent); `ochrum_plated_locometal` (Plated Ochrum Locometal); `ochrum_riveted_locometal` (Ochrum Riveted Locometal); `ochrum_round_pane_locometal_window` (Ochrum Round Pane Locometal Window) [axis]; `ochrum_single_pane_locometal_window` (Ochrum Single Pane Locometal Window) [axis]; `ochrum_slashed_locometal` (Ochrum Slashed Locometal); `ochrum_sliding_locometal_door` (Ochrum Sliding Locometal Door) [facing, half, hinge, open, visible, windowed]; `ochrum_two_pane_locometal_window` (Ochrum Two Pane Locometal Window) [axis]; `ochrum_wrapped_locometal_smokebox` (Ochrum Brass Wrapped Locometal Smokebox) [facing]; `olive_green_brass_wrapped_locometal` (Olive Green Brass Wrapped Locometal); `olive_green_brass_wrapped_locometal_boiler` (Olive Green Brass Wrapped Locometal Boiler) [axis, raised, style]; `olive_green_copper_wrapped_locometal` (Olive Green Copper Wrapped Locometal); `olive_green_copper_wrapped_locometal_boiler` (Olive Green Copper Wrapped Locometal Boiler) [axis, raised, style]; `olive_green_copper_wrapped_locometal_smokebox` (Olive Green Copper Wrapped Locometal Smokebox) [facing]; `olive_green_flat_riveted_locometal` (Flat Olive Green Riveted Locometal); `olive_green_flat_slashed_locometal` (Flat Olive Green Slashed Locometal); `olive_green_folding_locometal_door` (Olive Green Folding Locometal Door) [facing, half, hinge, open, visible, windowed]; `olive_green_four_pane_locometal_window` (Olive Green Four Pane Locometal Window) [axis]; `olive_green_hazard_stripes_chevron_on_black` (Olive Green on Black Chevron) [facing]; `olive_green_hazard_stripes_chevron_on_white` (Olive Green on White Chevron) [facing]; `olive_green_hazard_stripes_diagonal_on_black` (Olive Green on Black Hazard Stripes) [facing]; `olive_green_hazard_stripes_diagonal_on_white` (Olive Green on White Hazard Stripes) [facing]; `olive_green_hinged_locometal_door` (Olive Green Hinged Locometal Door) [facing, half, hinge, open, windowed]; `olive_green_iron_wrapped_locometal` (Olive Green Iron Wrapped Locometal); `olive_green_iron_wrapped_locometal_boiler` (Olive Green Iron Wrapped Locometal Boiler) [axis, raised, style]; `olive_green_iron_wrapped_locometal_smokebox` (Olive Green Iron Wrapped Locometal Smokebox) [facing]; `olive_green_locometal_boiler` (Olive Green Locometal Boiler) [axis, raised, style]; `olive_green_locometal_end_ladder` (Olive Green Locometal End Ladder) [facing, waterlogged]; `olive_green_locometal_flywheel` (Olive Green Locometal Flywheel) [axis]; `olive_green_locometal_pillar` (Olive Green Locometal Pillar) [axis]; `olive_green_locometal_rung_ladder` (Olive Green Locometal Rung Ladder) [facing, waterlogged]; `olive_green_locometal_smokebox` (Olive Green Locometal Smokebox) [facing]; `olive_green_locometal_trapdoor` (Olive Green Locometal Trapdoor) [facing, half, open, windowed]; `olive_green_locometal_vent` (Olive Green Locometal Vent); `olive_green_plated_locometal` (Plated Olive Green Locometal); `olive_green_riveted_locometal` (Olive Green Riveted Locometal); `olive_green_round_pane_locometal_window` (Olive Green Round Pane Locometal Window) [axis]; `olive_green_single_pane_locometal_window` (Olive Green Single Pane Locometal Window) [axis]; `olive_green_slashed_locometal` (Olive Green Slashed Locometal); `olive_green_sliding_locometal_door` (Olive Green Sliding Locometal Door) [facing, half, hinge, open, visible, windowed]; `olive_green_two_pane_locometal_window` (Olive Green Two Pane Locometal Window) [axis]; `olive_green_wrapped_locometal_smokebox` (Olive Green Brass Wrapped Locometal Smokebox) [facing]; `orange_brass_wrapped_locometal` (Orange Brass Wrapped Locometal); `orange_brass_wrapped_locometal_boiler` (Orange Brass Wrapped Locometal Boiler) [axis, raised, style]; `orange_copper_wrapped_locometal` (Orange Copper Wrapped Locometal); `orange_copper_wrapped_locometal_boiler` (Orange Copper Wrapped Locometal Boiler) [axis, raised, style]; `orange_copper_wrapped_locometal_smokebox` (Orange Copper Wrapped Locometal Smokebox) [facing]; `orange_flat_riveted_locometal` (Flat Orange Riveted Locometal); `orange_flat_slashed_locometal` (Flat Orange Slashed Locometal); `orange_folding_locometal_door` (Orange Folding Locometal Door) [facing, half, hinge, open, visible, windowed]; `orange_four_pane_locometal_window` (Orange Four Pane Locometal Window) [axis]; `orange_hazard_stripes_chevron_on_black` (Orange on Black Chevron) [facing]; `orange_hazard_stripes_chevron_on_white` (Orange on White Chevron) [facing]; `orange_hazard_stripes_diagonal_on_black` (Orange on Black Hazard Stripes) [facing]; `orange_hazard_stripes_diagonal_on_white` (Orange on White Hazard Stripes) [facing]; `orange_hinged_locometal_door` (Orange Hinged Locometal Door) [facing, half, hinge, open, windowed]; `orange_iron_wrapped_locometal` (Orange Iron Wrapped Locometal); `orange_iron_wrapped_locometal_boiler` (Orange Iron Wrapped Locometal Boiler) [axis, raised, style]; `orange_iron_wrapped_locometal_smokebox` (Orange Iron Wrapped Locometal Smokebox) [facing]; `orange_locometal_boiler` (Orange Locometal Boiler) [axis, raised, style]; `orange_locometal_end_ladder` (Orange Locometal End Ladder) [facing, waterlogged]; `orange_locometal_flywheel` (Orange Locometal Flywheel) [axis]; `orange_locometal_pillar` (Orange Locometal Pillar) [axis]; `orange_locometal_rung_ladder` (Orange Locometal Rung Ladder) [facing, waterlogged]; `orange_locometal_smokebox` (Orange Locometal Smokebox) [facing]; `orange_locometal_trapdoor` (Orange Locometal Trapdoor) [facing, half, open, windowed]; `orange_locometal_vent` (Orange Locometal Vent); `orange_plated_locometal` (Plated Orange Locometal); `orange_riveted_locometal` (Orange Riveted Locometal); `orange_round_pane_locometal_window` (Orange Round Pane Locometal Window) [axis]; `orange_single_pane_locometal_window` (Orange Single Pane Locometal Window) [axis]; `orange_slashed_locometal` (Orange Slashed Locometal); `orange_sliding_locometal_door` (Orange Sliding Locometal Door) [facing, half, hinge, open, visible, windowed]; `orange_two_pane_locometal_window` (Orange Two Pane Locometal Window) [axis]; `orange_wrapped_locometal_smokebox` (Orange Brass Wrapped Locometal Smokebox) [facing]; `pine_green_brass_wrapped_locometal` (Pine Green Brass Wrapped Locometal); `pine_green_brass_wrapped_locometal_boiler` (Pine Green Brass Wrapped Locometal Boiler) [axis, raised, style]; `pine_green_copper_wrapped_locometal` (Pine Green Copper Wrapped Locometal); `pine_green_copper_wrapped_locometal_boiler` (Pine Green Copper Wrapped Locometal Boiler) [axis, raised, style]; `pine_green_copper_wrapped_locometal_smokebox` (Pine Green Copper Wrapped Locometal Smokebox) [facing]; `pine_green_flat_riveted_locometal` (Flat Pine Green Riveted Locometal); `pine_green_flat_slashed_locometal` (Flat Pine Green Slashed Locometal); `pine_green_folding_locometal_door` (Pine Green Folding Locometal Door) [facing, half, hinge, open, visible, windowed]; `pine_green_four_pane_locometal_window` (Pine Green Four Pane Locometal Window) [axis]; `pine_green_hazard_stripes_chevron_on_black` (Pine Green on Black Chevron) [facing]; `pine_green_hazard_stripes_chevron_on_white` (Pine Green on White Chevron) [facing]; `pine_green_hazard_stripes_diagonal_on_black` (Pine Green on Black Hazard Stripes) [facing]; `pine_green_hazard_stripes_diagonal_on_white` (Pine Green on White Hazard Stripes) [facing]; `pine_green_hinged_locometal_door` (Pine Green Hinged Locometal Door) [facing, half, hinge, open, windowed]; `pine_green_iron_wrapped_locometal` (Pine Green Iron Wrapped Locometal); `pine_green_iron_wrapped_locometal_boiler` (Pine Green Iron Wrapped Locometal Boiler) [axis, raised, style]; `pine_green_iron_wrapped_locometal_smokebox` (Pine Green Iron Wrapped Locometal Smokebox) [facing]; `pine_green_locometal_boiler` (Pine Green Locometal Boiler) [axis, raised, style]; `pine_green_locometal_end_ladder` (Pine Green Locometal End Ladder) [facing, waterlogged]; `pine_green_locometal_flywheel` (Pine Green Locometal Flywheel) [axis]; `pine_green_locometal_pillar` (Pine Green Locometal Pillar) [axis]; `pine_green_locometal_rung_ladder` (Pine Green Locometal Rung Ladder) [facing, waterlogged]; `pine_green_locometal_smokebox` (Pine Green Locometal Smokebox) [facing]; `pine_green_locometal_trapdoor` (Pine Green Locometal Trapdoor) [facing, half, open, windowed]; `pine_green_locometal_vent` (Pine Green Locometal Vent); `pine_green_plated_locometal` (Plated Pine Green Locometal); `pine_green_riveted_locometal` (Pine Green Riveted Locometal); `pine_green_round_pane_locometal_window` (Pine Green Round Pane Locometal Window) [axis]; `pine_green_single_pane_locometal_window` (Pine Green Single Pane Locometal Window) [axis]; `pine_green_slashed_locometal` (Pine Green Slashed Locometal); `pine_green_sliding_locometal_door` (Pine Green Sliding Locometal Door) [facing, half, hinge, open, visible, windowed]; `pine_green_two_pane_locometal_window` (Pine Green Two Pane Locometal Window) [axis]; `pine_green_wrapped_locometal_smokebox` (Pine Green Brass Wrapped Locometal Smokebox) [facing]; `pink_brass_wrapped_locometal` (Pink Brass Wrapped Locometal); `pink_brass_wrapped_locometal_boiler` (Pink Brass Wrapped Locometal Boiler) [axis, raised, style]; `pink_copper_wrapped_locometal` (Pink Copper Wrapped Locometal); `pink_copper_wrapped_locometal_boiler` (Pink Copper Wrapped Locometal Boiler) [axis, raised, style]; `pink_copper_wrapped_locometal_smokebox` (Pink Copper Wrapped Locometal Smokebox) [facing]; `pink_flat_riveted_locometal` (Flat Pink Riveted Locometal); `pink_flat_slashed_locometal` (Flat Pink Slashed Locometal); `pink_folding_locometal_door` (Pink Folding Locometal Door) [facing, half, hinge, open, visible, windowed]; `pink_four_pane_locometal_window` (Pink Four Pane Locometal Window) [axis]; `pink_hazard_stripes_chevron_on_black` (Pink on Black Chevron) [facing]; `pink_hazard_stripes_chevron_on_white` (Pink on White Chevron) [facing]; `pink_hazard_stripes_diagonal_on_black` (Pink on Black Hazard Stripes) [facing]; `pink_hazard_stripes_diagonal_on_white` (Pink on White Hazard Stripes) [facing]; `pink_hinged_locometal_door` (Pink Hinged Locometal Door) [facing, half, hinge, open, windowed]; `pink_iron_wrapped_locometal` (Pink Iron Wrapped Locometal); `pink_iron_wrapped_locometal_boiler` (Pink Iron Wrapped Locometal Boiler) [axis, raised, style]; `pink_iron_wrapped_locometal_smokebox` (Pink Iron Wrapped Locometal Smokebox) [facing]; `pink_locometal_boiler` (Pink Locometal Boiler) [axis, raised, style]; `pink_locometal_end_ladder` (Pink Locometal End Ladder) [facing, waterlogged]; `pink_locometal_flywheel` (Pink Locometal Flywheel) [axis]; `pink_locometal_pillar` (Pink Locometal Pillar) [axis]; `pink_locometal_rung_ladder` (Pink Locometal Rung Ladder) [facing, waterlogged]; `pink_locometal_smokebox` (Pink Locometal Smokebox) [facing]; `pink_locometal_trapdoor` (Pink Locometal Trapdoor) [facing, half, open, windowed]; `pink_locometal_vent` (Pink Locometal Vent); `pink_plated_locometal` (Plated Pink Locometal); `pink_riveted_locometal` (Pink Riveted Locometal); `pink_round_pane_locometal_window` (Pink Round Pane Locometal Window) [axis]; `pink_single_pane_locometal_window` (Pink Single Pane Locometal Window) [axis]; `pink_slashed_locometal` (Pink Slashed Locometal); `pink_sliding_locometal_door` (Pink Sliding Locometal Door) [facing, half, hinge, open, visible, windowed]; `pink_two_pane_locometal_window` (Pink Two Pane Locometal Window) [axis]; `pink_wrapped_locometal_smokebox` (Pink Brass Wrapped Locometal Smokebox) [facing]; `plated_locometal` (Plated Locometal); `portable_fuel_interface` (Portable Fuel Interface) [facing]; `purple_brass_wrapped_locometal` (Purple Brass Wrapped Locometal); `purple_brass_wrapped_locometal_boiler` (Purple Brass Wrapped Locometal Boiler) [axis, raised, style]; `purple_copper_wrapped_locometal` (Purple Copper Wrapped Locometal); `purple_copper_wrapped_locometal_boiler` (Purple Copper Wrapped Locometal Boiler) [axis, raised, style]; `purple_copper_wrapped_locometal_smokebox` (Purple Copper Wrapped Locometal Smokebox) [facing]; `purple_flat_riveted_locometal` (Flat Purple Riveted Locometal); `purple_flat_slashed_locometal` (Flat Purple Slashed Locometal); `purple_folding_locometal_door` (Purple Folding Locometal Door) [facing, half, hinge, open, visible, windowed]; `purple_four_pane_locometal_window` (Purple Four Pane Locometal Window) [axis]; `purple_hazard_stripes_chevron_on_black` (Purple on Black Chevron) [facing]; `purple_hazard_stripes_chevron_on_white` (Purple on White Chevron) [facing]; `purple_hazard_stripes_diagonal_on_black` (Purple on Black Hazard Stripes) [facing]; `purple_hazard_stripes_diagonal_on_white` (Purple on White Hazard Stripes) [facing]; `purple_hinged_locometal_door` (Purple Hinged Locometal Door) [facing, half, hinge, open, windowed]; `purple_iron_wrapped_locometal` (Purple Iron Wrapped Locometal); `purple_iron_wrapped_locometal_boiler` (Purple Iron Wrapped Locometal Boiler) [axis, raised, style]; `purple_iron_wrapped_locometal_smokebox` (Purple Iron Wrapped Locometal Smokebox) [facing]; `purple_locometal_boiler` (Purple Locometal Boiler) [axis, raised, style]; `purple_locometal_end_ladder` (Purple Locometal End Ladder) [facing, waterlogged]; `purple_locometal_flywheel` (Purple Locometal Flywheel) [axis]; `purple_locometal_pillar` (Purple Locometal Pillar) [axis]; `purple_locometal_rung_ladder` (Purple Locometal Rung Ladder) [facing, waterlogged]; `purple_locometal_smokebox` (Purple Locometal Smokebox) [facing]; `purple_locometal_trapdoor` (Purple Locometal Trapdoor) [facing, half, open, windowed]; `purple_locometal_vent` (Purple Locometal Vent); `purple_plated_locometal` (Plated Purple Locometal); `purple_riveted_locometal` (Purple Riveted Locometal); `purple_round_pane_locometal_window` (Purple Round Pane Locometal Window) [axis]; `purple_single_pane_locometal_window` (Purple Single Pane Locometal Window) [axis]; `purple_slashed_locometal` (Purple Slashed Locometal); `purple_sliding_locometal_door` (Purple Sliding Locometal Door) [facing, half, hinge, open, visible, windowed]; `purple_two_pane_locometal_window` (Purple Two Pane Locometal Window) [axis]; `purple_wrapped_locometal_smokebox` (Purple Brass Wrapped Locometal Smokebox) [facing]; `red_brass_wrapped_locometal` (Red Brass Wrapped Locometal); `red_brass_wrapped_locometal_boiler` (Red Brass Wrapped Locometal Boiler) [axis, raised, style]; `red_copper_wrapped_locometal` (Red Copper Wrapped Locometal); `red_copper_wrapped_locometal_boiler` (Red Copper Wrapped Locometal Boiler) [axis, raised, style]; `red_copper_wrapped_locometal_smokebox` (Red Copper Wrapped Locometal Smokebox) [facing]; `red_flat_riveted_locometal` (Flat Red Riveted Locometal); `red_flat_slashed_locometal` (Flat Red Slashed Locometal); `red_folding_locometal_door` (Red Folding Locometal Door) [facing, half, hinge, open, visible, windowed]; `red_four_pane_locometal_window` (Red Four Pane Locometal Window) [axis]; `red_hazard_stripes_chevron_on_black` (Red on Black Chevron) [facing]; `red_hazard_stripes_chevron_on_white` (Red on White Chevron) [facing]; `red_hazard_stripes_diagonal_on_black` (Red on Black Hazard Stripes) [facing]; `red_hazard_stripes_diagonal_on_white` (Red on White Hazard Stripes) [facing]; `red_hinged_locometal_door` (Red Hinged Locometal Door) [facing, half, hinge, open, windowed]; `red_iron_wrapped_locometal` (Red Iron Wrapped Locometal); `red_iron_wrapped_locometal_boiler` (Red Iron Wrapped Locometal Boiler) [axis, raised, style]; `red_iron_wrapped_locometal_smokebox` (Red Iron Wrapped Locometal Smokebox) [facing]; `red_locometal_boiler` (Red Locometal Boiler) [axis, raised, style]; `red_locometal_end_ladder` (Red Locometal End Ladder) [facing, waterlogged]; `red_locometal_flywheel` (Red Locometal Flywheel) [axis]; `red_locometal_pillar` (Red Locometal Pillar) [axis]; `red_locometal_rung_ladder` (Red Locometal Rung Ladder) [facing, waterlogged]; `red_locometal_smokebox` (Red Locometal Smokebox) [facing]; `red_locometal_trapdoor` (Red Locometal Trapdoor) [facing, half, open, windowed]; `red_locometal_vent` (Red Locometal Vent); `red_plated_locometal` (Plated Red Locometal); `red_riveted_locometal` (Red Riveted Locometal); `red_round_pane_locometal_window` (Red Round Pane Locometal Window) [axis]; `red_single_pane_locometal_window` (Red Single Pane Locometal Window) [axis]; `red_slashed_locometal` (Red Slashed Locometal); `red_sliding_locometal_door` (Red Sliding Locometal Door) [facing, half, hinge, open, visible, windowed]; `red_two_pane_locometal_window` (Red Two Pane Locometal Window) [axis]; `red_wrapped_locometal_smokebox` (Red Brass Wrapped Locometal Smokebox) [facing]; `riveted_locometal` (Riveted Locometal); `round_pane_locometal_window` (Round Pane Locometal Window) [axis]; `royal_blue_brass_wrapped_locometal` (Royal Blue Brass Wrapped Locometal); `royal_blue_brass_wrapped_locometal_boiler` (Royal Blue Brass Wrapped Locometal Boiler) [axis, raised, style]; `royal_blue_copper_wrapped_locometal` (Royal Blue Copper Wrapped Locometal); `royal_blue_copper_wrapped_locometal_boiler` (Royal Blue Copper Wrapped Locometal Boiler) [axis, raised, style]; `royal_blue_copper_wrapped_locometal_smokebox` (Royal Blue Copper Wrapped Locometal Smokebox) [facing]; `royal_blue_flat_riveted_locometal` (Flat Royal Blue Riveted Locometal); `royal_blue_flat_slashed_locometal` (Flat Royal Blue Slashed Locometal); `royal_blue_folding_locometal_door` (Royal Blue Folding Locometal Door) [facing, half, hinge, open, visible, windowed]; `royal_blue_four_pane_locometal_window` (Royal Blue Four Pane Locometal Window) [axis]; `royal_blue_hazard_stripes_chevron_on_black` (Royal Blue on Black Chevron) [facing]; `royal_blue_hazard_stripes_chevron_on_white` (Royal Blue on White Chevron) [facing]; `royal_blue_hazard_stripes_diagonal_on_black` (Royal Blue on Black Hazard Stripes) [facing]; `royal_blue_hazard_stripes_diagonal_on_white` (Royal Blue on White Hazard Stripes) [facing]; `royal_blue_hinged_locometal_door` (Royal Blue Hinged Locometal Door) [facing, half, hinge, open, windowed]; `royal_blue_iron_wrapped_locometal` (Royal Blue Iron Wrapped Locometal); `royal_blue_iron_wrapped_locometal_boiler` (Royal Blue Iron Wrapped Locometal Boiler) [axis, raised, style]; `royal_blue_iron_wrapped_locometal_smokebox` (Royal Blue Iron Wrapped Locometal Smokebox) [facing]; `royal_blue_locometal_boiler` (Royal Blue Locometal Boiler) [axis, raised, style]; `royal_blue_locometal_end_ladder` (Royal Blue Locometal End Ladder) [facing, waterlogged]; `royal_blue_locometal_flywheel` (Royal Blue Locometal Flywheel) [axis]; `royal_blue_locometal_pillar` (Royal Blue Locometal Pillar) [axis]; `royal_blue_locometal_rung_ladder` (Royal Blue Locometal Rung Ladder) [facing, waterlogged]; `royal_blue_locometal_smokebox` (Royal Blue Locometal Smokebox) [facing]; `royal_blue_locometal_trapdoor` (Royal Blue Locometal Trapdoor) [facing, half, open, windowed]; `royal_blue_locometal_vent` (Royal Blue Locometal Vent); `royal_blue_plated_locometal` (Plated Royal Blue Locometal); `royal_blue_riveted_locometal` (Royal Blue Riveted Locometal); `royal_blue_round_pane_locometal_window` (Royal Blue Round Pane Locometal Window) [axis]; `royal_blue_single_pane_locometal_window` (Royal Blue Single Pane Locometal Window) [axis]; `royal_blue_slashed_locometal` (Royal Blue Slashed Locometal); `royal_blue_sliding_locometal_door` (Royal Blue Sliding Locometal Door) [facing, half, hinge, open, visible, windowed]; `royal_blue_two_pane_locometal_window` (Royal Blue Two Pane Locometal Window) [axis]; `royal_blue_wrapped_locometal_smokebox` (Royal Blue Brass Wrapped Locometal Smokebox) [facing]; `scorchia_brass_wrapped_locometal` (Scorchia Brass Wrapped Locometal); `scorchia_brass_wrapped_locometal_boiler` (Scorchia Brass Wrapped Locometal Boiler) [axis, raised, style]; `scorchia_copper_wrapped_locometal` (Scorchia Copper Wrapped Locometal); `scorchia_copper_wrapped_locometal_boiler` (Scorchia Copper Wrapped Locometal Boiler) [axis, raised, style]; `scorchia_copper_wrapped_locometal_smokebox` (Scorchia Copper Wrapped Locometal Smokebox) [facing]; `scorchia_flat_riveted_locometal` (Flat Scorchia Riveted Locometal); `scorchia_flat_slashed_locometal` (Flat Scorchia Slashed Locometal); `scorchia_folding_locometal_door` (Scorchia Folding Locometal Door) [facing, half, hinge, open, visible, windowed]; `scorchia_four_pane_locometal_window` (Scorchia Four Pane Locometal Window) [axis]; `scorchia_hazard_stripes_chevron_on_black` (Scorchia on Black Chevron) [facing]; `scorchia_hazard_stripes_chevron_on_white` (Scorchia on White Chevron) [facing]; `scorchia_hazard_stripes_diagonal_on_black` (Scorchia on Black Hazard Stripes) [facing]; `scorchia_hazard_stripes_diagonal_on_white` (Scorchia on White Hazard Stripes) [facing]; `scorchia_hinged_locometal_door` (Scorchia Hinged Locometal Door) [facing, half, hinge, open, windowed]; `scorchia_iron_wrapped_locometal` (Scorchia Iron Wrapped Locometal); `scorchia_iron_wrapped_locometal_boiler` (Scorchia Iron Wrapped Locometal Boiler) [axis, raised, style]; `scorchia_iron_wrapped_locometal_smokebox` (Scorchia Iron Wrapped Locometal Smokebox) [facing]; `scorchia_locometal_boiler` (Scorchia Locometal Boiler) [axis, raised, style]; `scorchia_locometal_end_ladder` (Scorchia Locometal End Ladder) [facing, waterlogged]; `scorchia_locometal_flywheel` (Scorchia Locometal Flywheel) [axis]; `scorchia_locometal_pillar` (Scorchia Locometal Pillar) [axis]; `scorchia_locometal_rung_ladder` (Scorchia Locometal Rung Ladder) [facing, waterlogged]; `scorchia_locometal_smokebox` (Scorchia Locometal Smokebox) [facing]; `scorchia_locometal_trapdoor` (Scorchia Locometal Trapdoor) [facing, half, open, windowed]; `scorchia_locometal_vent` (Scorchia Locometal Vent); `scorchia_plated_locometal` (Plated Scorchia Locometal); `scorchia_riveted_locometal` (Scorchia Riveted Locometal); `scorchia_round_pane_locometal_window` (Scorchia Round Pane Locometal Window) [axis]; `scorchia_single_pane_locometal_window` (Scorchia Single Pane Locometal Window) [axis]; `scorchia_slashed_locometal` (Scorchia Slashed Locometal); `scorchia_sliding_locometal_door` (Scorchia Sliding Locometal Door) [facing, half, hinge, open, visible, windowed]; `scorchia_two_pane_locometal_window` (Scorchia Two Pane Locometal Window) [axis]; `scorchia_wrapped_locometal_smokebox` (Scorchia Brass Wrapped Locometal Smokebox) [facing]; `sea_green_brass_wrapped_locometal` (Sea Green Brass Wrapped Locometal); `sea_green_brass_wrapped_locometal_boiler` (Sea Green Brass Wrapped Locometal Boiler) [axis, raised, style]; `sea_green_copper_wrapped_locometal` (Sea Green Copper Wrapped Locometal); `sea_green_copper_wrapped_locometal_boiler` (Sea Green Copper Wrapped Locometal Boiler) [axis, raised, style]; `sea_green_copper_wrapped_locometal_smokebox` (Sea Green Copper Wrapped Locometal Smokebox) [facing]; `sea_green_flat_riveted_locometal` (Flat Sea Green Riveted Locometal); `sea_green_flat_slashed_locometal` (Flat Sea Green Slashed Locometal); `sea_green_folding_locometal_door` (Sea Green Folding Locometal Door) [facing, half, hinge, open, visible, windowed]; `sea_green_four_pane_locometal_window` (Sea Green Four Pane Locometal Window) [axis]; `sea_green_hazard_stripes_chevron_on_black` (Sea Green on Black Chevron) [facing]; `sea_green_hazard_stripes_chevron_on_white` (Sea Green on White Chevron) [facing]; `sea_green_hazard_stripes_diagonal_on_black` (Sea Green on Black Hazard Stripes) [facing]; `sea_green_hazard_stripes_diagonal_on_white` (Sea Green on White Hazard Stripes) [facing]; `sea_green_hinged_locometal_door` (Sea Green Hinged Locometal Door) [facing, half, hinge, open, windowed]; `sea_green_iron_wrapped_locometal` (Sea Green Iron Wrapped Locometal); `sea_green_iron_wrapped_locometal_boiler` (Sea Green Iron Wrapped Locometal Boiler) [axis, raised, style]; `sea_green_iron_wrapped_locometal_smokebox` (Sea Green Iron Wrapped Locometal Smokebox) [facing]; `sea_green_locometal_boiler` (Sea Green Locometal Boiler) [axis, raised, style]; `sea_green_locometal_end_ladder` (Sea Green Locometal End Ladder) [facing, waterlogged]; `sea_green_locometal_flywheel` (Sea Green Locometal Flywheel) [axis]; `sea_green_locometal_pillar` (Sea Green Locometal Pillar) [axis]; `sea_green_locometal_rung_ladder` (Sea Green Locometal Rung Ladder) [facing, waterlogged]; `sea_green_locometal_smokebox` (Sea Green Locometal Smokebox) [facing]; `sea_green_locometal_trapdoor` (Sea Green Locometal Trapdoor) [facing, half, open, windowed]; `sea_green_locometal_vent` (Sea Green Locometal Vent); `sea_green_plated_locometal` (Plated Sea Green Locometal); `sea_green_riveted_locometal` (Sea Green Riveted Locometal); `sea_green_round_pane_locometal_window` (Sea Green Round Pane Locometal Window) [axis]; `sea_green_single_pane_locometal_window` (Sea Green Single Pane Locometal Window) [axis]; `sea_green_slashed_locometal` (Sea Green Slashed Locometal); `sea_green_sliding_locometal_door` (Sea Green Sliding Locometal Door) [facing, half, hinge, open, visible, windowed]; `sea_green_two_pane_locometal_window` (Sea Green Two Pane Locometal Window) [axis]; `sea_green_wrapped_locometal_smokebox` (Sea Green Brass Wrapped Locometal Smokebox) [facing]; `semaphore` (Semaphore) [facing, flipped, full, upside_down]; `single_pane_locometal_window` (Single Pane Locometal Window) [axis]; `singleaxle_bogey` (Single Axle Bogey) [axis, waterlogged]; `slashed_locometal` (Slashed Locometal); `sliding_locometal_door` (Sliding Locometal Door) [facing, half, hinge, open, visible, windowed]; `small_buffer` (Small Buffer) [facing]; `smokestack_caboosestyle` (Caboose Smokestack) [axis] — This smokestack is dyeable.; `smokestack_coalburner` (Coalburner Smokestack) [part, style]; `smokestack_coalburner_extension` (Coalburner Smokestack Extension) [part, style]; `smokestack_diesel` (Radiator Fan) [facing]; `smokestack_long` (Double Smokestack) [axis, part, style]; `smokestack_long_extension` (Double Smokestack Extension) [axis, part, style]; `smokestack_oilburner` (Oilburner Smokestack) [part, style]; `smokestack_oilburner_extension` (Oilburner Smokestack Extension) [part, style]; `smokestack_streamlined` (Streamlined Smokestack) [facing, part, style]; `smokestack_streamlined_extension` (Streamlined Smokestack Extension) [facing, part, style]; `smokestack_woodburner` (Woodburner Smokestack) [part, style]; `smokestack_woodburner_extension` (Woodburner Smokestack Extension) [part, style]; `track_acacia` (Acacia Train Track) [shape, turn, waterlogged]; `track_acacia_narrow` (Narrow Acacia Train Track) [shape, turn, waterlogged]; `track_acacia_wide` (Wide Acacia Train Track) [shape, turn, waterlogged]; `track_bamboo` (Bamboo Train Track) [shape, turn, waterlogged]; `track_bamboo_narrow` (Narrow Bamboo Train Track) [shape, turn, waterlogged]; `track_bamboo_wide` (Wide Bamboo Train Track) [shape, turn, waterlogged]; `track_biomesoplenty_dead` (Dead Train Track) [shape, turn, waterlogged]; `track_biomesoplenty_dead_narrow` (Narrow Dead Train Track) [shape, turn, waterlogged]; `track_biomesoplenty_dead_wide` (Wide Dead Train Track) [shape, turn, waterlogged]; `track_biomesoplenty_empyreal` (Empyreal Train Track) [shape, turn, waterlogged]; `track_biomesoplenty_empyreal_narrow` (Narrow Empyreal Train Track) [shape, turn, waterlogged]; `track_biomesoplenty_empyreal_wide` (Wide Empyreal Train Track) [shape, turn, waterlogged]; `track_biomesoplenty_fir` (Fir Train Track) [shape, turn, waterlogged]; `track_biomesoplenty_fir_narrow` (Narrow Fir Train Track) [shape, turn, waterlogged]; `track_biomesoplenty_fir_wide` (Wide Fir Train Track) [shape, turn, waterlogged]; `track_biomesoplenty_hellbark` (Hellbark Train Track) [shape, turn, waterlogged]; `track_biomesoplenty_hellbark_narrow` (Narrow Hellbark Train Track) [shape, turn, waterlogged]; `track_biomesoplenty_hellbark_wide` (Wide Hellbark Train Track) [shape, turn, waterlogged]; `track_biomesoplenty_jacaranda` (Jacaranda Train Track) [shape, turn, waterlogged]; `track_biomesoplenty_jacaranda_narrow` (Narrow Jacaranda Train Track) [shape, turn, waterlogged]; `track_biomesoplenty_jacaranda_wide` (Wide Jacaranda Train Track) [shape, turn, waterlogged]; `track_biomesoplenty_magic` (Magic Train Track) [shape, turn, waterlogged]; `track_biomesoplenty_magic_narrow` (Narrow Magic Train Track) [shape, turn, waterlogged]; `track_biomesoplenty_magic_wide` (Wide Magic Train Track) [shape, turn, waterlogged]; `track_biomesoplenty_mahogany` (Mahogany Train Track) [shape, turn, waterlogged]; `track_biomesoplenty_mahogany_narrow` (Narrow Mahogany Train Track) [shape, turn, waterlogged]; `track_biomesoplenty_mahogany_wide` (Wide Mahogany Train Track) [shape, turn, waterlogged]; `track_biomesoplenty_maple` (Maple Train Track) [shape, turn, waterlogged]; `track_biomesoplenty_maple_narrow` (Narrow Maple Train Track) [shape, turn, waterlogged]; `track_biomesoplenty_maple_wide` (Wide Maple Train Track) [shape, turn, waterlogged]; `track_biomesoplenty_palm` (Palm Train Track) [shape, turn, waterlogged]; `track_biomesoplenty_palm_narrow` (Narrow Palm Train Track) [shape, turn, waterlogged]; `track_biomesoplenty_palm_wide` (Wide Palm Train Track) [shape, turn, waterlogged]; `track_biomesoplenty_pine` (Pine Train Track) [shape, turn, waterlogged]; `track_biomesoplenty_pine_narrow` (Narrow Pine Train Track) [shape, turn, waterlogged]; `track_biomesoplenty_pine_wide` (Wide Pine Train Track) [shape, turn, waterlogged]; `track_biomesoplenty_redwood` (Redwood Train Track) [shape, turn, waterlogged]; `track_biomesoplenty_redwood_narrow` (Narrow Redwood Train Track) [shape, turn, waterlogged]; `track_biomesoplenty_redwood_wide` (Wide Redwood Train Track) [shape, turn, waterlogged]; `track_biomesoplenty_umbran` (Umbran Train Track) [shape, turn, waterlogged]; `track_biomesoplenty_umbran_narrow` (Narrow Umbran Train Track) [shape, turn, waterlogged]; `track_biomesoplenty_umbran_wide` (Wide Umbran Train Track) [shape, turn, waterlogged]; `track_biomesoplenty_willow` (Willow Train Track) [shape, turn, waterlogged]; `track_biomesoplenty_willow_narrow` (Narrow Willow Train Track) [shape, turn, waterlogged]; `track_biomesoplenty_willow_wide` (Wide Willow Train Track) [shape, turn, waterlogged]; `track_birch` (Birch Train Track) [shape, turn, waterlogged]; `track_birch_narrow` (Narrow Birch Train Track) [shape, turn, waterlogged]; `track_birch_wide` (Wide Birch Train Track) [shape, turn, waterlogged]; `track_blackstone` (Blackstone Train Track) [shape, turn, waterlogged]; `track_blackstone_narrow` (Narrow Blackstone Train Track) [shape, turn, waterlogged]; `track_blackstone_wide` (Wide Blackstone Train Track) [shape, turn, waterlogged]; `track_blue_skies_bluebright` (Bluebright Train Track) [shape, turn, waterlogged]; `track_blue_skies_bluebright_narrow` (Narrow Bluebright Train Track) [shape, turn, waterlogged]; `track_blue_skies_bluebright_wide` (Wide Bluebright Train Track) [shape, turn, waterlogged]; `track_blue_skies_dusk` (Dusk Train Track) [shape, turn, waterlogged]; `track_blue_skies_dusk_narrow` (Narrow Dusk Train Track) [shape, turn, waterlogged]; `track_blue_skies_dusk_wide` (Wide Dusk Train Track) [shape, turn, waterlogged]; `track_blue_skies_frostbright` (Frostbright Train Track) [shape, turn, waterlogged]; `track_blue_skies_frostbright_narrow` (Narrow Frostbright Train Track) [shape, turn, waterlogged]; `track_blue_skies_frostbright_wide` (Wide Frostbright Train Track) [shape, turn, waterlogged]; `track_blue_skies_lunar` (Lunar Train Track) [shape, turn, waterlogged]; `track_blue_skies_lunar_narrow` (Narrow Lunar Train Track) [shape, turn, waterlogged]; `track_blue_skies_lunar_wide` (Wide Lunar Train Track) [shape, turn, waterlogged]; `track_blue_skies_maple` (Maple Train Track) [shape, turn, waterlogged]; `track_blue_skies_maple_narrow` (Narrow Maple Train Track) [shape, turn, waterlogged]; `track_blue_skies_maple_wide` (Wide Maple Train Track) [shape, turn, waterlogged]; `track_blue_skies_starlit` (Starlit Train Track) [shape, turn, waterlogged]; `track_blue_skies_starlit_narrow` (Narrow Starlit Train Track) [shape, turn, waterlogged]; `track_blue_skies_starlit_wide` (Wide Starlit Train Track) [shape, turn, waterlogged]; `track_byg_aspen` (Aspen Train Track) [shape, turn, waterlogged]; `track_byg_aspen_narrow` (Narrow Aspen Train Track) [shape, turn, waterlogged]; `track_byg_aspen_wide` (Wide Aspen Train Track) [shape, turn, waterlogged]; `track_byg_baobab` (Baobab Train Track) [shape, turn, waterlogged]; `track_byg_baobab_narrow` (Narrow Baobab Train Track) [shape, turn, waterlogged]; `track_byg_baobab_wide` (Wide Baobab Train Track) [shape, turn, waterlogged]; `track_byg_blue_enchanted` (Blue Enchanted Train Track) [shape, turn, waterlogged]; `track_byg_blue_enchanted_narrow` (Narrow Blue Enchanted Train Track) [shape, turn, waterlogged]; `track_byg_blue_enchanted_wide` (Wide Blue Enchanted Train Track) [shape, turn, waterlogged]; `track_byg_bulbis` (Bulbis Train Track) [shape, turn, waterlogged]; `track_byg_bulbis_narrow` (Narrow Bulbis Train Track) [shape, turn, waterlogged]; `track_byg_bulbis_wide` (Wide Bulbis Train Track) [shape, turn, waterlogged]; `track_byg_cika` (Cika Train Track) [shape, turn, waterlogged]; `track_byg_cika_narrow` (Narrow Cika Train Track) [shape, turn, waterlogged]; `track_byg_cika_wide` (Wide Cika Train Track) [shape, turn, waterlogged]; `track_byg_cypress` (Cypress Train Track) [shape, turn, waterlogged]; `track_byg_cypress_narrow` (Narrow Cypress Train Track) [shape, turn, waterlogged]; `track_byg_cypress_wide` (Wide Cypress Train Track) [shape, turn, waterlogged]; `track_byg_ebony` (Ebony Train Track) [shape, turn, waterlogged]; `track_byg_ebony_narrow` (Narrow Ebony Train Track) [shape, turn, waterlogged]; `track_byg_ebony_wide` (Wide Ebony Train Track) [shape, turn, waterlogged]; `track_byg_embur` (Embur Train Track) [shape, turn, waterlogged]; `track_byg_embur_narrow` (Narrow Embur Train Track) [shape, turn, waterlogged]; `track_byg_embur_wide` (Wide Embur Train Track) [shape, turn, waterlogged]; `track_byg_ether` (Ether Train Track) [shape, turn, waterlogged]; `track_byg_ether_narrow` (Narrow Ether Train Track) [shape, turn, waterlogged]; `track_byg_ether_wide` (Wide Ether Train Track) [shape, turn, waterlogged]; `track_byg_fir` (Fir Train Track) [shape, turn, waterlogged]; `track_byg_fir_narrow` (Narrow Fir Train Track) [shape, turn, waterlogged]; `track_byg_fir_wide` (Wide Fir Train Track) [shape, turn, waterlogged]; `track_byg_green_enchanted` (Green Enchanted Train Track) [shape, turn, waterlogged]; `track_byg_green_enchanted_narrow` (Narrow Green Enchanted Train Track) [shape, turn, waterlogged]; `track_byg_green_enchanted_wide` (Wide Green Enchanted Train Track) [shape, turn, waterlogged]; `track_byg_holly` (Holly Train Track) [shape, turn, waterlogged]; `track_byg_holly_narrow` (Narrow Holly Train Track) [shape, turn, waterlogged]; `track_byg_holly_wide` (Wide Holly Train Track) [shape, turn, waterlogged]; `track_byg_imparius` (Imparius Train Track) [shape, turn, waterlogged]; `track_byg_imparius_narrow` (Narrow Imparius Train Track) [shape, turn, waterlogged]; `track_byg_imparius_wide` (Wide Imparius Train Track) [shape, turn, waterlogged]; `track_byg_jacaranda` (Jacaranda Train Track) [shape, turn, waterlogged]; `track_byg_jacaranda_narrow` (Narrow Jacaranda Train Track) [shape, turn, waterlogged]; `track_byg_jacaranda_wide` (Wide Jacaranda Train Track) [shape, turn, waterlogged]; `track_byg_lament` (Lament Train Track) [shape, turn, waterlogged]; `track_byg_lament_narrow` (Narrow Lament Train Track) [shape, turn, waterlogged]; `track_byg_lament_wide` (Wide Lament Train Track) [shape, turn, waterlogged]; `track_byg_mahogany` (Mahogany Train Track) [shape, turn, waterlogged]; `track_byg_mahogany_narrow` (Narrow Mahogany Train Track) [shape, turn, waterlogged]; `track_byg_mahogany_wide` (Wide Mahogany Train Track) [shape, turn, waterlogged]; `track_byg_maple` (Maple Train Track) [shape, turn, waterlogged]; `track_byg_maple_narrow` (Narrow Maple Train Track) [shape, turn, waterlogged]; `track_byg_maple_wide` (Wide Maple Train Track) [shape, turn, waterlogged]; `track_byg_nightshade` (Nightshade Train Track) [shape, turn, waterlogged]; `track_byg_nightshade_narrow` (Narrow Nightshade Train Track) [shape, turn, waterlogged]; `track_byg_nightshade_wide` (Wide Nightshade Train Track) [shape, turn, waterlogged]; `track_byg_palm` (Palm Train Track) [shape, turn, waterlogged]; `track_byg_palm_narrow` (Narrow Palm Train Track) [shape, turn, waterlogged]; `track_byg_palm_wide` (Wide Palm Train Track) [shape, turn, waterlogged]; `track_byg_pine` (Pine Train Track) [shape, turn, waterlogged]; `track_byg_pine_narrow` (Narrow Pine Train Track) [shape, turn, waterlogged]; `track_byg_pine_wide` (Wide Pine Train Track) [shape, turn, waterlogged]; `track_byg_rainbow_eucalyptus` (Rainbow Eucalyptus Train Track) [shape, turn, waterlogged]; `track_byg_rainbow_eucalyptus_narrow` (Narrow Rainbow Eucalyptus Train Track) [shape, turn, waterlogged]; `track_byg_rainbow_eucalyptus_wide` (Wide Rainbow Eucalyptus Train Track) [shape, turn, waterlogged]; `track_byg_redwood` (Redwood Train Track) [shape, turn, waterlogged]; `track_byg_redwood_narrow` (Narrow Redwood Train Track) [shape, turn, waterlogged]; `track_byg_redwood_wide` (Wide Redwood Train Track) [shape, turn, waterlogged]; `track_byg_skyris` (Skyris Train Track) [shape, turn, waterlogged]; `track_byg_skyris_narrow` (Narrow Skyris Train Track) [shape, turn, waterlogged]; `track_byg_skyris_wide` (Wide Skyris Train Track) [shape, turn, waterlogged]; `track_byg_sythian` (Sythian Train Track) [shape, turn, waterlogged]; `track_byg_sythian_narrow` (Narrow Sythian Train Track) [shape, turn, waterlogged]; `track_byg_sythian_wide` (Wide Sythian Train Track) [shape, turn, waterlogged]; `track_byg_white_mangrove` (White Mangrove Train Track) [shape, turn, waterlogged]; `track_byg_white_mangrove_narrow` (Narrow White Mangrove Train Track) [shape, turn, waterlogged]; `track_byg_white_mangrove_wide` (Wide White Mangrove Train Track) [shape, turn, waterlogged]; `track_byg_willow` (Willow Train Track) [shape, turn, waterlogged]; `track_byg_willow_narrow` (Narrow Willow Train Track) [shape, turn, waterlogged]; `track_byg_willow_wide` (Wide Willow Train Track) [shape, turn, waterlogged]; `track_byg_witch_hazel` (Witch Hazel Train Track) [shape, turn, waterlogged]; `track_byg_witch_hazel_narrow` (Narrow Witch Hazel Train Track) [shape, turn, waterlogged]; `track_byg_witch_hazel_wide` (Wide Witch Hazel Train Track) [shape, turn, waterlogged]; `track_byg_zelkova` (Zelkova Train Track) [shape, turn, waterlogged]; `track_byg_zelkova_narrow` (Narrow Zelkova Train Track) [shape, turn, waterlogged]; `track_byg_zelkova_wide` (Wide Zelkova Train Track) [shape, turn, waterlogged]; `track_cherry` (Cherry Train Track) [shape, turn, waterlogged]; `track_cherry_narrow` (Narrow Cherry Train Track) [shape, turn, waterlogged]; `track_cherry_wide` (Wide Cherry Train Track) [shape, turn, waterlogged]; `track_coupler` (Train Coupler) [mode]; `track_create_andesite_narrow` (Narrow Andesite Train Track) [shape, turn, waterlogged]; `track_create_andesite_wide` (Wide Andesite Train Track) [shape, turn, waterlogged]; `track_create_dd_rose` (Rose Train Track) [shape, turn, waterlogged]; `track_create_dd_rose_narrow` (Narrow Rose Train Track) [shape, turn, waterlogged]; `track_create_dd_rose_wide` (Wide Rose Train Track) [shape, turn, waterlogged]; `track_create_dd_rubber` (Rubber Train Track) [shape, turn, waterlogged]; `track_create_dd_rubber_narrow` (Narrow Rubber Train Track) [shape, turn, waterlogged]; `track_create_dd_rubber_wide` (Wide Rubber Train Track) [shape, turn, waterlogged]; `track_create_dd_smoked` (Smoked Train Track) [shape, turn, waterlogged]; `track_create_dd_smoked_narrow` (Narrow Smoked Train Track) [shape, turn, waterlogged]; `track_create_dd_smoked_wide` (Wide Smoked Train Track) [shape, turn, waterlogged]; `track_create_dd_spirit` (Spirit Train Track) [shape, turn, waterlogged]; `track_create_dd_spirit_narrow` (Narrow Spirit Train Track) [shape, turn, waterlogged]; `track_create_dd_spirit_wide` (Wide Spirit Train Track) [shape, turn, waterlogged]; `track_crimson` (Crimson Train Track) [shape, turn, waterlogged]; `track_crimson_narrow` (Narrow Crimson Train Track) [shape, turn, waterlogged]; `track_crimson_wide` (Wide Crimson Train Track) [shape, turn, waterlogged]; `track_dark_oak` (Dark Oak Train Track) [shape, turn, waterlogged]; `track_dark_oak_narrow` (Narrow Dark Oak Train Track) [shape, turn, waterlogged]; `track_dark_oak_wide` (Wide Dark Oak Train Track) [shape, turn, waterlogged]; `track_ender` (Ender Train Track) [shape, turn, waterlogged]; `track_ender_narrow` (Narrow Ender Train Track) [shape, turn, waterlogged]; `track_ender_wide` (Wide Ender Train Track) [shape, turn, waterlogged]; `track_hexcasting_edified` (Edified Train Track) [shape, turn, waterlogged]; `track_hexcasting_edified_narrow` (Narrow Edified Train Track) [shape, turn, waterlogged]; `track_hexcasting_edified_wide` (Wide Edified Train Track) [shape, turn, waterlogged]; `track_jungle` (Jungle Train Track) [shape, turn, waterlogged]; `track_jungle_narrow` (Narrow Jungle Train Track) [shape, turn, waterlogged]; `track_jungle_wide` (Wide Jungle Train Track) [shape, turn, waterlogged]; `track_mangrove` (Mangrove Train Track) [shape, turn, waterlogged]; `track_mangrove_narrow` (Narrow Mangrove Train Track) [shape, turn, waterlogged]; `track_mangrove_wide` (Wide Mangrove Train Track) [shape, turn, waterlogged]; `track_monorail` (Monorail Train Track) [shape, turn, waterlogged]; `track_natures_spirit_aspen` (Aspen Train Track) [shape, turn, waterlogged]; `track_natures_spirit_aspen_narrow` (Narrow Aspen Train Track) [shape, turn, waterlogged]; `track_natures_spirit_aspen_wide` (Wide Aspen Train Track) [shape, turn, waterlogged]; `track_natures_spirit_cypress` (Cypress Train Track) [shape, turn, waterlogged]; `track_natures_spirit_cypress_narrow` (Narrow Cypress Train Track) [shape, turn, waterlogged]; `track_natures_spirit_cypress_wide` (Wide Cypress Train Track) [shape, turn, waterlogged]; `track_natures_spirit_fir` (Fir Train Track) [shape, turn, waterlogged]; `track_natures_spirit_fir_narrow` (Narrow Fir Train Track) [shape, turn, waterlogged]; `track_natures_spirit_fir_wide` (Wide Fir Train Track) [shape, turn, waterlogged]; `track_natures_spirit_ghaf` (Ghaf Train Track) [shape, turn, waterlogged]; `track_natures_spirit_ghaf_narrow` (Narrow Ghaf Train Track) [shape, turn, waterlogged]; `track_natures_spirit_ghaf_wide` (Wide Ghaf Train Track) [shape, turn, waterlogged]; `track_natures_spirit_joshua` (Joshua Train Track) [shape, turn, waterlogged]; `track_natures_spirit_joshua_narrow` (Narrow Joshua Train Track) [shape, turn, waterlogged]; `track_natures_spirit_joshua_wide` (Wide Joshua Train Track) [shape, turn, waterlogged]; `track_natures_spirit_maple` (Maple Train Track) [shape, turn, waterlogged]; `track_natures_spirit_maple_narrow` (Narrow Maple Train Track) [shape, turn, waterlogged]; `track_natures_spirit_maple_wide` (Wide Maple Train Track) [shape, turn, waterlogged]; `track_natures_spirit_olive` (Olive Train Track) [shape, turn, waterlogged]; `track_natures_spirit_olive_narrow` (Narrow Olive Train Track) [shape, turn, waterlogged]; `track_natures_spirit_olive_wide` (Wide Olive Train Track) [shape, turn, waterlogged]; `track_natures_spirit_palo_verde` (Palo Verde Train Track) [shape, turn, waterlogged]; `track_natures_spirit_palo_verde_narrow` (Narrow Palo Verde Train Track) [shape, turn, waterlogged]; `track_natures_spirit_palo_verde_wide` (Wide Palo Verde Train Track) [shape, turn, waterlogged]; `track_natures_spirit_redwood` (Redwood Train Track) [shape, turn, waterlogged]; `track_natures_spirit_redwood_narrow` (Narrow Redwood Train Track) [shape, turn, waterlogged]; `track_natures_spirit_redwood_wide` (Wide Redwood Train Track) [shape, turn, waterlogged]; `track_natures_spirit_sugi` (Sugi Train Track) [shape, turn, waterlogged]; `track_natures_spirit_sugi_narrow` (Narrow Sugi Train Track) [shape, turn, waterlogged]; `track_natures_spirit_sugi_wide` (Wide Sugi Train Track) [shape, turn, waterlogged]; `track_natures_spirit_willow` (Willow Train Track) [shape, turn, waterlogged]; `track_natures_spirit_willow_narrow` (Narrow Willow Train Track) [shape, turn, waterlogged]; `track_natures_spirit_willow_wide` (Wide Willow Train Track) [shape, turn, waterlogged]; `track_natures_spirit_wisteria` (Wisteria Train Track) [shape, turn, waterlogged]; `track_natures_spirit_wisteria_narrow` (Narrow Wisteria Train Track) [shape, turn, waterlogged]; `track_natures_spirit_wisteria_wide` (Wide Wisteria Train Track) [shape, turn, waterlogged]; `track_oak` (Oak Train Track) [shape, turn, waterlogged]; `track_oak_narrow` (Narrow Oak Train Track) [shape, turn, waterlogged]; `track_oak_wide` (Wide Oak Train Track) [shape, turn, waterlogged]; `track_phantom` (Phantom Train Track) [shape, turn, waterlogged] — Phantom Tracks are compatible with all bogey types; `track_quark_ancient` (Ashen Train Track) [shape, turn, waterlogged]; `track_quark_ancient_narrow` (Narrow Ashen Train Track) [shape, turn, waterlogged]; `track_quark_ancient_wide` (Wide Ashen Train Track) [shape, turn, waterlogged]; `track_quark_azalea` (Azalea Train Track) [shape, turn, waterlogged]; `track_quark_azalea_narrow` (Narrow Azalea Train Track) [shape, turn, waterlogged]; `track_quark_azalea_wide` (Wide Azalea Train Track) [shape, turn, waterlogged]; `track_quark_blossom` (Trumpet Train Track) [shape, turn, waterlogged]; `track_quark_blossom_narrow` (Narrow Trumpet Train Track) [shape, turn, waterlogged]; `track_quark_blossom_wide` (Wide Trumpet Train Track) [shape, turn, waterlogged]; `track_spruce` (Spruce Train Track) [shape, turn, waterlogged]; `track_spruce_narrow` (Narrow Spruce Train Track) [shape, turn, waterlogged]; `track_spruce_wide` (Wide Spruce Train Track) [shape, turn, waterlogged]; `track_stripped_bamboo` (Stripped Bamboo Train Track) [shape, turn, waterlogged]; `track_stripped_bamboo_narrow` (Narrow Stripped Bamboo Train Track) [shape, turn, waterlogged]; `track_stripped_bamboo_wide` (Wide Stripped Bamboo Train Track) [shape, turn, waterlogged]; `track_switch_andesite` (Andesite Track Switch) [facing]; `track_switch_brass` (Brass Track Switch) [facing]; `track_tfc_acacia` (Acacia Train Track) [shape, turn, waterlogged]; `track_tfc_acacia_narrow` (Narrow Acacia Train Track) [shape, turn, waterlogged]; `track_tfc_acacia_wide` (Wide Acacia Train Track) [shape, turn, waterlogged]; `track_tfc_ash` (Ash Train Track) [shape, turn, waterlogged]; `track_tfc_ash_narrow` (Narrow Ash Train Track) [shape, turn, waterlogged]; `track_tfc_ash_wide` (Wide Ash Train Track) [shape, turn, waterlogged]; `track_tfc_aspen` (Aspen Train Track) [shape, turn, waterlogged]; `track_tfc_aspen_narrow` (Narrow Aspen Train Track) [shape, turn, waterlogged]; `track_tfc_aspen_wide` (Wide Aspen Train Track) [shape, turn, waterlogged]; `track_tfc_birch` (Birch Train Track) [shape, turn, waterlogged]; `track_tfc_birch_narrow` (Narrow Birch Train Track) [shape, turn, waterlogged]; `track_tfc_birch_wide` (Wide Birch Train Track) [shape, turn, waterlogged]; `track_tfc_blackwood` (Blackwood Train Track) [shape, turn, waterlogged]; `track_tfc_blackwood_narrow` (Narrow Blackwood Train Track) [shape, turn, waterlogged]; `track_tfc_blackwood_wide` (Wide Blackwood Train Track) [shape, turn, waterlogged]; `track_tfc_chestnut` (Chestnut Train Track) [shape, turn, waterlogged]; `track_tfc_chestnut_narrow` (Narrow Chestnut Train Track) [shape, turn, waterlogged]; `track_tfc_chestnut_wide` (Wide Chestnut Train Track) [shape, turn, waterlogged]; `track_tfc_douglas_fir` (Douglas Fir Train Track) [shape, turn, waterlogged]; `track_tfc_douglas_fir_narrow` (Narrow Douglas Fir Train Track) [shape, turn, waterlogged]; `track_tfc_douglas_fir_wide` (Wide Douglas Fir Train Track) [shape, turn, waterlogged]; `track_tfc_hickory` (Hickory Train Track) [shape, turn, waterlogged]; `track_tfc_hickory_narrow` (Narrow Hickory Train Track) [shape, turn, waterlogged]; `track_tfc_hickory_wide` (Wide Hickory Train Track) [shape, turn, waterlogged]; `track_tfc_kapok` (Kapok Train Track) [shape, turn, waterlogged]; `track_tfc_kapok_narrow` (Narrow Kapok Train Track) [shape, turn, waterlogged]; `track_tfc_kapok_wide` (Wide Kapok Train Track) [shape, turn, waterlogged]; `track_tfc_mangrove` (Mangrove Train Track) [shape, turn, waterlogged]; `track_tfc_mangrove_narrow` (Narrow Mangrove Train Track) [shape, turn, waterlogged]; `track_tfc_mangrove_wide` (Wide Mangrove Train Track) [shape, turn, waterlogged]; `track_tfc_maple` (Maple Train Track) [shape, turn, waterlogged]; `track_tfc_maple_narrow` (Narrow Maple Train Track) [shape, turn, waterlogged]; `track_tfc_maple_wide` (Wide Maple Train Track) [shape, turn, waterlogged]; `track_tfc_oak` (Oak Train Track) [shape, turn, waterlogged]; `track_tfc_oak_narrow` (Narrow Oak Train Track) [shape, turn, waterlogged]; `track_tfc_oak_wide` (Wide Oak Train Track) [shape, turn, waterlogged]; `track_tfc_palm` (Palm Train Track) [shape, turn, waterlogged]; `track_tfc_palm_narrow` (Narrow Palm Train Track) [shape, turn, waterlogged]; `track_tfc_palm_wide` (Wide Palm Train Track) [shape, turn, waterlogged]; `track_tfc_pine` (Pine Train Track) [shape, turn, waterlogged]; `track_tfc_pine_narrow` (Narrow Pine Train Track) [shape, turn, waterlogged]; `track_tfc_pine_wide` (Wide Pine Train Track) [shape, turn, waterlogged]; `track_tfc_rosewood` (Rosewood Train Track) [shape, turn, waterlogged]; `track_tfc_rosewood_narrow` (Narrow Rosewood Train Track) [shape, turn, waterlogged]; `track_tfc_rosewood_wide` (Wide Rosewood Train Track) [shape, turn, waterlogged]; `track_tfc_sequoia` (Sequoia Train Track) [shape, turn, waterlogged]; `track_tfc_sequoia_narrow` (Narrow Sequoia Train Track) [shape, turn, waterlogged]; `track_tfc_sequoia_wide` (Wide Sequoia Train Track) [shape, turn, waterlogged]; `track_tfc_spruce` (Spruce Train Track) [shape, turn, waterlogged]; `track_tfc_spruce_narrow` (Narrow Spruce Train Track) [shape, turn, waterlogged]; `track_tfc_spruce_wide` (Wide Spruce Train Track) [shape, turn, waterlogged]; `track_tfc_sycamore` (Sycamore Train Track) [shape, turn, waterlogged]; `track_tfc_sycamore_narrow` (Narrow Sycamore Train Track) [shape, turn, waterlogged]; `track_tfc_sycamore_wide` (Wide Sycamore Train Track) [shape, turn, waterlogged]; `track_tfc_white_cedar` (White Cedar Train Track) [shape, turn, waterlogged]; `track_tfc_white_cedar_narrow` (Narrow White Cedar Train Track) [shape, turn, waterlogged]; `track_tfc_white_cedar_wide` (Wide White Cedar Train Track) [shape, turn, waterlogged]; `track_tfc_willow` (Willow Train Track) [shape, turn, waterlogged]; `track_tfc_willow_narrow` (Narrow Willow Train Track) [shape, turn, waterlogged]; `track_tfc_willow_wide` (Wide Willow Train Track) [shape, turn, waterlogged]; `track_tieless` (Tieless Train Track) [shape, turn, waterlogged]; `track_tieless_narrow` (Narrow Tieless Train Track) [shape, turn, waterlogged]; `track_tieless_wide` (Wide Tieless Train Track) [shape, turn, waterlogged]; `track_twilightforest_canopy` (Canopy Train Track) [shape, turn, waterlogged]; `track_twilightforest_canopy_narrow` (Narrow Canopy Train Track) [shape, turn, waterlogged]; `track_twilightforest_canopy_wide` (Wide Canopy Train Track) [shape, turn, waterlogged]; `track_twilightforest_darkwood` (Darkwood Train Track) [shape, turn, waterlogged]; `track_twilightforest_darkwood_narrow` (Narrow Darkwood Train Track) [shape, turn, waterlogged]; `track_twilightforest_darkwood_wide` (Wide Darkwood Train Track) [shape, turn, waterlogged]; `track_twilightforest_mangrove` (Mangrove Train Track) [shape, turn, waterlogged]; `track_twilightforest_mangrove_narrow` (Narrow Mangrove Train Track) [shape, turn, waterlogged]; `track_twilightforest_mangrove_wide` (Wide Mangrove Train Track) [shape, turn, waterlogged]; `track_twilightforest_minewood` (Minewood Train Track) [shape, turn, waterlogged]; `track_twilightforest_minewood_narrow` (Narrow Minewood Train Track) [shape, turn, waterlogged]; `track_twilightforest_minewood_wide` (Wide Minewood Train Track) [shape, turn, waterlogged]; `track_twilightforest_sortingwood` (Sortingwood Train Track) [shape, turn, waterlogged]; `track_twilightforest_sortingwood_narrow` (Narrow Sortingwood Train Track) [shape, turn, waterlogged]; `track_twilightforest_sortingwood_wide` (Wide Sortingwood Train Track) [shape, turn, waterlogged]; `track_twilightforest_timewood` (Timewood Train Track) [shape, turn, waterlogged]; `track_twilightforest_timewood_narrow` (Narrow Timewood Train Track) [shape, turn, waterlogged]; `track_twilightforest_timewood_wide` (Wide Timewood Train Track) [shape, turn, waterlogged]; `track_twilightforest_transwood` (Transwood Train Track) [shape, turn, waterlogged]; `track_twilightforest_transwood_narrow` (Narrow Transwood Train Track) [shape, turn, waterlogged]; `track_twilightforest_transwood_wide` (Wide Transwood Train Track) [shape, turn, waterlogged]; `track_twilightforest_twilight_oak` (Twilight Oak Train Track) [shape, turn, waterlogged]; `track_twilightforest_twilight_oak_narrow` (Narrow Twilight Oak Train Track) [shape, turn, waterlogged]; `track_twilightforest_twilight_oak_wide` (Wide Twilight Oak Train Track) [shape, turn, waterlogged]; `track_warped` (Warped Train Track) [shape, turn, waterlogged]; `track_warped_narrow` (Narrow Warped Train Track) [shape, turn, waterlogged]; `track_warped_wide` (Wide Warped Train Track) [shape, turn, waterlogged]; `tripleaxle_bogey` (Triple Axle Bogey) [axis, waterlogged]; `tuff_brass_wrapped_locometal` (Tuff Brass Wrapped Locometal); `tuff_brass_wrapped_locometal_boiler` (Tuff Brass Wrapped Locometal Boiler) [axis, raised, style]; `tuff_copper_wrapped_locometal` (Tuff Copper Wrapped Locometal); `tuff_copper_wrapped_locometal_boiler` (Tuff Copper Wrapped Locometal Boiler) [axis, raised, style]; `tuff_copper_wrapped_locometal_smokebox` (Tuff Copper Wrapped Locometal Smokebox) [facing]; `tuff_flat_riveted_locometal` (Flat Tuff Riveted Locometal); `tuff_flat_slashed_locometal` (Flat Tuff Slashed Locometal); `tuff_folding_locometal_door` (Tuff Folding Locometal Door) [facing, half, hinge, open, visible, windowed]; `tuff_four_pane_locometal_window` (Tuff Four Pane Locometal Window) [axis]; `tuff_hazard_stripes_chevron_on_black` (Tuff on Black Chevron) [facing]; `tuff_hazard_stripes_chevron_on_white` (Tuff on White Chevron) [facing]; `tuff_hazard_stripes_diagonal_on_black` (Tuff on Black Hazard Stripes) [facing]; `tuff_hazard_stripes_diagonal_on_white` (Tuff on White Hazard Stripes) [facing]; `tuff_hinged_locometal_door` (Tuff Hinged Locometal Door) [facing, half, hinge, open, windowed]; `tuff_iron_wrapped_locometal` (Tuff Iron Wrapped Locometal); `tuff_iron_wrapped_locometal_boiler` (Tuff Iron Wrapped Locometal Boiler) [axis, raised, style]; `tuff_iron_wrapped_locometal_smokebox` (Tuff Iron Wrapped Locometal Smokebox) [facing]; `tuff_locometal_boiler` (Tuff Locometal Boiler) [axis, raised, style]; `tuff_locometal_end_ladder` (Tuff Locometal End Ladder) [facing, waterlogged]; `tuff_locometal_flywheel` (Tuff Locometal Flywheel) [axis]; `tuff_locometal_pillar` (Tuff Locometal Pillar) [axis]; `tuff_locometal_rung_ladder` (Tuff Locometal Rung Ladder) [facing, waterlogged]; `tuff_locometal_smokebox` (Tuff Locometal Smokebox) [facing]; `tuff_locometal_trapdoor` (Tuff Locometal Trapdoor) [facing, half, open, windowed]; `tuff_locometal_vent` (Tuff Locometal Vent); `tuff_plated_locometal` (Plated Tuff Locometal); `tuff_riveted_locometal` (Tuff Riveted Locometal); `tuff_round_pane_locometal_window` (Tuff Round Pane Locometal Window) [axis]; `tuff_single_pane_locometal_window` (Tuff Single Pane Locometal Window) [axis]; `tuff_slashed_locometal` (Tuff Slashed Locometal); `tuff_sliding_locometal_door` (Tuff Sliding Locometal Door) [facing, half, hinge, open, visible, windowed]; `tuff_two_pane_locometal_window` (Tuff Two Pane Locometal Window) [axis]; `tuff_wrapped_locometal_smokebox` (Tuff Brass Wrapped Locometal Smokebox) [facing]; `turquoise_brass_wrapped_locometal` (Turquoise Brass Wrapped Locometal); `turquoise_brass_wrapped_locometal_boiler` (Turquoise Brass Wrapped Locometal Boiler) [axis, raised, style]; `turquoise_copper_wrapped_locometal` (Turquoise Copper Wrapped Locometal); `turquoise_copper_wrapped_locometal_boiler` (Turquoise Copper Wrapped Locometal Boiler) [axis, raised, style]; `turquoise_copper_wrapped_locometal_smokebox` (Turquoise Copper Wrapped Locometal Smokebox) [facing]; `turquoise_flat_riveted_locometal` (Flat Turquoise Riveted Locometal); `turquoise_flat_slashed_locometal` (Flat Turquoise Slashed Locometal); `turquoise_folding_locometal_door` (Turquoise Folding Locometal Door) [facing, half, hinge, open, visible, windowed]; `turquoise_four_pane_locometal_window` (Turquoise Four Pane Locometal Window) [axis]; `turquoise_hazard_stripes_chevron_on_black` (Turquoise on Black Chevron) [facing]; `turquoise_hazard_stripes_chevron_on_white` (Turquoise on White Chevron) [facing]; `turquoise_hazard_stripes_diagonal_on_black` (Turquoise on Black Hazard Stripes) [facing]; `turquoise_hazard_stripes_diagonal_on_white` (Turquoise on White Hazard Stripes) [facing]; `turquoise_hinged_locometal_door` (Turquoise Hinged Locometal Door) [facing, half, hinge, open, windowed]; `turquoise_iron_wrapped_locometal` (Turquoise Iron Wrapped Locometal); `turquoise_iron_wrapped_locometal_boiler` (Turquoise Iron Wrapped Locometal Boiler) [axis, raised, style]; `turquoise_iron_wrapped_locometal_smokebox` (Turquoise Iron Wrapped Locometal Smokebox) [facing]; `turquoise_locometal_boiler` (Turquoise Locometal Boiler) [axis, raised, style]; `turquoise_locometal_end_ladder` (Turquoise Locometal End Ladder) [facing, waterlogged]; `turquoise_locometal_flywheel` (Turquoise Locometal Flywheel) [axis]; `turquoise_locometal_pillar` (Turquoise Locometal Pillar) [axis]; `turquoise_locometal_rung_ladder` (Turquoise Locometal Rung Ladder) [facing, waterlogged]; `turquoise_locometal_smokebox` (Turquoise Locometal Smokebox) [facing]; `turquoise_locometal_trapdoor` (Turquoise Locometal Trapdoor) [facing, half, open, windowed]; `turquoise_locometal_vent` (Turquoise Locometal Vent); `turquoise_plated_locometal` (Plated Turquoise Locometal); `turquoise_riveted_locometal` (Turquoise Riveted Locometal); `turquoise_round_pane_locometal_window` (Turquoise Round Pane Locometal Window) [axis]; `turquoise_single_pane_locometal_window` (Turquoise Single Pane Locometal Window) [axis]; `turquoise_slashed_locometal` (Turquoise Slashed Locometal); `turquoise_sliding_locometal_door` (Turquoise Sliding Locometal Door) [facing, half, hinge, open, visible, windowed]; `turquoise_two_pane_locometal_window` (Turquoise Two Pane Locometal Window) [axis]; `turquoise_wrapped_locometal_smokebox` (Turquoise Brass Wrapped Locometal Smokebox) [facing]; `two_pane_locometal_window` (Two Pane Locometal Window) [axis]; `vermilion_brass_wrapped_locometal` (Vermilion Brass Wrapped Locometal); `vermilion_brass_wrapped_locometal_boiler` (Vermilion Brass Wrapped Locometal Boiler) [axis, raised, style]; `vermilion_copper_wrapped_locometal` (Vermilion Copper Wrapped Locometal); `vermilion_copper_wrapped_locometal_boiler` (Vermilion Copper Wrapped Locometal Boiler) [axis, raised, style]; `vermilion_copper_wrapped_locometal_smokebox` (Vermilion Copper Wrapped Locometal Smokebox) [facing]; `vermilion_flat_riveted_locometal` (Flat Vermilion Riveted Locometal); `vermilion_flat_slashed_locometal` (Flat Vermilion Slashed Locometal); `vermilion_folding_locometal_door` (Vermilion Folding Locometal Door) [facing, half, hinge, open, visible, windowed]; `vermilion_four_pane_locometal_window` (Vermilion Four Pane Locometal Window) [axis]; `vermilion_hazard_stripes_chevron_on_black` (Vermilion on Black Chevron) [facing]; `vermilion_hazard_stripes_chevron_on_white` (Vermilion on White Chevron) [facing]; `vermilion_hazard_stripes_diagonal_on_black` (Vermilion on Black Hazard Stripes) [facing]; `vermilion_hazard_stripes_diagonal_on_white` (Vermilion on White Hazard Stripes) [facing]; `vermilion_hinged_locometal_door` (Vermilion Hinged Locometal Door) [facing, half, hinge, open, windowed]; `vermilion_iron_wrapped_locometal` (Vermilion Iron Wrapped Locometal); `vermilion_iron_wrapped_locometal_boiler` (Vermilion Iron Wrapped Locometal Boiler) [axis, raised, style]; `vermilion_iron_wrapped_locometal_smokebox` (Vermilion Iron Wrapped Locometal Smokebox) [facing]; `vermilion_locometal_boiler` (Vermilion Locometal Boiler) [axis, raised, style]; `vermilion_locometal_end_ladder` (Vermilion Locometal End Ladder) [facing, waterlogged]; `vermilion_locometal_flywheel` (Vermilion Locometal Flywheel) [axis]; `vermilion_locometal_pillar` (Vermilion Locometal Pillar) [axis]; `vermilion_locometal_rung_ladder` (Vermilion Locometal Rung Ladder) [facing, waterlogged]; `vermilion_locometal_smokebox` (Vermilion Locometal Smokebox) [facing]; `vermilion_locometal_trapdoor` (Vermilion Locometal Trapdoor) [facing, half, open, windowed]; `vermilion_locometal_vent` (Vermilion Locometal Vent); `vermilion_plated_locometal` (Plated Vermilion Locometal); `vermilion_riveted_locometal` (Vermilion Riveted Locometal); `vermilion_round_pane_locometal_window` (Vermilion Round Pane Locometal Window) [axis]; `vermilion_single_pane_locometal_window` (Vermilion Single Pane Locometal Window) [axis]; `vermilion_slashed_locometal` (Vermilion Slashed Locometal); `vermilion_sliding_locometal_door` (Vermilion Sliding Locometal Door) [facing, half, hinge, open, visible, windowed]; `vermilion_two_pane_locometal_window` (Vermilion Two Pane Locometal Window) [axis]; `vermilion_wrapped_locometal_smokebox` (Vermilion Brass Wrapped Locometal Smokebox) [facing]; `white_brass_wrapped_locometal` (White Brass Wrapped Locometal); `white_brass_wrapped_locometal_boiler` (White Brass Wrapped Locometal Boiler) [axis, raised, style]; `white_copper_wrapped_locometal` (White Copper Wrapped Locometal); `white_copper_wrapped_locometal_boiler` (White Copper Wrapped Locometal Boiler) [axis, raised, style]; `white_copper_wrapped_locometal_smokebox` (White Copper Wrapped Locometal Smokebox) [facing]; `white_flat_riveted_locometal` (Flat White Riveted Locometal); `white_flat_slashed_locometal` (Flat White Slashed Locometal); `white_folding_locometal_door` (White Folding Locometal Door) [facing, half, hinge, open, visible, windowed]; `white_four_pane_locometal_window` (White Four Pane Locometal Window) [axis]; `white_hazard_stripes_chevron_on_black` (White on Black Chevron) [facing]; `white_hazard_stripes_chevron_on_white` (White on White Chevron) [facing]; `white_hazard_stripes_diagonal_on_black` (White on Black Hazard Stripes) [facing]; `white_hazard_stripes_diagonal_on_white` (White on White Hazard Stripes) [facing]; `white_hinged_locometal_door` (White Hinged Locometal Door) [facing, half, hinge, open, windowed]; `white_iron_wrapped_locometal` (White Iron Wrapped Locometal); `white_iron_wrapped_locometal_boiler` (White Iron Wrapped Locometal Boiler) [axis, raised, style]; `white_iron_wrapped_locometal_smokebox` (White Iron Wrapped Locometal Smokebox) [facing]; `white_locometal_boiler` (White Locometal Boiler) [axis, raised, style]; `white_locometal_end_ladder` (White Locometal End Ladder) [facing, waterlogged]; `white_locometal_flywheel` (White Locometal Flywheel) [axis]; `white_locometal_pillar` (White Locometal Pillar) [axis]; `white_locometal_rung_ladder` (White Locometal Rung Ladder) [facing, waterlogged]; `white_locometal_smokebox` (White Locometal Smokebox) [facing]; `white_locometal_trapdoor` (White Locometal Trapdoor) [facing, half, open, windowed]; `white_locometal_vent` (White Locometal Vent); `white_plated_locometal` (Plated White Locometal); `white_riveted_locometal` (White Riveted Locometal); `white_round_pane_locometal_window` (White Round Pane Locometal Window) [axis]; `white_single_pane_locometal_window` (White Single Pane Locometal Window) [axis]; `white_slashed_locometal` (White Slashed Locometal); `white_sliding_locometal_door` (White Sliding Locometal Door) [facing, half, hinge, open, visible, windowed]; `white_two_pane_locometal_window` (White Two Pane Locometal Window) [axis]; `white_wrapped_locometal_smokebox` (White Brass Wrapped Locometal Smokebox) [facing]; `wide_comically_large_bogey` (Wide Gauge Comically Large Bogey) [axis, waterlogged]; `wide_doubleaxle_bogey` (Wide Gauge Double Axle Bogey) [axis, waterlogged]; `wide_scotch_bogey` (Wide Gauge Scotch Yoke Bogey) [axis, waterlogged]; `wrapped_locometal_smokebox` (Brass Wrapped Locometal Smokebox) [facing]; `yellow_brass_wrapped_locometal` (Yellow Brass Wrapped Locometal); `yellow_brass_wrapped_locometal_boiler` (Yellow Brass Wrapped Locometal Boiler) [axis, raised, style]; `yellow_copper_wrapped_locometal` (Yellow Copper Wrapped Locometal); `yellow_copper_wrapped_locometal_boiler` (Yellow Copper Wrapped Locometal Boiler) [axis, raised, style]; `yellow_copper_wrapped_locometal_smokebox` (Yellow Copper Wrapped Locometal Smokebox) [facing]; `yellow_flat_riveted_locometal` (Flat Yellow Riveted Locometal); `yellow_flat_slashed_locometal` (Flat Yellow Slashed Locometal); `yellow_folding_locometal_door` (Yellow Folding Locometal Door) [facing, half, hinge, open, visible, windowed]; `yellow_four_pane_locometal_window` (Yellow Four Pane Locometal Window) [axis]; `yellow_hazard_stripes_chevron_on_black` (Yellow on Black Chevron) [facing]; `yellow_hazard_stripes_chevron_on_white` (Yellow on White Chevron) [facing]; `yellow_hazard_stripes_diagonal_on_black` (Yellow on Black Hazard Stripes) [facing]; `yellow_hazard_stripes_diagonal_on_white` (Yellow on White Hazard Stripes) [facing]; `yellow_hinged_locometal_door` (Yellow Hinged Locometal Door) [facing, half, hinge, open, windowed]; `yellow_iron_wrapped_locometal` (Yellow Iron Wrapped Locometal); `yellow_iron_wrapped_locometal_boiler` (Yellow Iron Wrapped Locometal Boiler) [axis, raised, style]; `yellow_iron_wrapped_locometal_smokebox` (Yellow Iron Wrapped Locometal Smokebox) [facing]; `yellow_locometal_boiler` (Yellow Locometal Boiler) [axis, raised, style]; `yellow_locometal_end_ladder` (Yellow Locometal End Ladder) [facing, waterlogged]; `yellow_locometal_flywheel` (Yellow Locometal Flywheel) [axis]; `yellow_locometal_pillar` (Yellow Locometal Pillar) [axis]; `yellow_locometal_rung_ladder` (Yellow Locometal Rung Ladder) [facing, waterlogged]; `yellow_locometal_smokebox` (Yellow Locometal Smokebox) [facing]; `yellow_locometal_trapdoor` (Yellow Locometal Trapdoor) [facing, half, open, windowed]; `yellow_locometal_vent` (Yellow Locometal Vent); `yellow_plated_locometal` (Plated Yellow Locometal); `yellow_riveted_locometal` (Yellow Riveted Locometal); `yellow_round_pane_locometal_window` (Yellow Round Pane Locometal Window) [axis]; `yellow_single_pane_locometal_window` (Yellow Single Pane Locometal Window) [axis]; `yellow_slashed_locometal` (Yellow Slashed Locometal); `yellow_sliding_locometal_door` (Yellow Sliding Locometal Door) [facing, half, hinge, open, visible, windowed]; `yellow_two_pane_locometal_window` (Yellow Two Pane Locometal Window) [axis]; `yellow_wrapped_locometal_smokebox` (Yellow Brass Wrapped Locometal Smokebox) [facing]

## farmersdelight  (132 blocks, jar: FarmersDelight-1.20.1-1.3.4)

`acacia_cabinet` (Acacia Cabinet) [facing, open]; `apple_pie` (Apple Pie) [bites, facing]; `bamboo_basket` (Bamboo Basket) [facing]; `bamboo_cabinet` (Bamboo Cabinet) [facing, open]; `beetroot_crate` (Beetroot Crate); `birch_cabinet` (Birch Cabinet) [facing, open]; `black_canvas_sign` (Black Canvas Sign); `black_canvas_wall_sign` (Black Canvas Wall Sign); `black_hanging_canvas_sign` (Black Hanging Canvas Sign); `black_wall_hanging_canvas_sign` (Black Wall Hanging Canvas Sign); `blue_canvas_sign` (Blue Canvas Sign); `blue_canvas_wall_sign` (Blue Canvas Wall Sign); `blue_hanging_canvas_sign` (Blue Hanging Canvas Sign); `blue_wall_hanging_canvas_sign` (Blue Wall Hanging Canvas Sign); `brown_canvas_sign` (Brown Canvas Sign); `brown_canvas_wall_sign` (Brown Canvas Wall Sign); `brown_hanging_canvas_sign` (Brown Hanging Canvas Sign); `brown_mushroom_colony` (Brown Mushroom Colony) [age]; `brown_wall_hanging_canvas_sign` (Brown Wall Hanging Canvas Sign); `budding_tomatoes` (Budding Tomato Vine) [age]; `cabbage_crate` (Cabbage Crate); `cabbages` (Cabbage) [age]; `canvas_rug` (Canvas Rug); `canvas_sign` (Canvas Sign); `canvas_wall_sign` (Canvas Wall Sign); `carrot_crate` (Carrot Crate); `cherry_cabinet` (Cherry Cabinet) [facing, open]; `chocolate_pie` (Chocolate Pie) [bites, facing]; `cooking_pot` (Cooking Pot) [facing, support]; `crimson_cabinet` (Crimson Cabinet) [facing, open]; `cutting_board` (Cutting Board) [facing]; `cyan_canvas_sign` (Cyan Canvas Sign); `cyan_canvas_wall_sign` (Cyan Canvas Wall Sign); `cyan_hanging_canvas_sign` (Cyan Hanging Canvas Sign); `cyan_wall_hanging_canvas_sign` (Cyan Wall Hanging Canvas Sign); `dark_oak_cabinet` (Dark Oak Cabinet) [facing, open]; `full_tatami_mat` (Full Tatami Mat) [facing, part]; `gleaming_salad_block` (Gleaming Salad) [facing, servings]; `gray_canvas_sign` (Gray Canvas Sign); `gray_canvas_wall_sign` (Gray Canvas Wall Sign); `gray_hanging_canvas_sign` (Gray Hanging Canvas Sign); `gray_wall_hanging_canvas_sign` (Gray Wall Hanging Canvas Sign); `green_canvas_sign` (Green Canvas Sign); `green_canvas_wall_sign` (Green Canvas Wall Sign); `green_hanging_canvas_sign` (Green Hanging Canvas Sign); `green_wall_hanging_canvas_sign` (Green Wall Hanging Canvas Sign); `half_tatami_mat` (Half Tatami Mat) [facing]; `hanging_canvas_sign` (Hanging Canvas Sign); `honey_glazed_ham_block` (Honey Glazed Ham) [facing, servings]; `jungle_cabinet` (Jungle Cabinet) [facing, open]; `light_blue_canvas_sign` (Light Blue Canvas Sign); `light_blue_canvas_wall_sign` (Light Blue Canvas Wall Sign); `light_blue_hanging_canvas_sign` (Light Blue Hanging Canvas Sign); `light_blue_wall_hanging_canvas_sign` (Light Blue Wall Hanging Canvas Sign); `light_gray_canvas_sign` (Light Gray Canvas Sign); `light_gray_canvas_wall_sign` (Light Gray Canvas Wall Sign); `light_gray_hanging_canvas_sign` (Light Gray Hanging Canvas Sign); `light_gray_wall_hanging_canvas_sign` (Light Gray Wall Hanging Canvas Sign); `lime_canvas_sign` (Lime Canvas Sign); `lime_canvas_wall_sign` (Lime Canvas Wall Sign); `lime_hanging_canvas_sign` (Lime Hanging Canvas Sign); `lime_wall_hanging_canvas_sign` (Lime Wall Hanging Canvas Sign); `magenta_canvas_sign` (Magenta Canvas Sign); `magenta_canvas_wall_sign` (Magenta Canvas Wall Sign); `magenta_hanging_canvas_sign` (Magenta Hanging Canvas Sign); `magenta_wall_hanging_canvas_sign` (Magenta Wall Hanging Canvas Sign); `mangrove_cabinet` (Mangrove Cabinet) [facing, open]; `oak_cabinet` (Oak Cabinet) [facing, open]; `onion_crate` (Onion Crate); `onions` (Onions) [age]; `orange_canvas_sign` (Orange Canvas Sign); `orange_canvas_wall_sign` (Orange Canvas Wall Sign); `orange_hanging_canvas_sign` (Orange Hanging Canvas Sign); `orange_wall_hanging_canvas_sign` (Orange Wall Hanging Canvas Sign); `organic_compost` (Organic Compost) [composting]; `pink_canvas_sign` (Pink Canvas Sign); `pink_canvas_wall_sign` (Pink Canvas Wall Sign); `pink_hanging_canvas_sign` (Pink Hanging Canvas Sign); `pink_wall_hanging_canvas_sign` (Pink Wall Hanging Canvas Sign); `potato_crate` (Potato Crate); `pumpkin_pie` (Pumpkin Pie) [bites, facing]; `purple_canvas_sign` (Purple Canvas Sign); `purple_canvas_wall_sign` (Purple Canvas Wall Sign); `purple_hanging_canvas_sign` (Purple Hanging Canvas Sign); `purple_wall_hanging_canvas_sign` (Purple Wall Hanging Canvas Sign); `red_canvas_sign` (Red Canvas Sign); `red_canvas_wall_sign` (Red Canvas Wall Sign); `red_hanging_canvas_sign` (Red Hanging Canvas Sign); `red_mushroom_colony` (Red Mushroom Colony) [age]; `red_wall_hanging_canvas_sign` (Red Wall Hanging Canvas Sign); `rice` (Rice Crops) [age, supporting]; `rice_bag` (Bag of Rice); `rice_bale` (Rice Bale) [facing]; `rice_panicles` (Rice Panicles) [age]; `rice_roll_medley_block` (Rice Roll Medley) [facing, servings]; `rich_soil` (Rich Soil); `rich_soil_farmland` (Rich Soil Farmland) [moisture]; `roast_chicken_block` (Roast Chicken) [facing, servings]; `rope` (Rope) [tied_to_bell, north, east, south, west]; `rope_fence` (Rope Fence) [north, east, south, west]; `rope_fence_gate` (Rope Fence Gate) [facing, in_wall, open]; `safety_net` (Safety Net); `sandy_shrub` (Sandy Shrub); `shepherds_pie_block` (Shepherd's Pie) [facing, servings]; `skillet` (Skillet) [facing, support]; `spruce_cabinet` (Spruce Cabinet) [facing, open]; `stove` (Stove) [facing, lit]; `straw_bale` (Straw Bale) [axis]; `stuffed_pumpkin_block` (Stuffed Pumpkin) [facing, servings]; `sweet_berry_cheesecake` (Sweet Berry Cheesecake) [bites, facing]; `tatami` (Tatami Block) [facing, paired]; `tomato_crate` (Tomato Crate); `tomatoes` (Tomato Vine) [age, ropelogged]; `tomatoes_on_rope` (Tomato Vine on Rope) [age]; `wall_hanging_canvas_sign` (Wall Hanging Canvas Sign); `warped_cabinet` (Warped Cabinet) [facing, open]; `white_canvas_sign` (White Canvas Sign); `white_canvas_wall_sign` (White Canvas Wall Sign); `white_hanging_canvas_sign` (White Hanging Canvas Sign); `white_wall_hanging_canvas_sign` (White Wall Hanging Canvas Sign); `wild_beetroots` (Sea Beet); `wild_cabbages` (Wild Cabbage); `wild_carrots` (Wild Carrot); `wild_onions` (Wild Onion); `wild_potatoes` (Wild Potato); `wild_rice` (Wild Rice) [half]; `wild_tomatoes` (Tomato Shrub); `wooden_basket` (Basket) [facing]; `yellow_canvas_sign` (Yellow Canvas Sign); `yellow_canvas_wall_sign` (Yellow Canvas Wall Sign); `yellow_hanging_canvas_sign` (Yellow Hanging Canvas Sign); `yellow_wall_hanging_canvas_sign` (Yellow Wall Hanging Canvas Sign)

## sliceanddice  (4 blocks, jar: sliceanddice-forge-3.6.0)

`fertilizer` (Fertilizer); `slicer` (Slicer); `sprinkler` (Sprinkler); `wet_air` (Wet Air)

## valkyrienskies  (6 blocks, jar: valkyrienskies-120-2.4.11)

`test_antigrav` (Debug Levitator); `test_chair` (Debug Chair); `test_flap` (Debug Flap) [facing]; `test_hinge` (Debug Hinge) [facing]; `test_thruster` (Debug Thruster) [facing, powered]; `test_wing` (Debug Wing) [facing]

## vs_clockwork  (73 blocks, jar: clockwork-0.5.6)

`OLD_flap_bearing` [facing]; `afterblazer` (Afterblazer) [facing, powered]; `air_compressor` (Air Compressor); `alt_meter` (Altimeter); `andesite_flap_bearing` (Flap Bearing) [axis_along_first, facing]; `balloon_casing` (Balloon Casing); `balloon_encased_shaft` (Balloon Encased Shaft) [axis]; `ballooner` (Ballooner); `blade_controller` (Blade Controller) [facing]; `brass_propeller_bearing` (Brass Propeller Bearing) [facing]; `charged_nyx` (Charged Nyx); `chiseled_wanderlite` (Chiseled Wanderlite); `chocolate_frosting`; `clock` (Clock) [facing]; `coal_burner` (Coal Burner) [facing, AND]; `cobbled_nyx` (Cobbled Nyx); `combustion_engine` (Combustion Engine) [facing]; `command_seat` (Command Seat) [facing]; `copter_bearing` (Copter Bearing) [facing]; `creative_gas_generator` (Creative Gas Generator); `delivery_cannon` (Delivery Cannon) [facing]; `delivery_chute` (Delivery Chute); `distance_sensor` (Peepotron) [facing, max_distance]; `duct` (Duct) [up, down, east, west, north, south]; `duct_tank` (Gas Tank) [bottom, top]; `exhaust` (Exhaust) [facing]; `extendon` (Extendon) [facing]; `filter_duct` [axis]; `flap` (Wing) [facing]; `gas_backtank` (Gas Backtank) [facing]; `gas_crafter` (Gas Crafter) [facing]; `gas_engine` (Gas Engine) [axis]; `gas_heater` (Gas Heater) [facing]; `gas_nozzle` (Gas Nozzle) [facing]; `gas_thruster` (Gas Thruster) [facing]; `goo_block` (Block of Goo); `gyro` (Gyro); `gyroscopic_sensor` (Spinotron) [facing, reference_axis]; `heat_pipe` [down, north, south, up, east, west]; `hose_port` (Hose Port) [facing]; `impact_sensor` (Impactotron) [facing, predictiveness]; `intake` (Intake) [facing]; `juryrigged_propeller_bearing` (Jury-rigged Propeller Bearing) [facing]; `lodefocus` (Lodefocus); `nyx` (Meteoric Nyx); `old_andesite_flap_bearing` [facing]; `phys_bearing` (Phys Bearing) [facing]; `physics_infuser` (Physics Infuser); `propeller_bearing` [direction, facing]; `pump_duct` (Gas Pump) [facing]; `reactionwheel` (Reaction Wheel) [axis]; `redstone_duct` (Redstone Duct) [axis]; `redstone_resistor` (Redstone Resistor) [axis]; `slicker` (Slicker) [extended, facing, powered]; `smart_flap_bearing` (Smart Flap Bearing) [axis_along_first, facing]; `smooth_wanderlite` (Smooth Wanderlite); `smooth_wanderlite_slab` (Smooth Wanderlite Slab) [type]; `smooth_wanderlite_stairs` (Smooth Wanderlite Stairs) [facing, half, shape]; `spinoff_bearing` (Spinoff Bearing) [facing]; `steam_generator` (Steam Extractor) [face, facing]; `straight_duct` [axis]; `strawberry_frosting`; `sugar_rocket` (Sugar Rocket) [facing]; `universal_shaft` (Universal Shaft) [facing]; `valve_duct` (Gas Valve) [axis_along_first, facing]; `vanilla_frosting`; `wanderglass` (Wanderglass); `wanderlite_block` (Block of Wanderlite); `wanderlite_bricks` (Wanderlite Bricks); `wanderlite_deepslate_ore` (Deepslate Wanderlite Ore); `wanderlite_end_ore` (End Wanderlite Ore); `wanderlite_nyx_ore` (Meteoric Wanderlite Geode); `wing` (Cambered Wing) [facing]

## trackwork  (16 blocks, jar: trackwork-1.20.1-1.2.3)

`horn` (Car Horn) [facing, powered] — Activates via redstone, right click to set the pitch.; `large_simple_wheel` (Gigantic Wheel) [facing]; `large_simple_wheel_part` (Gigantic Spare Tire) [axis]; `large_suspension_track` (Large Suspension Track) [axis, axis_along_first, part]; `med_simple_wheel` (Medium Wheel) [facing] — Proprietary Final Drive technology allows this to reach the same speeds as the larger wheel!; `med_simple_wheel_part` (Medium Spare Tire) [axis]; `med_suspension_track` (Medium Suspension Track) [axis, axis_along_first, part]; `oleo_wheel` (Oleo Wheel); `phys_track` (Sprocket Track) [axis, axis_along_first, part]; `rigid_track` [axis, axis_along_first, part]; `simple_wheel` (Large Wheel) [facing]; `simple_wheel_part` (Large Spare Tire) [axis]; `small_simple_wheel` (Small Wheel) [facing]; `small_simple_wheel_part` (Small Spare Tire) [axis]; `suspension_track` (Suspension Track) [axis, axis_along_first, part]; `track_level_controller` (Suspension Controller) [axis] — Input rotation to tilt and adjust suspension across the ship!

## drivebywire  (3 blocks, jar: drivebywire-1.20.1-0.1.1)

`backup_block` (Network Backup Block) [facing] — Saves the ship network when put in a schematic, and loads when shipified; `controller_hub` (Linked Controller Hub) — Bind your controller to this block, and control this network from far away!; `tweaked_controller_hub` (Tweaked Controller Hub) — Bind your controller to this block, and control this network from far away!


---

# PART 6 – SESSION 11 (Sept 16–21 2026): dock repair, Neon Heights, Skyliner, Benson memorial

## What happened
- **Physics Mod collapse:** the user installed Physics Mod (physicsmod 3.0.15), broke one block and the marina walkway + west finger pier collapsed. He has since removed the mod (log shows `physicsmod -> MISSING`; `.physics_mod_cache` folder remains, harmless).
  - Fixed by re-running `statue:veh_marina` (it has no `fill air` pre-step, so it's safe to re-run) and `kill @e[type=item,x=-5,y=45,z=55,dx=40,dy=30,dz=40]` (82 dropped items).
  - A floating source-water column under the old pier end (x-4..0 y63..69 z75..77) was removed by `sky:city_skyport`.
- **New mods:** `betterblockz-0.2.10` (BetterBlockZ, 1,498 blocks, neon "cyberlight" blocks, zeon/zeno panels) and `paladin-furniture-mod-1.5.0` (namespace `pfm`). Block IDs verified from the jars (see "BetterBlockZ cheat sheet" below).
- **Benson died** (the user's wolf). No backup existed, and a scan of all entity region files found no wolf named Benson. The user chose **a memorial on the sky island** (not a respawn). Don't mention Benson casually; the old "check Benson's health" rule no longer applies.
- **Backups now exist:** `profiles/Create_ Remastered/backups/2026-09-21_15-45-53_Create!.zip` (before this session's builds) and `2026-09-21_16-11-22_Create!.zip` (after). Make one via Save and Quit → Singleplayer → select world → Edit → Make Backup before any big job.

## Neon Heights (sky island) – datapack `skyport`, namespace `sky`
- Centre (-3, 180, 270); top surface y180 (stand at y181); radius ~31; underside down to ~y135 with glowing cyberlight veins, stalactites and amethyst. 4 mini islets: (41,170,264), (-47,192,286), (9,205,318), (-33,160,240).
- Local coords below are relative to the centre (lx = x+3, lz = z-270). North (−z) faces the city.
  - **Landing pad** lx -9..9, lz -44..-24 (world x-12..6, z226..246), cantilevered past the rim, magenta "H", hazard stripes, end-rod edge lights. Call button at **-10 181 245** (polished blackstone button on a kiosk) + signs.
  - **NEON HEIGHTS billboard gate** at lz -21 (world z249), pylons at lx ±15, letters on the north face (3x5 font, mirrored so it reads correctly from the north).
  - **Plaza** centred (0,-8): rings of cyan/magenta light, a neon flame sculpture, seats, lamp posts.
  - **Neon Spire** centred (0, 8): octagon, 52 tall (to y232), beacon at (-3,181,278) on a 3x3 iron base with magenta glass → magenta beam into the sky. Square spiral stair inside to an **observation deck** at y200 (pfm sofas/armchairs).
  - **Pods A-01/A-02/A-03** (cyan/magenta/lime) at (18,-10), (21,5), (12,20): domes with iron doors, bed, pfm sofa/fridge/freezer/microwave/trashcan, pendant light.
  - **Neon Noodle Bar** lx -27..-15, lz -9..3 (counter, magenta seats, pfm stoves/range hoods/fridge, FD cooking pot + cutting board, rooftop fans/antenna).
  - **Neon Arcade** lx -26..-16, lz 8..18: 16-colour light dance floor, "cabinets", jukebox, note block, nixie tubes, froggy chairs.
  - **Fusion core** (0,22): rainbow `cyberlight_cycle` core in lime glass, 4 tesla coils, two spinning Create towers (creative motor 48 RPM → flywheels + large cogs) at lx ±5.
  - **Sky garden/pond** (-10,25), cherry trees, **SKYNET relay dish** (21,15), glass-pane railing with neon posts around the rim.
  - **Benson memorial** centred world (-23, 181, 254) (lx -20, lz -16), north-west rim next to the noodle bar: moss terrace with quartz and light-blue neon ring, bone-block pedestal, white-wool sitting wolf statue facing north (toward the city) with red collar, button eyes, grey snout/black nose, ears; front sign "BENSON / Good boy. / Forever loyal, / always home.", back sign "In loving memory"; soul-lantern posts, flowers, two light-blue seats. Hidden light blocks at (-26,184,252), (-20,184,252), (-23,186,249).
- Built by `sky:isle_build1..5` (+ `sky:isle_signfix` for the mirrored letters). These do NOT clear the area first, but re-running would overwrite anything the user changes up there.

## Neon Skyliner – VS ship `fireheart-sky01`
- 15 long x 13 wide (with wings) x 6 tall, 347 t. Nose = +z (south). White/cyan hull, glass cockpit + windows, magenta fin, blue engine pods, wings with red (port, +x) / green (starboard, −x) lamps. Door gap on the west side (lx -3, lz 5..6).
- Seats: 4 × `create:purple_seat` at local (±1,1,4),(±1,1,7) + pilot `create:magenta_seat` (0,1,10). Each sits on a marker block `betterblockz:reactor_7_blockz_13` (used nowhere else) – that's how riders are detected.
- **Shipyard origin:** local (lx,ly,lz) = (-28657664+lx, 125+ly, 12290041+lz). Seat lx-1,lz4 = -28657665 126 12290045.
- **Centre of mass** (the `/vs teleport` target): city pad **-2.439 72.789 84.758**; island pad **-2.439 182.789 234.758** (COM local ≈ (0.561, 1.789, 5.758) from the build origin OA=(-3,71,79); OB=(-3,181,229)).
- `/vs teleport` euler angles are **(pitch-about-X yaw-about-Y roll-about-Z)** in degrees, composed as Rz·Ry·Rx. Positive Y turns the nose (+z) toward +x. Velocity is the 3rd bracket (m/s), angular velocity the 4th.
- **City Skyport pad:** x-11..5, z77..95, y70 deck on black pillars to the sea bed (y34), built off the end of the old pier (the pier's end fence x-4..0 z76 was removed and replaced by a purple SKYPORT gate). Call button at **-9 71 77**.
- **How it works (tick logic, `sky:tick`):** `#state sky` 0 = docked at city, 1 = flying out, 2 = docked at island, 3 = flying back.
  - Docked: any player sitting on a Skyliner seat → title "NEON SKYLINER / Next stop …" + 3-2-1 countdown (80 ticks) → depart. Getting up cancels. After arrival `#lock` stays 1 until everyone has got off (so it doesn't bounce straight back).
  - Call buttons: pressing the button at the pad where the ship ISN'T sends it over empty.
  - Flights are precomputed per tick (`sky:path/out_*`, 700 ticks ≈ 35 s; `sky:path/back_*`, 820 ticks ≈ 41 s incl. a 180° turn after take-off and another before landing): `vs teleport` with position, euler angles (banking/pitch along an S-curve) and matching velocity; end-rod/electric-spark engine trails; beacon hum; everyone gets slow falling during flights.
  - Standing passengers are carried too (VS drags them) – verified.
- Helpers: `sky:status` (tellraw state), `sky:reset` (snap ship to the city pad, state 0), `sky:park_isle` (snap to island pad, state 2), `sky:seatdbg`, `sky:sit_test` (summons a seat entity in shipyard coords and mounts the executor – the only reliable way for computer use to sit in it).
- Forceload: `sky:load` forceloads the two pad areas only (`-11 77 5 95`, `-13 226 7 246`).
- **Verified:** outbound flight completed (state 2, ship on island pad), then a seated passenger triggered the countdown and the return flight landed back on the city pad in the original spot (state 0).

## BetterBlockZ cheat sheet (colour index 0-15 = white, light_gray, gray, black, brown, red, orange, yellow, lime, green, cyan, light_blue, blue, purple, magenta, pink)
- `cyberlight_blockz_N` full neon block (`lit` defaults true; **redstone power turns it OFF**). `cyberlight_alt_blockz_N` framed neon. `cyberlight_cycle_blockz_0` rainbow-cycling.
- `cyberlight_bar_N[facing]` (thin bar along x when facing=up), `cyberlight_bar_vertical_N[facing]` (along z when up), `cyberlight_thin_N[facing]` (1-px full panel, like a light carpet), `cyberlight_thin_alt_N` (small square panel).
- `zeon_<colour>_blockz_0..10`: black panels with neon trim (8 = solid neon, 9/10 black; 2,3,6 are pillars with `axis`). `zeno_<colour>_blockz_0..20`: plain sci-fi panels (2,3,4,5,7,8,10,13,14,15,20 pillars).
- `tintedglass_blockz_N` is dark, not see-through – use vanilla glass for windows. `hazard_blockz_0..9[facing]`, `lablink_19_blockz_N[axis]` (stripes), `reactor_7_blockz_N` (framed coloured core), `number_blockz_N[number,facing,horizontal_facing]`.
- Paladin furniture used: `pfm:<colour>_simple_sofa[facing,shape=straight]`, `<colour>_arm_chair`, `white_fridge/freezer[facing]`, `iron_microwave[facing,open]`, `iron_stove[open,facing]`, `iron_oven_range_hood[facing,down,drawer]`, `<colour>_modern_pendant[up,down,lit]`, `trashcan[open]`, `froggy_chair_<colour>[tucked,facing]`.
- `create:nixie_tube` does **not** accept `double_face=false` in this version (it made the whole function fail to load).

## Lessons (session 11)
- **Never click while the game has focus and no menu is open** – the click lands in the world and breaks a block (it broke road block 8 70 42 twice; restored from the old region data: yellow_concrete over dirt). Switch to spectator before any menu work. To open the pause menu: `alt+tab` (focus loss pauses the game), then click the menu button.
- Typing when chat didn't open sends keys to the game: the **k** in "sky" toggles Oculus shaders. Check shaders are on (MakeUp-UltraFast) at the end.
- `device_commit_files` to the **same stagedPath** twice can deliver the old content; copy the file to a fresh output path before re-committing. Don't `/reload` in the same batch as a commit (it can race).
- A function with one bad block property fails to load entirely ("Unknown function"); the reason is in latest.log ("Whilst parsing command on line N").
- Files deeper than 7 folders below the connected ModrinthApp folder can't be staged (datapack functions), but they can be committed.
- VS ship blocks are saved in their own region files (`region/r.-5597x.2400x.mca`); scanning them for a unique marker block gives the shipyard coordinates.
- Teleporting a creative player into a ship pushes the ship; always `sky:reset` after testing.
- `/ride` onto a summoned `create:seat` only works in the same tick as the summon (use a function) – the seat entity discards itself when empty.
- VS's COM differs from a simple block average; measure it by teleporting to the estimate and reading F3 "Targeted World Position" before/after.

## Session 11b – Skyliner back door (after user feedback: "I don't see an entrance")
- The side gap (lx -3, lz 5..6) was hidden behind the wing. Added a **rear door** facing the pier gate, done live in shipyard coords:
  - `fill -28657664 126 12290042 -28657664 127 12290042 air` (stern wall lx0 lz1 ly1..2)
  - `setblock -28657664 126 12290041 air` (magenta exhaust lx0 lz0 ly1)
  - `setblock -28657664 125 12290040 minecraft:polished_blackstone_brick_stairs[facing=south,half=bottom]` (boarding step, local 0,0,-1)
  - `setblock -28657664 127 12290044 air` (removed the pendant hanging over the aisle)
- City gate sign (-2 74 75, north face) now reads "SKYLINER / Walk in the / back door & / take a seat!".
- sky.py's ship_blocks() does NOT yet include this change; add it before any rebuild (and re-measure the COM after).

## Generator code (session 11) – recreate in /tmp/claude-0/city/ (needs lib.py from Part 4)

### skylib.py
```python
import os, math
from lib import Build

OUT = '/mnt/user-data/outputs/skyport_datapack/skyport'
FN = OUT + '/data/sky/functions'

def write(name, lines, chunk=6000):
    d = os.path.dirname(f'{FN}/{name}')
    os.makedirs(d, exist_ok=True)
    names = []
    base = name
    for i in range(0, max(1, len(lines)), chunk):
        n = f'{base}{i // chunk + 1}' if len(lines) > chunk else base
        open(f'{FN}/{n}.mcfunction', 'w').write('\n'.join(lines[i:i + chunk]) + '\n')
        names.append(n)
    return names

BB = 'betterblockz:'
COL = ['white', 'light_gray', 'gray', 'black', 'brown', 'red', 'orange', 'yellow', 'lime', 'green', 'cyan', 'light_blue', 'blue', 'purple', 'magenta', 'pink']
CI = {c: i for i, c in enumerate(COL)}

def neon(c):
    return f'{BB}cyberlight_blockz_{CI[c]}'

def neon_alt(c):
    return f'{BB}cyberlight_alt_blockz_{CI[c]}'

def zeon(c, v, axis=None):
    s = f'{BB}zeon_{c}_blockz_{v}'
    return s + (f'[axis={axis}]' if axis else '')

def zeno(c, v, axis=None):
    s = f'{BB}zeno_{c}_blockz_{v}'
    return s + (f'[axis={axis}]' if axis else '')

def thin(c, facing='up'):
    return f'{BB}cyberlight_thin_{CI[c]}[facing={facing}]'

def bar(c, along='x', facing='up'):
    return f'{BB}cyberlight_bar_{CI[c]}[facing={facing}]' if along == 'x' else f'{BB}cyberlight_bar_vertical_{CI[c]}[facing={facing}]'

def tglass(c):
    return f'{BB}tintedglass_blockz_{CI[c]}'

FONT = {
    'N': ['101', '111', '111', '111', '101'],
    'E': ['111', '100', '110', '100', '111'],
    'O': ['111', '101', '101', '101', '111'],
    'H': ['101', '101', '111', '101', '101'],
    'I': ['111', '010', '010', '010', '111'],
    'G': ['111', '100', '101', '101', '111'],
    'T': ['111', '010', '010', '010', '010'],
    'S': ['111', '100', '111', '001', '111'],
    'K': ['101', '110', '100', '110', '101'],
    'Y': ['101', '101', '010', '010', '010'],
    'P': ['111', '101', '111', '100', '100'],
    'R': ['110', '101', '110', '101', '101'],
    'A': ['010', '101', '111', '101', '101'],
    'C': ['111', '100', '100', '100', '111'],
    'D': ['110', '101', '101', '101', '110'],
    ' ': ['000'] * 5,
}

def text_width(t):
    return len(t) * 4 - 1

def text_xy(t):
    px = []
    cx = 0
    for ch in t:
        g = FONT[ch]
        for r, row in enumerate(g):
            for c, v in enumerate(row):
                if v == '1':
                    px.append((cx + c, 4 - r))
        cx += 4
    return px

```

### island.py
```python
import math, random, sys
sys.path.insert(0, '/tmp/claude-0/city')
from lib import Build, sign
from skylib import *

random.seed(4743)
CX, CY, CZ = -3, 180, 270
B = Build()


def S(lx, y, lz, b):
    B.set(CX + lx, y, CZ + lz, b)


def G(lx, y, lz):
    return B.get(CX + lx, y, CZ + lz)


def box(x0, y0, z0, x1, y1, z1, b):
    B.box(CX + x0, y0, CZ + z0, CX + x1, y1, CZ + z1, b)


def radius(t):
    return 31 + 3 * math.sin(3 * t + 0.5) + 2 * math.cos(5 * t + 1.3) + 1.2 * math.sin(9 * t)


def inside(lx, lz, pad=0.0):
    d = math.hypot(lx, lz)
    return d <= radius(math.atan2(lz, lx)) - pad


TOP = {}
ROCK = ['minecraft:deepslate', 'minecraft:tuff', 'minecraft:blackstone', 'minecraft:deepslate', 'minecraft:smooth_basalt', 'minecraft:cobbled_deepslate']
VEIN = [neon('cyan'), neon('magenta'), neon('purple'), neon('light_blue'), 'minecraft:amethyst_block']

for lx in range(-40, 41):
    for lz in range(-40, 41):
        if not inside(lx, lz):
            continue
        d = math.hypot(lx, lz)
        R = radius(math.atan2(lz, lx))
        f = max(0.0, 1 - d / R)
        depth = int(3 + 36 * f ** 0.75 + 3 * math.sin(lx * 0.7) * math.cos(lz * 0.6) + random.random() * 2)
        if random.random() < 0.04 and f > 0.15:
            depth += random.randint(4, 12)
        TOP[(lx, lz)] = depth
        S(lx, CY, lz, 'minecraft:grass_block')
        box(lx, CY - 3, lz, lx, CY - 1, lz, 'minecraft:dirt')
        bottom = CY - depth
        for y in range(bottom, CY - 3):
            k = CY - y
            b = ROCK[(lx * 7 + lz * 13 + k * 3) % len(ROCK)] if k > 4 else 'minecraft:dirt'
            S(lx, y, lz, b)
        edge = not all(inside(lx + a, lz + c) for a, c in ((1, 0), (-1, 0), (0, 1), (0, -1)))
        if random.random() < (0.2 if edge else 0.1):
            S(lx, bottom, lz, random.choice(VEIN[:3]))
        if edge and random.random() < 0.14 and depth > 8:
            c = random.choice(VEIN[:3])
            y0 = random.randint(bottom, CY - 7)
            for y in range(y0, min(CY - 5, y0 + random.randint(3, 9))):
                S(lx, y, lz, c)
        if random.random() < 0.05:
            S(lx, bottom - 1, lz, 'minecraft:amethyst_cluster[facing=down]')

for i in range(26):
    a = random.random() * math.tau
    r = random.random() * 22
    lx, lz = int(r * math.cos(a)), int(r * math.sin(a))
    if (lx, lz) not in TOP:
        continue
    bottom = CY - TOP[(lx, lz)]
    L = random.randint(6, 16)
    for k in range(1, L):
        w = max(0, int((L - k) / L * 2.2))
        for dx in range(-w, w + 1):
            for dz in range(-w, w + 1):
                if dx * dx + dz * dz <= w * w:
                    S(lx + dx, bottom - k, lz + dz, 'minecraft:blackstone' if k % 3 else 'minecraft:deepslate')
    S(lx, bottom - L, lz, neon(random.choice(['cyan', 'magenta', 'purple'])))
    S(lx, bottom - L - 1, lz, 'minecraft:amethyst_cluster[facing=down]')

for (ex, ez, c) in ((0, -12, 'cyan'), (14, 10, 'magenta'), (-14, 10, 'purple')):
    b = CY - min(TOP.get((ex, ez), 20), 30)
    for dx in range(-2, 3):
        for dz in range(-2, 3):
            if abs(dx) + abs(dz) <= 3:
                box(ex + dx, b - 3, ez + dz, ex + dx, b, ez + dz, zeon('black', 9))
    box(ex - 1, b - 5, ez - 1, ex + 1, b - 4, ez + 1, zeon(c, 0))
    S(ex, b - 6, ez, neon(c))
    for dx, dz in ((-1, 0), (1, 0), (0, -1), (0, 1)):
        S(ex + dx, b - 6, ez + dz, f'{BB}cyberlight_bar_{CI[c]}[facing=down]')

EDGE = set()
for (lx, lz) in TOP:
    if any((lx + a, lz + c) not in TOP for a, c in ((1, 0), (-1, 0), (0, 1), (0, -1))):
        EDGE.add((lx, lz))

FLOOR = zeno('black', 0)
TILE = zeno('gray', 11)
PATH = zeno('gray', 0)


def disc(cx, cz, r, b, y=CY):
    for lx in range(cx - r, cx + r + 1):
        for lz in range(cz - r, cz + r + 1):
            if (lx - cx) ** 2 + (lz - cz) ** 2 <= r * r + r:
                S(lx, y, lz, b)


def ring(cx, cz, r, b, y=CY, w=0.6):
    for lx in range(cx - r - 1, cx + r + 2):
        for lz in range(cz - r - 1, cz + r + 2):
            if abs(math.hypot(lx - cx, lz - cz) - r) < w:
                S(lx, y, lz, b)


def lamp_post(lx, lz, c, h=4):
    for i in range(1, h):
        S(lx, CY + i, lz, zeon('black', 2, 'y'))
    S(lx, CY + h, lz, neon_alt(c))
    S(lx, CY + h + 1, lz, 'minecraft:end_rod[facing=up]')


# ---------------- plaza ----------------
disc(0, -8, 9, TILE)
ring(0, -8, 9, neon('cyan'))
ring(0, -8, 6, neon('magenta'))
disc(0, -8, 2, zeon('purple', 7))
S(0, CY + 1, -8, zeno('black', 12))
S(0, CY + 2, -8, zeon('orange', 3, 'y'))
for i, (dx, dy, dz, c) in enumerate([(0, 3, 0, 'red'), (0, 4, 0, 'orange'), (0, 5, 0, 'yellow'), (-1, 4, 0, 'red'), (1, 4, 0, 'red'),
                                     (0, 4, -1, 'orange'), (0, 4, 1, 'orange'), (-1, 5, 0, 'orange'), (1, 6, 0, 'yellow'), (0, 6, 0, 'orange'),
                                     (0, 7, 0, 'yellow'), (0, 5, 1, 'red'), (0, 5, -1, 'red'), (-1, 6, 0, 'yellow'), (0, 8, 0, 'white')]):
    S(dx, CY + dy, -8 + dz, neon_alt(c))
for a in range(0, 360, 45):
    lx = round(7.5 * math.cos(math.radians(a)))
    lz = -8 + round(7.5 * math.sin(math.radians(a)))
    if a in (90, 270):
        continue
    S(lx, CY + 1, lz, 'create:purple_seat' if a % 90 else 'create:black_seat')
for (lx, lz, c) in ((9, -8, 'cyan'), (-9, -8, 'cyan'), (7, -15, 'magenta'), (-7, -15, 'magenta'), (7, -1, 'magenta'), (-7, -1, 'magenta')):
    S(lx, CY, lz, TILE)
    lamp_post(lx, lz, c)

# ---------------- landing pad + walkway ----------------
for lx in range(-9, 10):
    for lz in range(-44, -23):
        S(lx, CY, lz, FLOOR)
        S(lx, CY - 1, lz, zeno('gray', 0))
for lx in range(-9, 10):
    for lz in (-44, -24):
        S(lx, CY, lz, neon('cyan'))
for lz in range(-44, -23):
    for lx in (-9, 9):
        S(lx, CY, lz, neon('cyan'))
for lx in range(-8, 9):
    S(lx, CY, -43, f'{BB}hazard_blockz_0[facing=up]')
    S(lx, CY, -25, f'{BB}hazard_blockz_0[facing=up]')
for lz in range(-42, -25):
    S(-8, CY, lz, f'{BB}lablink_19_blockz_14[axis=z]')
    S(8, CY, lz, f'{BB}lablink_19_blockz_14[axis=z]')
for (px, pz) in text_xy('H'):
    pass
for lz in range(-39, -28):
    S(-4, CY, lz, neon('magenta'))
    S(4, CY, lz, neon('magenta'))
for lx in range(-4, 5):
    S(lx, CY, -34, neon('magenta'))
ring(0, -34, 7, zeon('magenta', 8), w=0.5)
for lx in (-9, 9):
    for lz in (-44, -24):
        box(lx, CY + 1, lz, lx, CY + 2, lz, zeon('black', 2, 'y'))
        S(lx, CY + 3, lz, 'createdeco:red_industrial_iron_lamp[facing=up,lit=true,inverted=true]')
for lx in range(-9, 10):
    for lz in range(-44, -23):
        if not inside(lx, lz, 1.5):
            dep = 3 if (lx + lz) % 2 else 2
            box(lx, CY - dep, lz, lx, CY - 2, lz, zeon('black', 9))
for (lx, lz) in ((-9, -44), (9, -44), (-9, -36), (9, -36)):
    for k in range(2, 16):
        zz = lz + k
        if (lx, zz) in TOP and CY - TOP[(lx, zz)] >= CY - k - 1:
            break
        S(lx, CY - k, zz, 'create:metal_girder[top=false,bottom=false,x=false,z=true]' if False else zeon('purple', 2, 'y'))
    for k in range(3, 18):
        S(lx, CY - k, lz + k - 1, zeno('black', 13, 'y'))
        if (lx, lz + k) in TOP:
            break
for lz in range(-44, -23, 4):
    S(-10, CY, lz, thin('cyan', 'up') if False else FLOOR)
for lz in range(-44, -23, 2):
    S(-10, CY + 1, lz, 'minecraft:end_rod[facing=up]')
    S(10, CY + 1, lz, 'minecraft:end_rod[facing=up]')
    S(-10, CY, lz, zeno('black', 9))
    S(10, CY, lz, zeno('black', 9))

for lz in range(-23, -16):
    for lx in range(-3, 4):
        S(lx, CY, lz, PATH)
    S(-3, CY, lz, neon('cyan'))
    S(3, CY, lz, neon('cyan'))
    if (lx, lz) not in TOP:
        pass
for lz in range(-24, -16):
    for lx in range(-3, 4):
        if not inside(lx, lz, 1):
            box(lx, CY - 3, lz, lx, CY - 1, lz, zeon('black', 9))

# pad control: call button + sign + kiosk
S(-8, CY + 1, -25, zeno('white', 17))
S(-8, CY + 2, -25, zeon('cyan', 5))
S(-7, CY + 1, -25, 'minecraft:polished_blackstone_button[face=wall,facing=east,powered=false]')
sign(B, CX - 8, CY + 1, CZ - 24, 'south', ['', 'SKYLINER', 'call button', ''], wood='crimson', color='light_blue')
sign(B, CX - 7, CY + 2, CZ - 25, 'east', ['NEON HEIGHTS', 'SKYPORT', 'sit to fly', 'to the city'], wood='crimson', color='magenta')

# ---------------- billboard gate ----------------
BZ = -21
for lx in (-15, 15):
    box(lx, CY - 2, BZ, lx, CY + 21, BZ, zeon('purple', 2, 'y'))
    box(lx, CY - 2, BZ + 1, lx, CY + 21, BZ + 1, zeon('black', 3, 'y'))
    S(lx, CY + 22, BZ, 'minecraft:end_rod[facing=up]')
    for y in range(CY - 6, CY - 2):
        S(lx, y, BZ, zeon('black', 9))
box(-14, CY + 8, BZ + 1, 14, CY + 21, BZ + 1, zeno('black', 0))
for lx in range(-14, 15):
    S(lx, CY + 8, BZ, zeon('cyan', 2, 'x'))
    S(lx, CY + 21, BZ, zeon('cyan', 2, 'x'))
w = text_width('HEIGHTS')
for (px, py) in text_xy('HEIGHTS'):
    S(w // 2 - px, CY + 10 + py, BZ, neon('magenta'))
w = text_width('NEON')
for (px, py) in text_xy('NEON'):
    S(w // 2 - px, CY + 15 + py, BZ, neon('cyan'))
for lx in range(-14, 15):
    S(lx, CY + 7, BZ + 1, f'{BB}cyberlight_bar_{CI["purple"]}[facing=down]')

# ---------------- central spire ----------------
TX, TZ = 0, 8
TR = 5
H_TOP = CY + 52
oct_cells = []
for lx in range(-TR, TR + 1):
    for lz in range(-TR, TR + 1):
        if abs(lx) + abs(lz) <= TR + 2:
            oct_cells.append((lx, lz))
cellset = set(oct_cells)
wall = [(x, z) for (x, z) in oct_cells if any((x + a, z + c) not in cellset for a, c in ((1, 0), (-1, 0), (0, 1), (0, -1)))]
corners = [(x, z) for (x, z) in wall if (abs(x) == TR and abs(z) == 2) or (abs(z) == TR and abs(x) == 2)]
disc(TX, TZ, 9, zeno('black', 6))
ring(TX, TZ, 9, neon('purple'))
for (x, z) in oct_cells:
    S(TX + x, CY, TZ + z, zeno('black', 17))
for y in range(CY + 1, H_TOP + 1):
    for (x, z) in wall:
        k = y - CY
        if (x, z) in corners:
            b = zeon('purple', 2, 'y')
        elif k % 8 == 0:
            b = zeon('cyan', 2, 'x' if abs(z) >= abs(x) else 'z')
        elif abs(x) + abs(z) == TR + 2 and k % 8 in (3, 4, 5):
            b = zeno('white', 3, 'y')
        else:
            b = 'minecraft:black_stained_glass'
        S(TX + x, y, TZ + z, b)
for (x, z) in wall:
    S(TX + x, H_TOP + 1, TZ + z, zeon('purple', 8))
    if (x, z) in corners:
        for k in range(2, 12 - abs(x) - abs(z) + 6):
            S(TX + x * 1, H_TOP + k, TZ + z * 1, zeon('purple', 2, 'y') if k < 6 else neon_alt('magenta'))
        S(TX + x, H_TOP + 12 - abs(x) - abs(z) + 6, TZ + z, 'minecraft:end_rod[facing=up]')
for (x, z) in oct_cells:
    if (x, z) not in cellset or (x, z) in wall:
        continue
    if abs(x) <= 1 and abs(z) <= 1:
        S(TX + x, H_TOP, TZ + z, 'minecraft:glass')
    else:
        S(TX + x, H_TOP, TZ + z, zeno('gray', 17))
S(TX, H_TOP, TZ, 'minecraft:glass')
box(TX - 1, CY, TZ - 1, TX + 1, CY, TZ + 1, 'minecraft:iron_block')
S(TX, CY + 1, TZ, 'minecraft:beacon')
S(TX, CY + 3, TZ, 'minecraft:magenta_stained_glass')
for (x, z) in ((-1, -1), (1, -1), (-1, 1), (1, 1)):
    S(TX + x, CY + 1, TZ + z, neon_alt('purple'))
DOOR = [(0, -TR), ]
for dz in (0,):
    for y in (CY + 1, CY + 2, CY + 3):
        for dx in (-1, 0, 1):
            S(TX + dx, y, TZ - TR - 2 + 0, 'minecraft:air') if False else None
for y in (CY + 1, CY + 2, CY + 3):
    for dx in (-1, 0, 1):
        for (x, z) in wall:
            if x == dx and z < 0 and abs(z) >= TR:
                S(TX + x, y, TZ + z, 'minecraft:air')
for dx in (-2, 2):
    S(TX + dx, CY + 1, TZ - TR - 2, neon_alt('cyan'))
path = []
side = 3
for i in range(-side, side + 1):
    path.append((i, side))
for i in range(side - 1, -side - 1, -1):
    path.append((side, i))
for i in range(side - 1, -side - 1, -1):
    path.append((i, -side))
for i in range(-side + 1, side + 1):
    path.append((-side, i))
path = [(x, z) for k, (x, z) in enumerate(path) if (x, z) not in path[:k]]
start = path.index((-side, side)) if (-side, side) in path else 0
path = path[start:] + path[:start]
seq = path * 2
y = CY
stairs = []
prev = None
for k, (x, z) in enumerate(seq):
    if y >= CY + 19:
        break
    nx, nz = seq[k + 1]
    dx, dz = nx - x, nz - z
    corner = False
    if k > 0:
        px, pz = seq[k - 1]
        corner = (x - px, z - pz) != (dx, dz)
    if corner:
        S(TX + x, y, TZ + z, zeno('white', 17))
        stairs.append((x, y, z))
        continue
    y += 1
    facing = {(1, 0): 'east', (-1, 0): 'west', (0, 1): 'south', (0, -1): 'north'}[(dx, dz)]
    S(TX + x, y, TZ + z, f'minecraft:polished_blackstone_brick_stairs[facing={facing},half=bottom]')
    for yy in range(CY + 1, y):
        if G(TX + x, yy, TZ + z) is None:
            S(TX + x, yy, TZ + z, zeno('gray', 2, 'y'))
    stairs.append((x, y, z))
    last = (nx, nz)
DECK = CY + 20
hole = {(x, z) for (x, yy, z) in stairs if yy >= DECK - 4}
for (x, z) in oct_cells:
    if (x, z) in wall:
        continue
    if abs(x) <= 1 and abs(z) <= 1:
        continue
    if (x, z) in hole:
        continue
    S(TX + x, DECK, TZ + z, zeno('white', 17))
lx0, lz0 = last
S(TX + lx0, DECK, TZ + lz0, zeno('white', 17))
for (x, z) in [(-1, -2), (0, -2), (1, -2), (-1, 2), (0, 2), (1, 2), (-2, -1), (-2, 0), (-2, 1), (2, -1), (2, 0), (2, 1)]:
    S(TX + x, DECK + 1, TZ + z, 'minecraft:glass_pane')
S(TX - 2, DECK + 1, TZ - 2, zeon('cyan', 8))
S(TX + 2, DECK + 1, TZ - 2, zeon('cyan', 8))
S(TX - 2, DECK + 1, TZ + 2, zeon('cyan', 8))
S(TX + 2, DECK + 1, TZ + 2, zeon('cyan', 8))
S(TX + 4, DECK + 1, TZ, 'pfm:black_simple_sofa[facing=west,shape=straight]')
S(TX + 4, DECK + 1, TZ + 1, 'pfm:black_simple_sofa[facing=west,shape=straight]')
S(TX + 4, DECK + 1, TZ - 1, 'pfm:black_simple_sofa[facing=west,shape=straight]')
S(TX, DECK + 1, TZ + 4, 'pfm:cyan_arm_chair[facing=north,shape=straight]')
S(TX + 1, DECK + 1, TZ + 4, 'pfm:cyan_arm_chair[facing=north,shape=straight]')
S(TX - 1, DECK + 1, TZ + 4, 'pfm:cyan_arm_chair[facing=north,shape=straight]')
sign(B, CX + TX, DECK + 2, CZ + TZ + 3, 'north', ['', 'OBSERVATION', 'DECK', ''], wood='crimson', color='cyan')
for (x, z) in wall:
    S(TX + x, DECK - 1, TZ + z, zeon('magenta', 2, 'x' if abs(z) >= abs(x) else 'z'))
for y in (CY + 8, CY + 16, DECK + 8, DECK + 16, DECK + 24):
    for (x, z) in oct_cells:
        if abs(x) + abs(z) == TR + 2 and (x, z) in wall:
            pass
sign(B, CX + TX, CY + 4, CZ + TZ - TR - 2, 'north', ['', 'NEON SPIRE', 'stairs inside', ''], wood='crimson', color='magenta')

# ---------------- paths ----------------
def path_seg(x0, z0, x1, z1, w=1, c='cyan'):
    n = max(abs(x1 - x0), abs(z1 - z0))
    for i in range(n + 1):
        x = round(x0 + (x1 - x0) * i / n)
        z = round(z0 + (z1 - z0) * i / n)
        for a in range(-w, w + 1):
            for b2 in range(-w, w + 1):
                if (x + a, z + b2) in TOP and G(x + a, CY, z + b2) in (None, 'minecraft:grass_block'):
                    S(x + a, CY, z + b2, PATH)


path_seg(0, -8, 17, -8)
path_seg(9, -2, 18, 8)
path_seg(6, 4, 14, 18)
path_seg(0, -8, -17, -3)
path_seg(-8, 0, -17, 14)
path_seg(0, 14, 0, 18)
path_seg(-6, 12, -15, 22)
path_seg(6, 12, 12, 15)
path_seg(0, 26, 0, 29)

# ---------------- east pods ----------------
PODS = [(18, -10, 'cyan', 'west'), (21, 5, 'magenta', 'west'), (12, 20, 'lime', 'north')]
for (px, pz, c, face) in PODS:
    r = 4
    for x in range(-r, r + 1):
        for z in range(-r, r + 1):
            d = math.hypot(x, z)
            if d <= r + 0.5:
                S(px + x, CY, pz + z, zeno('white', 17))
                S(px + x, CY + 5, pz + z, None) if False else None
    for y in range(CY + 1, CY + 8):
        k = y - CY
        rr = r + 0.5 if k <= 4 else math.sqrt(max(0, (r + 0.5) ** 2 - (k - 4) ** 2 * 2.2))
        for x in range(-r, r + 1):
            for z in range(-r, r + 1):
                d = math.hypot(x, z)
                if rr - 1 < d <= rr:
                    if k == 1:
                        b = zeon(c, 2, 'x' if abs(z) > abs(x) else 'z')
                    elif k in (2, 3):
                        b = 'minecraft:light_blue_stained_glass' if (x + z) % 3 else zeno('white', 3, 'y')
                    elif k == 4:
                        b = neon(c)
                    else:
                        b = zeno('white', 17)
                    S(px + x, y, pz + z, b)
                elif d <= rr - 1 and k >= 5 and rr - 2 < d:
                    S(px + x, y, pz + z, zeno('white', 17))
    top_y = CY + 7
    S(px, top_y, pz, zeno('white', 17))
    S(px, top_y + 1, pz, 'minecraft:lightning_rod[facing=up]')
    dx, dz = {'west': (-1, 0), 'north': (0, -1), 'east': (1, 0), 'south': (0, 1)}[face]
    for s in range(3, 6):
        for y in (CY + 1, CY + 2):
            S(px + dx * s, y, pz + dz * s, 'minecraft:air')
    S(px + dx * 4, CY + 1, pz + dz * 4, f'minecraft:iron_door[facing={ {"west": "east", "north": "south"}[face] },half=lower,hinge=left]')
    S(px + dx * 4, CY + 2, pz + dz * 4, f'minecraft:iron_door[facing={ {"west": "east", "north": "south"}[face] },half=upper,hinge=left]')
    S(px + dx * 5, CY + 1, pz + dz * 5, 'minecraft:heavy_weighted_pressure_plate')
    S(px + dx * 3, CY + 1, pz + dz * 3, 'minecraft:heavy_weighted_pressure_plate')
    for y in (CY + 1, CY + 2):
        S(px + dx * 5, y, pz + dz * 5, 'minecraft:air') if y == CY + 2 else None
    for s in (5, 6):
        S(px + dx * s, CY, pz + dz * s, PATH)
    ox, oz = -dx, -dz
    sx, sz = dz, dx
    S(px + ox * 3, CY + 1, pz + oz * 3, f'minecraft:{c}_bed[facing={ {"west": "west", "north": "north"}[face] },part=head]')
    S(px + ox * 2, CY + 1, pz + oz * 2, f'minecraft:{c}_bed[facing={ {"west": "west", "north": "north"}[face] },part=foot]')
    sofa_face = {'west': 'north', 'north': 'east'}[face]
    for s in (-1, 0, 1):
        S(px + sx * 3 + ox * s * 0 + (s * dx if False else 0) + (s if face == 'west' else 0) * 0, CY + 1, pz, None) if False else None
    if face == 'west':
        for s in (-1, 0, 1):
            S(px + s, CY + 1, pz + 3, f'pfm:{"white" if c != "lime" else "black"}_simple_sofa[facing=north,shape=straight]')
        S(px + 1, CY + 1, pz - 3, 'pfm:white_fridge[facing=south]')
        S(px + 1, CY + 2, pz - 3, 'pfm:white_freezer[facing=south]')
        S(px, CY + 1, pz - 3, 'pfm:iron_microwave[facing=south,open=false]')
        S(px - 1, CY + 1, pz - 3, 'pfm:trashcan[open=false]')
        S(px, CY + 1, pz, 'minecraft:quartz_slab[type=bottom]') if False else None
        S(px - 1, CY + 1, pz + 1, f'{BB}zeno_white_blockz_12')
        S(px - 1, CY + 2, pz + 1, 'minecraft:flower_pot')
    else:
        for s in (-1, 0, 1):
            S(px + 3, CY + 1, pz + s, 'pfm:black_simple_sofa[facing=west,shape=straight]')
        S(px - 3, CY + 1, pz - 1, 'pfm:white_fridge[facing=east]')
        S(px - 3, CY + 2, pz - 1, 'pfm:white_freezer[facing=east]')
        S(px - 3, CY + 1, pz, 'pfm:iron_microwave[facing=east,open=false]')
        S(px - 3, CY + 1, pz + 1, 'pfm:trashcan[open=false]')
        S(px + 1, CY + 1, pz + 1, f'{BB}zeno_white_blockz_12')
    S(px, CY + 5, pz, f'pfm:white_modern_pendant[up=false,down=false,lit=true]')
    S(px, CY + 1, pz, f'{BB}cyberlight_thin_{CI[c]}[facing=up]')
    sign(B, CX + px + dx * 5 + dz * 1, CY + 3, CZ + pz + dz * 5 + dx * 1, face, ['', 'POD', {'cyan': 'A-01', 'magenta': 'A-02', 'lime': 'A-03'}[c], ''], wood='crimson', color=c)
    S(px + dx * 5 + dz, CY + 1, pz + dz * 5 + dx, zeon('black', 2, 'y'))
    S(px + dx * 5 + dz, CY + 2, pz + dz * 5 + dx, zeon('black', 2, 'y'))
    if face == 'west':
        S(px + dx * 5, CY + 3, pz - 1, 'minecraft:air')

# ---------------- noodle bar (west) ----------------
X0, X1, Z0, Z1 = -27, -15, -9, 3
box(X0, CY, Z0, X1, CY, Z1, zeno('black', 11))
for y in range(CY + 1, CY + 6):
    for x in range(X0, X1 + 1):
        for z in range(Z0, Z1 + 1):
            if x in (X0, X1) or z in (Z0, Z1):
                k = y - CY
                if x == X1 and k in (1, 2, 3) and Z0 < z < Z1:
                    b = 'minecraft:magenta_stained_glass_pane' if k == 3 else 'minecraft:glass_pane'
                elif k == 5:
                    b = zeon('magenta', 2, 'x' if z in (Z0, Z1) else 'z')
                elif (x in (X0, X1)) and (z in (Z0, Z1)):
                    b = zeon('purple', 2, 'y')
                else:
                    b = zeno('black', 9)
                S(x, y, z, b)
box(X0, CY + 6, Z0, X1, CY + 6, Z1, zeno('gray', 17))
for x in range(X0, X1 + 1):
    for z in (Z0, Z1):
        S(x, CY + 7, z, zeon('magenta', 8) if x % 2 else zeon('black', 9))
for z in range(Z0, Z1 + 1):
    for x in (X0, X1):
        S(x, CY + 7, z, zeon('magenta', 8) if z % 2 else zeon('black', 9))
for y in (CY + 1, CY + 2):
    S(X1, y, -3, 'minecraft:air')
    S(X1, y, -4, 'minecraft:air')
S(X1, CY + 1, -3, 'create:framed_glass_door[facing=west,half=lower,hinge=left,open=false,powered=false]' if False else 'minecraft:air')
for z in range(Z0 + 1, Z1):
    S(-21, CY + 1, z, zeno('white', 12) if z not in (-3, -4) else 'minecraft:air')
    if z not in (-3, -4):
        S(-21, CY + 2, z, 'minecraft:smooth_quartz_slab[type=bottom]')
for z in (-7, -6, -1, 0, 1):
    S(-19, CY + 1, z, 'create:magenta_seat')
for z in range(Z0 + 1, Z1):
    S(-26, CY + 1, z, 'pfm:iron_stove[open=false,facing=east]' if z in (-6, 0) else zeno('gray', 12))
    S(-26, CY + 3, z, 'pfm:iron_oven_range_hood[facing=east,down=true,drawer=false]' if z in (-6, 0) else 'minecraft:air')
S(-26, CY + 2, -4, 'farmersdelight:cooking_pot')
S(-26, CY + 2, -2, 'pfm:iron_microwave[facing=east,open=false]')
S(-26, CY + 2, -8, 'pfm:iron_toaster[facing=east,on=false]') if False else None
S(-25, CY + 1, -8, 'pfm:gray_fridge[facing=east]')
S(-25, CY + 2, -8, 'pfm:gray_freezer[facing=east]')
S(-26, CY + 2, 2, 'farmersdelight:cutting_board[facing=east]')
S(-24, CY + 1, 2, 'minecraft:barrel[facing=up]')
S(-23, CY + 1, 2, 'pfm:trashcan[open=false]')
for x in range(X0 + 1, X1):
    for z in range(Z0 + 1, Z1):
        if (x + z) % 4 == 0:
            S(x, CY + 5, z, 'pfm:gray_modern_pendant[up=false,down=false,lit=true]')
for z in range(Z0 + 1, Z1):
    S(-20, CY, z, zeon('cyan', 2, 'z'))
sign(B, CX + X1 + 1, CY + 4, CZ - 3, 'east', ['', 'NEON', 'NOODLE BAR', ''], wood='crimson', color='magenta')
sign(B, CX + X1 + 1, CY + 4, CZ - 4, 'east', ['', 'OPEN', '24/7', ''], wood='crimson', color='cyan')
for z in range(Z0, Z1 + 1):
    S(X1 + 1, CY + 6, z, f'{BB}cyberlight_bar_vertical_{CI["magenta"]}[facing=down]') if False else None
for x in range(X0 + 2, X1 - 1, 3):
    S(x, CY + 8, Z0 + 2, 'create:encased_fan[facing=up]')
S(X0 + 2, CY + 8, Z1 - 2, 'minecraft:lightning_rod[facing=up]')
box(X0 + 2, CY + 9, Z1 - 2, X0 + 2, CY + 12, Z1 - 2, 'minecraft:iron_bars')
S(X0 + 2, CY + 13, Z1 - 2, 'createdeco:red_industrial_iron_lamp[facing=up,lit=true,inverted=true]')

# ---------------- arcade (south-west) ----------------
X0, X1, Z0, Z1 = -26, -16, 8, 18
box(X0, CY, Z0, X1, CY, Z1, zeno('black', 0))
for x in range(X0 + 1, X1):
    for z in range(Z0 + 1, Z1):
        S(x, CY, z, neon(COL[(x * 3 + z * 5) % 16]) if (x + z) % 2 == 0 else zeon('black', 9))
for y in range(CY + 1, CY + 6):
    for x in range(X0, X1 + 1):
        for z in range(Z0, Z1 + 1):
            if x in (X0, X1) or z in (Z0, Z1):
                k = y - CY
                if k == 5:
                    b = zeon('lime', 2, 'x' if z in (Z0, Z1) else 'z')
                elif x in (X0, X1) and z in (Z0, Z1):
                    b = zeon('lime', 2, 'y')
                elif z == Z0 and k in (2, 3) and X0 + 1 < x < X1 - 1:
                    b = 'minecraft:black_stained_glass_pane'
                else:
                    b = zeon('black', 10)
                S(x, y, z, b)
box(X0, CY + 6, Z0, X1, CY + 6, Z1, zeno('black', 17))
for y in (CY + 1, CY + 2):
    S(X1, y, 12, 'minecraft:air')
    S(X1, y, 13, 'minecraft:air')
for i, x in enumerate(range(X0 + 1, X1, 2)):
    c = COL[5 + i * 2 % 11]
    S(x, CY + 1, Z1 - 1, zeon('black', 9))
    S(x, CY + 2, Z1 - 1, neon(c))
    S(x, CY + 3, Z1 - 1, zeon(c if c in ('red', 'orange', 'yellow', 'lime', 'green', 'cyan', 'light_blue', 'blue', 'purple', 'magenta', 'pink') else 'cyan', 1))
    S(x, CY + 2, Z1 - 2, 'minecraft:stone_button[face=wall,facing=north,powered=false]' if False else 'minecraft:polished_blackstone_button[face=wall,facing=north,powered=false]')
    S(x, CY + 1, Z1 - 2, 'minecraft:air')
for x in range(X0 + 1, X1, 3):
    S(x, CY + 5, Z0 + 5, 'pfm:glass_modern_pendant[up=false,down=false,lit=true]')
S(X0 + 1, CY + 1, Z0 + 1, 'minecraft:jukebox')
S(X0 + 1, CY + 1, Z0 + 2, 'minecraft:note_block')
S(X0 + 1, CY + 1, Z0 + 5, 'create:nixie_tube[facing=east]')
S(X0 + 1, CY + 1, Z0 + 6, 'create:nixie_tube[facing=east]')
for z in (Z0 + 3, Z0 + 4):
    S(X0 + 1, CY + 1, z, 'pfm:froggy_chair_blue[tucked=false,facing=east]')
sign(B, CX + X1 + 1, CY + 4, CZ + 12, 'east', ['', 'NEON', 'ARCADE', ''], wood='crimson', color='lime')
sign(B, CX + X1 + 1, CY + 4, CZ + 13, 'east', ['', 'DANCE', 'FLOOR', ''], wood='crimson', color='yellow')

# ---------------- reactor / kinetic park (south) ----------------
disc(0, 22, 6, zeno('gray', 17))
ring(0, 22, 6, neon('lime'))
box(-1, CY + 1, 21, 1, CY + 1, 23, f'{BB}reactor_blockz_0')
for y in range(CY + 2, CY + 10):
    for x in (-1, 0, 1):
        for z in (21, 22, 23):
            if x == 0 and z == 22:
                S(x, y, z, f'{BB}cyberlight_cycle_blockz_0')
            elif abs(x) + abs(z - 22) == 2:
                S(x, y, z, f'{BB}reactor_7_blockz_{CI["lime"]}' if y % 3 == 0 else zeon('lime', 3, 'y'))
            else:
                S(x, y, z, 'minecraft:lime_stained_glass')
box(-1, CY + 10, 21, 1, CY + 10, 23, f'{BB}reactor_blockz_0')
S(0, CY + 11, 22, 'minecraft:end_rod[facing=up]')
for (x, z) in ((-3, 19), (3, 19), (-3, 25), (3, 25)):
    S(x, CY + 1, z, zeon('black', 9))
    S(x, CY + 2, z, 'createaddition:tesla_coil[facing=up,powered=false]')
for (x, z) in ((-5, 22), (5, 22)):
    S(x, CY + 1, z, 'create:creative_motor[facing=up]{ScrollValue:48}')
    for i, y in enumerate(range(CY + 2, CY + 13)):
        k = y - CY - 2
        if k in (1, 7):
            b = 'create:flywheel[axis=y]'
        elif k in (4, 10):
            b = 'create:large_cogwheel[axis=y]'
        else:
            b = 'create:shaft[axis=y]'
        S(x, y, z, b)
    S(x, CY + 13, z, neon_alt('lime'))
sign(B, CX, CY + 2, CZ + 16, 'north', ['', 'FUSION CORE', 'do not touch', ''], wood='warped', color='lime')

# ---------------- sky garden (south-west outer) ----------------
for (x, z) in ((-10, 22), (-20, 26), (-26, 20) if inside(-26, 20, 3) else (-12, 27)):
    pass
POND = (-10, 25)
for x in range(-14, -5):
    for z in range(21, 30):
        d = math.hypot(x - POND[0], (z - POND[1]) * 1.2)
        if d < 3.6 and inside(x, z, 2):
            S(x, CY, z, 'minecraft:water')
            S(x, CY - 1, z, 'minecraft:sea_lantern')
            S(x, CY - 2, z, 'minecraft:deepslate')
        elif d < 4.6 and inside(x, z, 1):
            S(x, CY, z, 'minecraft:moss_block')
            if (x + z) % 3 == 0:
                S(x, CY + 1, z, 'minecraft:azalea')
            elif (x + z) % 3 == 1:
                S(x, CY + 1, z, 'minecraft:pink_petals[facing=north,flower_amount=4]')
S(-10, CY, 25, 'minecraft:water')
TREES = [(-12, 16, 'cherry'), (-6, 30, 'cherry'), (-22, 24, 'cherry'), (24, -3, 'cherry'), (9, 26, 'cherry'), (-10, -20, 'cherry'), (10, -18, 'cherry'), (-25, -14, 'azalea_tree'), (26, 14, 'azalea_tree'), (-3, -30 + 60, 'cherry')]
TREES = [(x, z, t) for (x, z, t) in TREES if inside(x, z, 4)]
for (x, z, t) in TREES:
    S(x, CY, z, 'minecraft:grass_block')
flowers = ['minecraft:pink_petals[facing=east,flower_amount=3]', 'minecraft:allium', 'minecraft:blue_orchid', 'minecraft:lily_of_the_valley', 'minecraft:azure_bluet', 'minecraft:short_grass' if False else 'minecraft:grass']
for (lx, lz) in TOP:
    if G(lx, CY, lz) == 'minecraft:grass_block' and G(lx, CY + 1, lz) is None and (lx, lz) not in EDGE:
        r = random.random()
        if r < 0.12:
            S(lx, CY + 1, lz, random.choice(flowers))
        elif r < 0.14:
            S(lx, CY + 1, lz, 'minecraft:flowering_azalea')

# ---------------- comms dish (south-east) ----------------
DX, DZ = 21, 15
box(DX - 1, CY + 1, DZ - 1, DX + 1, CY + 1, DZ + 1, zeno('white', 17))
box(DX, CY + 2, DZ, DX, CY + 6, DZ, zeno('white', 3, 'y'))
for a in range(-4, 5):
    for b2 in range(-4, 5):
        d2 = a * a + b2 * b2
        if d2 <= 17:
            S(DX + a, CY + 7 + int(d2 / 6), DZ + b2 - 0, 'minecraft:white_concrete' if d2 > 2 else zeno('white', 17))
box(DX, CY + 8, DZ, DX, CY + 11, DZ, 'minecraft:iron_bars')
S(DX, CY + 12, DZ, 'createdeco:red_industrial_iron_lamp[facing=up,lit=true,inverted=true]')
sign(B, CX + DX, CY + 1, CZ + DZ - 2, 'north', ['', 'SKYNET', 'RELAY', ''], wood='crimson', color='light_blue')

# ---------------- Benson memorial (north-west rim, facing the city) ----------------
MX, MZ = -20, -16
for x in range(MX - 6, MX + 7):
    for z in range(MZ - 6, MZ + 7):
        d = math.hypot(x - MX, z - MZ)
        if (x, z) not in TOP or d > 6.4:
            continue
        if d > 5.6:
            S(x, CY, z, neon('light_blue'))
        elif d > 4.6:
            S(x, CY, z, 'minecraft:smooth_quartz')
        else:
            S(x, CY, z, 'minecraft:moss_block')
        for y in range(CY + 1, CY + 8):
            if G(x, y, z) is not None and 'glass' not in G(x, y, z):
                B.b.pop((CX + x, y, CZ + z), None)
box(MX - 2, CY + 1, MZ - 4, MX + 2, CY + 1, MZ + 4, 'minecraft:bone_block[axis=y]')
box(MX - 1, CY + 1, MZ - 4, MX + 1, CY + 1, MZ - 4, 'minecraft:smooth_quartz')
WY = CY + 2
WHITE, GREY, DARK = 'minecraft:white_wool', 'minecraft:light_gray_wool', 'minecraft:black_concrete'
wolf = {}
for x in (-1, 0, 1):
    for z in (1, 2):
        for y in (0, 1):
            wolf[(x, y, z)] = WHITE
    for y in (0, 1, 2):
        wolf[(x, y, 0)] = WHITE
    for z in (-1, 0):
        for y in (3, 4):
            wolf[(x, y, z)] = WHITE
    wolf[(x, 2, 0)] = 'minecraft:red_wool'
for x in (-1, 1):
    wolf[(x, 0, -1)] = GREY
    wolf[(x, 1, -1)] = GREY
    wolf[(x, 5, 0)] = GREY
    wolf[(x, 4, -2)] = 'minecraft:polished_blackstone_button[face=wall,facing=north]'
wolf[(0, 3, -2)] = GREY
wolf[(0, 4, -2)] = 'minecraft:air'
wolf[(0, 3, -3)] = 'minecraft:black_wool'
wolf[(0, 1, 3)] = WHITE
wolf[(0, 2, 3)] = GREY
for (x, y, z), b in wolf.items():
    S(MX + x, WY + y, MZ + z, b)
sign(B, CX + MX, CY + 1, CZ + MZ - 5, 'north', ['BENSON', 'Good boy.', 'Forever loyal,', 'always home.'], wood='cherry', color='light_blue')
sign(B, CX + MX, CY + 1, CZ + MZ + 5, 'south', ['', 'In loving', 'memory', ''], wood='cherry', color='white')
for (x, z) in ((MX - 3, MZ - 5), (MX + 3, MZ - 5), (MX - 3, MZ + 5), (MX + 3, MZ + 5)):
    if (x, z) in TOP:
        box(x, CY + 1, z, x, CY + 2, z, 'minecraft:cherry_fence')
        S(x, CY + 3, z, 'minecraft:soul_lantern[hanging=false]')
FL = ['minecraft:poppy', 'minecraft:cornflower', 'minecraft:lily_of_the_valley', 'minecraft:white_tulip', 'minecraft:pink_petals[facing=north,flower_amount=4]']
for x in range(MX - 4, MX + 5):
    for z in range(MZ - 4, MZ + 5):
        d = math.hypot(x - MX, z - MZ)
        if (x, z) in TOP and 3.2 < d <= 4.5 and G(x, CY + 1, z) is None and G(x, CY, z) == 'minecraft:moss_block':
            S(x, CY + 1, z, FL[(x * 3 + z) % len(FL)])
for x in (MX - 1, MX + 1):
    if (x, MZ + 6) in TOP:
        S(x, CY, MZ + 6, 'minecraft:smooth_quartz')
        S(x, CY + 1, MZ + 6, 'create:light_blue_seat')
S(MX, CY + 1, MZ + 3, 'minecraft:air') if False else None
MEMORIAL = {(x, z) for x in range(MX - 7, MX + 8) for z in range(MZ - 7, MZ + 8)}
TREES = [t for t in TREES if (t[0], t[1]) not in MEMORIAL]

# ---------------- railing ----------------
PROTECT = set()
for lz in range(-24, -13):
    for lx in range(-3, 4):
        PROTECT.add((lx, lz))
for (lx, lz) in EDGE:
    if (lx, lz) in PROTECT:
        continue
    if G(lx, CY + 1, lz) is not None:
        continue
    if -9 <= lx <= 9 and -44 <= lz <= -23:
        continue
    b = G(lx, CY, lz)
    if b in ('minecraft:water',):
        continue
    if (lx * 5 + lz * 3) % 9 == 0:
        S(lx, CY + 1, lz, zeon('black', 2, 'y'))
        S(lx, CY + 2, lz, neon_alt(['cyan', 'magenta', 'purple'][(lx + lz) % 3]))
    else:
        S(lx, CY + 1, lz, 'minecraft:light_blue_stained_glass_pane')
    if b == 'minecraft:grass_block':
        S(lx, CY, lz, zeno('black', 9))

# ---------------- mini islets ----------------
ISLETS = [(44, 170, -6, 5, 'cyan'), (-44, 192, 16, 4, 'magenta'), (12, 205, 48, 5, 'purple'), (-30, 160, -30, 3, 'light_blue')]
for (ix, iy, iz, r, c) in ISLETS:
    for x in range(-r, r + 1):
        for z in range(-r, r + 1):
            d = math.hypot(x, z)
            if d <= r:
                depth = int((r - d) * 1.8) + 1
                S(ix + x, iy, iz + z, 'minecraft:moss_block' if d < r - 1 else 'minecraft:grass_block')
                for k in range(1, depth + 1):
                    S(ix + x, iy - k, iz + z, 'minecraft:deepslate' if k > 1 else 'minecraft:dirt')
                if random.random() < 0.3:
                    S(ix + x, iy - depth, iz + z, neon(c))
    S(ix, iy - int(r * 1.8) - 2, iz, 'minecraft:amethyst_cluster[facing=down]')
    for k in range(1, 5):
        S(ix, iy + k, iz, neon_alt(c) if k < 4 else 'minecraft:end_rod[facing=up]')
    for (a, b2) in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        S(ix + a, iy + 1, iz + b2, 'minecraft:amethyst_cluster[facing=up]')
    S(ix + 2, iy + 1, iz + 1, 'minecraft:flowering_azalea')

# forceload + trees + finish
pre = [f'forceload add {CX - 48} {CZ - 50} {CX + 48} {CZ + 50}']
B.pre = pre
tree_cmds = [f'place feature minecraft:{t} {CX + x} {CY + 1} {CZ + z}' for (x, z, t) in TREES]
cmds = B.commands()
unknown = set()
import json
IDS = json.load(open('/tmp/claude-0/city/ids.json'))
import glob, os
BBS = {'betterblockz:' + os.path.basename(p)[:-5] for p in glob.glob('/tmp/claude-0/bb/assets/betterblockz/blockstates/*.json')}
PFM = {'pfm:' + os.path.basename(p)[:-5] for p in glob.glob('/tmp/claude-0/pf/assets/pfm/blockstates/*.json')}
for s in set(B.b.values()):
    bid = s.split('[')[0].split('{')[0]
    ns = bid.split(':')[0]
    if ns == 'betterblockz' and bid not in BBS or ns == 'pfm' and bid not in PFM or ns not in ('minecraft', 'betterblockz', 'pfm') and bid not in IDS:
        unknown.add(bid)
print('unknown', unknown)
names = write('isle_build', cmds + tree_cmds + ['say ISLE_DONE'], chunk=12000)
print(names, len(cmds), len(B.b))
json.dump({'EDGE': len(EDGE)}, open('/tmp/claude-0/city/isle_info.json', 'w'))

```

### sky.py
```python
import math, json, sys, os
import numpy as np
sys.path.insert(0, '/tmp/claude-0/city')
from lib import Build, sign
from skylib import *

SHIP = 'fireheart-sky01'
OA = (-3, 71, 79)
OB = (-3, 181, 229)
CFG = json.load(open('/tmp/claude-0/city/sky_cfg.json')) if os.path.exists('/tmp/claude-0/city/sky_cfg.json') else {}
EULER_ORDER = CFG.get('order', 'xyz')
YAW_SIGN = CFG.get('yaw_sign', 1)
COM_ADJ = CFG.get('com_adj', [0, 0, 0])
MARK = f'{BB}reactor_7_blockz_{CI["purple"]}'
CITY_BTN = (-9, 71, 77)
ISLE_BTN = (-10, 181, 245)


def ship_blocks():
    s = {}

    def p(x, y, z, b):
        s[(x, y, z)] = b

    rows = {0: (-2, 2), 12: (-2, 2), 13: (-1, 1)}
    for z in range(0, 14):
        a, b = rows.get(z, (-3, 3))
        for x in range(a, b + 1):
            if x in (a, b) and 1 <= z <= 11:
                blk = zeon('cyan', 2, 'z')
            elif x in (a, b) or z in (0, 13):
                blk = zeno('white', 17)
            else:
                blk = zeno('gray', 11)
            p(x, 0, z, blk)
    seats = [(-1, 4), (1, 4), (-1, 7), (1, 7)]
    for (x, z) in seats:
        p(x, 0, z, MARK)
        p(x, 1, z, 'create:purple_seat')
    p(0, 0, 10, MARK)
    p(0, 1, 10, 'create:magenta_seat')
    for y in (1, 2):
        for z in range(1, 12):
            for x in (-3, 3):
                if z in (1, 2, 11):
                    blk = zeno('white', 3, 'y')
                elif x == -3 and z in (5, 6):
                    continue
                else:
                    blk = 'minecraft:glass'
                p(x, y, z, blk)
        for x in (-2, -1, 1, 2):
            p(x, y, 1, zeno('white', 17))
        p(0, y, 1, zeon('magenta', 8))
        for x in (-2, 2):
            p(x, y, 12, 'minecraft:glass')
        for x in (-1, 0, 1):
            p(x, y, 13, 'minecraft:glass')
    for x in (-2, -1, 1, 2):
        p(x, 1, 11, zeon('cyan', 5) if abs(x) == 1 else zeno('black', 12))
    p(0, 1, 12, 'minecraft:lever[face=floor,facing=south,powered=false]')
    for z in range(1, 11):
        for x in range(-3, 4):
            if x == 0:
                blk = neon('cyan')
            elif abs(x) == 3:
                blk = zeon('cyan', 2, 'z')
            else:
                blk = zeno('white', 17)
            p(x, 3, z, blk)
    for z in (11, 12):
        for x in range(-2, 3):
            p(x, 3, z, 'minecraft:glass')
    p(-3, 3, 11, zeno('white', 17))
    p(3, 3, 11, zeno('white', 17))
    for z in (1, 2, 3, 4):
        p(0, 4, z, zeon('magenta', 8))
    for z in (1, 2):
        p(0, 5, z, zeon('magenta', 8))
    p(0, 4, 5, neon_alt('magenta'))
    for sx in (-1, 1):
        for z in range(0, 4):
            for y in (0, 1):
                p(4 * sx, y, z, zeon('blue', 5))
        for y in (0, 1):
            p(4 * sx, y, -1, neon('blue'))
        for z in range(4, 9):
            p(5 * sx, 0, z, 'minecraft:smooth_quartz_slab[type=top]')
            if 5 <= z <= 7:
                p(5 * sx, 1, z, thin('cyan', 'up'))
        for z in range(5, 8):
            p(6 * sx, 0, z, 'minecraft:smooth_quartz_slab[type=top]')
        p(6 * sx, 1, 6, 'createdeco:{}_industrial_iron_lamp[facing=up,lit=true,inverted=true]'.format('red' if sx > 0 else 'green'))
    for x in (-1, 0, 1):
        p(x, 1, 0, neon_alt('magenta') if x == 0 else zeon('black', 9))
    p(0, 2, 6, 'minecraft:light[level=15]')
    p(0, 2, 9, 'minecraft:light[level=12]')
    p(0, 2, 3, 'pfm:white_modern_pendant[up=true,down=false,lit=true]')
    return s


SB = ship_blocks()
MASS = {'glass': 0.5, 'slab': 0.5, 'light': 0.0, 'lever': 0.1, 'thin': 0.1, 'lamp': 0.3, 'seat': 0.5, 'pendant': 0.2}


def mass(b):
    for k, v in MASS.items():
        if k in b.split('[')[0] or (k == 'light' and b.startswith('minecraft:light')):
            return v
    return 1.0


tot = 0
cm = np.zeros(3)
for (x, y, z), b in SB.items():
    m = mass(b)
    tot += m
    cm += m * np.array([x + 0.5, y + 0.5, z + 0.5])
COM = cm / tot + np.array(COM_ADJ)
A = np.array(OA, float) + COM
Bp = np.array(OB, float) + COM


def smooth(u):
    u = min(1, max(0, u))
    return u * u * u * (u * (u * 6 - 15) + 10)


def ease(u):
    u = min(1, max(0, u))
    return u * u * (3 - 2 * u)


def rotm(yaw, pitch, roll):
    y, p, r = map(math.radians, (yaw, pitch, roll))
    Ry = np.array([[math.cos(y), 0, math.sin(y)], [0, 1, 0], [-math.sin(y), 0, math.cos(y)]])
    Rx = np.array([[1, 0, 0], [0, math.cos(p), -math.sin(p)], [0, math.sin(p), math.cos(p)]])
    Rz = np.array([[math.cos(r), -math.sin(r), 0], [math.sin(r), math.cos(r), 0], [0, 0, 1]])
    return Ry @ Rx @ Rz


def zyx(M):
    ay = math.asin(max(-1, min(1, -M[2, 0])))
    if abs(M[2, 0]) < 0.9999:
        ax = math.atan2(M[2, 1], M[2, 2])
        az = math.atan2(M[1, 0], M[0, 0])
    else:
        ax = 0
        az = math.atan2(-M[0, 1], M[1, 1])
    return math.degrees(ax), math.degrees(ay), math.degrees(az)


def euler_str(M):
    ax, ay, az = zyx(M)
    d = {'x': ax, 'y': ay, 'z': az}
    return ' '.join(f'{d[c]:.2f}' for c in EULER_ORDER)


def out_pose(t):
    T1, T2, T = 40, 620, 700
    if t <= T1:
        u = t / T1
        return A + [0, 5 * ease(u), 0], 0, 0, 2 * math.sin(t * 0.3) * u
    if t <= T2:
        u = (t - T1) / (T2 - T1)
        s = smooth(u)
        x = A[0] + 12 * math.sin(math.pi * s)
        z = A[2] + (Bp[2] - A[2]) * s
        y = (A[1] + 5) + ((Bp[1] + 10) - (A[1] + 5)) * ease(u ** 0.8) + 12 * math.sin(math.pi * u)
        return np.array([x, y, z]), None, None, None
    u = (t - T2) / (T - T2)
    return Bp + [0, 10 * (1 - ease(u)), 0], 0, 0, 1.5 * math.sin(t * 0.3) * (1 - u)


def back_pose(t):
    L, TURN, TR, TURN2, T = 40, 100, 680, 740, 820
    if t <= L:
        u = t / L
        return Bp + [0, 5 * ease(u), 0], 0, 0, 0
    if t <= TURN:
        u = (t - L) / (TURN - L)
        return Bp + [0, 5, 0], 180 * ease(u), 0, 6 * math.sin(math.pi * u)
    if t <= TR:
        u = (t - TURN) / (TR - TURN)
        s = smooth(u)
        x = Bp[0] - 12 * math.sin(math.pi * s)
        z = Bp[2] + (A[2] - Bp[2]) * s
        y = (Bp[1] + 5) + ((A[1] + 10) - (Bp[1] + 5)) * ease(u ** 1.2) + 10 * math.sin(math.pi * u)
        return np.array([x, y, z]), None, None, None
    if t <= TURN2:
        u = (t - TR) / (TURN2 - TR)
        return A + [0, 10, 0], 180 + 180 * ease(u), 0, -6 * math.sin(math.pi * u)
    u = (t - TURN2) / (T - TURN2)
    return A + [0, 10 * (1 - ease(u)), 0], 0, 0, 1.5 * math.sin(t * 0.3) * (1 - u)


def build_path(fn, T, base_yaw):
    P = []
    for t in range(T + 2):
        pos, yaw, pitch, roll = fn(min(t, T))
        P.append([np.array(pos, float), yaw, pitch, roll])
    for t in range(T + 1):
        if P[t][1] is None:
            d = P[min(t + 1, T)][0] - P[max(t - 1, 0)][0]
            h = math.hypot(d[0], d[2])
            P[t][1] = math.degrees(math.atan2(d[0], d[2]))
            P[t][2] = -max(-18, min(18, math.degrees(math.atan2(d[1], max(h, 1e-6))) * 0.7))
    yaws = [P[t][1] for t in range(T + 1)]
    for t in range(1, T + 1):
        while yaws[t] - yaws[t - 1] > 180:
            yaws[t] -= 360
        while yaws[t] - yaws[t - 1] < -180:
            yaws[t] += 360
    for t in range(T + 1):
        P[t][1] = yaws[t]
        if P[t][3] is None:
            dy = (yaws[min(t + 3, T)] - yaws[max(t - 3, 0)]) / 6
            P[t][3] = max(-20, min(20, -dy * 12))
    for k in range(3):
        for t in range(1, T):
            for j in (2, 3):
                P[t][j] = (P[t - 1][j] + 2 * P[t][j] + P[t + 1][j]) / 4
    return P[:T + 1]


ENG = [np.array([4.5, 1.0, -1.2]), np.array([-3.5, 1.0, -1.2]), np.array([0.5, 1.5, -0.2])]


def path_funcs(name, P, T, final_state, arrive_fn):
    lines_by_chunk = {}
    CH = 20
    for t in range(1, T + 1):
        pos, yaw, pitch, roll = P[t]
        M = rotm(yaw * YAW_SIGN, pitch, roll)
        if t < T:
            v = (P[t + 1][0] - P[t - 1][0]) * 10
        else:
            v = np.zeros(3)
        cond = f'execute if score #t sky matches {t} run '
        L = [cond + f'vs teleport {SHIP} {pos[0]:.3f} {pos[1]:.3f} {pos[2]:.3f} ({euler_str(M)}) ({v[0]:.3f} {v[1]:.3f} {v[2]:.3f}) (0 0 0)']
        if t < T and t > 20:
            for e in ENG[:2]:
                w = pos + M @ (e - COM)
                L.append(cond + f'particle minecraft:end_rod {w[0]:.2f} {w[1]:.2f} {w[2]:.2f} 0.08 0.08 0.08 0.01 2')
            if t % 2 == 0:
                w = pos + M @ (ENG[2] - COM)
                L.append(cond + f'particle minecraft:electric_spark {w[0]:.2f} {w[1]:.2f} {w[2]:.2f} 0.3 0.2 0.3 0.05 3')
        if t % 30 == 0 and t < T:
            L.append(cond + f'playsound minecraft:block.beacon.ambient master @a {pos[0]:.1f} {pos[1]:.1f} {pos[2]:.1f} 3 1.6')
        if t % 20 == 0:
            L.append(cond + 'effect give @a minecraft:slow_falling 12 0 true')
        lines_by_chunk.setdefault((t - 1) // CH, []).extend(L)
    disp = []
    for k, L in sorted(lines_by_chunk.items()):
        lo, hi = k * CH + 1, (k + 1) * CH
        write(f'path/{name}_{k}', L)
        disp.append(f'execute if score #t sky matches {lo}..{hi} run function sky:path/{name}_{k}')
    write(f'path/{name}', disp)


OUT_T, BACK_T = 700, 820
POUT = build_path(out_pose, OUT_T, 0)
PBACK = build_path(back_pose, BACK_T, 180)
path_funcs('out', POUT, OUT_T, 2, 'arrive_isle')
path_funcs('back', PBACK, BACK_T, 0, 'arrive_city')

sel_rider = '@a[tag=sky_rider]'
write('load', [
    'scoreboard objectives add sky dummy',
    'execute unless score #state sky matches 0.. run scoreboard players set #state sky 0',
    'execute unless score #lock sky matches 0.. run scoreboard players set #lock sky 0',
    'scoreboard players set #wait sky 0',
    'execute if score #state sky matches 1 run scoreboard players set #state sky 0',
    'execute if score #state sky matches 3 run scoreboard players set #state sky 2',
    'forceload add -11 77 5 95',
    'forceload add -13 226 7 246',
])
write('tick', [
    'execute if score #state sky matches 0 run function sky:dock',
    'execute if score #state sky matches 2 run function sky:dock',
    'execute if score #state sky matches 1 run function sky:fly_out',
    'execute if score #state sky matches 3 run function sky:fly_back',
])
write('riders', [
    'tag @a remove sky_rider',
    f'execute as @e[type=create:seat] at @s if block ~ ~-1 ~ {MARK} on passengers if entity @s[type=player] run tag @s add sky_rider',
    f'execute as @e[type=create:seat] at @s if block ~ ~-0.6 ~ {MARK} on passengers if entity @s[type=player] run tag @s add sky_rider',
])
write('dock', [
    'function sky:riders',
    'execute unless entity @a[tag=sky_rider] run scoreboard players set #wait sky 0',
    'execute unless entity @a[tag=sky_rider] run scoreboard players set #lock sky 0',
    f'execute if score #state sky matches 0 if block {CITY_BTN[0]} {CITY_BTN[1]} {CITY_BTN[2]} minecraft:polished_blackstone_button[powered=true] run title @a actionbar {{"text":"The Skyliner is already here - take a seat!","color":"aqua"}}',
    f'execute if score #state sky matches 2 if block {ISLE_BTN[0]} {ISLE_BTN[1]} {ISLE_BTN[2]} minecraft:polished_blackstone_button[powered=true] run title @a actionbar {{"text":"The Skyliner is already here - take a seat!","color":"aqua"}}',
    f'execute if score #state sky matches 2 if block {CITY_BTN[0]} {CITY_BTN[1]} {CITY_BTN[2]} minecraft:polished_blackstone_button[powered=true] unless entity @a[tag=sky_rider] run function sky:call',
    f'execute if score #state sky matches 0 if block {ISLE_BTN[0]} {ISLE_BTN[1]} {ISLE_BTN[2]} minecraft:polished_blackstone_button[powered=true] unless entity @a[tag=sky_rider] run function sky:call',
    'execute if entity @a[tag=sky_rider] if score #lock sky matches 0 run function sky:countdown',
])
write('call', [
    'title @a actionbar {"text":"Skyliner called - on its way!","color":"light_purple"}',
    'execute as @a at @s run playsound minecraft:block.note_block.chime master @s ~ ~ ~ 1 1.5',
    'function sky:depart',
])
write('countdown', [
    'scoreboard players add #wait sky 1',
    f'execute if score #wait sky matches 1 run title {sel_rider} times 10 50 20',
    f'execute if score #wait sky matches 1 run title {sel_rider} subtitle ' + '{"text":"Next stop: NEON HEIGHTS","color":"aqua"}' ,
    f'execute if score #wait sky matches 1 if score #state sky matches 2 run title {sel_rider} subtitle ' + '{"text":"Next stop: FIREHEART CITY","color":"gold"}',
    f'execute if score #wait sky matches 1 run title {sel_rider} title ' + '{"text":"NEON SKYLINER","color":"light_purple","bold":true}',
    f'execute if score #wait sky matches 20 run title {sel_rider} actionbar ' + '{"text":"Departing in 3...","color":"aqua"}',
    f'execute if score #wait sky matches 40 run title {sel_rider} actionbar ' + '{"text":"Departing in 2...","color":"aqua"}',
    f'execute if score #wait sky matches 60 run title {sel_rider} actionbar ' + '{"text":"Departing in 1...","color":"aqua"}',
    f'execute if score #wait sky matches 20 as {sel_rider} at @s run playsound minecraft:block.note_block.pling master @s ~ ~ ~ 1 1',
    f'execute if score #wait sky matches 40 as {sel_rider} at @s run playsound minecraft:block.note_block.pling master @s ~ ~ ~ 1 1',
    f'execute if score #wait sky matches 60 as {sel_rider} at @s run playsound minecraft:block.note_block.pling master @s ~ ~ ~ 1 1',
    'execute if score #wait sky matches 80.. run function sky:depart',
])
write('depart', [
    'scoreboard players set #wait sky 0',
    'scoreboard players set #t sky 0',
    'scoreboard players set #lock sky 1',
    'execute if score #state sky matches 2 run scoreboard players set #state sky 3',
    'execute if score #state sky matches 0 run scoreboard players set #state sky 1',
    'title @a[tag=sky_rider] actionbar {"text":"Lift-off!","color":"light_purple","bold":true}',
    'execute as @a at @s run playsound minecraft:block.beacon.activate master @s ~ ~ ~ 1 0.8',
    'execute as @a[tag=sky_rider] at @s run playsound minecraft:entity.firework_rocket.launch master @s ~ ~ ~ 1 0.6',
])
write('fly_out', [
    'scoreboard players add #t sky 1',
    'function sky:path/out',
    f'execute if score #t sky matches {OUT_T}.. run function sky:arrive_isle',
])
write('fly_back', [
    'scoreboard players add #t sky 1',
    'function sky:path/back',
    f'execute if score #t sky matches {BACK_T}.. run function sky:arrive_city',
])
write('arrive_isle', [
    'scoreboard players set #state sky 2',
    'scoreboard players set #t sky 0',
    'title @a times 10 60 20',
    'title @a subtitle {"text":"Sneak to step off - sit again to fly home","color":"aqua"}',
    'title @a title {"text":"NEON HEIGHTS","color":"light_purple","bold":true}',
    'execute as @a at @s run playsound minecraft:block.beacon.deactivate master @s ~ ~ ~ 1 1.2',
    'execute as @a at @s run playsound minecraft:ui.toast.challenge_complete master @s ~ ~ ~ 0.6 1.4',
])
write('arrive_city', [
    'scoreboard players set #state sky 0',
    'scoreboard players set #t sky 0',
    'title @a times 10 60 20',
    'title @a subtitle {"text":"Sneak to step off - sit again to fly to Neon Heights","color":"gold"}',
    'title @a title {"text":"FIREHEART CITY","color":"gold","bold":true}',
    'execute as @a at @s run playsound minecraft:block.beacon.deactivate master @s ~ ~ ~ 1 1.2',
])
e0 = ' '.join('0' for _ in range(3))
write('reset', [
    f'vs teleport {SHIP} {A[0]:.3f} {A[1]:.3f} {A[2]:.3f} (0 0 0) (0 0 0) (0 0 0)',
    'scoreboard players set #state sky 0',
    'scoreboard players set #t sky 0',
    'scoreboard players set #lock sky 0',
    'say SKY_RESET',
])
write('park_isle', [
    f'vs teleport {SHIP} {Bp[0]:.3f} {Bp[1]:.3f} {Bp[2]:.3f} (0 0 0) (0 0 0) (0 0 0)',
    'scoreboard players set #state sky 2',
    'scoreboard players set #t sky 0',
    'say SKY_PARK_ISLE',
])
write('status', [
    'tellraw @a ["",{"text":"[Skyliner] state="},{"score":{"name":"#state","objective":"sky"}},{"text":" t="},{"score":{"name":"#t","objective":"sky"}},{"text":" lock="},{"score":{"name":"#lock","objective":"sky"}},{"text":" wait="},{"score":{"name":"#wait","objective":"sky"}}]',
])
write('seatdbg', [
    'execute as @a at @s on vehicle run tellraw @a ["SEATDBG ",{"entity":"@s","nbt":"Pos"}]',
    f'execute as @a on vehicle at @s if block ~ ~-1 ~ {MARK} run tellraw @a "SEATDBG marker-1 ok"',
    f'execute as @a on vehicle at @s if block ~ ~-0.6 ~ {MARK} run tellraw @a "SEATDBG marker-0.6 ok"',
])

# ---------------- city skyport pad + ship build ----------------
C = Build()
X0, X1, Z0, Z1, Y = -11, 5, 77, 95, 70
C.pre.append(f'fill -4 63 75 0 69 77 air replace minecraft:water')
C.pre.append(f'fill {X0} 71 {Z0} {X1} 78 {Z1} air')
C.pre.append('fill -4 71 76 0 71 76 air')
for x in range(X0, X1 + 1):
    for z in range(Z0, Z1 + 1):
        edge = x in (X0, X1) or z in (Z0, Z1)
        C.set(x, Y, z, neon('cyan') if edge else zeno('black', 0))
        C.set(x, Y - 1, z, zeno('gray', 0))
for x in range(X0 + 1, X1):
    C.set(x, Y, Z1 - 1, f'{BB}hazard_blockz_0[facing=up]')
for z in range(Z0 + 1, Z1):
    C.set(X0 + 1, Y, z, f'{BB}lablink_19_blockz_14[axis=z]')
    C.set(X1 - 1, Y, z, f'{BB}lablink_19_blockz_14[axis=z]')
for x in range(-7, 2):
    for z in range(80, 92):
        if (abs(x + 3) == 4 or z in (80, 91)):
            C.set(x, Y, z, zeon('magenta', 8))
for z in range(82, 90):
    C.set(-5, Y, z, neon('magenta'))
    C.set(-1, Y, z, neon('magenta'))
for x in range(-5, 0):
    C.set(x, Y, 86, neon('magenta'))
for x in (X0, X1):
    for z in (Z0, Z1, 86):
        C.box(x, 34, z, x, Y - 1, z, zeon('black', 2, 'y'))
for x in (X0 + 8,):
    for z in (Z0 + 6, Z1 - 6):
        pass
for x in range(X0, X1 + 1, 4):
    C.box(x, Y - 3, Z1, x, Y - 2, Z1, zeno('black', 13, 'y'))
for (x, z) in ((X0, Z1), (X1, Z1), (X1, Z0)):
    C.box(x, 71, z, x, 72, z, zeon('black', 2, 'y'))
    C.set(x, 73, z, 'createdeco:red_industrial_iron_lamp[facing=up,lit=true,inverted=true]')
for z in range(Z0 + 2, Z1, 2):
    C.set(X1, 71, z, 'minecraft:end_rod[facing=up]')
    C.set(X0, 71, z, 'minecraft:end_rod[facing=up]')
C.set(-10, 71, 77, zeno('white', 17))
C.set(-10, 72, 77, zeon('cyan', 5))
C.set(-9, 71, 77, 'minecraft:polished_blackstone_button[face=wall,facing=east,powered=false]')
sign(C, -10, 71, 76, 'north', ['', 'SKYLINER', 'call button', ''], wood='crimson', color='light_blue')
for x in (-5, 1):
    C.box(x, 71, 76, x, 74, 76, zeon('purple', 2, 'y'))
    C.set(x, 75, 76, neon_alt('magenta'))
C.box(-5, 75, 76, 1, 75, 76, zeon('purple', 2, 'x'))
C.set(-2, 75, 76, neon_alt('cyan'))
sign(C, -2, 74, 77, 'south', ['NEON', 'SKYPORT', 'sit in the', 'Skyliner to fly'], wood='crimson', color='magenta')
sign(C, -2, 74, 75, 'north', ['', 'NEON SKYPORT', 'Skyliner to', 'Neon Heights'], wood='crimson', color='aqua')
for (lx, ly, lz), b in SB.items():
    C.set(OA[0] + lx, OA[1] + ly, OA[2] + lz, b)
C.set(OA[0] + 6, OA[1] + 5, OA[2] + 13, 'minecraft:black_concrete')
write('city_skyport', C.commands() + ['say CITY_SKYPORT_DONE'])

S2 = Build()
S2.pre.append(f'fill {OA[0]-6} {OA[1]} {OA[2]-1} {OA[0]+6} {OA[1]+5} {OA[2]+13} air')
for (lx, ly, lz), b in SB.items():
    S2.set(OA[0] + lx, OA[1] + ly, OA[2] + lz, b)
S2.set(OA[0] + 6, OA[1] + 5, OA[2] + 13, 'minecraft:black_concrete')
write('ship_build', S2.commands() + ['say SHIP_BUILD_DONE'])

info = {'COM': COM.tolist(), 'A': A.tolist(), 'B': Bp.tolist(), 'first': [OA[0] - 6, OA[1], OA[2] - 1], 'click': [OA[0] + 6, OA[1] + 5, OA[2] + 13], 'blocks': len(SB)}
json.dump(info, open('/tmp/claude-0/city/sky_info.json', 'w'))
print(info)
write('pack_meta_dummy', ['#'])
os.remove(f'{FN}/pack_meta_dummy.mcfunction')
os.makedirs(OUT + '/data/minecraft/tags/functions', exist_ok=True)
json.dump({'values': ['sky:tick']}, open(OUT + '/data/minecraft/tags/functions/tick.json', 'w'))
json.dump({'values': ['sky:load']}, open(OUT + '/data/minecraft/tags/functions/load.json', 'w'))
json.dump({'pack': {'pack_format': 15, 'description': 'Neon Skyport - Fireheart'}}, open(OUT + '/pack.mcmeta', 'w'))

```

### region.py
```python
import zlib, io, nbtlib
RD = "/mnt/user-data/uploads/ModrinthApp/profiles/Create_ Remastered/saves/Create!/region/"
def decode(data, n, bits):
    out = []; mask = (1 << bits) - 1; per = 64 // bits
    for l in data:
        v = int(l) & ((1 << 64) - 1)
        for i in range(per):
            out.append((v >> (i * bits)) & mask)
            if len(out) == n: return out
    return out
def load(X0, X1, Y0, Y1, Z0, Z1, props=True):
    W = {}
    regs = {(x >> 9, z >> 9) for x in (X0, X1) for z in (Z0, Z1)}
    for rx in range(X0 >> 9, (X1 >> 9) + 1):
        for rz in range(Z0 >> 9, (Z1 >> 9) + 1):
            try: f = open(f"{RD}r.{rx}.{rz}.mca", 'rb').read()
            except FileNotFoundError: continue
            for i in range(1024):
                off = int.from_bytes(f[i*4:i*4+3], 'big')
                if not off: continue
                p = off * 4096; ln = int.from_bytes(f[p:p+4], 'big')
                if f[p+4] != 2: continue
                nb = nbtlib.File.parse(io.BytesIO(zlib.decompress(f[p+5:p+4+ln])))
                cx, cz = int(nb['xPos']) * 16, int(nb['zPos']) * 16
                if cx + 16 <= X0 or cx > X1 or cz + 16 <= Z0 or cz > Z1: continue
                for s in nb['sections']:
                    sy = int(s['Y']) * 16
                    if sy + 16 <= Y0 or sy > Y1 or 'block_states' not in s: continue
                    bs = s['block_states']; pal = []
                    for e in bs['palette']:
                        n = str(e['Name']).replace('minecraft:', '')
                        if props and 'Properties' in e:
                            n += '[' + ','.join(f'{k}={v}' for k, v in e['Properties'].items()) + ']'
                        pal.append(n)
                    if len(pal) == 1 and pal[0] == 'air': continue
                    idx = decode(bs['data'], 4096, max(4, (len(pal) - 1).bit_length())) if 'data' in bs else [0] * 4096
                    for k, v in enumerate(idx):
                        y = sy + k // 256; z = cz + (k // 16) % 16; x = cx + k % 16
                        if X0 <= x <= X1 and Y0 <= y <= Y1 and Z0 <= z <= Z1 and pal[v] != 'air':
                            W[(x, y, z)] = pal[v]
    return W

```

### preview.py
```python
import sys, glob, os, re
from PIL import Image
import numpy as np
BBT='/tmp/claude-0/bb/assets/betterblockz/textures/block/'
cache={}
def color(s):
    b=s.split('[')[0].split('{')[0]
    if b in cache: return cache[b]
    c=None
    if b.startswith('betterblockz:'):
        n=b.split(':')[1]
        m=re.match(r'(cyberlight_(?:bar_vertical|bar|thin_alt|thin|alt_blockz|blockz))_(\d+)',n)
        if m: n='cyberlight_blockz_'+m.group(2)
        n=n.replace('lablink_19_blockz','lablink_19_blockz')
        p=BBT+n+'.png'
        if not os.path.exists(p):
            ps=glob.glob(BBT+n+'*.png'); p=ps[0] if ps else None
        if p:
            im=np.array(Image.open(p).convert('RGB')); w=im.shape[1]; c=tuple(int(v) for v in im[:w,:w].reshape(-1,3).mean(0))
    if c is None:
        K=[('grass_block',(90,150,60)),('water',(50,90,220)),('glass',(170,200,230)),('deepslate',(70,70,75)),('blackstone',(40,35,40)),('tuff',(110,110,100)),('basalt',(80,80,85)),('dirt',(120,85,60)),('moss',(80,120,40)),('azalea',(70,130,50)),('petals',(240,160,200)),('amethyst',(160,110,220)),('end_rod',(250,250,240)),('seat',(140,70,190)),('lamp',(250,100,80)),('iron',(200,200,200)),('beacon',(120,230,230)),('white',(235,235,235)),('stairs',(50,45,55)),('pfm',(200,200,210)),('create',(170,140,90)),('sea_lantern',(200,230,220)),('quartz',(230,225,220)),('bed',(200,80,200))]
        c=(255,0,255)
        for k,v in K:
            if k in b: c=v; break
    cache[b]=c; return c
def render(blocks, axis, out, scale=4, rng=None):
    xs=[k[0] for k in blocks]; ys=[k[1] for k in blocks]; zs=[k[2] for k in blocks]
    x0,x1,y0,y1,z0,z1=min(xs),max(xs),min(ys),max(ys),min(zs),max(zs)
    if axis=='top':
        W,H=x1-x0+1,z1-z0+1; depth={}
        img=np.zeros((H,W,3),np.uint8)+20; best={}
        for (x,y,z),s in blocks.items():
            k=(x,z)
            if k not in best or y>best[k][0]: best[k]=(y,s)
        for (x,z),(y,s) in best.items():
            sh=0.6+0.4*(y-y0)/(y1-y0+1)
            img[z-z0,x-x0]=[min(255,int(v*sh)) for v in color(s)]
    else:
        if axis=='south':
            W,H=x1-x0+1,y1-y0+1; img=np.zeros((H,W,3),np.uint8)+20; best={}
            for (x,y,z),s in blocks.items():
                k=(x,y)
                if k not in best or z>best[k][0]: best[k]=(z,s)
            for (x,y),(z,s) in best.items():
                img[y1-y,x-x0]=color(s)
        else:
            W,H=z1-z0+1,y1-y0+1; img=np.zeros((H,W,3),np.uint8)+20; best={}
            for (x,y,z),s in blocks.items():
                k=(z,y)
                if k not in best or x<best[k][0]: best[k]=(x,s)
            for (z,y),(x,s) in best.items():
                img[y1-y,z-z0]=color(s)
    Image.fromarray(img).resize((img.shape[1]*scale,img.shape[0]*scale),Image.NEAREST).save(out)

```

### sky_cfg.json
```json
{"order":"xyz","yaw_sign":1,"com_adj":[0.036,0.0,-0.399]}

```


---

# PART 7 – SESSION 12 (Sept 21 2026): crash fixes + "Fireheart City Residents" custom mod

**Read this part first when continuing the mod work.** The full mod source and build tools are in `Claude outputs\fireheartcity_mod_source.zip` (the cloud workspace does NOT carry over to a new chat; unzip it into the new workspace, e.g. `/tmp/modbuild`).

## 7.1 What the user asked for (in order)
1. Fix the crash ("it keeps crashing") -> done (two different crashes, see 7.3).
2. A custom mod that makes the city lively: player-like residents with names, jobs, homes, daily routines, who meet each other as strangers, gradually learn names/jobs/homes, become friends, visit each other, chat, and take trips to Neon Heights. Chosen options: look = "player-like people", Skyliner = "their own shuttle", start = "test mod first".
3. Conversations only in person (not across the map), and no chat spam: custom animated speech-bubble UI instead of chat messages -> done (v0.3).
4. Fill holes accidentally broken by my clicks while controlling the PC -> done (5 blocks restored by diffing against the 16:11 backup: -4 69 58 dirt, -4 70 58 / -2 70 60 createdieselgenerators:asphalt_block, -2 70 75 spruce_planks, 68 77 108 grass_block).
5. LATEST (user went to bed, wants it done by morning): fix the apartment elevator; residents must really use it, only one rider at a time, others call it and wait their turn until the previous rider has left the cabin and reached their room; make residents smarter: trade items, eat, social status, relationships, awareness of what happens in the world and of changes. "Way more lively and smarter."

## 7.2 Current state (end of session 12)
- Installed in the game: `profiles\Create_ Remastered\mods\fireheartcity-0.1.0.jar` = **v0.4.0 first build (125,694 bytes)**. The filename stays 0.1.0 because files can't be deleted from here; the real version is in mods.toml.
- A **newer, better v0.4 build (≈129.6 KB)** is in the zip as `fireheartcity-latest.jar` and was also installed at the end of session 12 if the commit succeeded (check the size of the jar in the mods folder: ≈129.6 KB = latest). It adds: try/catch around every tick/event/render (a mod bug can no longer crash the world), a working elevator auto-repair, correct cabin detection, faster boarding/exiting, a routine-day fix (residents no longer flip plans at midnight), and `/city elevator debug`.
- The latest build was tested on a **headless Forge dedicated server in the cloud** with a copy of the user's world (see 7.6). Verified there: the elevator auto-rebuilds, and residents queue and ride one at a time (log: Leo, Rosa, Ava, Theo, Mia, Zara, Nina, Omar each CALL -> BOARD -> RIDE -> EXIT -> WALKOFF, others waiting in the queue). It has NOT yet been seen in the real game client, so the bubbles/gestures/eating animations of v0.4 still need an in-game look.
- The user's world has 16 residents spawned (via `/city spawnall`). Saved data lives in `saves\Create!\data\fireheartcity.dat`.

### Pending / next steps
1. In the real game: check v0.4 visually (elevator line, riding, bubbles, eating, gestures, trading, name-tag stars/hearts). Run `/city elevator`, `/city census`, `/city events`, `/city who mia`.
2. Known issue seen on the test server: walking into/out of the cabin sometimes fails (Create contraption door/collision), so boarding/exiting falls back to a short teleport after 3 s. Could be improved (e.g. open the cabin door or leave the door out of the cabin).
3. Residents' shuttle is still "virtual" (invisible for 35 s, then appear at the other pad). A real second airship for residents was promised earlier and is still owed.
4. Ideas not done yet: residents sitting on seats, sleeping in beds, carrying visible items for longer, more jobs/buildings, a notice board block showing `/city events`.
5. Keep backups before big changes (backups folder has 15:45 and 16:11 zips from Sept 21).

## 7.3 Crash fixes
- **"Image is not allocated" at startup** (armor-trim texture generation): resource pack `resourcepacks\FireheartStability\` (pack_format 15) with `assets/minecraft/atlases/armor_trims.json` and `blocks.json` = `{"sources":[{"type":"filter","pattern":{"path":"trims/.*"}}]}`. Enabled in options.txt: `resourcePacks:["pfm-asset-resources","vanilla","mod_resources","file/FireheartStability"]`. Side effect: armor trims show no texture (harmless in creative).
- **"Error loading mods" ConcurrentModificationException** (random, also happened Sept 16 before the mod existed): Ponder's `StitchedSprite.ALL` HashMap and the `SpriteShifter`/`CTSpriteShifter` `ENTRY_CACHE` HashMaps are filled from several mod-loading threads at once (triggered by Create Encased). Fixed by mixins inside fireheartcity: `StitchedSpriteMixin` (ALL -> ConcurrentHashMap, lists -> CopyOnWriteArrayList), `SpriteShifterMixin`, `CTSpriteShifterMixin` (ENTRY_CACHE -> ConcurrentHashMap). Config `fireheartcity.mixins.json`, MANIFEST `MixinConfigs: fireheartcity.mixins.json`, all injections `remap=false`, no refmap.
- After these, launches were clean. One launch "froze" at ~1 fps after loading the world (laptop RAM: 4 GB allocated, 71 % used); closing and relaunching fixed it.

## 7.4 Mod design (package com.fireheart.city)
- **FireheartCity**: @Mod("fireheartcity"), entity `fireheartcity:resident` (0.6x1.8, MISC), attributes, commands, event listeners (level tick, block break/place, explosion, login, server stopped), `LOG`.
- **Place**: named places (bakery -36 71 9, diner 7 71 53, supply 19 71 53, market 31 71 53, garage -2 71 -16, factory 2 71 4, gravel -30 71 52, port -25 71 -21, marina 22 64 62, fuel 19 71 -17, plaza -20 71 30, park -20 71 18, clock -26 71 20, pier -2 71 72, boardwalk 9 64 67, skyport -7 71 90, isle_pad -11 181 236, isle_plaza -4 181 261, noodle -26 181 268, arcade -25 181 283, memorial -24 182 251; apartments apt1A..apt5D at x21/29 z20/29 y75..91 (reached by walking + elevator, no teleport any more); pods pod1-3 on the island with entrances). CITY_PORT -7 71 90, ISLE_PORT -11 181 236. Hangout, indoor (rain), fun and date spot lists.
- **Job** (16 jobs, each with workplace, tool, shout, customer order/reply) / **Trait** (chattiness, sky-trip chance) / **Cast** (16 residents: mia baker, leo cook, ava clerk, omar grocer, rosa mechanic, ben factory, kai quarry, nina crane, sam dockmaster, ivy gardener, theo attendant, zara clockkeeper — all in Ember Heights; luna noodle chef, rex arcade, nova guide, jet pilot — in pods on Neon Heights).
- **CityData** (SavedData "fireheartcity"): Profile (job, trait, home, skin, coins, hunger, social, fun, rep, tier, partner, inventory map, known event ids), Rel per pair (familiarity, chats, met, knowsJob, knowsHome, affinity, romance, dates, rival), plans, news (60), events (80), shop stock, what each player has been told, `chatlog` flag. Status score -> tiers Newcomer / Local / Well-known / Respected / Pillar of the Community.
- **Economy**: food items + hunger restore + price, product lists per job (food goes to shop stock, other goods to the worker's inventory), "wants" per job for trades, job prestige, trait compatibility table.
- **Resident**: daily routine with per-person shift offset (morning/breakfast, work, lunch, work, leisure, evening, sleep). Destinations: breakfast at bakery/noodle bar if no food, lunch at eateries/partner's/friend's workplace, leisure = plans, dates with partner, visiting friends (if home known), sky trips, shopping, indoor places when raining. Needs decay; eats with animation; earns coins at work; buys food in person from the vendor or self-service; flees from monsters; reacts to rain; notices new builds; greets players (with "Did you hear…?" news). Right-click: intro, then status bubble + action-bar info (tier, coins, hunger, mood, partner, friends). Everything wrapped in try/catch.
- **Dialogue**: intros; shop scripts (price, sold out, free meal from friendly vendor); dates (can become official couple); arguments (trait incompatibility / bad mood) -> rivals, making up; gossip passes events on; food sharing when hungry; trades and gifts (flowers); topics: weather, work & stock, homes/neighbours, introducing third people, the player, Benson memorial, friends, status/wealth, elevator, couples, new builds. Relationship milestones become events.
- **Conversation**: must be within 3.5 blocks with line of sight to speak (pauses otherwise, ends after 10 s apart or >10 blocks). Lines can carry gestures/actions (show item, particles, item transfer).
- **Events**: tracks player building/tearing-down per chunk and reports "Fireheart_4743 was building something out of X near Y"; explosions; monster sightings; weather changes; player login; elevator breakdown/fixed; status changes. Residents learn events by witnessing (within 14 blocks) or gossip.
- **Elevator**: see 7.5.
- **Client**: `ResidentRenderer` (9 vanilla wide skins; neon speech bubbles with pop-in, bouncing "typing" dots, typewriter text, soft voice blips (note_block hat), bob, distance fade, hidden behind walls; floating Z's when asleep), `ResidentModel` (gestures: wave, eat, give, cheer, angry, think, talking motion).
- **Commands** (`/city …`, op): `spawnall`, `census`, `news`, `events`, `shops`, `who <name>`, `find <name>`, `respawn <name>`, `chatlog <true|false>`, `elevator`, `elevator repair`, `elevator debug`, `clear`.

## 7.5 Elevator (Ember Heights) as the mod handles it
- Create elevator pulley 36 99 24 (diesel engine 32 99 24), cabin glued at the roof stop (floor y94 smooth quartz with black concrete centre, sea-lantern ceiling y98, framed glass door 35 95-96 24, controls 37 95 24). The cabin entity type is **`create:stationary_contraption`**; the mod finds it via the pulley's `movedContraption` field (reflection).
- Floors (slab y): G 70, 1 74, 2 78, 3 82, 4 86, 5 90, R 94. Landing buttons at 33 (y+3) 23, contacts at 34 (y+3) 23, waiting spot 32 (y+1) 24, queue spots at x26-30 z23-25.
- **Found broken at the start of session 12**: pulley `Running:1b` but no cabin entity (Create's contraption vanished). Also a contact stuck at `calling=true` made Create ignore calls to that floor.
- **Auto-repair** (after ~30 s missing, or `/city elevator repair`): set pulley running=false/offset=0 via reflection, clear the shaft, place the cabin blocks at the roof stop, re-summon super glue (From 0,0,0 To 3,5,3 at 35 94 23), reset calling/powered on all contacts, set assembleNextTick=true. Verified on the test server.
- **Riding**: queue, one rider at a time; phases CALL (walk to landing, press button, wait) -> BOARD -> RIDE (press destination button, stay centred, follow the cabin) -> EXIT -> WALKOFF (next rider is only dispatched after this one reached their apartment or left the lobby). If the elevator stays broken after two repairs: "service lift" teleport, still one at a time.

## 7.6 Toolchain (how to rebuild the mod)
- No internet to maven/mojang from the cloud; everything is built from the user's own `meta\libraries` (stage the jars).
- `mkmap.py` builds `srg2named.srg` (SRG -> Mojang names) from `client-…-mappings.txt` (ProGuard) + `mcp_config-…-mappings-merged.txt` (tsrg2). `srg2named.srg` is included in the zip.
- Named jars for compiling: run ForgeAutoRenamingTool 0.1.22 (`meta\libraries\net\minecraftforge\ForgeAutoRenamingTool\0.1.22\…-all.jar`) with `--map srg2named.srg` on client-…-srg.jar, forge-…-client.jar and forge-…-universal.jar -> `named-*.jar` (not included in the zip; regenerate).
- `build.sh`: javac --release 17 against cp.txt (named jars + forge libs + mixin 0.8.5), jar with manifest.txt + res/, then ART `--reverse` back to SRG -> `fhc.jar`. Check that every m_/f_ reference in the output resolves (script in the transcript; inherited members show as false positives). Record accessors must be called by SRG name in source (e.g. `BlockStateParser.BlockResult.f_234748_()`).
- Deliver with a **new, uniquely named staged file** each time (re-committing the same outputs path once delivered an old copy, which is why the game kept running v0.1 for a while).
- **Headless test server** (cloud): libraries staged from meta/libraries into `/tmp/srv/libraries`, server-srg/extra symlinked to the client ones, forge-server symlinked to forge-client, gson/log4j from the pyspark sdist (`pip download pyspark==4.0.1`) because deep paths can't be staged (8+ folders), embeddium/oculus removed, `config/pfm.json` deleted before every start, launched with BootstrapLauncher `--launchTarget forgeserver`, 4 GB. Commands via a FIFO fed from cmds.txt. `/forceload add -48 -40 48 112` + `-32 224 32 300` keeps the city ticking without a player.

## 7.7 Lessons (session 12)
- Clicking in the game while it is focused breaks blocks in creative: only click menus; to type commands press T first; Escape didn't always register (use the window close button or ask the user).
- The game can become extremely slow (1 fps) with shaders + 4 GB; turning shaders off (K) helps. My typing once toggled shaders off by accident.
- The Claude desktop window can pop over the game and steal clicks.
- The remote-devices bridge sometimes disconnects mid-task and the safety classifier can be temporarily unavailable; just retry.

---

# PART 8 – SESSION 13 (Sept 22 2026): v0.4.1 → v0.6.2, voices, furniture, library, pets, Skyliner, motorbike

**Read this part first.** Mod source + build tools: `Claude outputs\fireheartcity_mod_source.zip` (unzip to `/tmp/modbuild`; `part7.md` inside has the same notes). The jar installed in `mods\fireheartcity-0.1.0.jar` is **v0.6.2 (172,014 bytes)** but at the moment of writing the running game still had **v0.6.1 loaded** – v0.6.2 (Skyliner rides) loads on the next restart.

## 8.1 What the user asked for this session (in order)
1. Continue Part 7 → verify v0.4 in the real game. Done (elevator queue/ride verified; BOARD fix → v0.4.1).
2. ElevenLabs voices for residents, fall back to text bubbles when credits/key fail, only when the player is nearby. Done (v0.4.2), verified working (clips cached).
3. "Start building" the next update: beds/sitting/notice board (v0.5.0), then "decorate the apartments with the furniture mod" + "implement everything you listed": birthdays, pets, promotions, weather/fishing, voice tone + quiet hours, NE district with a new job, real resident shuttle, motorbike brakes/reverse (v0.6.x).
4. Last (typed, possibly not sent yet): **"Make sure they have good memory so they're not just saying random things – if they make plans they remember them, remember conversations and people."** → NOT started. This is the top next task (see 8.9).

## 8.2 Current mod features (all in source, v0.6.2)
- **Voice** (`VoiceConfig`, `client/VoiceClient`): config `config\fireheartcity-voice.properties` (apiKey already filled in by me, enabled, hearRangeBlocks 18, outputFormat pcm_22050, modelId eleven_turbo_v2_5, quietHours true 13500–23000). Client-only; distance checked BEFORE the API call; cache `config\fireheartcity\voicecache\<sha1>.pcm` keyed by voice|format|tone|text; tone from gesture/punctuation (angry/excited/calm/normal → voice_settings); `*stage directions*` stripped; 401/403/429/quota → disabled 10 min, bubbles keep working. Voice IDs per resident in `Cast` (premade ElevenLabs voices), synced via entity data `VOICE_ID`.
- **Sleeping**: villager-style `startSleeping` in a free bed within 7 of home on the home floor; Paladin's furniture beds extend vanilla BedBlock so they work. `isImmobile()` while asleep. `/city beds`, `/city beds furnish` (places beds on free floor; not run – apartments now have furniture beds anyway: `/city beds` → 15 residents can sleep in a bed).
- **Sitting** (`Furniture`): reflection support for Create seats AND Another Furniture seats (`com.starfish_studios.another_furniture.block.SeatBlock` – same `sitDown`/`isSeatOccupied` API + `isSittable`). Residents sit 30–90 s during leisure/lunch/evening/morning near their destination (sofas at home, plaza seats, library chairs). Stands up on activity change / flee / elevator queue. Verified: "Theo sitting down at the Auto Bakery".
- **Notice boards / Fireheart Gazette** (`Gazette`): lecterns in `CityData.boards` hold a written book (front page, latest 18 events, Who's who), refreshed every 60 s when something changes. Residents read boards they pass (learn 2 events, "*reads the Gazette* Says here …"), 12 % of leisure = "read the news". Boards registered: **Ember Heights lobby 24 71 20** and **library 36 71 -31**. `/city board`, `/city board remove`, `/city board refresh`.
- **Careers**: Profile `xp`/`level` (Junior/–/Senior/Head title prefix, `jobTitle()`); xp +1 per ~2 min at work; level 2 at 15 xp, 3 at 40; higher level = faster pay; promotion event + cheer.
- **Birthdays & anniversaries** (`Life.daily`): birthday every 16 days per resident (`birthdayIndex`), event + party plan at the plaza/isle plaza for friends+partner; party behaviour: cake, gifts, "Happy birthday", affinity. Couples: anniversary every 8 days since `partnerSince` → event + date plan.
- **Pets** (`Pets`): Mia cat Biscuit, Ivy cat Clover, Zara cat Tick, Omar cat Pumpkin, Sam parrot Captain, Luna cat Neko (island, spawns when her pod is loaded). Tamed (no owner), tagged `fhc_pet_<id>`, live at owner's home, greet owner (hearts + lines), curl up on the owner's bed at night, teleport home if lost. Verified: 5 adopted events. (Deliberately no dogs – Benson.)
- **Fishing & weather**: 14 % leisure = fishing at boardwalk/pier (rod, splash particles, catches cooked cod, big-fish events, every 10th fish an event). Lantern in hand outdoors at night. Status text fixed ("heading off on a trip to …", "at Mia's birthday party at …").
- **Library + Librarian**: job `LIBRARIAN` (book), place `library` (34 71 -34, entrance 33 71 -29) in CITY_HANGOUTS/CITY_INDOOR; new resident **Nell** (apt2B, voice Rachel) – spawned with `/city spawnall` (17 residents now).
- **Skyliner rides** (`Skyliner`, v0.6.2, NOT YET TESTED): reads scoreboard `sky` (`#state` 0 city/1 up/2 isle/3 down, `#lock`, `#wait`). Residents at the pad board when the ship is docked there (hidden, `skyRider`), manager runs `function sky:depart` 10 s after boarding (only if lock/wait are 0, so a player's countdown wins), calls the ship with `function sky:call` if it's at the other pad; passengers reappear at the destination port when `#state` flips. Falls back to the old invisible shuttle if the sky datapack is missing. `/city skyliner`, `/city skyliner debug`.
- Also still: elevator (v0.4.1 BOARD fix: walk straight onto the landing, teleport after 60 t), crash-fix mixins, etc. (Part 7).

## 8.3 World builds this session (datapack `statue`, mirror in my outputs)
- `statue:furnish` – all 20 Ember Heights units (floors 1–5) furnished: Paladin's (pfm) classic bed + nightstand, kitchen counter/sink/counter-oven, iron fridge+freezer, modern coffee table, grey pendant; Another Furniture (AF) sofa pair, floor lamp, table + chairs (unit D: custom layout around the ladder/barrel/fern). Woods per floor spruce/dark_oak/birch/cherry/mangrove; bed colours A red, B blue, C green, D yellow. Old generator props at x22-24 z19-21 and carpets 31-32 z17 removed. Corridor z23-25, home spots and ladder x31 z31 kept clear.
- `statue:city_library` – **Fireheart Library** x29..38 z-38..-30 (NE lot, 1 block inside the walls): brick + dark-oak, framed glass, 2 floors (loft y75 floor, stairs along x30), bookshelves, AF tables/chairs, pfm desks, reading nook with AF sofas, pendants, parapet roof, glowing sign, lantern posts, flower boxes, andesite path to the garage apron.
- Furniture facts: pfm IDs are generated at runtime (`profiles\...\pfm\cache\pfm-assetpack\assets\pfm\blockstates`, 1840 files; too deep to stage – list the dir instead). pfm facing = front direction for nightstand/kitchen/fridge/stove; pfm sofas/armchairs/chairs are reversed. AF facing = front for sofa/chair/drawer; AF sofa pair: `type=left` is on the sitter's left.
- Fixes: 4 lobby blocks broken by my clicks restored (20 70 24 andesite, 20 71 24 carpet + fill replace air x19-21 z21-27). Time: I used /time set once (reset day count) and restored with `/time set 25000` (day index 1).

## 8.4 Motorbike `fireheart-mb01` – IN PROGRESS (not finished)
- Shipyard origin O = **(-28661763, 126, 12290046)** (from hub long -7878484547791228801 = hub at O+(3,1,1)). Local +x = front.
- DBW network could not be replaced (backup block only loads if the ship has NO network; DBW has no clear command), so the existing sinks were reused: keyUp → (1,1,1), keyDown → (1,0,1), A/D → front wheels (4,0,0)/(4,0,2).
- New layout (placed live in shipyard coords, functions `bike2`, `bikefix1-3`, plus chat commands):
  - (1,1,1) Redstone Link **transmitter X** (lime_dye+feather, facing up); (1,0,1) **transmitter Y** (red_dye+feather, facing west, attached to iron block (2,0,1)).
  - Motor at (0,2,1): originally diesel engine+pump+tank, but the diesel **stalls** when the clutch engages → replaced by `create:creative_motor[facing=west]{ScrollValue:96}`; tank (0,0,1) → blackstone top slab, pump removed.
  - (-1,2,1) gearbox z, (-1,1,1) cog y, (-1,0,1) gearbox z → (-2,0,1) **clutch** → (-3,0,1) **gearshift (reverse)** → (-4,0,1) gearbox y → (-4,0,0) shaft z / (-4,0,2) gearshift z (powered by redstone block (-4,1,2)) → rear wheels (-4,0,-1) S, (-4,0,3) N (currently `simple_wheel` again; med wheels made the drive read 0).
  - Clutch logic: comparator (-2,0,0) facing north, subtract, back = redstone block (-2,0,-1), sides = receiver X (-1,0,0) and receiver Y (-3,0,0) (Y also powers the reverse gearshift). Comparator needs the support slab at (-2,-1,0) (it popped off without it). Verified: idle → comparator 15, clutch disengaged (bike parks/brakes); powering transmitter A → rxX 15 (links work).
  - Rear cowl slabs, flag pole (-4,1..3,1), tail lamp (-5,0,1).
- **Open problem**: with clutch engaged the drive reads 0 at the wheels in the latest tests (earlier, with the clutch stuck engaged and simple wheels, the whole chain spun at -96 and the bike did drive off once). Needs a real test: the player sits on the bike, activates the gold Motorbike controller (hotbar 3; a fresh bound one: `create:linked_controller{Hub:-7878484547791228801L}`) and holds W/S. Computer-use clicks always look straight down, so I can't activate the controller myself – ask the user to test. If W drives backwards, swap the receiver frequencies at (-1,0,0)/(-3,0,0); if it spins, move the redstone block from (-4,1,2) to above a gearshift at (-4,0,0).
- Parked: `/vs teleport fireheart-mb01 12 73 -15 (0 0 0)` (garage apron). Helper `statue:bikesit` (summons a seat at the bike seat and mounts you), `bikediag`, `bikediag2`, `bikelinks`.

## 8.5 How to restart the game (important – Escape/Alt+F4 don't work through computer use)
- Switching focus to the Modrinth App pauses the game and **saves** ("Saving and pausing game"). Then either: click the paused game's **Save and Quit to Title** (694,604) → **Quit Game** (842,630, click twice) → Modrinth **Play** on the world row; or Modrinth world-row Play (shows "already running" + Stop) → Stop → Play.
- Modrinth often loses track of the instance ("No instances running" + a stale Stop that doesn't respond). At the end of this session I was stuck exactly there with the game paused/saved – if the game is still open, the user can just close/restart it.
- Never click the Minecraft title bar while in the world (clicks break blocks). Click menus only; chat must be open (T) before clicking into the game.
- `/reload` lags the server 20-30 s; wait before running functions. A function file with a bad command silently fails to load.

## 8.6 Toolchain notes (additions)
- Renaming forge jars needs `-e client-srg.jar` plus all cp.txt libs, else ART NPE. Named jars are regenerated per session (stage the 26 jars listed in cp.txt + ART + forge client/universal + client-srg).
- Gson isn't on the classpath (write JSON by hand). Verify builds with the "unmapped Minecraft member names" javap scan (only enum constants, Iterable/List methods, `Message.getString`, `Explosion.getPosition` may remain).
- Region reader `region.py` (nbtlib + numpy) reads a box of blockstates from `saves\Create!\region\r.X.Z.mca` – used to plan the furniture and the library lot.

## 8.7 Test status
- Verified in the client: v0.4.2 voices (clip cached), v0.5 sitting, gazette book, beds count, v0.6 pets + birthday events, Nell spawned, library + board.
- NOT yet verified: residents actually lying in beds at night, pets curling up, parties/fishing/promotions over time, voice tone/quiet hours, **Skyliner rides (v0.6.2 not loaded yet)**, the motorbike drive.

## 8.8 Files
- `Claude outputs\Fireheart_Project_Handoff.md` (this file), `Claude outputs\fireheartcity_mod_source.zip` (src, res, build.sh, cp.txt, srg2named.srg, part7.md, fireheartcity-latest.jar = v0.6.2).
- Datapack functions added: furnish, city_library, pfmtest (test row, removed from world), bike2, bikechk, bikediag, bikediag2, bikefix1-3, bikeon, bikelinks, bikemotor, bikesit.

## 8.9 Next steps (priority order)
1. **Resident memory** (user request): residents should remember plans they make (and follow through / mention them: "See you at the plaza tonight!" → next day "Thanks for coming yesterday"), remember past conversations (topics already discussed, what the other person said, last chat day), and remember people (names, jobs, homes, gifts, favours, arguments) so dialogue references real history instead of random lines. Ideas: per-pair memory log in `CityData.Rel` (last N topics + facts learned + promises), plan confirmations in Dialogue, dialogue picks topics not recently used and references remembered facts, gossip carries the source ("Mia told me …"), memories persist in `fireheartcity.dat`.
2. Restart the game → v0.6.2 → test Skyliner rides (`/city skyliner debug`, watch log `[Skyliner]`, residents with island plans).
3. Motorbike: get the user to test-drive; fix direction/traction as in 8.4. Optionally restore the diesel engine for looks (it stalls under wheel load).
4. Watch a night: residents in furniture beds, pets on beds; check `/city events` for parties, promotions, fish.
5. Keep backups before big world changes.

---

# PART 9 – SESSION 14 (Sept 22 2026, evening): Sky Organ, v0.7.x (memory, real jobs, calendar, money, weather), contraption maintenance

**Read this part first.** Mod source: `Claude outputs\fireheartcity_mod_source.zip` (now includes v0.7.1 source and `fireheartcity-latest.jar`). Installed jar `mods\fireheartcity-0.1.0.jar` = **v0.7.1 (236 KB)**, loaded after the 18:53 restart.

## 9.1 Sky Organ (datapack `saves\Create!\datapacks\FireheartSkyOrgan.zip`, namespace `organ`)
- Neon stage east of Neon Heights: platform x40..64 z271..299 y180, bridge z283..287 from the island rim (~x24). Organ face x59: 3 rows of real note blocks – bass y182 (MIDI 34..53 on birch/dark-oak planks, key z = 277 + (p-38)), harp y185 (54..78 on snow blocks), bell y188 (79..97 on gold). Triggers at x60 (polished blackstone ↔ redstone block for 2 ticks), redstone lamps above each trigger flash per note.
- Songs (from the user's MIDI files): 1 An Ending, 2 Fallen Down, 3 Finale, 4 Frozen Time. Console x53: warped button 53 181 283 = PLAY, polished blackstone button 53 181 285 = NEXT SONG, crimson 53 181 287 = STOP; sign 53 182 285 shows the selected song.
- Functions: build, upgrade (adds low bass keys + selector), play/stop/next/sel1-4, tp. Scores: #song #on #t on objective `organ`. Generator: `organ_gen.py` (needs lib.py/skylib.py); add songs to the SONGS list and rezip.

## 9.2 v0.7.0/0.7.1 mod features
- **Memory (step 8 done)**: Rel stores `talked` (topic→day, no repeats within 2 days), `facts` (food, pet, hobby, birthday, owe/lent), `shared` memories, `heard` (what the other told them + day), kept/broken promise counts. Plans store `by`, `came` (attendance, marked on arrival) and `reviewed`; next day(s) residents thank each other / call out no-shows. Gossip keeps its source (`heardFrom`, "Mia told me that…"). Loans between friends with repayment.
- **DayLog** per profile (today/yesterday): made items, served, earned, spent, tasks, notes. "How was your day?" evening topic uses it; `/city diary <name>`.
- **Work.java** – real job task queues (stand spots auto-found next to the block, look/swing/particles/sounds):
  - Baker: check wheat hopper -28 75 4 → take bread from -39 70 5 → flour from hopper -28 72 4 → knead on cutting board -40 72 9 → bake at pot -38 72 11 (recipes use pantry bakery:sugar/egg/milk/cocoa/apple/pumpkin: apple pie, cookies, pumpkin pie, cake, else bread) → soup/stuffed potato at skillet → stock depots -38/-37/-36 71 6.
  - Factory worker: iron sheets from 6 70 7, refill ingots 0 73 6 from pantry factory:iron, wheat from farm chest -4 70 -3 → delivers to bakery mill -28 75 4 and sheets to supply depot 17 71 53 (B2B payments).
  - Grocer: produce to market barrels, delivers baking supplies (bakery cabinet -40 71 8) and diner groceries (barrel 7 71 57). Crane: unloads shipments (pantry market:goods, factory:iron). Quarry: loads crusher inputs y75, hauls outputs y71 (x-36..-26 step 2, z54). Cook: diner smoker/furnace recipes from diner:supplies. Clerk: cogwheels/shafts. Noodle chef: scanned stoves. Others: scanned stations per job keywords.
  - `/city work <name>` shows the current task and bag. Verified in game: Mia/Ben bakery chain end to end.
- **Shop.java**: stock = real display containers (bakery depots, supply depots, market barrels) + back shelf (`CityData.shelf`). Replaces the old `stock` counter (migrated).
- **Money**: `CityData.pay()` ledger (300 tx), accounts `biz:<workKey>`, `biz:rent_city`, `biz:rent_isle`, `city`. Purchases pay the shop till, wages paid after each shift from the business till (city covers shortfall), weekly rent on Sunday, tips, loans, B2B deliveries. `/city bank [name]`, `/city ledger`, `/city pantry`.
- **Calendar**: day 0 = Monday; weekday title at dawn and on login; Sat/Sun = no work (work slots become leisure, 3 leisure slots/day, weekend shopping trips). `/city day`. Gazette shows the weekday.
- **v0.7.1 weather**: synced WEATHER flags – umbrella rendered over the head (carpet canopy + chain handle) outdoors in rain, shivering (jitter + snowflakes + lines) when cold (snowy biome, thunder, island at night, rainy night), sunbathing (lying pose) at pier/boardwalk/park/plaza/isle on clear days. Name tags coloured by level (white/aqua/gold) and level-based shout lines. NOT yet seen in game (it was sunny) – check next rain.

## 9.3 Maintenance (datapack `FireheartMaint.zip`, namespace `fix`)
- `fix:restock` refills bakery wheat (27×64), press ingots (27×64), crusher inputs, trims crusher outputs to 4 stacks, re-places the farm diesel engine with fuel. `fix:farmcheck` prints speeds.
- Session 14 checkup: farm engine had stalled (0 RPM) → fixed (-96, frame moving); bakery wheat and ingot chests were empty → refilled; gravel output chests were full (1728) → trimmed. Door, clock, elevator, water-wheel press, belt line all running.
- "farm contraption MISSING" in statue:checkup is normal while the frame is parked (it only exists as an entity while moving).

## 9.4 Feature checklist (user's list)
- Done: sitting/sleeping, birthdays/anniversaries, pets, promotions (+ colours/shouts), voice tone + quiet hours, NE district (library + librarian), weather reactions (v0.7.1), resident memory, real jobs, calendar/weekends, money.
- Open: more jobs/buildings (e.g. a school with a "kids" archetype, second industrial zone); watch sleeping in beds at night; see umbrellas in rain.

## 9.5 Lessons (session 14)
- Resuming from the pause menu: focus the title bar, press **Tab** then **Enter** (selects Back to Game) – no click in the world, no broken blocks. Clicking Back to Game broke 30 70 24 and 47 180 285 earlier (both restored).
- The user may be typing/playing while I control the PC – keystrokes interleave (chat got "one?", "wwww"). Screenshot before typing; release the lock when he's active.
- Opening the Modrinth App steals focus → game saves and pauses.

## 9.6 v0.7.2 – Sky Organ parties (Party.java)
- Rare event, rolled once per day between tod 8500-10300: Mon-Thu 3 %, Fri 12 %, Sat 15 %, Sun 8 %. Everyone without a birthday party gets a "dance" plan at place `organ` (the Sky Organ stage, 47 181 285); city residents ride the Skyliner up. Title banner to players.
- Music: from tod 10300 (or as soon as 2+ residents are within 18 blocks) a random track (never the same twice in a row) is started via scoreboard `#song organ` + `function organ:selN` + `function organ:play`; when it finishes another random track starts. Stops at tod 13200 (`organ:stop`).
- Dancing: residents on the stage wander the floor, jump, spin, wave/cheer, note particles, party lines; +fun/+social; diary note and shared memory ("dancing at the Sky Organ party").
- Commands: `/city party` (start one now, music whenever residents arrive), `/city party song` (next random track), `/city party stop`.

## 9.7 NEXT TASK (top priority for the next session): Fireheart City Bank
The user wants a **bank** added to the city. Build on the v0.7 money system (`CityData.pay()`, ledger, `accounts`, wages/rent/purchases, `/city bank`).
- **Building**: a proper bank in the city (e.g. on free land near the shops/plaza – scan the region files first with region.py; don't overwrite existing builds). Vault look, teller counter, ATM-style kiosk outside, glowing sign "FIREHEART CITY BANK". Generate it as a datapack function (statue or a new namespace), commit, `/reload`, run, verify with screenshots.
- **Banker job**: new Job + resident (teller) who works the counter with real Work.java tasks (count cash, open the vault, serve customers). Add to Cast/Place/Economy; spawn with `/city spawnall`.
- **Accounts for residents**: wallet (`coins`) vs bank balance (`savings`). Residents deposit part of their wages after payday, withdraw when the wallet is low, and earn small weekly interest (e.g. Sunday). Loans from the bank with repayment schedules and interest; missed payments become events/gossip.
- **Business accounts**: shop tills (`biz:*`) are held at the bank; weekly statement in the Gazette (richest business, biggest spender, savings leaderboard).
- **Player**: let Fireheart_4743 open an account (right-click the teller) – deposit/withdraw emeralds or gold nuggets as coins, pay residents, buy from shops.
- **Dialogue/memory**: residents talk about savings goals ("I'm saving up for …"), bank visits appear in DayLog/diaries, and friends remember loans.
- **Commands**: extend `/city bank` (per-resident savings, loans, interest history).
- Verify in game (restart via Tab+Enter trick / ask the user), update this handoff with Part 10.
