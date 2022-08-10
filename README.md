# Path marker
There are two categories:
- Active path: Highlights your currently active path.
- Hover-path: Highlights the path you'd do if you would leftclick.  

![image](https://user-images.githubusercontent.com/52377234/183302759-365a8b06-7340-4c2b-8a9d-cf776cc1b7bf.png)  
### Config options:
- Display: Always, While keybind pressed, Toggle on keybind, Never
- Draw location(s): Game map, Minimap, Both
- Draw mode: Full path, Target tile
- Keybind: sets the keybind for the Display setting
- Main tile color: the color of the path's tiles you're going to visit
- Secondary tile color: the color of the path's tiles you're going to "skip" due to running over them
- Draw only if no active path: stops the hover-path from being drawn if there's an active path visible
### Known issues:
- Paths cannot be calculated to outside the currently loaded area, as the server hasn't sent the data of tht area yet.
- There's no path overlay when following a player. This won't be fixed as it's rare to happen and hard/impossible to implement.
- The data the plugin uses to check from which side(s) you can interact with an object is manually generated from the client's cache. RuneLite does not offer an api to get that info, and I can't add it to RuneLite either because it requires Mixin edits (which aren't open source). So if/when new objects are added of which you want to see a correct path towards, feel free to let me know.
- The data the plugin uses to check if you can't move through a NPC (e.g. bearded gorillas on Ape Atoll or Brawlers in Pest Control) is manually made, as the client does not have that info. There are probably a lot of such NPC's I didn't implement. If you happen to stumble on such NPC, please let me know and I'll update the data.
- There's a small area around the minimap of which the plugin thinks it's on the minimap, but it's actually not. RuneLite currently doesn't offer the API needed to do a true onMinimapClicked check, and I can't add it to RuneLite either because it requires Mixin edits (which aren't open source).
### Contact options:
You can submit an Issue on this github project, but I'll be best available on Discord (GeChallengeM#9201), either in DM's or through RuneLite's discord server if that's not possible.
