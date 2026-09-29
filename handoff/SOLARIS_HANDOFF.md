# SOLARIS (formerly Fireheart City): Project Handoff, v1.12.0 (2026-09-29)

This file carries the project into a new chat. Read this file first. The older, much longer `Fireheart_Project_Handoff.md` in this folder has the full history, the toolchain derivation, the block-ID lists and the generator code from earlier sessions. Use it as a reference when you need detail.

## v1.20.0 - merge (latest, built and tested)
This branch merges the cloud session's v1.13-v1.19 work with the local session's parallel work, which was built on v1.12.0 (cbaa322): the Watcher black-screen fix and scarier Watcher, Gus placing blocks like a player, the cosy Stellar House interior, and the hotel/GPS/SolEats work below. Version is 1.20.0. It compiles against MC 1.20.1/Forge 47 without changes. `/city test player`: 82/83 (the group chat check depends on the save's broken phones), all lifeTests pass, `/city event` works for every event, and a full day soak logged no tick exceptions.

### Local-session features included
- **Hotel** (`Hotel.java`): magmagamer9's hotel at x44..56 z25..38, lobby floor y72, entrance (49,72,38), service bell (46,73,33), desk stand (44,72,33), guest spot (47,72,33). Places hotel (lobby), hotel_desk, hotel1..5 (rooms: 1 (46,78,27), 2 (52,78,31), 3 (52,84,30), 4 (46,84,27), 5 (52,90,37)). New cast: Marco (Job.CONCIERGE, hotel5, shift t6000-18000), Finn (GARDENER, hotel1), Priya (CLERK, hotel2), Mateo (DOCKMASTER, hotel3). Residents heading to a room are routed through the lobby (also on catch-up teleports) and check in at the bell with Marco (self check-in when he's off). Daily booking of Room 4 (a resident, plus partner), 12 coins each paid to magmagamer9's bank (`biz:hotel` if no account); `CityData.Profile.homePlace()` returns the stay room. Right-clicking the bell gets a room key (master key for magmagamer9). `/city hotel [movein]`.
- **Visits to magmagamer9's house** (place magma_house (-48,71,42)): leisure option, weighted by whether he is online and home; greet him or knock and leave a letter. Knowledge facts added.
- **SolNav GPS** (`Gps.java`, `client/GpsHud.java`, item fireheartcity:gps given once on login): `/sol gps` clickable menu, `/sol gps <place>`, `player <name>` (live), `xyz`, `here`, `off`; `/sol base` explains the secret base and points the GPS at the hatch (71,85,10). HUD compass with distance, direction, and ETA in vehicles; particle trail on the ground. Msg `#gps|x|y|z|p/d|label` / `#gps|off`.
- **SolEats tracking**: Letter has placed/ready/out/cook/notified. `Extras.eatsTick` advances received > cooking > packed > on the way > delivered, sends toasts and refreshes the phone; a rider delivers directly after 1200 ticks if Pip hasn't. The EatsApp shows a 5-step tracker with detail text.
- **Repair**: player-lit TNT and explosions are now queued too (previously ignored).
- `/city garage give|park|replace` alias (op). Tutorial typing ticks stop exactly when the text finishes.

## 0000000000. v1.19.0 - 50 improvements + 50 features (2026-09-29, source only, not compiled)
New files: `Crowd` (per-tick resident cache), `Bonds` (friendship milestones, nicknames, anniversaries, proposals, goodnight texts), `Applause`, `Errands` (hide and seek, races, guiding, home visits, litter; uses the new `Resident.errand(...)` override), `Health` (colds), `Streets` (street life), `Happenings` (scheduled events + `/city event`), `Info` (calendar/who/couples/gossip/memories/selfie/album). New config section `[life]` in fireheartcity-common.toml. Checked here with javac without MC jars (no syntax errors, no missing project symbols) plus an audit of every Minecraft/Forge name not used before v1.13.

### Improvements
1. `Crowd`: loaded residents looked up once per tick for all ambient systems.
2. Ambient moments only run for residents within `ambientRange` (default 96) of a player.
3. Cooldowns expire individually instead of the whole map being cleared.
4. Static event state resets on server stop (switching worlds in singleplayer).
5. Pending letter replies are saved in the world (settings `~post|pending`).
6. `/sol courier cancel`; parcels for missing residents or a day overdue are taken back.
7. Speech bubbles stay up long enough to read (30 + 2 ticks per character, max 200).
8. Config `[life]`: ambientMoments, ambientRange, weddings, festivals, skyEffects, chimes, snowballs, luckyFinds.
9. `/city event <name>` forces wedding, rainbow, sick, meteor, lantern, kindness, spooky, starlight, blossom, quiz, karaoke, movie, fishing, run, beach (prints the /time to use).
10. `/city test player` gains 11 checks (resident cache, daily bonus, achievements, quest counters, distinct quests, no-repeat stories, letters, sick-day schedule, wishlist, calendar, nicknames, seasons).
11. `/sol settings`: clickable per-player toggles (chimes, sky effects, bulletin, gifts, action-bar hints, home visits, goodnight texts).
12. `/sol whereis` also finds online players.
13. `/sol achievements` shows progress (e.g. 3/5 couriers).
14. Daily streak: one missed day a week is forgiven.
15. Courier compass on the action bar while holding a parcel (distance, direction, time left).
16. `/sol treasure hint`: warmer/colder and rough distance.
17. Horoscope lucky resident: gifts to them count double that day.
18. Rock-paper-scissors win/loss record per resident.
19. Jokes, stories, fun facts and compliments don't repeat until you've heard them all.
20. Hugs raise affection once per resident per day.
21. Tips give less affection each extra time the same day.
22. Letter replies sometimes mention what the resident is doing right now.
23. 20% of rainbows are double rainbows.
24. Shooting stars have coloured trails (white/gold/blue/green) that fade.
25. Aurora strength and colours vary night to night.
26. Lanterns drift on a shared nightly wind and shrink as they rise.
27. On the first day of each season residents greet you with it.
28. Chimes respect settings and are quieter indoors.
29. Bulletin: all today's events, birthdays, sick count, clickable Quests/Treasure/Calendar/Horoscope.
30. Joggers run laps through 4 waypoints around the place.
31. Yoga: the first resident there becomes the instructor, faces the class and calls out each pose.
32. Picnics: food crumbs and eating sounds.
33. Hit a free, friendly resident with a snowball and they throw one back (grumpy ones complain; no longer counted as punching).
34. Selfie posts tag friends standing nearby.
35. Partners greet each other with pet names and a blown kiss.
36. Happy humming plays harp notes.
37. Card games: curious residents are better at them; wins are counted.
38. Bystanders react to rival arguments.
39. Morning coffee gives a pace boost for 2.5 minutes.
40. Fireflies only appear over grass, flowers and leaves.
41. Quick gestures (throw, high five, sneeze, salute, punches) blend in 2 ticks; naps/reading/sighs in 9.
42. Standing residents glance around now and then (client).
43. Seated residents lean back and relax (client).
44. Idle residents step out of the way of moving vehicles.
45. `/city stuck`: where residents get stuck most (4x4 areas), to find map problems.
46. When rain starts, joggers, yoga, sunbathers and anglers change plans.
47. Monster scares go in residents' diaries.
48. Residents comment on armour (elytra, netherite, diamond, turtle helmet, pumpkin head).
49. Every 10 achievements: +50 coin milestone bonus.
50. `/sol` and `/sol help` show a clickable, grouped command list.
Also fixed: greeting gifts/birthday lines were being overwritten by other greeting text; residents on errands no longer sit down mid-trip; residents at work turn down games; game/guide replies bypass Groq.

### Features
1. **Home visits**: `/sol home set`; close friends (affection 60+) sometimes walk over during leisure, knock, visit with a small gift, or text you if you're out.
2. **Daily quests**: `/sol quests`, 3 of 12 tasks a day, 15 coins each, +20 for all three.
3. **Sunday fishing tournament** at the boardwalk (09:00-14:00), residents and players, 30-coin prize.
4. **Friendship milestones**: at affection 50 a Friendship Bracelet, at 80 a Best Friends Locket (named keepsakes; texted if they're not nearby).
5. **Nicknames**: fond residents give you one and use it.
6. **`/sol wishlist <name>`**.
7. **Quiz night** (Thursdays 17:24, library): 5 questions, answer in chat, residents compete, 25-coin prize.
8. **Karaoke night** (Fridays, Magma Beach Bar).
9. **Movie night** (Saturdays, cinema) with reactions and snacks.
10. **Fun Run** (every 14 days, 08:30 plaza -> old pier): join by standing at the plaza; placings and prizes.
11. **Beach days** on summer weekends.
12. **Spooky Night** (autumn, day 18): jack o'lanterns, soul particles, treats.
13. **Starlight Festival** (winter, day 25): gift exchange, presents for you, a star over the plaza.
14. **Blossom Day** (spring, day 3): flowers and petals.
15. **Colds**: residents get sick (more in winter), stay home coughing, a friend brings soup; give them soup to cure them.
16. **Litter**: residents pick up items left on the ground and hand them to the nearest player.
17. **Comforting**: friends hug residents who are feeling low.
18. **Applause**: residents nearby cheer when you unlock an achievement.
19. **Hungry residents** ask for a bite when you hold food.
20. **Constellations**: a named constellation in the northern sky each clear night; residents point them out.
21. **`/sol donate`**: City Fund; every 250 coins city-wide triggers a plaza celebration.
22. **`/sol calendar`**: next 14 days of events, weddings and birthdays.
23. **`/sol who`**.
24. **`/sol couples`**.
25. **`/sol gossip`**.
26. **`/sol memories <name>`**: what a resident remembers about you, and their trust.
27. Handshake on first meeting (G_SHAKE).
28. Police salute players with good reputation (G_SALUTE).
29. Sweethearts and partners blow kisses (G_BLOW_KISS).
30. Bench naps in the evening (G_NAP).
31. Whistling while walking (G_WHISTLE).
32. Confetti at weddings, fun runs and City Fund milestones (G_CONFETTI).
33. Knocking at your door (G_KNOCK).
34. Coughing (G_COUGH).
35. Picking things up (G_PICKUP).
36. Welcome back after 3+ days away.
37. Reactions when you ride past on a horse, boat, minecart or Solaris vehicle.
38. **`/sol selfie <name>`**: posted to SolFeed.
39. Thank-you letters the next morning after big gifts, tips or on-time deliveries.
40. Anniversaries every 28 days for married couples (a date) and your sweetheart (a text).
41. **Proposals**: `/sol propose` to your sweetheart (romance 80+); plaza wedding with vows (type "I do").
42. Shy and dreamy residents go home during thunderstorms.
43. **Hide and seek**: say "hide and seek"; they hide out of sight, 3 minutes to find them.
44. **Races**: "race me to the pier".
45. **Guides**: "show me the way to the library" - they walk you there and wait if you fall behind.
46. **`/sol album`**: SolFeed posts that mention you.
47. First snow and first blossoms days.
48. Umbrella sharing between partners and friends walking together.
49. Residents dance near jukeboxes that are playing.
50. Goodnight texts from your sweetheart around 20:15.
10 new achievements (40 total).

## 000000000. v1.18.0 - batch 5, features 93-100 (2026-09-29, source only, not compiled)
New file `Finale.java`; `/sol` with no arguments now prints help.
93. **Resident weddings**: on a weekend morning a couple (partners on both sides, romance >= 60, not yet married, at most one wedding a week, city-side homes) announces a wedding. Banner + news, and everyone in the city plans to be at Solaris Plaza. From 16:54 (tod 10900) the mayor (or a guest) officiates in 8 steps two in-game minutes apart: welcome, vows, "I do" x2, pronounced married (hugs, hearts, guests clap and cheer, fireworks, city event, memories, diary, romance 100), finale fireworks. Postponed if the couple isn't at the plaza by 18:12. State in settings under `~city` (`wedPlan`, `wedLast`, `wed:<pair>`).
94. **`/sol diary <name>`**: read a close friend's diary (affection >= 50): yesterday, today, what they're thinking and hoping to do.
95. **Build reactions**: every 25 blocks a player places around the city, a free resident nearby comments on the build (counts saved every 50).
96. **`/sol report`**: weekly city report (residents, average mood, season, money in the city, richest, couples, friendships, event counts by kind, top headlines).
97. **`/sol help`** (and plain `/sol`): all player commands and chat ideas.
98. **Thought bubbles**: idle residents sometimes show a thought in brackets (their intent for the day, hunger for their favourite food, loneliness, boredom, their partner, their savings goal, stray thoughts).
99. **Time-of-day greetings**: residents say good night on their way home in the evening and morning lines before work.
100. **3 final achievements**: Wedding Guest, Architect (500 blocks), Confidant (read a diary). 30 achievements in total.

### Before shipping 1.13-1.18
1. Rebuild the toolchain and compile; fix any errors in Minecraft/Forge API calls (only the project-side calls were checked here).
2. `/city test player`.
3. In the client: gesture blending and the 19 new poses (tune angles), rainbow/aurora/lanterns visibility, fireflies, snowballs, weddings (`/time set` to a weekend morning with a couple at romance >= 60).

## 00000000. v1.17.0 - batch 4: animations + realism (2026-09-29, source only, not compiled)
**Animation system** (`client/ResidentModel`): gestures no longer snap. The model records the pose before the gesture, and when the gesture changes it blends from the previous gesture's pose to the new one over 5 ticks with smoothstep (`Resident.cGest/cGestPrev/cGestAt`, client-only fields). `pose(e, g, t, lt)` gets `lt` = ticks since the gesture started, for timed moves (sneeze, throw, sigh).
Realism:
63. Smooth blending between all gestures (in and out).
64. Idle weight shifting: standing residents slowly move their weight from one leg to the other, with hip and head counter-tilt.
65. Breathing: subtle chest movement.
66. Running lean: body tilts forward at high speed.
67. Three talking styles picked per sentence (casual, open palms, emphatic beat).
68. Personal walking pace (`Resident.gait()`): 0.92-1.08 per person, livelier traits faster, laid-back/dreamy slower, slower when hungry or in the evening, hurrying when late and far from work.
69. Dripping water for ~45 s after coming in from the rain.
70. Visible breath outdoors in winter and on Neon Heights at night.
71. Sweat drops in the summer heat.
72. Occasional slip on wet ground when moving in the rain.
73. Conversation personal space: residents step back if they end up too close while chatting.
New gestures (41-59), used by existing features:
74. READ (holds book, turns pages) - bench reading.
75. SIP (cup to mouth) - morning coffee.
76. HIGHFIVE - high fives.
77. HUG (arms wrap, sway) - hugs.
78. JOG (pumping bent arms, lean) - jogging.
79. YOGA_TREE and 80. YOGA_WARRIOR - yoga cycle (stretch, tree, warrior, bow).
81. SNEEZE (wind-up then jerk forward) - spring sneezes.
82. FAN (fanning face) - heat.
83. THROW (overhead wind-up, release) - snowballs.
84. CARDS (holding hand, plays a card) - card games (both players).
85. LOOKUP (shading eyes, looking up) - sunsets, shooting stars, full moon.
86. HOWL - full moon.
87. SIGH (shoulders drop) - sad moods.
88. FEED (leaning down, offering food) - animals.
89. PHOTO (phone held up with both hands) - selfies (shows the phone item).
90. SING (hand on chest, arm out) - singing.
91. ARGUE (finger jabbing, hand on hip) - rival arguments.
92. WINDED (hands on knees, heavy breathing) - jog breaks.
Checked here with javac without MC jars: no syntax errors, no missing project methods/wrong arities. Client animation angles are untested; tune in game.

## 0000000. v1.16.0 - feature batch 3 of 5 (2026-09-29, source only, not compiled)
New files `Letters.java` (player) and `Moments.java` (residents/ambient); new intents in `Pastimes.chat`; listeners `Letters::onBreak` and `Letters::onJoin`; `/sol mail|horoscope|stats`.
42. **Letters**: `/sol mail <name> <message>` (5 a day). Kind, rude, sorry, love, miss and thank-you letters change affection and memories; the resident writes back 1-3 in-game hours later (chat + a named paper). Pending replies are in memory only (lost on restart).
43. **`/sol horoscope`**: daily sign, luck stars, omen, lucky resident, place and number.
44. **`/sol stats`**: reputation, residents met, achievements, streak, deliveries, treasures, fish, letters, lucky finds, savings, birthday.
45. **Lucky finds**: breaking grass/flowers within 220 blocks of the plaza has a 1-in-40 chance (max 3 a day) to drop gold nuggets, cookies, berries or an emerald.
46. **Lightning reactions**: residents within 40 blocks of a strike flinch and shout.
47. **Rival arguments**: rivals who meet trade barbs; 15% chance they call a truce (clears the rivalry, city event).
48. **Card and board games**: acquaintances idling together at the plaza, library, diner, park, arcade or home play cards/chess/dominoes/checkers; winner cheers, diary notes.
49. **Jokes**: cheerful/talkative/friendly residents tell each other jokes; grumpy listeners facepalm.
50. **Bench reading**: seated residents (especially curious, shy and dreamy ones) read books.
51. **Morning coffee** before work.
52. **Animals**: residents fuss over nearby animals (seeds, hearts, uses pet names).
53. **Spring sneezes** (pollen).
54. **Summer heat** complaints at midday.
55. **Autumn** pumpkin pie and cosy-weather lines.
56. **Sunsets and sunrises**: residents stop to watch and note it in their diary.
57. **Full moon** nights (moon phase 0): residents remark on it; cheerful ones howl.
58. **Fireflies** around outdoor players on summer nights.
59. **"Sing"**: residents sing a line with flute notes.
60. **"What should I build?"**: build ideas for the city.
61. **"Rate me" / "are we friends?"**: honest answer from affection and trust.
62. **3 more achievements**: Pen Pal, Lucky Find, Night Owl.
Checked here with javac without MC jars: no syntax errors, no missing project methods/wrong arities.

## 000000. v1.15.0 - feature batch 2 of 5 (2026-09-29, source only, not compiled)
New files `Quests.java` (player) and `Hobbies.java` (residents); additions to `Skies`, `Perks.ACHS`, `Resident` (yoga leisure, birthday greeting), `FireheartCity` (ItemFishedEvent listener), `/sol` commands.
22. **`/sol profile <name>`**: a resident's card (job, personality, mood, hunger, favourite food and hobby, home once you know them, partner, friends, savings goal, what they're doing, your friendship).
23. **`/sol tip <name> <amount>`**: pay a nearby resident from the gold you carry; raises affection and they remember it.
24. **`/sol emote wave|cheer|dance|bow|clap|laugh`**: nearby residents react (wave back, dance along, applaud); other players see the emote in chat.
25. **`/sol rep`**: city reputation rank (Stranger -> Solaris Legend) from how residents feel about you.
26. **Daily treasure hunt**: `/sol treasure` gives a riddle about a place; stand there to claim 15-30 coins plus a bonus item.
27. **Player fishing**: residents nearby cheer your catches; your fish are counted.
28. **Player birthdays**: `/sol birthday <1-28>`; on the day you get a banner, cake, 25 coins, fireworks, a news item, and every resident you meet wishes you happy birthday.
29. **Picnics**: friends/partners idling together at the park, plaza, gardens, beach or pier share food (hunger, fun, affection, diary, news).
30. **Selfies**: residents with phones take photos at places (flash, shutter sound) and post them on SolFeed, once per day.
31. **Rain dancing**: cheerful, adventurous and dreamy residents sometimes dance in the rain.
32. **Snowball fights**: in winter, free residents outdoors throw real snowballs at each other and throw back.
33. **Morning yoga**: new early-morning leisure choice at the park/gardens; everyone there moves through the same poses in sync.
34. **Passing hellos**: residents who know each other wave and greet by name when they pass, without stopping for a full chat.
35. **Flying players**: residents point and shout when a player flies over with an elytra.
36. **Mood moments**: very happy residents hum with music notes; very unhappy ones sigh.
37. **Meteor showers** every 14 days (day%14==6): many more shooting stars; in the bulletin.
38. **Aurora**: green/cyan/purple curtains in the northern sky on clear winter nights.
39. **Clock tower chimes**: the bell rings the hour from 07:00 to 22:00 for players within 160 blocks of the clock tower.
40. **Morning mist** drifting over the marina at dawn.
41. **6 more achievements**: Crowd Pleaser, Solaris Legend, Treasure Hunter, Angler, Another Year, Northern Lights.
Checked here with javac without MC jars: no syntax errors, no missing project methods/wrong arities.

## 00000. v1.14.0 - feature batch 1 of 5 (2026-09-29, source only, not compiled)
Goal: 100 new features in batches of ~20. Batch 1 (21 features). New files `Perks.java` (player), `Skies.java` (world/sky), `Pastimes.java` (residents). Hooks: `Events.onLogin/onLevelTick`, `Chat.reply0` (after Romance), `Chat.useAi` (games skip Groq), `Resident` (dreams, greet extras, courier delivery, jog leisure), `/sol` subcommands.
1. **Daily bonus** with 7-day streak (5-35 coins; bank savings if the player has an account, otherwise gold nuggets). `/sol daily`.
2. **Achievements** (12, coin rewards, title banner + toast sound), checked every 10 s. `/sol achievements`.
3. **`/sol whereis <name>`**: what a resident is doing, distance and compass direction.
4. **`/sol friends`**: residents you've met with heart ratings.
5. **`/sol top`**: richest, most friends, top anglers, happiest, game high scores.
6. **Morning bulletin** (once per day around 06:30-08:30): date, season, weather, yesterday's headlines, holidays. `/sol bulletin on|off`.
7. **Weather forecast** from real rain/thunder timers. `/sol forecast`.
8. **Courier jobs**: `/sol courier` gives a named parcel for a resident 25-180 blocks away; right-click them to deliver. Pay scales with distance, half pay after 10 in-game hours, 5 per day.
9. **Jogging**: new leisure choice before 15:00 (adventurous/cheerful love it), laps around the park/boardwalk/gardens with dust puffs and lines.
10. **Gifts from close friends**: residents with affection >= 60 sometimes give you a flower or their favourite food when greeting (once per day each).
11. **Held-item remarks** when greeting (swords, diamonds, TNT, flint and steel, cake, books, phones, food...).
12. **Coin flip and dice** in chat ("flip a coin", "roll a dice").
13. **Rock-paper-scissors** in chat (two-step: "rock paper scissors" -> "rock"), resident shows their pick as an item.
14. **Hugs, high fives, fist bumps and "dance for me"** with gestures, hearts and sounds.
15. **Stories, fun facts, compliments and roasts** on request.
16. **Sleep-talking**: sleeping residents mumble dreams about their job, favourite food, partner, games.
17. **Rainbows**: when rain stops before sunset a 7-band particle rainbow appears north of the city (~80 s); residents point at it.
18. **Shooting stars** on clear nights (~1 per minute of night while someone is outside); residents say "make a wish".
19. **Seasons** (7 days each): shown in the bulletin/forecast; snowflakes around outdoor players in winter, cherry petals in spring; residents answer "what season is it".
20. **Lantern Night** (every 28 days, day%28==10): everyone plans to meet at the plaza at sunset, residents hold lanterns and glowing lanterns float up from them and from players.
21. **Kindness Day** (day%28==20): residents give each other small gifts and compliments (affection +3), and give players one gift each.
Checked here with javac without MC jars: no syntax errors, no missing project methods/wrong arities.

## 0000. v1.13.0 - resident AI & behaviour (2026-09-29, source only)
Source in the repo's `fireheart-city/` is ahead of the zip here. **Not compiled yet**: the cloud container had no access to Mojang/Forge Maven, so rebuild with the usual toolchain before shipping (only `javac` syntax-checked).
- **Per-tick decision cache** (`Resident.activityName/destination`): computed once per game tick (keyed on game time + day time) instead of dozens of times per resident per tick. `rethink()` invalidates; called from `replan`, `doneErrand` and the bank/skydive `leisureKey` switches.
- **Personal arrival spots** (`Resident.arrivalSpot`, `Nav`): for leisure/lunch/morning places each resident gets a stable walkable spot 1.5-3.7 blocks from the place centre (seeded by resident + place, line-of-sight checked so it never lands behind a wall), re-validated every 5 s. Skipped for homes, the tower, ATM/bank, SolTech, Sky Launch, Sky Organ and the skyports.
- **Stuck recovery** (`CommuteGoal`): 3 s without progress -> hop + re-path; 7 s -> sidestep detour; 13 s -> if the goal is unreachable, walk to a reachable spot near it; 20 s -> teleport only if no player is within 12 blocks or can see the resident or the landing spot (was: any player within 24 blocks blocked it forever, even behind walls).
- **Walking together** (`Resident.companion`): partners/friends heading to the same place within 10 blocks pace each other (slow down / catch up) and glance at each other; occasional "Wait up!" line.
- **Make way** (`MakeWayGoal`, priority 2): idle/free residents step 1.8 blocks aside (walkable, clear) when a player walks into them or stands within 1.1 blocks, with an occasional "After you!"; residents overlapping another resident step apart. Not while working a task, seated, on duty, seeking/following a player, fleeing or in an emergency.
- **Mingling & fidgets** (`MingleGoal`, `Resident.fidget`): idle residents at a hangout turn to face nearby residents (prefer whoever is speaking); every ~45 s+ they may yawn (late/early), hug themselves (cold), check their phone, stretch, think, dance a little (cheerful/laid-back) or look up at the sky.
- **Fleeing** (`Resident.flee`): runs to a pathable spot away from the threat (`DefaultRandomPos.getPosAway`), picks the nearest monster, surprised gesture instead of cheering, and panic spreads to free residents within 10 blocks who can see the one fleeing. Firefighters no longer flee (like police).
- mods.toml version 1.13.0.

### Check in the real client (v1.13)
1. Build + `/city test player` (64 checks).
2. Watch a busy hangout (plaza, park, diner): residents should spread out and face each other rather than stack.
3. Walk into an idle resident in a corridor; they should step aside.
4. Couples/friends leaving together should walk side by side.

## 000. v1.12.0 - newest (2026-09-29)
Player-facing commands moved to a no-op-needed root **/sol** (tutorial, gender, romance, stalk, garage). Admin stays under /city (root requires op).
- **Photo/video kick fixed** (`PcNet`): `Blob` was registered twice for one class, so uploads went out with the client-bound id -> Forge "Illegal packet received". Now `BlobUp` (id 3, to server) and `Blob` (id 4, to client). Photo downloads by other players were affected too.
- **Video recording** (`ClientVideo`): pressing record closes the phone so you can walk around; HUD shows REC timer/progress; phone key stops; up to 24 s (160 frames at 128x72); upload is queued 4 chunks/tick. `Photos.MAX_VIDEO` 6 MB.
- **Voices** (`VoiceApi` shared, `VoiceServer`, `client/VoiceClient`): players without their own ElevenLabs key get audio relayed from the host (Act `voice_req` -> Blob kind `voice`, cached by sha1 in config/fireheartcity/voicecache). Legacy ElevenLabs voice ids fall back to modern ones (MODERN map) on 404. quietHours default now false. `/city voice` tests the key live (subscription/characters) and prints relay stats + last error. Ellie uses `Cast.SULTRY` (Charlotte XB0fDUnXU5powFXDhCwa, calm/flirty settings). **Groq**: the user's `fireheartcity-ai.properties` still had an empty `groqApiKey=` on 2026-09-28.
- **Police** (`Police.java`, `client/CinemaFx`): sidearm (item `fireheartcity:sidearm`, drawn during SHOOT/SPRAY/RAIL, holstered after) - hitscan with tracers vs creepers (from range), flyers, crowds. Tanks (warden, ravager, wither, elder guardian, >=60 HP) trigger `callBackup` (all officers incl. sleeping ones, radio sound, action-bar alert) and heavy moves (BARRAGE, RAIL), x1.7 damage. **Ultimate** (chance per 20 ticks, much higher vs tanks): 44-tick cinematic power-up (`#fx|ult` -> letterbox, title, FOV, roll), 420 ticks of WARP/METEOR/RAIL/BARRAGE with x2.4 damage, then 4800 ticks resting (slowness, won't be dispatched) and a 36000-tick cooldown. **Finisher** when a tank is <12% or any target during ult is low: `#fx|finish` cutscene, 60 ticks, guaranteed kill. Sounds police.shot/rail/ult/finisher/radio (synthesised, `tools/sfx_police.py`). Gestures G_AIM 36, G_ULT 37.
- **Firefighters** (`FireDept`): arcing hose up to 12-16 blocks with line of sight; when stuck and the fire is above them they pillar up on scaffolding (CLIMB) and tear it down afterwards (DOWN); otherwise throw a water bomb (3-block radius). G_HOSE 39.
- **Repair crew** (`Repair.java`, resident **Gus**, Job.REPAIR, skin 33, apt5C): `res/data/fireheartcity/blueprint.bin` (from `tools/blueprint.py` over the pre-damage world copy; natural blocks, consumables and new mod builds excluded) is scanned 3 chunks every 2 s for holes (air/fire/replaceable where a building block should be; must stay missing 60 s) plus non-player explosion damage anywhere near the city. Player breaks are remembered as intentional (`ignored`). Gus walks there and every block flies from his hands (block_display with interpolation) into place. SavedData `fireheartcity_repair`. `/city repair [scan|accept <r>|clear|on|off]`. The police-station west wall (x48-50 z-47..-43) damage is in the blueprint.
- **Magma Beach Bar** (`BeachBar.java`): queued as a Gus construction job once (CityData.beachBar) at x84-98 z38-51 deck y71 on stilts, thatched tiki bar, stools, umbrellas, torches. `/city beachbar` re-queues. Places `beach_bar`, `hotel` (magmagamer9's hotel, 51 78 33), `beach` added to CITY_HANGOUTS.
- **Traders** (`Traders.java`): residents spot wandering traders / employed villagers, walk over, haggle over a real offer; fair -> deal, rip-off -> brawl until the trader dies.
- **Headphones**: `Color` NBT tinted (8 phone colours, picker in SolTech), SolBeats **Magma Edition** (`hpmagma`, Magma NBT, own 3D model, ember particles while worn).
- **The Watcher** (`client/Stalker.java`): `/sol stalk <player> [stop]`, only StellarFox1/Fireheart_4743. Fully client-side for the target: tall thin figure appears behind them, breathing (synthesised stalker.breath, not downloaded), follows; when looked at it cracks its head sideways, lunges with a scream and vanishes.
- **Romance** (`Romance.java`): gender asked once on login (clickable), saved permanently (`/city gender reset <p>` for admins). Residents in `Romance.WOMEN` only date/confess to "m" players and vice versa. Flirting/compliments build Rel.romance; confessions at romance>=30 & aff>=40 with clickable yes/no (`/sol romance yes|no <id>`); ask out; dating = settings `sweetheart`, hearts, pet names, sweet texts, kiss/hug, dates (they follow you), breakups.
- **Messaging**: read receipts ("✓ Delivered" / "✓✓ Read") and "X is typing…" bubbles in phone Messages and PC Messenger (`Receipts` server+client). Residents read after ~1/3 of their reply delay, type ~2-5 s before replying; player-player threads relay reads/typing.
- **Tutorial** (`Tour.java`, `client/TourHud`): `/sol tutorial` (offered on first join): ride Nova's holo-drone (invisible armour stand) across 13 stops incl. Neon Heights, buildings outlined + beamed, typewriter narration, camera drifts to each sight, "open your phone" task, 3-question quiz with coin reward, fireworks. Sneak to leave.
- **SolEats**: dishes arrive as `fireheartcity:dish` items (NBT Dish + CookedAt) with individual 3D models (`tools/dishes.py`, atlas `dish_atlas.png`), hot for 15 min (steam particle `fireheartcity:steam` from hands, ground and displays; hot meal = regen). `Kitchen.java`: the shop's cook walks to the nearest stove and cooks the order with item displays (raw -> tossed -> cooked), sizzle/bubble/chop sounds, plates it steaming, "Order up!".
- **SolPlay Gen 2** (`Gen2Game` engine: countdown, P pause, particles, shake, persistent achievements in config/fireheartcity/solplay.properties, `Gamepad` GLFW controller support): Neon Drift (pseudo-3D racer), Stellar Fox Run (parallax runner with the user's fox), Neon Maze 3D (raycaster), Beat Solaris (rhythm). On PC/SolPad/SolBox and SolPhone 2; SolPhone 1 and the SolStation keep the classics (Gen 2 shown locked). Console: gamepad, crash guards (any app/game exception returns to menu instead of crashing), resume a game within 10 min, hints.
- **Vehicles** (`Vehicle`, `Vehicles`, `VehicleDrive`, client `VehicleRenderer`/`VehicleClient`): Solaris Coupe, Street Bike (leans), Speedboat (bobs, planes, wake). Boat-style client-authoritative driving, W/S/A/D, Space handbrake drift, H horn, L headlights, 1-block kerb climbing, 6 paints, speedometer HUD, engine loops. Keys place them; hit 3x (owner) to pack up. `/sol garage give [paint]|park`, `/city garage replace` (op) runs `vs delete fireheart-cr01/mb01/sea01` and parks new ones at 2 71 -20, 5 71 -20 and the marina.
- Sounds are all in `res/assets/fireheartcity/sounds.json`, generated by `tools/sfx_*.py` (numpy + ffmpeg).

### Not done / next (v1.12)
1. Nothing here was seen in the real client yet (server-tested only: 64/64, repair/beach bar build, warden fight). Check: vehicle model orientation/feel, dish models, steam, stalker, tour camera, Gen 2 games.
2. First real-world blueprint scan may flag blocks the user deliberately removed after 18:14 on 09-28 outside the excluded zones - `/city repair accept 30` where needed.
3. magmagamer9 must install the same jar.

## 00. v1.11.0
- **Beach** (live world only, datapack `beach.zip` in the world's datapacks): `beach:go` cleared x42..108 z24..60 to grass/sand down to the sea; `beach:smooth` sloped the surrounding hills (north band only to z>=5 to spare Solaris East). For magmagamer9's beach bar.
- **Stellar House** - `StellarHome.java`. StellarFox1's futuristic house on the hilltop north of the beach (X66..98, Z7..21, floors y84/y90, roof y96), navy/black + white/light-blue star theme (betterblockz zeon/cyberlight/aurora/azur/zenohex, `"a|b"` fallback specs). Automations: sliding glass doors, presence and night lights, animated neon streak ring at y84, infinity pool (z23..26), fox-constellation crest + antenna + landing ring on the roof, night searchlights, ASTRA actionbar voice, ambient sci-fi sounds. Levitation lifts: beach (63,20) y71<->84, stairs (92,11) y84<->90, **secret lift** 3x3 at (71,10) y84<->56, owners only (StellarFox1, magmagamer9, Fireheart_4743): sneak on the study hatch to drop, stand on the pad to rise. **Secret base** x64..100 z6..22 y56..68: reactor core (88,57,14), TV wall, computers, holo map table (76,58,16, particle heightmap + live residents/players), server racks, jet (69,57,17), red laser grid at x74 that zaps non-owners. Builds itself once (`CityData.stellarHome`) when chunks load; `/city home` teleports, `/city home rebuild` rebuilds.
- **SolPhone Maps overhaul** - `client/pc/MapApp.java`. Baked satellite textures `textures/gui/map_city.png` (x-176..175, z-112..175, 2 px/block) and `map_isle.png` (x-80..79, z208..351) made by `scratchpad/mapgen.py` from the test world regions (averaged block textures, water depth, hillshade). Zoom 1-10x (wheel at cursor, +/-), drag-pan (new `App.drag`, wired in PhoneScreen/ComputerScreen `mouseDragged`), arrow keys pan, ◎ recenters on you, you-are-here pulse + heading arrow, resident face markers, ranked labels that appear with zoom (incl. Police, Fire, Lab, Cinema, Stellar House, Firework Machine, Beach, Hall of Lights), scale bar, compass; list row click focuses that resident, "➤ Go" = directions. To refresh the map after big builds, rerun mapgen.py on an updated world copy.
- **Cinema via WATERMeDIA 3** - `client/pc/CinemaVideo.java`, `TvRender.cinema()`. Optional dep (`mods.toml`, client, `[3,)`); compiled against `libs/watermedia-3.0.0.23.jar`. Booth TV program `url;<path|link>` -> `MediaAPI.mrl` -> `createPlayer(mrl, glEngine, alEngine)`, texture drawn letterboxed on the big screen (x71..94, y73..82, z~1), synced by seeking to elapsed time, loops. Audio source placed at (82.5,77.5,1) (ref 6, max 48, volume = master x records). Set it with `/city cinema play <path|url>` / `/city cinema stop`, or right-click the booth TV (82,78,-16) with a book whose first page is the link. Saved in `CityData.cinemaUrl`. Without WATERMeDIA the screen shows an install hint.
- **Groq**: the user says he added his key. `/city ai` shows status after restarting on 1.11.

### Not done / next (v1.11)
1. In-client checks: WATERMeDIA playback + 3D audio (untested), Stellar House automations with a real player, map textures/drag in the phone, Groq with his real key.
2. The baked map predates the beach (the test world copy is older); regenerate from a fresh world copy.

## 0. Latest session (v1.8.0 -> v1.10.0) - read this first
- **Version:** mods.toml now carries the real version (was stuck at 1.4.0). Current: **1.10.0**. The jar filename stays `fireheartcity-0.1.0.jar`.
- **Toolchain in this session:** jars staged to `/mnt/user-data/uploads/libraries/...`; `mkdir /mnt/user-data/uploads/meta && ln -s ../libraries /mnt/user-data/uploads/meta/libraries` makes cp.txt paths resolve. Test server `/tmp/srv` libraries are `cp -rs` of that plus symlinks server-srg/extra -> client ones, forge-server -> forge-client, and stub jars for java-objc-bridge and the linux epoll natives (not on his PC). Mods for the server: everything except Essential, CustomSkinLoader, embeddium, oculus, effortlessbuilding.
- **Skins:** `Resident.SKINS = 33`. 20-23 = HD 256x256 slim (Slate/Sage/Rose/Auburn), 24-27 = 64x64 slim from JPGs (white made transparent on outer layers), 28-30 police uniforms, 31-32 firefighter uniforms (generated in Python from base skins). `ResidentRenderer.slim()` = 20..27 -> PLAYER_SLIM model. Assignments: Mia 22, Nina 20, Ivy 21, Nell 23, Ellie 24, Zara 25, Ava 26, Nova 27, Dex 28, Kira 29, Bruno 30, Hank 31, Sofia 32.
- **Ellie (Receptionist, LAIDBACK + flirty, apt4D)** - `Reception.java`. Works 5000-15800 at the Ember Heights front desk (stand 30 71 17, desk 28..32 71 18, bell 30 72 18). Residents entering the lobby heading upstairs queue at the desk (spots z19-21), she serves the nearest within 3.2, greeting -> guest line -> bell + key handover (sayLine so repeats aren't deduped), stalled guests dropped after 30 s. No random chats while she's on duty. Player: "check in"/"key"/"room" -> named tripwire hook once per day. Persona lines in `Reception.persona/flirt`. New `Trait.LAIDBACK`; `Cast.flirty()`.
- **New residents arrive automatically** (`Events.newcomers`): any Cast member missing from the save is spawned at the lobby with a news item.
- **Sky Organ** - `OrganConsole.java`. Re-places the PLAY/NEXT/STOP buttons if broken (PLAY warped 53 181 283 needs its backing block 54 181 283). New **venue button** (mangrove, 53 181 281, sign above): toggles `CityData.organInHall`. In hall mode PLAY cancels the datapack (#on 0) and the mod plays the song in the Hall of Lights from `res/data/fireheartcity/organ/songN.txt` (extracted from the datapack seq functions: tick, instrument b/h/e, note 0-24), lamps light per note column + dust/note particles.
- **Firework Machine** - `FireworkMachine.java`. Island centred -2 70 130 (radius 15, deck y70 on pillars) + bridge x-3..-1 from the Skyport (z96) to z115. Launch buttons: bridge -4 72 97 (east face), island -2 72 117. `/city fireshow [stop]`. Auto show Wed + Sat (and festival) at tod 12300. 2400-tick show: power-up + 3-2-1 titles, opening salvo, ring of fire, sweeping tower fans, sky shapes (heart, stars, smiley, planet, spiral), flame/water jets, SOLARIS sky text, finale barrage, giant gold burst, willow. Big effects are forced long-range particles (visible city-wide). Old pier show disabled once built (`Fireworks.schedule`). Residents cheer.
- **Solaris PD** - `Police.java`, `client/HitAnim.java`. Dex (night) & Bruno (night) live in the new bunkhouse x65..69 z-51..-42; Kira (day) apt2D. `Police.dispatch` sends the nearest free officer to any hostile within 16 of a resident/player; moves: jab combo, dash, taser, uppercut -> air spike, spin kick, ground slam shockwave, creeper punt; hit-stop freeze then launch. `#hit|id|kind|dx|dz|power` messages drive client impact anims (recoil, launch spin, spin, pancake slam, taser shake, camera shake on slams). Gestures 27-35 in ResidentModel.
- **Solaris Fire Dept** - `FireDept.java`. Hank (day) & Sofia (night), bunks added on the fire station ground floor (77/79 71 -51). Scans loaded city chunks every 2 s for fire (palette check), rings the station bell, nearest firefighter runs over (emergency routing via `Resident.emergencyTarget()` + CommuteGoal at speed 1.6) and hoses fires out. `/city firedrill` lights a practice fire at 64 71 -29.
- **Groq AI** - `Groq.java`. Key in `config/fireheartcity-ai.properties` (`groqApiKey=`, `model=openai/gpt-oss-20b`). Chat.handle: rule reply is computed first (keeps side effects), then for non-action messages the persona + rule reply as "facts" is sent to Groq async; "..." bubble meanwhile. 429 -> pause until retry-after/x-ratelimit-reset (fallback to rule dialogue), 401/403 -> disabled until the key changes, 3 failures -> 5 min pause. `/city ai` shows status. Not yet tested with a real key.
- **Effortless Building** installed via the Modrinth App.

### Not done / next
1. Verify v1.10 in the real client: firework particle visibility from the city, police hit animations, organ hall mode sound, police/fire skins. (Launching failed this session: Modrinth said the instance was "already running" while the game was open with magmagamer9.)
2. Test Groq with his real key; tune `model` if Groq rejects it.
3. WATERMeDIA shows as installed in the Modrinth UI but is NOT in the mods folder.

## 1. The user
- **Who:** Daniel.
  - His Minecraft name is now **StellarFox1**. It was Fireheart_4743, and there's a new 256x256 HD skin.
  - He plays creatively and writes casually with typos, so read his messages for intent.
  - He wants short, professional replies, and code without comments unless they're needed.
  - Use the AskUserQuestion tool for big jobs; just do quick fixes.
- **His brother:**
  - Minecraft name **magmagamer9**; he joins through Essential (LAN-style multiplayer).
  - His house is at **x -67..-51, z 32..51**, front door at **-51 71 41/42** (facing west, onto the road at x -45).
  - It's decorated, and has a mailbox at **-46 71 39** (facing east).
- **City name:** **Solaris**. The apartment tower keeps the name "Ember Heights" and the sky island is "Neon Heights".
- **Apps:** SolPhone, SolFeed, SolTube, SolEats, SolTech, SolOS, SolNet, SolBeats, SolPad, SolWatch, SolStation, and SolBox (the console block).

## 2. Setup (paths)
- **Game:** Forge 1.20.1 (47.4.20), Modrinth App, profile **"Create_ Remastered"**, world **"Create!"**.
- **Profile folder:** `C:\Users\whosh\AppData\Roaming\ModrinthApp\profiles\Create_ Remastered\`
  - **Mod file:** `mods\fireheartcity-0.1.0.jar`. Keep this filename, and replace it only while the game is closed.
- **World datapacks:** `saves\Create!\datapacks\`.
  - `brohouse` (`/function brohouse:go` re-runs the brother-house decoration).
  - `statue` has no pack.mcmeta, so it's inactive. The statue is now built by the mod.
- **Voice:** `config\fireheartcity-voice.properties` holds the ElevenLabs key.
- **Device bridge:**
  - Remote-device tools: stage and commit files, computer use (javaw + Modrinth App), and sometimes `device_bash`.
  - The v1.7.0 jar **was copied into the mods folder** at the end of this session. The game must be restarted to load it.

## 3. Build and test (cloud workspace)
1. Unzip `fireheartcity_mod_source.zip` to `/tmp/modbuild`.
2. Rebuild the named jars and `cp.txt` as described in the old handoff, Part 7 (search "mkmap.py").
3. Run `bash build.sh`, which produces `fhc.jar`. It aborts on compile errors: never ship a failed build.
- **Compiling rules:**
  - Compile against Mojang-named jars; ART then reverses the jar to SRG.
  - Record accessors must use SRG names, e.g. `BlockStateParser.BlockResult.f_234748_()`.
  - Mixins use `remap=false` with SRG method names.
- **Test server:** `/tmp/srv`, scripts in `test_server_scripts/`.
  - `restart.sh [fresh]` copies `/mnt/user-data/uploads/Create!` (a staged copy of the world) as `world` and installs `fhc.jar`.
  - `./c.sh "cmd"` sends a command; output goes to `out.log`.
  - Re-apply forceloads after every restart: `-48 -40 48 112`, `-72 218 62 336`, `-30 -92 26 -28`, `41 -62 106 6`.
- **Useful commands:**
  - `/city test player`: 64 automated checks; all pass.
  - `/city ask <residentId> <text>`: prints the resident's reply. This is the best way to test dialogue.
  - `/city festival [start|stop|skip <ticks>]`
  - `/city mailtest <player>`: queues a letter with a gift plus a SolEats parcel.
  - `/city dump <from> <to>`, `/city dumpents`, `/city findblock <id>`
  - `/city surface <from> <to>`: writes `world/surface.txt` (x z y block) for map renders.
  - `/city skytower`

## 4. What changed this session (v1.7.0, commits in git log order)
### Fixes
- **Time:** the world had `doDaylightCycle=false`, which stopped the watch, the clock tower and the festival. `Events` now re-enables it every 10 s while config `timeMoves` is true.
- **Chat:** resident speech no longer goes to chat (`d.chatter` is forced false; speech bubbles only).
- **Vibrating residents:** fixed; the dance no longer snaps its angle every tick, and the shiver is a subtle occasional sway.
- **Factory door:** the Create piston contraption was lost. `AutoDoor` is now a code-driven sliding iron door at -3..-1 71..73 z12.
- **Factory worker:** fixes stalled or empty diesel engines through `Maintenance.fix` (with the wrench animation) and harvests and replants the wheat field. `Maintenance` also refreshes every diesel engine city-wide every 5 minutes.
- **Sky Launch:** duplicate tower at -61 156 removed. The tower was moved into the city at **98 71 -44**; the old one at -86 145 is removed when its chunk loads.

### Festival of the Founder
It now runs on its own timeline, so it works at any time of day.
- The stages are gather, sermon, chant, offerings, finale, then a **3-minute feast**:
  - cake stalls and lanterns;
  - a music loop;
  - residents dancing and eating;
  - fireworks every 30 s;
  - a hamper for the player and gifts from residents.
- Everything is cleaned up afterwards.

### Residents' intelligence
- **`Intents.meet`:** "meet me at <place> at <time>" in person, by text or on a call. The resident agrees, or counter-offers if they're working or asleep, then walks there and waits (`Meets.java`). They text when they arrive, greet the player and follow them. If stood up, they send a sad text and lose some trust.
- **`Intents.more`:** job, home, time, weather, directions, jokes, advice, feelings, insults and more.
- **`Understand.java` (large):**
  - Sentence patterns: do you like, what do you think of, have you ever, can you, would you, are you, did you, what is, where is, how many, how do, when, why, X or Y, and statements like I like / I built / I'm going to / my X is / I think / I have.
  - Opinions are stable per resident.
  - Follow-ups: why, what about you, really, same, tell me more.
  - It replaces the old "Interesting..." fallback with replies that echo the player's words.
- **`Knowledge.java`:** facts about Solaris places, gadgets, people, the Founder and magmagamer9.
- **Pets:** residents notice players' pets (name tag or animal type), fuss over them and talk about them.
- **Body language:** speech drives gestures (`Resident.emoteFor`), with expression particles per gesture.

### Rebrand
- **Solaris:** all display strings and app names renamed. `Rebrand.java` rewrites old names on signs chunk by chunk.
- **Statue:** rebuilt from the StellarFox1 HD skin by `statue_gen.py`, whose output lives in `res/data/fireheartcity/functions/statue/stellar1..2`. It runs once when its chunks load (`SkyTower.statue`, flag `statueV2`).

### Phone and tech
- **Real photos:**
  - Captured from the world render with no GUI (`ClientPhoto`), uploaded in chunks through the `PcNet.Blob` channel and stored in `<world>/fhc_photos/`.
  - Gallery keys are `ph_<id>`, fetched on demand by `PhotoCache`.
  - Captions when posting, a 3 s timer, and "set as wallpaper".
- **Video:** camera VIDEO mode records about 7 fps for up to 9 s as a 128x72 sprite sheet (`vd_<id>`, `ClientVideo`). Videos can be posted to SolFeed or **uploaded to SolTube** (`d.uploads`, action `tubeup`).
- **SolTube:** 3 new, richer premade videos (skyline day-to-night, festival highlights, ocean documentary). Uploads show in SolTube and play on TVs and in the cinema.
- **Settings:** wallpapers (8 painted presets or a photo), 24-hour clock, message previews, app labels.
- **App icons:** painted icons in `textures/gui/icons/*.png` (`AppIcons`).
- **Multiplayer:**
  - Other players appear in contacts as `player:<Name>`.
  - Texts go to their inbox and trigger a toast.
  - Live player-to-player calls with text lines (`PlayerLink.java`).
- **Headphones:**
  - They're an Equipable HEAD item with a 3D model (forge separate_transforms: 3D on the head, 2D icon in the GUI).
  - Right-click plays a two-hands put-on animation (`#deva|id|don`); music auto-plays while worn and stops when removed.
  - Sneak + right-click skips track.
- **TV:**
  - Everyone sees what's on it: `TvShows` (server) plus `TvRender` (client), which draws `GuiGraphics` into the world with depth testing.
  - Idle TVs show a "SolTV" channel.
  - The player holds a **SolTube Remote** while using a TV.
- **SolBox console block:**
  - Costs 150 at SolTech, the most expensive item.
  - Right-click needs a TV within 6 blocks. The camera turns to the TV, which plays a boot intro, then an animated menu with Snake, 2048, Flap, Mines, Blocks, SolTube and Photos.
  - The picture is on the world TV and visible to others; F toggles full screen.
  - The player holds a **SolBox Controller**.
- **SolEats overhaul:** shop cards with open/closed status and ratings, menus with item icons, a cart with quantities, a tip for Pip, and order tracking (preparing, out for delivery, at your door).

### Post
- **Mailbox block:** the owner is whoever places it. Every player gets one on first login.
- **Letters and gifts:** the postman walks to the player's mailbox, opens it, puts the post in with an animation and raises the flag. The player gets a toast.
- **SolEats orders:** left as a **delivery box block** beside the mailbox (at the front door). Right-click the box to open it.

### New blocks and items
- Ceiling lights (panel, round, spot, pendant; right-click toggles, light 15).
- Mailbox, delivery box, SolBox, remote, controller.

### Solaris East (island expansion)
- **Location:** x 41..106, z -62..6, built once via `Expansion.java` and `Builder.java`.
- **Road:** a new road (z -28..-24) links to the x=30 road by the library.
- **Buildings:**
  - **Police station:** x48..64 z-52..-34, with cells, desk, computers and a police car.
  - **Fire station:** x70..88 z-52..-34, with two fire trucks, fire pole, bunks and a siren bell.
  - **Research lab:** x46..62 z-20..-4.
    - Glowing froglight tubes and Create fluid tanks (lava/water) with pumps and pipes.
    - A water containment chamber with a conduit.
    - Brewing, cauldrons and computers.
  - **Cinema:** x68..96 z-21..2.
    - Marquee, lobby with snacks and tickets, red sofa rows (another_furniture) and curtains.
    - The **big screen** is drawn at x71..93 y73..81 z1.
    - It mirrors the projection-booth TV at **82 78 -16**; otherwise it runs a "Now Showing" schedule.
- **Places for residents:** police, fire, lab, cinema. The cinema is a hangout and date spot.

### Other additions
- **Hall of Lights:** on the Sky Organ bridge (x21..38 z282..288, floor y180). Walls and ceiling of redstone lamps glow around players, ripple waves with rising chimes play as they walk, and there's a finale at the organ end (`MusicHall.java`).
- **Parrots:** players' parrots dance while their owner listens to music, greet and flutter to them, and chirp with hearts. Sneak + right-click to pet. Client-side they bob and sway (`ParrotAnim`).
- **Brother's house:** decorated with rugs, ceiling lanterns, plants, a garden, a welcome sign, a parrot perch and a gift chest, via the `brohouse` datapack.

## 5. Key files (`src/com/fireheart/city/`)
- **Residents and dialogue:** Resident, Chat (dispatch), Intents, Understand, Knowledge, Meets, Pets, Festival, Work (jobs and tasks), Maintenance, AutoDoor, Post (+ MailboxBlock, ParcelBlock).
- **Phone and network:** Phones (texts and calls), PlayerLink, Photos, PcNet (network: Data/Msg/Act/Blob), Computers, Extras (phone actions, SolEats, gallery, settings), TvShows, TechStore, DeviceItem.
- **World building:** Expansion, Builder, MusicHall, SkyTower, Rebrand, ParrotLove, CeilingLightBlock, ConsoleBlock, TvBlock.
- **Client (`client/`, `client/pc/`):**
  - PhoneScreen, CameraApp, EatsApp, TubeApp, SettingsApp, Wallpapers, AppIcons, PhotoCache, ClientPhoto, ClientVideo, ConsoleScreen, ConsoleUi, TvRender, DeviceAnim, ParrotAnim.
  - ResidentModel/Renderer (gestures G_* 0..26).
- **Saved data:** CityData holds all flags: expanded, hallBuilt, statueV2, towerFixed, mailboxSetup, oldTowerGone, doorsMigrated.

## 6. Not done yet / next steps
1. **Check v1.7.0 in the real game:**
   - The jar is already in the mods folder; restart the game to load it. If needed, copy `fireheartcity-0.1.0.jar` from this folder over `mods\fireheartcity-0.1.0.jar` while the game is closed.
   - On first load:
     - The statue rebuilds.
     - Solaris East builds; it takes about 0.5 s, when the player is near x 70 z -30.
     - The Hall of Lights builds when the player is near the Sky Organ.
     - The brother's mailbox appears, and the old towers are removed.
   - Check each in-game.
2. **Mods not installed yet** (container network blocks Modrinth; install through the Modrinth App with computer use):
   - **WATERMeDIA:** needed for the cinema to play video files from his computer. Hook it into `TvRender.cinema()` / the booth TV program string (add e.g. `url;<path>`).
   - **Effortless Building:** no code conflicts expected. Mailboxes are claimed on first right-click even if Effortless skips `setPlacedBy`.
3. **Client-only features, not verified in the real client:** photo and video capture, TV/cinema world rendering, headphone 3D model, SolBox screen, parrot bob. Test them after installing, and watch `logs/latest.log` for errors from `TvRender`, `ClientPhoto` or `ClientVideo`.
4. **Ideas that came up:**
   - Police and fire staff jobs (new Job entries), so residents actually work in Solaris East.
   - Brother's own letters and SolEats to his mailbox (already works if he orders).
   - More premade films.

## 7. Contents of this handoff folder
- `SOLARIS_HANDOFF.md`: this file.
- `Fireheart_Project_Handoff.md`: the previous full handoff (history, toolchain, block IDs, generators).
- `fireheartcity-0.1.0.jar`: the v1.7.0 build, ready to install.
- `fireheartcity_mod_source.zip`: `src/`, `res/`, `build.sh`, `cp.txt`, `manifest.txt`, `mkmap.py`, `srg2named.srg`, `statue_gen.py`, `tools/`, plus the git history as `git.bundle`.
- `datapacks/brohouse/`: the brother-house decoration datapack.
- `test_server_scripts/`: `restart.sh`, `run.sh`, `c.sh`.
