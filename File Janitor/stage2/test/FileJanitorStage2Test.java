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
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.hyperskill.hstest.testing.expect.Expectation.expect;

@SuppressWarnings("unused")
public class FileJanitorStage2Test extends StageTest<Object> {
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
            """;
    private final Map<String, String> helpFile = Map.of(filename, helpFileContent);

    private final String[] unsupportedArgs = {"unknown", "-h", "unsupported", "arg1"};

    @DynamicTest(order = 1)
    CheckResult testScriptTitle() {
        TestedProgram program = new TestedProgram();
        String output = program.start();

        List<String> lines = expect(output).toContainAtLeast(2).lines();

        if (!program.isFinished()) {
            return CheckResult.wrong("Your script must display its title and exit.");
        }

        return checkInfo(lines);
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

        return checkInfo(lines);
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
}
