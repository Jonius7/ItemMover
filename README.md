# ItemMover

For Minecraft 1.7.10

Adds an item mover block that can move items between adjacent blocks.

![](https://github.com/user-attachments/assets/0672be7f-d470-4d01-8afd-9c0d8d650e60)

ItemMover block has 18 slot internal inventory, 12 Pull and 12 Push fully-featured ghost slots, and Pull and Push side selection.

- Can increase stack size of ghost slot by **left clicking**, decrease stack size by **right clicking**.
- Can put in held stack size in ghost slot by **shift + clicking**.
- Pulls items from specific slots in input (not incl. chests which pull from first available slot)
- Pushes items into specific slots in output
- Has **Smart Push** toggle to only push if all required items configured in ghost slots are in internal inventory
- Can configure input and output sides (NORTH, SOUTH, WEST, EAST, DOWN, UP)
- Can change slot number for each ghost slot: 
  - **left click** to increase by 1
  - **right click** to decrease by 1
  - **ctrl + click** to increment/decrement by 5
  - **middle mouse click** to reset to default