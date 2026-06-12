package com.example.game;

import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.files.FileHandle;

/**
 * Resolves asset paths against the JVM's current working directory.
 */
public final class WorkingDirectoryFileHandleResolver implements FileHandleResolver {
    @Override
    public FileHandle resolve(String fileName) {
        return new FileHandle(fileName);
    }
}
