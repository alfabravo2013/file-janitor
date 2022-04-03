import org.hyperskill.hstest.dynamic.DynamicTest;
import org.hyperskill.hstest.stage.StageTest;
import org.hyperskill.hstest.testcase.CheckResult;
import org.hyperskill.hstest.testing.TestedProgram;

import java.time.LocalDate;
import java.util.List;
import java.util.regex.Pattern;

import static org.hyperskill.hstest.testing.expect.Expectation.expect;

public class FileJanitorTest extends StageTest<Object> {
    private final int year = LocalDate.now().getYear();
    private final String lineOneRegex = String.format("\\s*File\\s+Janitor\\s*,?\\s+%d\\s*", year);
    private final String lineTwoRegex = "\\s*Powered\\s+by\\s+Bash\\s*";
    private final Pattern lineOne = Pattern.compile(lineOneRegex, Pattern.CASE_INSENSITIVE);
    private final Pattern lineTwo = Pattern.compile(lineTwoRegex, Pattern.CASE_INSENSITIVE);

    @DynamicTest
    CheckResult testHelloWorld() {
        TestedProgram program = new TestedProgram();
        String output = program.start();

        List<String> lines = expect(output).toContain(2).lines();

        if (!lineOne.matcher(lines.get(0)).matches()) {
            return CheckResult.wrong(
                    "The first line must contain the script title and the current year " +
                            "but your output was: " + lines.get(1));
        }

        if (!lineTwo.matcher(lines.get(1)).matches()) {
            return CheckResult.wrong(
                    "The second line is expected to indicate that the scrip is a Bash script " +
                            "but your output was: " + lines.get(2));
        }

        return CheckResult.correct();
    }
}
