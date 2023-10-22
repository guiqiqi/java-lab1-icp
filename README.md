### Конфигурация среды разработки

Я использовал систему macOS и установил инструменты JDK, Maven и Git через менеджер пакетов `brew`:

```bash
brew install java
brew install maven
brew install git
```

Я проверил, что они были успешно установлены с помощью командной строки:

```bash
java --version
mvn --version
git --version
```

Для создания проекта я использую следующую команду:

```bash
mvn archetype:generate -DgroupId=ru.spbstu.telematics.java -DartifactId=icp -DarchetypeArtifactId=maven-archetype-quickstart -DinteractiveMode=false
```

Я вхожу в каталог `icp` и использую следующую команду для создания нового проекта Git:

```bash
git init
```

### Логика программы

Эта программа имеет поведение, аналогичное команде `cp`, и поддерживает некоторые дополнительные параметры командной строки посредством кодирования кода Java. Поведение программы следующее:

- С помощью кода внутри программы можно установить четыре дополнительных параметра:
   - `allowCopyRecrusivity`: этот параметр ведет себя аналогично параметру `-r`. Если этот параметр имеет значение `true`, программа разрешает рекурсивное копирование каталога и всех его подфайлов/подпапок; в противном случае, когда пользователь пытается скопировать каталог , будет выдана ошибка
   - `doNotOverwrite`: этот параметр действует аналогично параметру `-n`. Если этот параметр имеет значение true, программа пропустит копирование соответствующего файла при обнаружении повторяющегося имени файла.
   - `verboseMode`: этот параметр ведет себя аналогично параметру `-v`. Если этот параметр имеет значение true, будет отображаться процесс копирования всех файлов.
   - `permitToOverwrite`: этот параметр ведет себя аналогично параметру `-i`. Если этот параметр имеет значение `true`, копия файла с тем же именем запрашивает у пользователя: когда пользовательский ввод `y/yes` ( без учета регистра), файл будет перезаписан.
- Основная программа `App.main` получает два параметра из командной строки, представляющие исходный файл/исходную папку и целевой файл/папку назначения.
- При возникновении следующих ситуаций пользователю будет выведено сообщение об ошибке:
   - Если путь к исходному файлу/каталогу не существует, появится сообщение об ошибке.
   - Если исходный каталог совпадает с целевым каталогом, программа не будет копировать файл, но выдаст сообщение об ошибке.
   - При копировании из каталога в файл будет выдана ошибка.
   - При сбое чтения/записи файла из-за недостаточных разрешений будет выдано сообщение об ошибке.

### Код для реализации

Программа использует модуль `java.nio` для управления файлами и путями, а три класса предназначены для абстрагирования копируемых файлов/путей:

```javascript
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

Абстрактный класс верхнего уровня `CopyablePath` относится ко всем объектам, которые могут быть скопированы (файлы, папки). Подклассы, которые его наследуют, должны реализовать метод `copy`. Благодаря полиморфизму они могут вызывать различные типы методов копирования. Реализация его и правильно скопируют в другой файл/каталог. Он также управляет объектом пути копируемого объекта (типа `java.nio.file.Path`) и устанавливает четыре дополнительных параметра через статические переменные для управления поведением во время процесса копирования.

`CopyablePath` также предоставляет метод `pathFactory`, который используется для создания объекта `CopyablePath` (конкретный тип — `CopyableFile/CopyableFolder`) на основе фактического пути.

`CopyablePath` также предоставляет метод `promptOverwrite`, позволяющий запрашивать у пользователя решения в случае возникновения конфликтов при копировании файлов.

Более конкретно, логика реализации подкласса `CopyableFile/CopyableFolder` выглядит следующим образом:

- Если исходным путем является файл: (A)

   - Если целевой путь существует: (A.1)
     - Если целевой путь представляет собой файл: спросить пользователя/проверьть, следует ли перезаписать файл на основе элементов конфигурации.
     - Если целевой путь является папкой: рекурсивно создавать путь к целевому файлу до (A).
   - Если целевой путь не существует: (A.2)
     - Если родительский путь целевого пути существует: записать в файл
     - если родительский путь целевого пути не существует: ошибка

- Если исходным путем является папка: (B)

   - Если целевой путь существует: (B.1)

     - если целевой путь является файлом: ошибка
     - Если целевым путем является папка: выполнить итерацию по файлам, повторяя (A.1) для каждого файла и (B.1) для каждой папки.

   - Если целевой путь не существует: (B.2)
     - Если родительский путь целевого пути существует: создавать папку рекурсивно к (B.1).
     - если родительский путь целевого пути не существует: ошибка

Конкретную его реализацию можно посмотреть в коде и комментариях.

Исключение `CopyBaseException` определено в проекте, чтобы взять на себя все другие исключения и предоставить пользователю разумные подсказки:

```java
public class CopyBaseException extends RuntimeException{
    CopyBaseException(String message) {
        super("cp: " + message);
    }
}
```

### Тест

Я написал несколько тестов для этого проекта, которые проверяют нормальную функциональность, которую должна реализовать программа, а также некоторые граничные условия:

- `testCopyFileWithFilename`: функция копирования тестового файла.
- `testCopyFileWithoutFilename`: протестирует функцию копирования файлов, когда указан только целевой каталог.
- `testCopyFolderWithName`: функция копирования тестового каталога.
- `testCopyFolderWithoutName`: протестирует функцию копирования каталога, если имя целевого каталога не указано.
- `testCopyEmptyFolder`: протестирует функцию копирования пустых папок.
- `testSameFileDoNotCopyWithFilename`: функция для отображения ошибок при тестировании одной и той же копии файла.
- `testSameFileDoNotCopyWithoutFilename`: протестирует функцию запроса ошибки при копировании того же файла в один и тот же каталог.

В то же время тестовый класс AppTest также содержит несколько вспомогательных функций тестирования, а именно:

- `private boolean copyPartialVersion(String src_path, String dest_path)`: предоставляет тестовые функции с параметрами по умолчанию:
   - `allowCopyRecrusivity: true`
   - `doNotOverwrite: false`
   - `verboseMode: true`
   - `permitToOverwrite: false`
- `private PathgeneratePath(String path)`: используется для преобразования пути к каталогу, используемого в тестовой среде, в правильный путь.
- `private void removeFileOrDirectory(Path path)`: очистка временных файлов, созданных в процессе тестирования.

В процессе тестирования используются некоторые файлы ресурсов. Их расположение находится в каталоге `src/test/resources` и указывается в конфигурации `pom.xml`:

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

Я очищаю, компилирую и тестирую проект, используя следующие команды:

```bash
mvn clean
mvn compile
mvn test
```

Пройдены все тесты:

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

