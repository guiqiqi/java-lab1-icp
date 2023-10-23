package ru.spbstu.telematics.java;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Unit test for simple App.
 */
public class AppTest 
    extends TestCase
{
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public AppTest( String testName )
    {
        super( testName );
    }

    final private String path_prefix = "src/test/resources/";

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( AppTest.class );
    }

    private boolean copyPartialVersion(String src_path, String dest_path) {
        src_path = path_prefix + src_path;
        dest_path = path_prefix + dest_path;
        try {
            App.copy(src_path, dest_path, true,false,true,false);
        } catch (CopyBaseException error) {
            return false;
        }
        return true;
    }

    private Path generatePath(String path) {
        return Paths.get(path_prefix + path).toAbsolutePath();
    }

    private void removeFileOrDirectory(Path path) {
        try {
             // Remove .DS_Store file
            if (Files.isDirectory(path))
                Files.deleteIfExists(path.resolve(".DS_Store"));
            Files.deleteIfExists(path);
        } catch (IOException error) {
            return;
        }
    }

    public void testCopyFileWithFilename() {
        assertTrue(copyPartialVersion("root.file", "parent/root.file"));
        Path root_file = generatePath("parent/root.file");
        assertTrue(Files.isRegularFile(root_file));

        // Clean resource
        removeFileOrDirectory(root_file);
    }

    public void testCopyFileWithoutFilename() {
        assertTrue(copyPartialVersion("root.file", "parent"));
        Path root_file = generatePath("parent/root.file");
        assertTrue(Files.isRegularFile(root_file));

        // Clean resource
        removeFileOrDirectory(root_file);
    }

    public void testCopyFolderWithName() {
        assertTrue(copyPartialVersion("parent/child", "parent/new_child"));
        Path new_child_folder = generatePath("parent/new_child");
        assertTrue(Files.isDirectory(new_child_folder));
        Path new_child_folder_file = generatePath("parent/new_child/child.file");
        assertTrue(Files.isRegularFile(new_child_folder_file));

        // Clean resource
        removeFileOrDirectory(new_child_folder_file);
        removeFileOrDirectory(new_child_folder);
    }

    public void testCopyFolderWithoutName() {
        assertTrue(copyPartialVersion("parent/child", "."));
        Path new_child_folder = generatePath("child");
        assertTrue(Files.isDirectory(new_child_folder));
        Path new_child_folder_file = generatePath("child/child.file");
        assertTrue(Files.isRegularFile(new_child_folder_file));

        // Clean resource
        removeFileOrDirectory(new_child_folder_file);
        removeFileOrDirectory(new_child_folder);
    }

    public void testCopyEmptyFolder() {
        assertTrue(copyPartialVersion("parent/empty", "."));
        Path new_empty_folder = generatePath("empty");
        assertTrue(Files.isDirectory(new_empty_folder));

        // Clean resource
        Path gitkeep_file = generatePath("empty/.gitkeep");
        removeFileOrDirectory(gitkeep_file);
        removeFileOrDirectory(new_empty_folder);
    }

    public void testSameFileDoNotCopyWithFilename() {
        assertFalse(copyPartialVersion("parent/parent.file", "parent/parent.file"));
    }

    public void testSameFileDoNotCopyWithoutFilename() {
        assertFalse(copyPartialVersion("parent/parent.file", "parent"));
    }
}
