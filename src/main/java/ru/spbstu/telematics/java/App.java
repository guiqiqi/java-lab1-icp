package ru.spbstu.telematics.java;

import java.nio.file.Files;

public class App {
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
        if (Files.exists(dest.path))
            dest = new CopyableFolder(dest.path.resolve(src.path.getFileName()));

        // Check if source and destination are same
        if (src.path.equals(dest.path))
            throw new CopyBaseException(String.format("%s and %s are identical (not copied).", src.path.getFileName(), dest.path.getFileName()));

        // Start copying
        src.copy(dest);
    }
}
