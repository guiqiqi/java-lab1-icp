package ru.spbstu.telematics.java;

import java.io.IOException;

import java.nio.file.CopyOption;
import java.nio.file.StandardCopyOption;
import java.nio.file.Path;
import java.nio.file.Files;

public class CopyableFile extends CopyablePath {

    CopyableFile(String path) { super(path); }
    CopyableFile(Path path) { super(path); }

    /**
     * Proxy copy function for Files.Copy and proxy all IOException with CopyBaseException.
     * If two file's paths are same, skip copying and throw CopyBaseException.
     * @param src: source path
     * @param dest: destination path
     * @param option: copy option except StandardCopyOption.COPY_ATTRIBUTES
     * @throws CopyBaseException: proxied all IOException errors
     * @see java.nio.file.Files
     * @see java.nio.file.StandardCopyOption
     */
    private void copyProxy(Path src, Path dest, CopyOption option) throws CopyBaseException {
        // Check whether
        if (src.equals(dest))
            throw new CopyBaseException(String.format("%s and %s are identical (not copied).", src, dest));
        try {
            if (verboseMode)
                System.out.printf("%s -> %s\n", src, dest);
            Files.copy(src, dest, option, StandardCopyOption.COPY_ATTRIBUTES);
        } catch (IOException error) {
            throw new CopyBaseException(String.format("permission denied: %s", dest.getFileName()));
        }
    }

    /**
     * Default version copyProxy with option StandardCopyOption.COPY_ATTRIBUTES.
     * @param src: source path
     * @param dest: destination path
     * @throws CopyBaseException: proxied all IOException errors
     */
    private void copyProxy(Path src, Path dest) throws CopyBaseException {
        this.copyProxy(src, dest, StandardCopyOption.COPY_ATTRIBUTES);
    }

    /**
     * Copy file to another file(rewriting) or directory.
     * <ul>
     *     <li>If destination path not exists: write into it</li>
     *     <li>If destination is a file: ask if user wants to rewrite it</li>
     *     <li>If destination is a directory: enter it and check the file path under it</li>
     * </ul>
     * @param dest : target path
     */
    @Override
    public void copy(CopyablePath dest) throws CopyBaseException {
        if (Files.notExists(dest.path)) {
            this.copyProxy(this.path, dest.path);
            return;
        }
        if (Files.exists(dest.path) && Files.isRegularFile(dest.path)) {
            if (this.promptOverwrite(dest))
                this.copyProxy(this.path, dest.path, StandardCopyOption.REPLACE_EXISTING);
            return;
        }
        if (Files.exists(dest.path) && Files.isDirectory(dest.path)) {
            Path filename = this.path.getFileName();
            Path file_path_under_directory = dest.path.resolve(filename);
            this.copy(new CopyableFile(file_path_under_directory));
        }
    }
}
