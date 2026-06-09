libGDX uses the `Game` and `Screen` interfaces to manage different application states, such as menus, loading screens, and gameplay loops.

You need to create a main `MyGame` class extending `Game` and two screen classes (`MainMenuScreen` and `PlayScreen`). Implement the logic to set the initial screen to `MainMenuScreen` on startup and transition to `PlayScreen` upon a screen tap or mouse click. 

**Constraints:**
- Must extend `Game` for the main application and implement `Screen` for the views.
- Do NOT leave graphical resources un-disposed; ensure the `dispose()` method is correctly utilized in both screens.
- Must use `Gdx.input.justTouched()` or register an `InputProcessor` to trigger the screen transition.