package com.example.core;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.viewport.*;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ProjectionApp extends ApplicationAdapter {
    private String scenarioPath;
    private String outputPath;
    
    private Viewport viewport;
    private OrthographicCamera camera;
    
    private JsonValue scenario;
    private int currentFrameIndex = 0;
    private JsonValue frames;
    
    private List<RoundTrip> roundTrips = new ArrayList<>();
    private int totalFrames = 0;
    private int totalPoints = 0;
    
    private PrintWriter out;

    public ProjectionApp(String scenarioPath, String outputPath) {
        this.scenarioPath = scenarioPath;
        this.outputPath = outputPath;
    }
    
    private static class RoundTrip {
        int frame;
        float wx, wy;
    }
    
    private int currentGraphicsWidth = 0;
    private int currentGraphicsHeight = 0;

    @Override
    public void create() {
        final com.badlogic.gdx.Graphics originalGraphics = Gdx.graphics;
        Gdx.graphics = new com.badlogic.gdx.Graphics() {
            @Override public boolean isGL30Available() { return originalGraphics.isGL30Available(); }
            @Override public com.badlogic.gdx.graphics.GL20 getGL20() { return originalGraphics.getGL20(); }
            @Override public void setGL20(com.badlogic.gdx.graphics.GL20 gl20) { originalGraphics.setGL20(gl20); }
            @Override public com.badlogic.gdx.graphics.GL30 getGL30() { return originalGraphics.getGL30(); }
            @Override public void setGL30(com.badlogic.gdx.graphics.GL30 gl30) { originalGraphics.setGL30(gl30); }
            @Override public boolean isGL31Available() { return originalGraphics.isGL31Available(); }
            @Override public com.badlogic.gdx.graphics.GL31 getGL31() { return originalGraphics.getGL31(); }
            @Override public void setGL31(com.badlogic.gdx.graphics.GL31 gl31) { originalGraphics.setGL31(gl31); }
            @Override public boolean isGL32Available() { return originalGraphics.isGL32Available(); }
            @Override public com.badlogic.gdx.graphics.GL32 getGL32() { return originalGraphics.getGL32(); }
            @Override public void setGL32(com.badlogic.gdx.graphics.GL32 gl32) { originalGraphics.setGL32(gl32); }
            @Override public int getWidth() { return currentGraphicsWidth; }
            @Override public int getHeight() { return currentGraphicsHeight; }
            @Override public int getBackBufferWidth() { return currentGraphicsWidth; }
            @Override public int getBackBufferHeight() { return currentGraphicsHeight; }
            @Override public long getFrameId() { return originalGraphics.getFrameId(); }
            @Override public float getDeltaTime() { return originalGraphics.getDeltaTime(); }
            @Override public float getRawDeltaTime() { return originalGraphics.getRawDeltaTime(); }
            @Override public int getFramesPerSecond() { return originalGraphics.getFramesPerSecond(); }
            @Override public GraphicsType getType() { return originalGraphics.getType(); }
            @Override public com.badlogic.gdx.graphics.glutils.GLVersion getGLVersion() { return originalGraphics.getGLVersion(); }
            @Override public float getPpiX() { return originalGraphics.getPpiX(); }
            @Override public float getPpiY() { return originalGraphics.getPpiY(); }
            @Override public float getPpcX() { return originalGraphics.getPpcX(); }
            @Override public float getPpcY() { return originalGraphics.getPpcY(); }
            @Override public float getDensity() { return originalGraphics.getDensity(); }
            @Override public boolean supportsDisplayModeChange() { return originalGraphics.supportsDisplayModeChange(); }
            @Override public Monitor getPrimaryMonitor() { return originalGraphics.getPrimaryMonitor(); }
            @Override public Monitor getMonitor() { return originalGraphics.getMonitor(); }
            @Override public Monitor[] getMonitors() { return originalGraphics.getMonitors(); }
            @Override public DisplayMode[] getDisplayModes() { return originalGraphics.getDisplayModes(); }
            @Override public DisplayMode[] getDisplayModes(Monitor monitor) { return originalGraphics.getDisplayModes(monitor); }
            @Override public DisplayMode getDisplayMode() { return originalGraphics.getDisplayMode(); }
            @Override public DisplayMode getDisplayMode(Monitor monitor) { return originalGraphics.getDisplayMode(monitor); }
            @Override public boolean setFullscreenMode(DisplayMode displayMode) { return originalGraphics.setFullscreenMode(displayMode); }
            @Override public boolean setWindowedMode(int width, int height) { return originalGraphics.setWindowedMode(width, height); }
            @Override public void setTitle(String title) { originalGraphics.setTitle(title); }
            @Override public void setUndecorated(boolean undecorated) { originalGraphics.setUndecorated(undecorated); }
            @Override public void setResizable(boolean resizable) { originalGraphics.setResizable(resizable); }
            @Override public void setVSync(boolean vsync) { originalGraphics.setVSync(vsync); }
            @Override public void setForegroundFPS(int fps) { originalGraphics.setForegroundFPS(fps); }
            @Override public BufferFormat getBufferFormat() { return originalGraphics.getBufferFormat(); }
            @Override public boolean supportsExtension(String extension) { return originalGraphics.supportsExtension(extension); }
            @Override public void setContinuousRendering(boolean isContinuous) { originalGraphics.setContinuousRendering(isContinuous); }
            @Override public boolean isContinuousRendering() { return originalGraphics.isContinuousRendering(); }
            @Override public void requestRendering() { originalGraphics.requestRendering(); }
            @Override public boolean isFullscreen() { return originalGraphics.isFullscreen(); }
            @Override public com.badlogic.gdx.graphics.Cursor newCursor(com.badlogic.gdx.graphics.Pixmap pixmap, int xHotspot, int yHotspot) { return originalGraphics.newCursor(pixmap, xHotspot, yHotspot); }
            @Override public void setCursor(com.badlogic.gdx.graphics.Cursor cursor) { originalGraphics.setCursor(cursor); }
            @Override public void setSystemCursor(com.badlogic.gdx.graphics.Cursor.SystemCursor systemCursor) { originalGraphics.setSystemCursor(systemCursor); }
            @Override public int getSafeInsetLeft() { return originalGraphics.getSafeInsetLeft(); }
            @Override public int getSafeInsetTop() { return originalGraphics.getSafeInsetTop(); }
            @Override public int getSafeInsetBottom() { return originalGraphics.getSafeInsetBottom(); }
            @Override public int getSafeInsetRight() { return originalGraphics.getSafeInsetRight(); }
            @Override public float getBackBufferScale() { return originalGraphics.getBackBufferScale(); }
        };
        
        Gdx.gl = new com.badlogic.gdx.graphics.GL20() {
            public void glActiveTexture(int texture) {}
            public void glBindTexture(int target, int texture) {}
            public void glBlendFunc(int sfactor, int dfactor) {}
            public void glClear(int mask) {}
            public void glClearColor(float red, float green, float blue, float alpha) {}
            public void glClearDepthf(float depth) {}
            public void glClearStencil(int s) {}
            public void glColorMask(boolean red, boolean green, boolean blue, boolean alpha) {}
            public void glCompressedTexImage2D(int target, int level, int internalformat, int width, int height, int border, int imageSize, java.nio.Buffer data) {}
            public void glCompressedTexSubImage2D(int target, int level, int xoffset, int yoffset, int width, int height, int format, int imageSize, java.nio.Buffer data) {}
            public void glCopyTexImage2D(int target, int level, int internalformat, int x, int y, int width, int height, int border) {}
            public void glCopyTexSubImage2D(int target, int level, int xoffset, int yoffset, int x, int y, int width, int height) {}
            public void glCullFace(int mode) {}
            public void glDeleteTextures(int n, java.nio.IntBuffer textures) {}
            public void glDeleteTexture(int texture) {}
            public void glDepthFunc(int func) {}
            public void glDepthMask(boolean flag) {}
            public void glDepthRangef(float zNear, float zFar) {}
            public void glDisable(int cap) {}
            public void glDrawArrays(int mode, int first, int count) {}
            public void glDrawElements(int mode, int count, int type, java.nio.Buffer indices) {}
            public void glDrawElements(int mode, int count, int type, int indices) {}
            public void glEnable(int cap) {}
            public void glFinish() {}
            public void glFlush() {}
            public void glFrontFace(int mode) {}
            public void glGenTextures(int n, java.nio.IntBuffer textures) {}
            public int glGenTexture() { return 0; }
            public int glGetError() { return 0; }
            public void glGetIntegerv(int pname, java.nio.IntBuffer params) {}
            public String glGetString(int name) { return ""; }
            public void glHint(int target, int mode) {}
            public void glLineWidth(float width) {}
            public void glPixelStorei(int pname, int param) {}
            public void glPolygonOffset(float factor, float units) {}
            public void glReadPixels(int x, int y, int width, int height, int format, int type, java.nio.Buffer pixels) {}
            public void glScissor(int x, int y, int width, int height) {}
            public void glStencilFunc(int func, int ref, int mask) {}
            public void glStencilMask(int mask) {}
            public void glStencilOp(int fail, int zfail, int zpass) {}
            public void glTexImage2D(int target, int level, int internalformat, int width, int height, int border, int format, int type, java.nio.Buffer pixels) {}
            public void glTexParameterf(int target, int pname, float param) {}
            public void glTexParameterfv(int target, int pname, java.nio.FloatBuffer params) {}
            public void glTexParameteri(int target, int pname, int param) {}
            public void glTexParameteriv(int target, int pname, java.nio.IntBuffer params) {}
            public void glTexSubImage2D(int target, int level, int xoffset, int yoffset, int width, int height, int format, int type, java.nio.Buffer pixels) {}
            public void glViewport(int x, int y, int width, int height) {}
            public void glAttachShader(int program, int shader) {}
            public void glBindAttribLocation(int program, int index, String name) {}
            public void glBindBuffer(int target, int buffer) {}
            public void glBindFramebuffer(int target, int framebuffer) {}
            public void glBindRenderbuffer(int target, int renderbuffer) {}
            public void glBlendColor(float red, float green, float blue, float alpha) {}
            public void glBlendEquation(int mode) {}
            public void glBlendEquationSeparate(int modeRGB, int modeAlpha) {}
            public void glBlendFuncSeparate(int srcRGB, int dstRGB, int srcAlpha, int dstAlpha) {}
            public void glBufferData(int target, int size, java.nio.Buffer data, int usage) {}
            public void glBufferSubData(int target, int offset, int size, java.nio.Buffer data) {}
            public int glCheckFramebufferStatus(int target) { return 0; }
            public void glCompileShader(int shader) {}
            public int glCreateProgram() { return 0; }
            public int glCreateShader(int type) { return 0; }
            public void glDeleteBuffer(int buffer) {}
            public void glDeleteBuffers(int n, java.nio.IntBuffer buffers) {}
            public void glDeleteFramebuffer(int framebuffer) {}
            public void glDeleteFramebuffers(int n, java.nio.IntBuffer framebuffers) {}
            public void glDeleteProgram(int program) {}
            public void glDeleteRenderbuffer(int renderbuffer) {}
            public void glDeleteRenderbuffers(int n, java.nio.IntBuffer renderbuffers) {}
            public void glDeleteShader(int shader) {}
            public void glDetachShader(int program, int shader) {}
            public void glDisableVertexAttribArray(int index) {}
            public void glDrawElements(int mode, int count, int type, long indices) {}
            public void glEnableVertexAttribArray(int index) {}
            public void glFramebufferRenderbuffer(int target, int attachment, int renderbuffertarget, int renderbuffer) {}
            public void glFramebufferTexture2D(int target, int attachment, int textarget, int texture, int level) {}
            public int glGenBuffer() { return 0; }
            public void glGenBuffers(int n, java.nio.IntBuffer buffers) {}
            public void glGenerateMipmap(int target) {}
            public int glGenFramebuffer() { return 0; }
            public void glGenFramebuffers(int n, java.nio.IntBuffer framebuffers) {}
            public int glGenRenderbuffer() { return 0; }
            public void glGenRenderbuffers(int n, java.nio.IntBuffer renderbuffers) {}
            public String glGetActiveAttrib(int program, int index, java.nio.IntBuffer size, java.nio.IntBuffer type) { return ""; }
            public String glGetActiveUniform(int program, int index, java.nio.IntBuffer size, java.nio.IntBuffer type) { return ""; }
            public void glGetAttachedShaders(int program, int maxcount, java.nio.Buffer count, java.nio.IntBuffer shaders) {}
            public int glGetAttribLocation(int program, String name) { return 0; }
            public void glGetBooleanv(int pname, java.nio.Buffer params) {}
            public void glGetBufferParameteriv(int target, int pname, java.nio.IntBuffer params) {}
            public void glGetFloatv(int pname, java.nio.FloatBuffer params) {}
            public void glGetFramebufferAttachmentParameteriv(int target, int attachment, int pname, java.nio.IntBuffer params) {}
            public void glGetProgramiv(int program, int pname, java.nio.IntBuffer params) {}
            public String glGetProgramInfoLog(int program) { return ""; }
            public void glGetRenderbufferParameteriv(int target, int pname, java.nio.IntBuffer params) {}
            public void glGetShaderiv(int shader, int pname, java.nio.IntBuffer params) {}
            public String glGetShaderInfoLog(int shader) { return ""; }
            public void glGetShaderPrecisionFormat(int shadertype, int precisiontype, java.nio.IntBuffer range, java.nio.IntBuffer precision) {}
            public void glGetTexParameterfv(int target, int pname, java.nio.FloatBuffer params) {}
            public void glGetTexParameteriv(int target, int pname, java.nio.IntBuffer params) {}
            public void glGetUniformfv(int program, int location, java.nio.FloatBuffer params) {}
            public void glGetUniformiv(int program, int location, java.nio.IntBuffer params) {}
            public int glGetUniformLocation(int program, String name) { return 0; }
            public void glGetVertexAttribfv(int index, int pname, java.nio.FloatBuffer params) {}
            public void glGetVertexAttribiv(int index, int pname, java.nio.IntBuffer params) {}
            public void glGetVertexAttribPointerv(int index, int pname, java.nio.Buffer pointer) {}
            public boolean glIsBuffer(int buffer) { return false; }
            public boolean glIsEnabled(int cap) { return false; }
            public boolean glIsFramebuffer(int framebuffer) { return false; }
            public boolean glIsProgram(int program) { return false; }
            public boolean glIsRenderbuffer(int renderbuffer) { return false; }
            public boolean glIsShader(int shader) { return false; }
            public boolean glIsTexture(int texture) { return false; }
            public void glLinkProgram(int program) {}
            public void glReleaseShaderCompiler() {}
            public void glRenderbufferStorage(int target, int internalformat, int width, int height) {}
            public void glSampleCoverage(float value, boolean invert) {}
            public void glShaderBinary(int n, java.nio.IntBuffer shaders, int binaryformat, java.nio.Buffer binary, int length) {}
            public void glShaderSource(int shader, String string) {}
            public void glStencilFuncSeparate(int face, int func, int ref, int mask) {}
            public void glStencilMaskSeparate(int face, int mask) {}
            public void glStencilOpSeparate(int face, int sfail, int dpfail, int dppass) {}
            public void glTexParameterfv(int target, int pname, float[] params, int offset) {}
            public void glTexParameteri(int target, int pname, int[] params, int offset) {}
            public void glUniform1f(int location, float x) {}
            public void glUniform1fv(int location, int count, java.nio.FloatBuffer v) {}
            public void glUniform1fv(int location, int count, float[] v, int offset) {}
            public void glUniform1i(int location, int x) {}
            public void glUniform1iv(int location, int count, java.nio.IntBuffer v) {}
            public void glUniform1iv(int location, int count, int[] v, int offset) {}
            public void glUniform2f(int location, float x, float y) {}
            public void glUniform2fv(int location, int count, java.nio.FloatBuffer v) {}
            public void glUniform2fv(int location, int count, float[] v, int offset) {}
            public void glUniform2i(int location, int x, int y) {}
            public void glUniform2iv(int location, int count, java.nio.IntBuffer v) {}
            public void glUniform2iv(int location, int count, int[] v, int offset) {}
            public void glUniform3f(int location, float x, float y, float z) {}
            public void glUniform3fv(int location, int count, java.nio.FloatBuffer v) {}
            public void glUniform3fv(int location, int count, float[] v, int offset) {}
            public void glUniform3i(int location, int x, int y, int z) {}
            public void glUniform3iv(int location, int count, java.nio.IntBuffer v) {}
            public void glUniform3iv(int location, int count, int[] v, int offset) {}
            public void glUniform4f(int location, float x, float y, float z, float w) {}
            public void glUniform4fv(int location, int count, java.nio.FloatBuffer v) {}
            public void glUniform4fv(int location, int count, float[] v, int offset) {}
            public void glUniform4i(int location, int x, int y, int z, int w) {}
            public void glUniform4iv(int location, int count, java.nio.IntBuffer v) {}
            public void glUniform4iv(int location, int count, int[] v, int offset) {}
            public void glUniformMatrix2fv(int location, int count, boolean transpose, java.nio.FloatBuffer value) {}
            public void glUniformMatrix2fv(int location, int count, boolean transpose, float[] value, int offset) {}
            public void glUniformMatrix3fv(int location, int count, boolean transpose, java.nio.FloatBuffer value) {}
            public void glUniformMatrix3fv(int location, int count, boolean transpose, float[] value, int offset) {}
            public void glUniformMatrix4fv(int location, int count, boolean transpose, java.nio.FloatBuffer value) {}
            public void glUniformMatrix4fv(int location, int count, boolean transpose, float[] value, int offset) {}
            public void glUseProgram(int program) {}
            public void glValidateProgram(int program) {}
            public void glVertexAttrib1f(int indx, float x) {}
            public void glVertexAttrib1fv(int indx, java.nio.FloatBuffer values) {}
            public void glVertexAttrib2f(int indx, float x, float y) {}
            public void glVertexAttrib2fv(int indx, java.nio.FloatBuffer values) {}
            public void glVertexAttrib3f(int indx, float x, float y, float z) {}
            public void glVertexAttrib3fv(int indx, java.nio.FloatBuffer values) {}
            public void glVertexAttrib4f(int indx, float x, float y, float z, float w) {}
            public void glVertexAttrib4fv(int indx, java.nio.FloatBuffer values) {}
            public void glVertexAttribPointer(int indx, int size, int type, boolean normalized, int stride, java.nio.Buffer ptr) {}
            public void glVertexAttribPointer(int indx, int size, int type, boolean normalized, int stride, int ptr) {}
        };
        try {
            out = new PrintWriter(new FileWriter(outputPath, false));
        } catch (IOException e) {
            e.printStackTrace();
            Gdx.app.exit();
            return;
        }
        
        JsonReader reader = new JsonReader();
        scenario = reader.parse(Gdx.files.absolute(scenarioPath));
        
        String vpType = scenario.getString("viewport");
        float worldWidth = scenario.getFloat("worldWidth", 0);
        float worldHeight = scenario.getFloat("worldHeight", 0);
        
        camera = new OrthographicCamera();
        
        if ("fit".equals(vpType)) {
            viewport = new FitViewport(worldWidth, worldHeight, camera);
        } else if ("extend".equals(vpType)) {
            viewport = new ExtendViewport(worldWidth, worldHeight, camera);
        } else if ("stretch".equals(vpType)) {
            viewport = new StretchViewport(worldWidth, worldHeight, camera);
        } else if ("screen".equals(vpType)) {
            viewport = new ScreenViewport(camera);
        } else {
            viewport = new FitViewport(worldWidth, worldHeight, camera);
        }
        
        JsonValue camPos = scenario.get("cameraPosition");
        if (camPos != null) {
            camera.position.set(camPos.getFloat("x"), camPos.getFloat("y"), 0);
        }
        
        frames = scenario.get("frames");
        
        // Initial viewport update if there is a first frame with resize?
        // Let's just let it happen in render() or we could do it here if needed.
        // Wait, the instructions say:
        // "Call viewport.update(width, height, true) (note the centerCamera = true flag) the first time before any projection (e.g., inside create() with a sensible default like the scenario's first resize), and again whenever a frame supplies a new resize."
        if (frames != null && frames.size > 0) {
            JsonValue firstFrame = frames.get(0);
            JsonValue resize = firstFrame.get("resize");
            if (resize != null) {
                currentGraphicsWidth = resize.getInt("width");
                currentGraphicsHeight = resize.getInt("height");
                viewport.update(currentGraphicsWidth, currentGraphicsHeight, true);
            } else {
                currentGraphicsWidth = 800;
                currentGraphicsHeight = 600;
                viewport.update(800, 600, true);
            }
        } else {
            currentGraphicsWidth = 800;
            currentGraphicsHeight = 600;
            viewport.update(800, 600, true);
        }
    }
    
    @Override
    public void render() {
        if (frames == null || currentFrameIndex >= frames.size) {
            finish();
            return;
        }
        
        JsonValue frameData = frames.get(currentFrameIndex);
        int frameNum = frameData.getInt("frame");
        
        JsonValue resize = frameData.get("resize");
        if (resize != null) {
            int width = resize.getInt("width");
            int height = resize.getInt("height");
            currentGraphicsWidth = width;
            currentGraphicsHeight = height;
            viewport.update(width, height, true);
        }
        
        JsonValue points = frameData.get("points");
        if (points != null) {
            for (JsonValue pt : points) {
                float wx = pt.getFloat("x");
                float wy = pt.getFloat("y");
                
                Vector2 screenPt = viewport.project(new Vector2(wx, wy));
                float sx = screenPt.x;
                float sy = screenPt.y;
                
                out.printf(Locale.US, "FRAME %d PROJECT (%.3f,%.3f) -> (%.3f,%.3f)\n", frameNum, wx, wy, sx, sy);
                
                RoundTrip rt = new RoundTrip();
                rt.frame = frameNum;
                rt.wx = wx;
                rt.wy = wy;
                roundTrips.add(rt);
                
                totalPoints++;
            }
        }
        
        totalFrames++;
        currentFrameIndex++;
        
        if (currentFrameIndex >= frames.size) {
            finish();
        }
    }
    
    private boolean finished = false;
    
    private void finish() {
        if (finished) return;
        finished = true;
        
        for (RoundTrip rt : roundTrips) {
            Vector2 point = new Vector2(rt.wx, rt.wy);
            viewport.unproject(viewport.project(point));
            
            float rx = point.x;
            float ry = point.y;
            
            boolean ok = Math.abs(rx - rt.wx) <= 0.01f && Math.abs(ry - rt.wy) <= 0.01f;
            String status = ok ? "OK" : "MISMATCH";
            
            out.printf(Locale.US, "ROUNDTRIP %d (%.3f,%.3f) -> (%.3f,%.3f) %s\n", rt.frame, rt.wx, rt.wy, rx, ry, status);
        }
        
        out.printf(Locale.US, "END frames=%d points=%d\n", totalFrames, totalPoints);
        out.flush();
        out.close();
        
        Gdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
                Gdx.app.exit();
            }
        });
    }
}
