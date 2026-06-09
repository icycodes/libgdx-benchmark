Handling multiple aspect ratios seamlessly across Windows, Android, and iOS is critical for cross-platform deployments without stretching graphics or displaying black bars unnecessarily.

You need to configure an `OrthographicCamera` coupled with an `ExtendViewport` set to a minimum virtual resolution of 800x480. Ensure the camera and viewport dynamically update correctly when the application window is resized.

**Constraints:**
- Must override the `resize(int width, int height)` method in your application lifecycle to call `viewport.update(width, height, true)`.
- The camera must be automatically centered on the virtual screen coordinates upon resizing.
- Do NOT use `StretchViewport` or `FitViewport` for this task.