import org.hyperskill.hstest.dynamic.DynamicTest;
import org.hyperskill.hstest.stage.StageTest;
import org.hyperskill.hstest.testcase.CheckResult;
import org.hyperskill.hstest.testing.TestedProgram;

import java.time.LocalDate;
import java.util.List;
import java.util.regex.Pattern;

import static org.hyperskill.hstest.testing.expect.Expectation.expect;

@SuppressWarnings("unused")
public class FileJanitorStage1Test extends StageTest<Object> {
    private final int year = LocalDate.now().getYear();
    private final String lineOneRegex = String.format(".*File\\s+Janitor\\s*,?.*\\s*%d\\s*.*", year);
    private final String lineTwoRegex = ".*Powered\\s+by\\s+Bash.*";
    private final Pattern lineOne = Pattern.compile(lineOneRegex, Pattern.CASE_INSENSITIVE);
    private final Pattern lineTwo = Pattern.compile(lineTwoRegex, Pattern.CASE_INSENSITIVE);

    @DynamicTest
    CheckResult testScriptTitle() {
        TestedProgram program = new TestedProgram();
        String output = program.start();

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
}
