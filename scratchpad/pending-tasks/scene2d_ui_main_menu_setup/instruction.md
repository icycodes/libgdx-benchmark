libGDX's Scene2D is a 2D scene graph API heavily utilized for creating complex user interfaces, HUDs, and menus without manual positioning math.

You need to implement a basic Main Menu using a `Stage`, `Table`, and two `TextButton` actors. Center a "Play" and "Settings" button vertically in the middle of the screen using a default UI `Skin`. 

**Constraints:**
- Must instantiate the `Stage` with a `ScreenViewport`.
- The `Table` must fill the parent stage using `setFillParent(true)`.
- Do NOT hardcode exact pixel coordinates; you must use table cell properties (e.g., `.fillX()`, `.pad()`, `.row()`) for button alignment and layout.