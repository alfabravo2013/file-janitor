import org.hyperskill.hstest.dynamic.DynamicTest;
import org.hyperskill.hstest.stage.StageTest;
import org.hyperskill.hstest.testcase.CheckResult;
import org.hyperskill.hstest.testing.TestedProgram;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hyperskill.hstest.testing.expect.Expectation.expect;

@SuppressWarnings({"unused", "SimplifyStreamApiCallChains", "FieldCanBeLocal"})
public class FileJanitorStage3Test extends StageTest<Object> {
    private final int year = LocalDate.now().getYear();
    private final String lineOneRegex = String.format(".*File\\s+Janitor\\s*,?.*\\s*%d\\s*.*", year);
    private final String lineTwoRegex = ".*Powered\\s+by\\s+Bash.*";
    private final Pattern lineOne = Pattern.compile(lineOneRegex, Pattern.CASE_INSENSITIVE);
    private final Pattern lineTwo = Pattern.compile(lineTwoRegex, Pattern.CASE_INSENSITIVE);

    private final String filename = "file-janitor-help.txt";
    private final String helpFileContent = """
            File Janitor is a tool to discover files and clean directories

            Usage: file-janitor.sh [option] <file_path>

                options:
                    help            displays this help file
                    list [path]     lists files in the specified or working directory
            """;
    private final Map<String, String> helpFile = Map.of(filename, helpFileContent);

    private final String[] unsupportedArgs = {"unknown", "-h", "unsupported", "arg1"};

    private final String noArgsHint = "Type file-janitor.sh help to see available options";

    private final String notDirectoryName = UUID.randomUUID().toString();
    private final Map<String, String> notDirectory = Map.of(notDirectoryName, "");
    private final String[] currentDir = {"", "."};
    private final String[] pathsToList = {"../", "../../", "../../../", "test"};
    private final Map<String, String> filesToList = Map.of(
            "file1", "",
            "file2", "",
            ".file1", "",
            "file-1", "",
            "File1", "",
            "file.extension", "",
            "test/.tricky.Name", ""
    );

    private final String currentDirMessage = "Listing files in the current directory";
    private final String someDirMessage = "Listing files in ";
    private final String pathNotFoundMessage = " is not found";
    private final String notDirectoryMessage = " is not a directory";

    @DynamicTest(order = 1)
    CheckResult testScriptTitle() {
        TestedProgram program = new TestedProgram();
        String output = program.start();

        List<String> lines = expect(output).toContainAtLeast(3).lines();

        if (!program.isFinished()) {
            return CheckResult.wrong("Your script must display its title and exit.");
        }

        CheckResult checkInfoResult = checkInfo(lines);
        if (!checkInfoResult.isCorrect()) {
            return checkInfoResult;
        }

        return checkHint(lines);
    }

    @DynamicTest(order = 2, files = "helpFile")
    CheckResult testHelp() throws IOException {
        TestedProgram program = new TestedProgram();

        List<String> fileContent = Files.readAllLines(Path.of(filename));
        String output = program.start("help");

        List<String> lines = expect(output).toContainAtLeast(fileContent.size() + 2).lines();

        if (!program.isFinished()) {
            return CheckResult.wrong("Your script must display its title and exit.");
        }

        int helpLinesStart = lines.indexOf(fileContent.get(0));

        if (helpLinesStart == -1) {
            return CheckResult.wrong(
                    "Failed to find the first line of the help file in your script's output"
            );
        }

        List<String> infoLines = lines.subList(0, helpLinesStart);
        List<String> helpLines = lines.stream()
                .skip(helpLinesStart)
                .dropWhile(String::isBlank)
                .collect(Collectors.toList());

        CheckResult checkInfoResult = checkInfo(infoLines);

        if (!checkInfoResult.isCorrect()) {
            return checkInfoResult;
        }

        return checkHelpFileContent(fileContent, helpLines);
    }

    @DynamicTest(order = 3, data = "unsupportedArgs")
    CheckResult testUnsupportedArgs(String unsupportedArg) {
        TestedProgram program = new TestedProgram();

        String output = program.start(unsupportedArg);

        List<String> lines = expect(output).toContainAtLeast(2).lines();

        if (!program.isFinished()) {
            return CheckResult.wrong("Your script must display its title and exit.");
        }

        CheckResult checkInfoResult = checkInfo(lines);
        if (!checkInfoResult.isCorrect()) {
            return checkInfoResult;
        }

        return checkHint(lines);
    }

    @DynamicTest(order = 4, data = "currentDir", files = "filesToList")
    CheckResult testListFilesInCurrentDir(String path) {
        TestedProgram program = new TestedProgram();

        String output = path.isBlank() ? program.start("list") : program.start("list", path);
        List<String> lines = expect(output).toContainAtLeast(3).lines();

        CheckResult checkInfoResult = checkInfo(lines);
        if (!checkInfoResult.isCorrect()) {
            return checkInfoResult;
        }

        if (lines.stream().noneMatch(line -> currentDirMessage.equalsIgnoreCase(line.strip()))) {
            return CheckResult.wrong("When listing files in the current directory, " +
                    "your script must print this line: " + currentDirMessage);
        }

        List<String> fileList = lines.stream()
                .dropWhile(line -> !currentDirMessage.equalsIgnoreCase(line.strip()))
                .skip(1)
                .filter(line -> !line.isBlank())
                .collect(Collectors.toList());

        return checkFileList(fileList, path);
    }

    @DynamicTest(order = 5)
    CheckResult testListFilesAtNonExistingPath() {
        TestedProgram program = new TestedProgram();

        String nonExistingPath = UUID.randomUUID().toString();
        String expected = nonExistingPath + pathNotFoundMessage;
        String output = program.start("list", nonExistingPath);
        List<String> lines = expect(output).toContainAtLeast(3).lines();

        CheckResult checkInfoResult = checkInfo(lines);
        if (!checkInfoResult.isCorrect()) {
            return checkInfoResult;
        }

        if (lines.stream().noneMatch(line -> expected.equalsIgnoreCase(line.strip()))) {
            return CheckResult.wrong("When listing files in a non-existing directory, " +
                    "your script must print that such directory is not found");
        }

        return CheckResult.correct();
    }

    @DynamicTest(order = 6, files = "notDirectory")
    CheckResult testListFilesAtNotDirectory() {
        TestedProgram program = new TestedProgram();

        String expected = notDirectoryName + notDirectoryMessage;
        String output = program.start("list", notDirectoryName);
        List<String> lines = expect(output).toContainAtLeast(3).lines();

        CheckResult checkInfoResult = checkInfo(lines);
        if (!checkInfoResult.isCorrect()) {
            return checkInfoResult;
        }

        if (lines.stream().noneMatch(line -> expected.equalsIgnoreCase(line.strip()))) {
            return CheckResult.wrong("If listing at a path that does not refer to a directory, " +
                    "your script must print that that path is not a directory");
        }

        return CheckResult.correct();
    }

    @DynamicTest(order = 7, data = "pathsToList", files = "filesToList")
    CheckResult testListFilesAtPath(String path) {
        TestedProgram program = new TestedProgram();

        String output = program.start("list", path);
        List<String> lines = expect(output).toContainAtLeast(3).lines();

        CheckResult checkInfoResult = checkInfo(lines);
        if (!checkInfoResult.isCorrect()) {
            return checkInfoResult;
        }

        String expected = someDirMessage + path;
        if (lines.stream().noneMatch(line -> expected.equalsIgnoreCase(line.strip()))) {
            return CheckResult.wrong("If listing at a specified path" +
                    "your script must print " + someDirMessage + " %PATH%");
        }

        List<String> fileList = lines.stream()
                .dropWhile(line -> !expected.equalsIgnoreCase(line.strip()))
                .skip(1)
                .filter(line -> !line.isBlank())
                .collect(Collectors.toList());

        return checkFileList(fileList, path);
    }

    private CheckResult checkInfo(List<String> infoLines) {
        boolean hasLineOne = infoLines.stream()
                .anyMatch(line -> lineOne.matcher(line).matches());
        boolean hasLineTwo = infoLines.stream()
                .dropWhile(line -> !lineOne.matcher(line).matches())
                .anyMatch(line -> lineTwo.matcher(line).matches());

        if (!hasLineOne) {
            return CheckResult.wrong("Your script must output its title and the current year");
        }

        if (!hasLineTwo) {
            return CheckResult.wrong("Below the title, your script must claim that it is a Bash script");
        }

        return CheckResult.correct();
    }

    private CheckResult checkHelpFileContent(List<String> fileLines, List<String> outputLines) {
        for (int i = 0; i < fileLines.size(); i++) {
            String expected = fileLines.get(i).strip();
            String actual = outputLines.get(i).strip();
            if (!expected.equals(actual)) {
                return CheckResult.wrong(
                        "You script failed to output the content of file-janitor-help.txt correctly"
                );
            }
        }

        return CheckResult.correct();
    }

    private CheckResult checkHint(List<String> lines) {
        boolean hasCorrectHint = lines.stream()
                .dropWhile(line -> lineTwo.matcher(line).matches())
                .anyMatch(line -> noArgsHint.equalsIgnoreCase(line.strip()));

        return hasCorrectHint ?
                CheckResult.correct() :
                CheckResult.wrong("When executed with no arg or with an unsupported arg, " +
                        "your script should print a hint: Type file-janitor help to see available options");
    }

    private CheckResult checkFileList(List<String> fileList, String path) {
        Path p = Path.of(path);
        try (Stream<Path> stream = Files.list(p)) {
            List<String> actualFileList = stream
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .collect(Collectors.toList());

            if (actualFileList.containsAll(fileList) && fileList.containsAll(actualFileList)) {
                return CheckResult.correct();
            }

            return CheckResult.wrong("Your script output incorrect list of files at " + path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
