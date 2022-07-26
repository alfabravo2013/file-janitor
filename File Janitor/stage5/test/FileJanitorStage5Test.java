import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.hyperskill.hstest.dynamic.DynamicTest;
import org.hyperskill.hstest.stage.StageTest;
import org.hyperskill.hstest.testcase.CheckResult;
import org.hyperskill.hstest.testing.TestedProgram;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

import static org.hyperskill.hstest.testing.expect.Expectation.expect;

@SuppressWarnings({"unused", "SimplifyStreamApiCallChains", "FieldCanBeLocal"})
public class FileJanitorStage5Test extends StageTest<Object> {
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
                    clean [path]    processes (archives, deletes or moves) certain files
                                    in the specified or working directory
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

    private final String reportLineFormat = "(?i)%d\\s+%s\\s+.+total size.*\\s+%d.*";
    private final String currentDirReportMsg = "The current directory contains:";
    private final String someDirReportMsg = " contains:";

    private final String currentDirCleanMsg = "Cleaning the current directory...";
    private final String currentDirCleanDone = "Clean up of the current directory is complete!";

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

    @DynamicTest(order = 9, data = "pathsToList", files = "getFilesToReport")
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

    @DynamicTest(order = 10, files = "getFilesToReport")
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

    @DynamicTest(order = 11, files = "notDirectory")
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

    @DynamicTest(order = 8, data = "currentDir", files = "getFilesToReport")
    CheckResult checkCleanFilesAtCurrentDir(String path) {
        TestedProgram program = new TestedProgram();

        String output = path.isBlank() ? program.start("clean") : program.start("clean", path);
        List<String> lines = expect(output).toContainAtLeast(7).lines();

        if (lines.stream().noneMatch(line -> currentDirCleanMsg.equalsIgnoreCase(line.strip()))) {
            return CheckResult.wrong("When reporting files in the current directory, " +
                    "your script must print this line: " + currentDirCleanMsg);
        }

        List<String> cleanReport = lines.stream()
                .dropWhile(line -> !currentDirCleanMsg.equalsIgnoreCase(line.strip()))
                .skip(1)
                .filter(line -> !line.isBlank())
                .takeWhile(it -> !currentDirCleanDone.equalsIgnoreCase(it))
                .collect(Collectors.toList());

        return checkCleanReport(cleanReport, path); // todo remove py scripts with their dir and tarball
    }

    // todo check if all the above works for the pwd
    // todo check if all the above works for an arbitrary dir
    // todo check if an error is displayed for a missing dir and for a not a dir

    private CheckResult checkCleanReport(List<String> cleanReport, String path) {
        var actualTmp = getFileSizeAndCount(path, "tmp");
        var actualTmpCount = actualTmp.get("count");
        if (actualTmpCount != 0L) {
            return CheckResult.wrong("There must not be *.tmp files in the " + path +
                    "directory, but " + actualTmpCount + " files were found");
        }

        var actualLog = getFileSizeAndCount(path, "log");
        var actualLogCount = actualLog.get("count");
        if (actualLogCount != 0L) {
            return CheckResult.wrong("There must not be *.log files in the " + path +
                    "directory, but " + actualLogCount + " files were found");
        }

        var archFilename = path.isBlank() ? "logs.tar.gz" : path + "/logs.tar.gz";
        var logFiles = getFilenamesByExtExclPath(path.startsWith("/") ? path : "", "log");

        try (GZIPInputStream gzis = new GZIPInputStream(new FileInputStream(archFilename));
             TarArchiveInputStream tais = new TarArchiveInputStream(gzis)) {
            ArchiveEntry entry;
            List<String> compressedFiles = new ArrayList<>();

            while ((entry = tais.getNextEntry()) != null) {
                var entryName = entry.getName().replaceFirst("\\./", "");
                compressedFiles.add(entryName);
            }

            if (!compressedFiles.containsAll(logFiles) || !logFiles.containsAll(compressedFiles)) {
                return CheckResult.wrong("Expected " + logFiles + " but found " + compressedFiles);
            }
        } catch (IOException e) {
            return CheckResult.wrong("Error happened during testing: " + e.getMessage());
        }

        var scriptsDir = path.isBlank() ? "." + "/py_scripts" : path + "/py_scripts";
        var scriptsPath = Paths.get(scriptsDir);
        if (!Files.exists(scriptsPath)) {
            return CheckResult.wrong(scriptsDir + " is not found");
        } else if (!scriptsPath.toFile().isDirectory()) {
            return CheckResult.wrong(scriptsDir + "is not a directory");
        }

        var actualPy = getFileSizeAndCount(scriptsDir, "py");
        var actualPyCount = actualPy.get("count");

        // todo check if py files are moved to a sub-dir
        return CheckResult.correct();

        // todo check if reported number of files correspond to the actual processed number
    }

    // TODO: 7/26/22 check if script does not create /py_scripts dir when there is no py files

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
        } catch (Exception e) {
            return CheckResult.wrong("An error occurred during testing: " + e.getMessage());
        }
    }

    private CheckResult checkFileReport(List<String> report, String path) {
        try {
            var actualTmp = getFileSizeAndCount(path, "tmp");
            var actualLog = getFileSizeAndCount(path, "log");
            var actualPy = getFileSizeAndCount(path, "py");

            var tmpActualSize = actualTmp.get("size");
            var tmpActualCount = actualTmp.get("count");
            var logActualSize = actualLog.get("size");
            var logActualCount = actualLog.get("count");
            var pyActualSize = actualPy.get("size");
            var pyActualCount = actualPy.get("count");

            var tmpPattern = Pattern.compile(String.format(reportLineFormat, tmpActualCount, "tmp", tmpActualSize));
            var logPattern = Pattern.compile(String.format(reportLineFormat, logActualCount, "log", logActualSize));
            var pyPattern = Pattern.compile(String.format(reportLineFormat, pyActualCount, "py", pyActualSize));

            var isTmpReportOk = report.stream().anyMatch(line -> tmpPattern.matcher(line).matches());
            var isLogReportOk = report.stream().anyMatch(line -> logPattern.matcher(line).matches());
            var isPyReportOk = report.stream().anyMatch(line -> pyPattern.matcher(line).matches());

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
        } catch (RuntimeException e) {
            return CheckResult.wrong("An error occurred during testing: " + e.getMessage());
        }
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

    private List<String> getFilenamesByExtExclPath(String path, String extension) {
        return List.of("file.tmp", "File.tmp", ".File-1.tmp", "test/.tricky.log.tmp",
                        "logfile.log", ".hidden-log-file", "test/.hidden-Tricky.log.file.log",
                        "python-script.py", "test/another-one.py").stream()
                .filter(it -> {
                    if (path.isBlank()) {
                        return !it.startsWith("test/");
                    } else {
                        return it.startsWith("test/");
                    }
                })
                .filter(it -> it.endsWith(extension))
                .collect(Collectors.toList());
    }

    private String getRandomFileContent() {
        Random rnd = ThreadLocalRandom.current();
        return rnd.ints().limit(rnd.nextInt(25, 1250))
                .mapToObj(String::valueOf)
                .collect(Collectors.joining());
    }
}
