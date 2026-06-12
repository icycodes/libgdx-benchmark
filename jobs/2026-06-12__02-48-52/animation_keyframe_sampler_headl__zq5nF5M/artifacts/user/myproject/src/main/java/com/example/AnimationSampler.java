package com.example;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Animation.PlayMode;
import com.badlogic.gdx.utils.Array;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

public class AnimationSampler extends ApplicationAdapter {
    private String configPath;
    private String outputPath;

    public AnimationSampler(String configPath, String outputPath) {
        this.configPath = configPath;
        this.outputPath = outputPath;
    }

    @Override
    public void create() {
        try {
            FileHandle configFile = Gdx.files.absolute(configPath);
            String configContent = configFile.readString("UTF-8");
            
            float frameDuration = 0;
            PlayMode playMode = PlayMode.NORMAL;
            Array<Integer> keyFrames = new Array<Integer>();
            List<String> sampleTimesStr = new ArrayList<String>();
            List<Float> sampleTimes = new ArrayList<Float>();
            
            BufferedReader reader = new BufferedReader(new StringReader(configContent));
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                
                String[] parts = line.split("\\s+");
                if (parts[0].equals("frameDuration")) {
                    frameDuration = Float.parseFloat(parts[1]);
                } else if (parts[0].equals("playMode")) {
                    playMode = PlayMode.valueOf(parts[1]);
                } else if (parts[0].equals("keyFrames")) {
                    for (int i = 1; i < parts.length; i++) {
                        keyFrames.add(Integer.parseInt(parts[i]));
                    }
                } else if (parts[0].equals("sample")) {
                    sampleTimesStr.add(parts[1]);
                    sampleTimes.add(Float.parseFloat(parts[1]));
                }
            }
            
            Animation<Integer> animation = new Animation<Integer>(frameDuration, keyFrames, playMode);
            
            StringBuilder out = new StringBuilder();
            for (int i = 0; i < sampleTimes.size(); i++) {
                float time = sampleTimes.get(i);
                String timeStr = sampleTimesStr.get(i);
                Integer frame = animation.getKeyFrame(time);
                boolean finished = animation.isAnimationFinished(time);
                
                out.append(timeStr).append(" ").append(frame).append(" ").append(finished).append("\n");
            }
            
            FileHandle outputFile = Gdx.files.absolute(outputPath);
            outputFile.writeString(out.toString(), false, "UTF-8");
            
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            Gdx.app.exit();
        }
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: AnimationSampler <config_path> <output_path>");
            System.exit(1);
        }
        
        HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
        config.updatesPerSecond = 0;
        
        new HeadlessApplication(new AnimationSampler(args[0], args[1]), config);
    }
}
