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
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hyperskill.hstest.testing.expect.Expectation.expect;

@SuppressWarnings({"unused", "SimplifyStreamApiCallChains", "FieldCanBeLocal"})
public class FileJanitorStage4Test extends StageTest<Object> {
    private final int year = LocalDate.now().getYear();
    private final String lineOneRegex = String.format("(?i).*File\\s+Janitor\\s*,?.*\\s*%d\\s*.*", year);
    private final String lineTwoRegex = "(?i).*Powered\\s+by\\s+Bash.*";
    private final Pattern lineOne = Pattern.compile(lineOneRegex);
    private final Pattern lineTwo = Pattern.compile(lineTwoRegex);

    private final String filename = "file-janitor-help.txt";
    private final String helpFileContent = """
            File Janitor is a tool to discover files and clean directories

            Usage: file-janitor.sh [option] <file_path>

                options:
                    help            displays this help file
                    list [path]     lists files in the specified or working directory
                    report [path]   outputs a summary of files in the specified or working directory
            """;
    private final Map<String, String> helpFile = Map.of(filename, helpFileContent);

    private final String[] unsupportedArgs = {"unknown", "-h", "unsupported", "arg1"};

    private final String noArgsHint = "Type file-janitor.sh help to see available options";

    private final String notDirectoryName = "not-a-dir";
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

    private final String reportLineFormat = "(?i)%d\\s+%s\\s+.+total size.*\\s+%d.*";
    private final String currentDirReportMsg = "The current directory contains:";
    private final String someDirReportMsg = " contains:";

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

    @DynamicTest(order = 8, data = "currentDir", files = "getFilesToReport")
    CheckResult checkReportFilesAtCurrentDir(String path) {
        TestedProgram program = new TestedProgram();

        String output = path.isBlank() ? program.start("report") : program.start("report", path);
        List<String> lines = expect(output).toContainAtLeast(3).lines();

        if (lines.stream().noneMatch(line -> currentDirReportMsg.equalsIgnoreCase(line.strip()))) {
            return CheckResult.wrong("When reporting files in the current directory, " +
                    "your script must print this line: " + currentDirReportMsg);
        }

        List<String> fileReport = lines.stream()
                .dropWhile(line -> !currentDirReportMsg.equalsIgnoreCase(line.strip()))
                .skip(1)
                .filter(line -> !line.isBlank())
                .collect(Collectors.toList());

        return checkFileReport(fileReport, path);
    }

    @DynamicTest(order = 8, data = "pathsToList", files = "getFilesToReport")
    CheckResult checkReportFilesAtPath(String path) {
        TestedProgram program = new TestedProgram();

        String output = program.start("report", path);
        List<String> lines = expect(output).toContainAtLeast(3).lines();

        CheckResult checkInfoResult = checkInfo(lines);
        if (!checkInfoResult.isCorrect()) {
            return checkInfoResult;
        }

        String expected = path + someDirReportMsg;
        if (lines.stream().noneMatch(line -> expected.equalsIgnoreCase(line.strip()))) {
            return CheckResult.wrong("When reporting files in the current directory, " +
                    "your script must print this line: " + someDirReportMsg);
        }

        List<String> fileReport = lines.stream()
                .dropWhile(line -> !expected.equalsIgnoreCase(line.strip()))
                .skip(1)
                .filter(line -> !line.isBlank())
                .collect(Collectors.toList());

        return checkFileReport(fileReport, path);
    }

    @DynamicTest(order = 9, files = "getFilesToReport")
    CheckResult checkReportFilesAtNonExistingPath() {
        TestedProgram program = new TestedProgram();

        String nonExistingPath = UUID.randomUUID().toString();
        String expected = nonExistingPath + pathNotFoundMessage;
        String output = program.start("report", nonExistingPath);
        List<String> lines = expect(output).toContainAtLeast(3).lines();

        CheckResult checkInfoResult = checkInfo(lines);
        if (!checkInfoResult.isCorrect()) {
            return checkInfoResult;
        }

        if (lines.stream().noneMatch(line -> expected.equalsIgnoreCase(line.strip()))) {
            return CheckResult.wrong("When reporting files in a non-existing directory, " +
                    "your script must print that such directory is not found");
        }

        return CheckResult.correct();
    }

    @DynamicTest(order = 10, files = "notDirectory")
    CheckResult checkReportFilesAtNotDirectory() {
        TestedProgram program = new TestedProgram();

        String expected = notDirectoryName + notDirectoryMessage;
        String output = program.start("report", notDirectoryName);
        List<String> lines = expect(output).toContainAtLeast(3).lines();

        CheckResult checkInfoResult = checkInfo(lines);
        if (!checkInfoResult.isCorrect()) {
            return checkInfoResult;
        }

        if (lines.stream().noneMatch(line -> expected.equalsIgnoreCase(line.strip()))) {
            return CheckResult.wrong("If reporting at a path that does not refer to a directory, " +
                    "your script must print that that path is not a directory");
        }

        return CheckResult.correct();
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
        try (Stream<Path> stream = Files.list(Path.of(path))) {
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

    private CheckResult checkFileReport(List<String> report, String path) {
        long tmpActualSize = getFileSizeAndCount(path, "tmp").get("size");
        long tmpActualCount = getFileSizeAndCount(path, "tmp").get("count");
        long logActualSize = getFileSizeAndCount(path, "log").get("size");
        long logActualCount = getFileSizeAndCount(path, "log").get("count");
        long pyActualSize = getFileSizeAndCount(path, "py").get("size");
        long pyActualCount = getFileSizeAndCount(path, "py").get("count");

        Pattern tmpPattern = Pattern.compile(String.format(reportLineFormat, tmpActualCount, "tmp", tmpActualSize));
        Pattern logPattern = Pattern.compile(String.format(reportLineFormat, logActualCount, "log", logActualSize));
        Pattern pyPattern = Pattern.compile(String.format(reportLineFormat, pyActualCount, "py", pyActualSize));

        boolean isTmpReportOk = report.stream().anyMatch(line -> tmpPattern.matcher(line).matches());
        boolean isLogReportOk = report.stream().anyMatch(line -> logPattern.matcher(line).matches());
        boolean isPyReportOk = report.stream().anyMatch(line -> pyPattern.matcher(line).matches());

        if (!isTmpReportOk) {
            return CheckResult.wrong("Your script outputs an incorrect report for tmp files at "
                    + path + ": the actual count was " + tmpActualCount + " and actual size was " + tmpActualSize);
        }

        if (!isLogReportOk) {
            return CheckResult.wrong("Your script outputs an incorrect report for log files at "
                    + path + ": the actual count was " + logActualCount + " and actual size was " + logActualSize);
        }

        if (!isPyReportOk) {
            return CheckResult.wrong("Your script outputs an incorrect report for py files at "
                    + path + ": the actual count was " + pyActualCount + " and actual size was " + pyActualSize);
        }

        return CheckResult.correct();
    }

    private Map<String, Long> getFileSizeAndCount(String path, String extension) {
        try (Stream<Path> stream = Files.list(Path.of(path))) {
            List<Path> filteredFiles = stream
                    .filter(file -> !Files.isDirectory(file))
                    .filter(p -> p.getFileName().toString().matches(".+\\." + extension))
                    .collect(Collectors.toList());

            long size = filteredFiles.stream().mapToLong(p -> {
                try {
                    return Files.size(p);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }).sum();

            long count = filteredFiles.size();

            return Map.of("count", count, "size", size);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Map<String, String> getFilesToReport() {
        return Map.of(
                "file.tmp", getRandomFileContent(),
                "File.tmp", getRandomFileContent(),
                ".File-1.tmp", getRandomFileContent(),
                "test/.tricky.log.tmp", getRandomFileContent(),
                "logfile.log", getRandomFileContent(),
                ".hidden-log-file", getRandomFileContent(),
                "test/.hidden-Tricky.log.file.log", getRandomFileContent(),
                "python-script.py", getRandomFileContent(),
                "test/another-one.py", getRandomFileContent()
                );
    }

    private String getRandomFileContent() {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        return rnd.ints().limit(rnd.nextInt(25, 125))
                .mapToObj(String::valueOf)
                .collect(Collectors.joining());

    }
}
