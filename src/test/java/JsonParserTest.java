import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import io.github.zoltan_sipeki.json_parser.Json;
import io.github.zoltan_sipeki.json_parser.JsonSyntaxError;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JsonParserTest {

    private String[] wellFormed;

    private String[] malformed;

    @BeforeAll
    void init() throws IOException {
        wellFormed = new String(getClass().getResourceAsStream("/resources/well_formed_json.txt").readAllBytes())
                .split("\n---\n");
        malformed = new String(getClass().getResourceAsStream("/resources/malformed_json.txt").readAllBytes())
                .split("\n---\n");
    }

    @Test
    @DisplayName("Parse well-formed JSON")
    void parseWellFormed() {
        for (var json : wellFormed) {
            try {
                Json.parse(json);
            } catch (Exception e) {
                fail("Expected no exception, but got " + e.getClass().getSimpleName()
                        + " for JSON:\n" + json + "\nException message:\n" + e.getMessage(), e);
            }
        }
    }

    @Test
    @DisplayName("Parse malformed JSON")
    void parseMalformed() {
        for (var json : malformed) {
            try {
                Json.parse(json);
                fail("Expected JsonSyntaxError, but none was thrown for JSON:\n" + json);
            } catch (JsonSyntaxError expected) {
                // pass
            } catch (Exception e) {
                fail("Expected JsonSyntaxError, but got " + e.getClass().getSimpleName()
                        + " for JSON:\n" + json, e);
            }
        }
    }

    @Test
    @DisplayName("Stringify JSON")
    void stringify() {
        for (var json : wellFormed) {
            try {
                Json.stringify(Json.parse(json));
            } catch (Exception e) {
                fail("Expected no exception, but got " + e.getClass().getSimpleName()
                        + " for JSON:\n" + json + "\nException message:\n" + e.getMessage(), e);
            }
        }
    }
}