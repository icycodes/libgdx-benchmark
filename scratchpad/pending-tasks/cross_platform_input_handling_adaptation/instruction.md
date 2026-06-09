Desktop environments often rely on constant keyboard polling, whereas mobile and HTML5 builds typically require touch and gesture recognition for a proper user experience.

You need to refactor a polling-based input system into an event-driven `GestureDetector.GestureListener`. Implement the `fling()` method to detect horizontal swipe gestures to move a player character, while simultaneously retaining desktop keyboard support via a standard `InputAdapter`.

**Constraints:**
- Must register both input processors using an `InputMultiplexer` to handle desktop keyboards and mobile touch events concurrently.
- Do NOT rely on manual `Gdx.input.isTouched()` coordinate math to determine swipes; the swipe must be event-driven via the `GestureDetector`.
- Ensure keyboard fallback explicitly supports the left and right arrow keys.