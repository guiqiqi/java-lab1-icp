package ru.spbstu.telematics.java;

import java.nio.file.Files;
import java.util.Scanner;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * CopyablePath defines all behaviours copyable item should have and gives some help functions.
 * All subclasses extend this abstract class should implement self copy function.
 * Static variables are using as flags.
 */
public abstract class CopyablePath {

    /**
     * Path object storing file/folder path with type java.nio.file.Path.
     */
    public final Path path;

    /**
     * permitToOverwrite using as <strong>-i</strong> option in cp command.
     * Once this flag was set to true, user's opinion will be asked in conflict filename case.
     */
    static public boolean permitToOverwrite = false;

    /**
     * allowCopyRecursively using as <strong>-r</strong> option in cp command.
     * If this flag was set to false, coping of folder will be forbidden.
     */
    static public boolean allowCopyRecursively = false;

    /**
     * verboseMode flag using as <strong>-v</strong> option in cp command.
     * If this flag was set to true, all file coping operations will be shown in console.
     */
    static public boolean verboseMode = false;

    /**
     * doNotOverwrite flag using as <strong>-n</strong> option in cp command.
     * If this flag was set to true, files with conflicted filename will be skipped in copying.
     */
    static public boolean doNotOverwrite = false;  // -n

    CopyablePath(String path) { this.path = Paths.get(path).normalize().toAbsolutePath();}
    CopyablePath(Path path) { this.path = path.normalize().toAbsolutePath();}

    /**
     * Abstract copy method, copies current object to destination
     * @param dest: destination of copying
     * @throws CopyBaseException when any kinds of error occurred in copying process
     */
    public abstract void copy(CopyablePath dest) throws CopyBaseException;

    /**
     * Ask user whether they want to overwrite file in given position.
     * Only the basename will be shown in prompt.
     * If permitToRewrite are set to true, then it will not ask user's choice.
     * If doNotOverwrite are set to true, then it will not ask user's choice and not copy file.
     * @param dest: target overwriting file CopyablePath object
     * @return whether overwriting is permitted
     */
    public boolean promptOverwrite(CopyablePath dest) {
        if (doNotOverwrite)
            return false;
        if (!permitToOverwrite)
            return true;
        Scanner input = new Scanner(System.in);
        System.out.printf("overwrite '%s'? ", dest.path.getFileName());
        String choice = input.next().toLowerCase();
        return choice.equals("y") || choice.equals("yes");
    }

    /**
     * Generate source CopyablePath object by checking its type.
     * @param path: source path
     * @return correspond CopyablePath object
     */
    public static CopyablePath pathFactory(String path) {
        Path item = Paths.get(path).toAbsolutePath();

        // Assert path must be existed
        if (!Files.exists(item)) {
            throw new CopyBaseException(String.format("%s: No such file or directory", item));
        }

        if (Files.isRegularFile(item))
            return new CopyableFile(item);
        if (Files.isDirectory(item))
            return new CopyableFolder(item);

        // If source item is not a file nor a directory
        throw new CopyBaseException(String.format("%s: No such file or directory", item));
    }
}
