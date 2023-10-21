package ru.spbstu.telematics.java;

import java.nio.file.Files;
import java.util.Scanner;

import java.nio.file.Path;
import java.nio.file.Paths;

public abstract class CopyablePath {
    public final Path path;
    static public boolean permitToOverwrite = false;  // -i
    static public boolean allowCopyRecursively = false;  // -r
    static public boolean verboseMode = false;  // -v
    static public boolean doNotOverwrite = false;  // -n

    CopyablePath(String path) { this.path = Paths.get(path).normalize().toAbsolutePath();}
    CopyablePath(Path path) { this.path = path.normalize().toAbsolutePath();}

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
