### 开发环境配置

我使用了 macOS 系统，并通过 `brew` 包管理器安装了 JDK、 Maven 以及 Git 工具：

```bash
brew install java
brew install maven
brew install git
```

通过使用命令行验证它们已经被成功安装：

```bash
java --version
mvn --version
git --version
```

使用下面的命令新建该项目：

```bash
mvn archetype:generate -DgroupId=ru.spbstu.telematics.java -DartifactId=icp -DarchetypeArtifactId=maven-archetype-quickstart -DinteractiveMode=false
```

进入 `icp` 目录，使用下面的命令新建一个 Git 项目：

```bash
git init
```

### 程序逻辑

该程序与 `cp` 命令有着相似的行为，并通过 Java 代码编码支持其一部分可选的命令行参数，程序的行为如下：

- 程序内部通过代码可以设置四个可选参数，他们分别为：
  - `allowCopyRecrusively`：该参数与 `-r` 参数的行为类似，当该参数为 `true` 时，程序允许递归的复制目录及其所有子文件/子文件夹；否则，当用户尝试复制目录时，将会提示一个错误
  - `doNotOverwrite`：该参数与 `-n` 参数的行为类似，当该参数为 `true` 时，程序在检测到重复的文件名时将跳过对应文件的复制。
  - `verboseMode`：该参数与 `-v` 参数的行为类似，当该参数为 `true` 时，所有文件的复制过程将会被显示。
  - `permitToOverwrite`：该参数与 `-i` 参数的行为类似，当该参数为 `true` 时，重名文件的复制将会询问用户：当用户输入为 `y/yes` （不区分大小写）时，文件将被覆盖。
- 主程序 `App.main` 从命令行接收两个参数，分别代表源文件/源文件夹与目标文件/目标文件夹。
- 当出现以下情况时，将会提示用户出现错误：
  - 当源文件/目录路径不存在时，将会提示错误。
  - 当源目录与目标目录相同时，程序将不会复制文件，而是提示错误。
  - 当从目录复制到文件时，将会提示错误。
  - 当读取/写入文件因为权限不足而失败时，将会提示错误。

### 代码实现

程序使用了 `java.nio` 模块来管理文件与路径，并且设计了三个类来抽象可拷贝的文件/路径：

```java
public abstract class CopyablePath {
    public final Path path;
    static public boolean permitToOverwrite = false;  // -i
    static public boolean allowCopyRecursively = false;  // -r
    static public boolean verboseMode = false;  // -v
    static public boolean doNotOverwrite = false;  // -n
  
    public abstract void copy(CopyablePath dest) throws CopyBaseException;
  
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
}

public class CopyableFile extends CopyablePath {
  public void copy(CopyablePath dest) throws CopyBaseException {...}  // Implemention
}

public class CopyableFolder extends CopyablePath {
  public void copy(CopyablePath dest) throws CopyBaseException {...}  // Implemention
}
```

最顶层的抽象类 `CopyablePath` 指代所有可以拷贝的对象（文件、文件夹），继承它的子类均应该实现一个 `copy` 方法，通过多态性它们得以调用不同类型 `copy` 方法的具体实现并将自身正确地拷贝至另一个文件/目录。它还管理了可拷贝对象的路径对象（具有 `java.nio.file.Path` 类型），并且通过静态变量设置了四个可选参数，用以控制拷贝过程中的行为。

`CopyablePath` 还提供了一个 `pathFactory` 方法，该方法用以根据实际路径生成一个 `CopyablePath` 对象（具体类型为 `CopyableFile/CopyableFolder`）。

`CopyablePath` 还提供了一个 `promptOverwrite` 方法，用以在拷贝文件出现冲突时询问用户的解决方案。

更具体地，子类 `CopyableFile/CopyableFolder` 的实现逻辑如下：

- 如果源路径是文件：(A)

  - 如果目标路径存在：(A.1)
    - 如果目标路径是文件：询问用户/根据配置项检测是否应该覆盖文件
    - 如果目标路径是文件夹：重新生成目标文件路径，递归至 (A)
  - 如果目标路径不存在：(A.2)
    - 如果目标路径的父路径存在：写入文件
    - 如果目标路径的父路径不存在：错误

- 如果源路径是文件夹：(B)

  - 如果目标路径存在：(B.1)

    - 如果目标路径是文件：错误
    - 如果目标路径是文件夹：遍历文件，对每个文件递归至 (A.1)，对每个文件夹递归至 (B.1)

  - 如果目标路径不存在：(B.2)
    - 如果目标路径的父路径存在：建立文件夹，递归至 (B.1)
    - 如果目标路径的父路径不存在：错误

其具体实现可以在代码及注释中查看。

在项目中定义了 `CopyBaseException` 异常，用以接管所有其他异常，并向用户进行合理的用户提示：

```java
public class CopyBaseException extends RuntimeException{
    CopyBaseException(String message) {
        super("cp: " + message);
    }
}
```

### 测试

我为该项目编写了一些测试，它们测试了程序应该实现的正常功能，以及一些边界条件：

- `testCopyFileWithFilename`：测试文件拷贝功能
- `testCopyFileWithoutFilename`：测试仅给定目标目录时的文件拷贝功能
- `testCopyFolderWithName`：测试目录拷贝功能
- `testCopyFolderWithoutName`：测试不提供目标目录名称时的目录拷贝功能
- `testCopyEmptyFolder`：测试空文件夹的拷贝功能
- `testSameFileDoNotCopyWithFilename`：测试相同文件拷贝时提示错误的功能
- `testSameFileDoNotCopyWithoutFilename`：测试相同文件在同一目录内拷贝时提示错误的功能

同时，测试类 `AppTest` 中还包含了几个测试帮助函数，它们分别为：

- `private boolean copyPartialVersion(String src_path, String dest_path)`：提供默认选项下的测试函数：
  - `allowCopyRecrusively: true`
  - `doNotOverwrite: false`
  - `verboseMode: true`
  - `permitToOverwrite: false`
- `private Path generatePath(String path)`：用以将测试环境使用的目录路径转换为正确路径。
- `private void removeFileOrDirectory(Path path)`：清理测试过程中产生的临时文件。

测试过程使用了一些资源文件，它们的位置在 `src/test/resources` 目录中，并通过 `pom.xml` 的配置指定使用：

```xml
<build>
    <testResources>
        <testResource>
          	<directory>${project.basedir}/src/test/resources</directory>
          	<filtering>true</filtering>
        </testResource>
    </testResources>
</build>
```

通过使用下列的命令对项目进行清理、编译、测试：

```bash
mvn clean
mvn compile
mvn test
```

测试结果为全部通过：

```
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running ru.spbstu.telematics.java.AppTest
/Users/doge/repos/java-lab1-icp/src/test/resources/root.file -> /Users/doge/repos/java-lab1-icp/src/test/resources/parent/root.file
/Users/doge/repos/java-lab1-icp/src/test/resources/parent/child/.DS_Store -> /Users/doge/repos/java-lab1-icp/src/test/resources/parent/new_child/.DS_Store
/Users/doge/repos/java-lab1-icp/src/test/resources/parent/child/child.file -> /Users/doge/repos/java-lab1-icp/src/test/resources/parent/new_child/child.file
/Users/doge/repos/java-lab1-icp/src/test/resources/parent/child/.DS_Store -> /Users/doge/repos/java-lab1-icp/src/test/resources/child/.DS_Store
/Users/doge/repos/java-lab1-icp/src/test/resources/parent/child/child.file -> /Users/doge/repos/java-lab1-icp/src/test/resources/child/child.file
/Users/doge/repos/java-lab1-icp/src/test/resources/root.file -> /Users/doge/repos/java-lab1-icp/src/test/resources/parent/root.file
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.042 s -- in ru.spbstu.telematics.java.AppTest
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0
```

