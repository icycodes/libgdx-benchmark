Loading heavy game assets (textures, music) synchronously blocks the render thread, resulting in a frozen screen or operating system "App Not Responding" crashes.

You need to implement a loading screen that utilizes `AssetManager` to queue and load a set of textures asynchronously. During the render loop, update and display a progress bar representing the current load percentage based on `AssetManager.getProgress()`.

**Constraints:**
- Must call `AssetManager.update()` within the `render(float delta)` method to step the loading process.
- Must transition to the main game screen ONLY when `AssetManager.update()` evaluates to `true`.
- Do NOT use blocking load calls like `AssetManager.finishLoading()` in the render loop.