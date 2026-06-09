# libGDX Research Report for Evaluation Dataset Creation

## 1. Library Overview

*   **Description**: libGDX is a mature, cross-platform Java game development framework built on OpenGL (ES). It allows developers to write their game logic once in Java and deploy it to Windows, Linux, macOS, Android, iOS, and web browsers.
*   **Ecosystem Role**: It sits between low-level graphics APIs (OpenGL) and high-level game engines (Unity/Unreal). It is "code-centric," meaning there is no official visual editor; everything is handled via code and external tools (like Tiled for maps or Spine for animation).
*   **Project Setup**:
    1.  **Tool**: Use the official [gdx-setup](https://libgdx.com/wiki/start/project-generation) tool or the modern community-preferred [gdx-liftoff](https://github.com/libgdx/gdx-liftoff).
    2.  **CLI Initialization**:
        ```bash
        java -jar gdx-setup.jar --dir my-game --name MyGame --package com.example.game --mainClass MyGame --sdkLocation /path/to/android/sdk
        ```
    3.  **Standard Commands**:
        *   Run Desktop: `./gradlew lwjgl3:run` (or `desktop:run` for older setups)
        *   Run Android: `./gradlew android:installDebug android:run`
        *   Build HTML: `./gradlew html:dist`
    4.  **Structure**: A multi-module Gradle project where `core` contains the game logic, and other modules (`lwjgl3`, `android`, `ios`, `html`) contain platform-specific bootstrap code.

## 2. Core Primitives & APIs

*   **ApplicationListener / Game / Screen**: The lifecycle and navigation backbone.
    *   [Wiki: Lifecycle](https://libgdx.com/wiki/app/the-life-cycle)
    *   [Wiki: Screens & Game](https://libgdx.com/wiki/start/simple-game-extended)
    ```java
    public class MyGame extends Game {
        @Override
        public void create() {
            setScreen(new MainMenuScreen(this));
        }
    }
    ```
*   **SpriteBatch & Texture**: For 2D rendering.
    *   [Wiki: SpriteBatch](https://libgdx.com/wiki/graphics/2d/spritebatch-textureregions-and-sprites)
    ```java
    batch.begin();
    batch.draw(texture, x, y);
    batch.end();
    ```
*   **OrthographicCamera & Viewport**: Managing aspect ratios and resolutions.
    *   [Wiki: Viewports](https://libgdx.com/wiki/graphics/viewports)
*   **Scene2D (UI)**: A 2D scene graph for menus and HUDs.
    *   [Wiki: Scene2D](https://libgdx.com/wiki/graphics/2d/scene2d/scene2d)
    ```java
    Stage stage = new Stage(new ScreenViewport());
    Table table = new Table();
    table.setFillParent(true);
    stage.addActor(table);
    table.add(new TextButton("Play", skin)).fillX().uniformX();
    ```
*   **AssetManager**: Asynchronous loading and management of resources.
    *   [Wiki: AssetManager](https://libgdx.com/wiki/asset-management/assetmanager)

## 3. Real-World Use Cases & Templates

*   **SaaS/Game Templates**:
    *   [gdx-liftoff Templates](https://github.com/libgdx/gdx-liftoff): Provides "Game," "Simple," and "Empty" templates.
    *   [libgdx-template (halfcutdev)](https://github.com/halfcutdev/libgdx-template): A game-jam-ready template with asset loading and pixel-art scaling.
*   **Showcase Projects**:
    *   **Mindustry**: Complex automation game (Open Source).
    *   **Shattered Pixel Dungeon**: Roguelike (Open Source).
    *   **Unciv**: Civilization clone (Open Source).
*   **Integration Patterns**:
    *   **Box2D**: Physics integration where pixels are mapped to meters (usually 100px = 1m).
    *   **Tiled (TMX)**: Loading maps created in the Tiled Map Editor.

## 4. Developer Friction Points

*   **Reflection & Obfuscation (ProGuard/R8)**:
    *   **Challenge**: Scene2D `Skin` files and `Json` serialization use reflection. In Android release builds, ProGuard/R8 obfuscates class names, causing "Field not found" or "Class not found" crashes.
    *   [GitHub Issue #7331](https://github.com/libgdx/libgdx/issues/7331)
*   **GWT/HTML5 Backend Limitations**:
    *   **Challenge**: No standard file system access; assets must be pre-listed in a `GdxDefinition.gwt.xml` or handled by the setup tool. Reflection is extremely limited and requires "GWT reflection" configuration.
*   **Android Life-cycle (Context Loss)**:
    *   **Challenge**: On older devices or specific triggers, the OpenGL context is lost when the app is paused, requiring manual management of some native resources (though libGDX handles most automatically).
*   **Asset Pathing**:
    *   **Challenge**: Desktop runs usually expect the working directory to be the `assets` folder (often located in the `android` module), leading to "File not found" errors if not configured in the IDE/Gradle.

## 5. Evaluation Ideas

*   **Basic**: Create a "Drop" clone where the user catches falling objects with a bucket.
*   **UI/UX**: Implement a multi-page main menu with a "Settings" screen using `Scene2D` and a `Skin`.
*   **Asset Management**: Refactor a game to use `AssetManager` for asynchronous loading with a progress bar.
*   **Resolution Handling**: Implement a responsive UI that works on both 16:9 and 4:3 screens using `ExtendViewport`.
*   **Physics**: Set up a basic `Box2D` world with a player character that can jump and collide with platforms.
*   **Advanced**: Debug and fix a ProGuard configuration that is causing a `Skin` loading crash in an Android release build.
*   **Cross-platform**: Modify a desktop-only input system to support touch gestures (swipe/tap) for mobile backends.

## 6. Sources

1.  [libGDX Official Website](https://libgdx.com/) - Main documentation and showcase.
2.  [libGDX Wiki](https://libgdx.com/wiki/) - Detailed technical guides.
3.  [libGDX GitHub Repository](https://github.com/libgdx/libgdx) - Source code and issue tracker.
4.  [gdx-liftoff GitHub](https://github.com/libgdx/gdx-liftoff) - Modern setup tool documentation.
5.  [Awesome libGDX](https://github.com/rafaskb/awesome-libgdx) - Curated list of community resources.
6.  [Happy Coding libGDX Tutorials](https://happycoding.io/tutorials/libgdx/) - Beginner-friendly setup and examples.