package ru.spbstu.telematics.java;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.nio.file.Files;

/**
 * Copyable folder object.
 * Defining operations of folder copy process.
 */
public class CopyableFolder extends CopyablePath {

    CopyableFolder(String path) { super(path); }
    CopyableFolder(Path path) { super(path); }

    /**
     * Copy files and subdirectories into destination folder.
     * Copy files firstly and then copy subdirectories.
     * @param dest: target path
     */
    private void copyFilesAndSubdirectories(CopyableFolder dest) {
        // List all files and subdirectories
        List<Path> files = new ArrayList<>();
        List<Path> subdirectories = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(this.path)) {
            for (Path item: stream) {
                if (Files.isRegularFile(item)) files.add(item);
                if (Files.isDirectory(item)) subdirectories.add(item);
            }
        } catch (IOException | DirectoryIteratorException error) {
            throw new CopyBaseException(String.format("permission denied: %s", this.path.getFileName()));
        }

        // Copy files
        for (Path item: files) {
            CopyableFile file = new CopyableFile(item);
            file.copy(dest);
        }

        // Copy subdirectories
        for (Path item: subdirectories) {
            CopyableFolder subdirectory = new CopyableFolder(item);
            CopyableFolder new_dest_subdirectory = new CopyableFolder(dest.path.resolve(item.getFileName()));
            subdirectory.copy(new_dest_subdirectory);
        }
    }

    /**
     * Copy folder to another directory.
     * <ul>
     *     <li>If not permitToCopyRecursively: throw an exception</li>
     *     <li>If destination path exists and belongs to a file: throw an exception</li>
     *     <li>If destination path not exist: try to make new folder with corresponded path and copy files into it</li>
     *     <li>If destination path exists and belongs to a directory: copy files into it</li>
     * </ul>
     * @param dest: target path
     */
    @Override
    public void copy(CopyablePath dest) throws CopyBaseException {
        if (!allowCopyRecursively)
            throw new CopyBaseException(String.format("%s is a directory (not copied).", this.path.getFileName()));
        if (Files.exists(dest.path) && Files.isRegularFile(dest.path))
            throw new CopyBaseException(String.format("%s is a directory (not copied).", this.path.getFileName()));
        if (Files.notExists(dest.path)) {
            // Try to create a new directory with given path.
            // If this operation failed, means its parent folder not exist, so throw an exception
            try {
                Files.createDirectory(dest.path);
            } catch (IOException error) {
                throw new CopyBaseException(String.format("permission denied: %s", this.path.getFileName()));
            }
            this.copy(dest);
            return;
        }
        if (Files.exists(dest.path) && Files.isDirectory(dest.path))
            this.copyFilesAndSubdirectories(new CopyableFolder(dest.path));
    }


}
