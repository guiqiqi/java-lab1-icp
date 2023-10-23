package ru.spbstu.telematics.java;

import java.nio.file.Files;

public class App {

    /**
     * Require two command line arguments as source path and destination path.
     * Other flag options will be set to:
     * <ul>
     *     <li>-r: true</li>
     *     <li>-n: false</li>
     *     <li>-v: true</li>
     *     <li>-i: false</li>
     * </ul>
     * Equally to <pre>cp -rv src dest</pre>
     * @param args: source path and destination path, if length not equals to 2, nothing will be executed
     */
    public static void main( String[] args ) {
        if (args.length != 2)
            return;
        try {
            copy(
                    args[0],
                    args[1],
                    true,
                    false,
                    true,
                    false
            );            
        } catch (CopyBaseException error) {
            System.out.println(error.getMessage());
        }
    }

    /**
     * Copy source to destination.
     * @param src_path: source path string
     * @param dest_path: destination path string
     * @param allowCopyRecursively: -r flag option
     * @param doNotOverwrite: -n flag option
     * @param verboseMode: -v flag option
     * @param permitToOverwrite: -i flag option
     */
    public static void copy(
            String src_path,
            String dest_path,
            boolean allowCopyRecursively,
            boolean doNotOverwrite,
            boolean verboseMode,
            boolean permitToOverwrite
    ) {
        // Set parameters
        CopyablePath.allowCopyRecursively = allowCopyRecursively;
        CopyablePath.doNotOverwrite = doNotOverwrite;
        CopyablePath.verboseMode = verboseMode;
        CopyablePath.permitToOverwrite = permitToOverwrite;

        // Generating source and destination objects
        CopyablePath src = CopyablePath.pathFactory(src_path);
        CopyablePath dest;
        if (src instanceof CopyableFile)
            dest = new CopyableFile(dest_path);
        else
            dest = new CopyableFolder(dest_path);

        // Special case with cp command behaviour - if destination folder already exist, create a new one with basename of source
        if (Files.exists(dest.path) && Files.isDirectory(dest.path))
            dest = new CopyableFolder(dest.path.resolve(src.path.getFileName()));

        // Start copying
        src.copy(dest);
    }
}
