package io.github.zoltan_sipeki.json_parser;

import java.util.ArrayList;
import java.util.List;

public class Tokenizer {

    public record Result(List<Token> tokens, List<Integer> lineOffsets) {
    }

    private int i = 0;

    private String input;

    private List<Token> tokens = new ArrayList<>();

    private List<Integer> lineOffsets = new ArrayList<>();

    public Tokenizer(String input) {
        this.input = input;
    }

    public Result tokenize() {
        lineOffsets.add(0);

        while (i < input.length()) {
            var c = input.charAt(i);

            if (c == '\n') {
                lineOffsets.add(i++);
                continue;
            }

            if (c == ' ' || c == '\t' || c == '\r') {
                ++i;
                continue;
            }

            if (c == '-' || c >= '0' && c <= '9') {
                readNumber();
            } else if (c >= 'a' && c <= 'z') {
                readKeyword();
            } else if (c == '"') {
                readString();
            } else {
                switch (c) {
                    case '{' -> {
                        tokens.add(new Token(Token.Type.OBJECT_START, "{", i, i));
                    }
                    case '}' -> {
                        tokens.add(new Token(Token.Type.OBJECT_END, "}", i, i));
                    }
                    case '[' -> {
                        tokens.add(new Token(Token.Type.ARRAY_START, "[", i, i));
                    }
                    case ']' -> {
                        tokens.add(new Token(Token.Type.ARRAY_END, "]", i, i));
                    }
                    case ',' -> {
                        tokens.add(new Token(Token.Type.COMMA, ",", i, i));
                    }
                    case ':' -> {
                        tokens.add(new Token(Token.Type.COLON, ":", i, i));
                    }
                    default -> {
                        throw syntaxError("unexpected character");
                    }
                }
                ++i;
            }

        }

        return new Result(tokens, lineOffsets);
    }

    private void readString() {
        enum StringState {
            START,
            CONTENT,
            ESCAPE,
            UNICODE,
            END
        }

        int start = i;
        int unicodeDigits = 0;
        int unicodeValue = 0;
        var state = StringState.START;

        var str = new StringBuilder();
        while (i < input.length() && state != StringState.END) {
            var c = input.charAt(i);

            switch (state) {
                case START -> {
                    if (c == '"') {
                        state = StringState.CONTENT;
                        ++i;
                    } else {
                        throw syntaxError("unexpected start of string");
                    }
                }

                case CONTENT -> {
                    if (c == '\\') {
                        state = StringState.ESCAPE;
                        ++i;
                    } else if (c == '"') {
                        state = StringState.END;
                        tokens.add(new Token(Token.Type.STRING, str.toString(), start + 1, i));
                        ++i;
                    } else if (c >= 0x0020) {
                        str.append(c);
                        ++i;
                    } else {
                        throw syntaxError("unexpected end of string");
                    }
                }

                case ESCAPE -> {
                    switch (c) {
                        case '"', '\\', '/' -> str.append(c);
                        case 'b' -> str.append('\b');
                        case 'f' -> str.append('\f');
                        case 'n' -> str.append('\n');
                        case 'r' -> str.append('\r');
                        case 't' -> str.append('\t');
                        case 'u' -> {
                            state = StringState.UNICODE;
                            unicodeDigits = 0;
                            unicodeValue = 0;
                            ++i;
                            continue;
                        }
                        default -> {
                            throw syntaxError("invalid escape character in string", c);
                        }
                    }

                    state = StringState.CONTENT;
                    ++i;
                }

                case UNICODE -> {
                    int hex = Character.digit(c, 16);
                    if (hex == -1) {
                        throw syntaxError("invalid unicode sequence in string");

                    }

                    unicodeValue <<= 4;
                    unicodeValue |= hex;

                    if (++unicodeDigits == 4) {
                        str.append((char) unicodeValue);
                        state = StringState.CONTENT;
                    }

                    ++i;
                }

                case END -> {

                }
            }
        }

        if (state != StringState.END) {
            throw syntaxError("unexpected end of string");
        }
    }

    private void readNumber() {
        enum NumberState {
            START,
            SIGN,
            ZERO,
            INTEGER,
            DECIMAL_POINT,
            FRACTION,
            EXPONENT,
            EXPONENT_SIGN,
            EXPONENT_INTEGER,
            END
        }

        var nState = NumberState.START;
        int start = i;

        while (i < input.length() && nState != NumberState.END) {
            var c = input.charAt(i);

            switch (nState) {
                case START -> {
                    if (c == '-') {
                        nState = NumberState.SIGN;
                        ++i;
                    } else if (c == '0') {
                        nState = NumberState.ZERO;
                        ++i;
                    } else if (c >= '1' && c <= '9') {
                        nState = NumberState.INTEGER;
                        ++i;
                    } else {
                        throw syntaxError("invalid number");
                    }
                }

                case SIGN -> {
                    if (c == '0') {
                        nState = NumberState.ZERO;
                        ++i;
                    } else if (c >= '1' && c <= '9') {
                        nState = NumberState.INTEGER;
                        ++i;
                    } else {
                        throw syntaxError("unexpected end of number");
                    }
                }

                case ZERO -> {
                    if (c == '.') {
                        nState = NumberState.DECIMAL_POINT;
                        ++i;
                    } else if (c >= '0' && c <= '9') {
                        throw syntaxError("invalid number");
                    } else if (c == 'e' || c == 'E') {
                        nState = NumberState.EXPONENT;
                        ++i;
                    } else {
                        tokens.add(new Token(Token.Type.NUMBER, Double.parseDouble(input.substring(start, i)),
                                start, i));
                        nState = NumberState.END;
                    }

                }

                case INTEGER -> {
                    if (c >= '0' && c <= '9') {
                        ++i;
                    } else if (c == '.') {
                        nState = NumberState.DECIMAL_POINT;
                        ++i;
                    } else if (c == 'e' || c == 'E') {
                        nState = NumberState.EXPONENT;
                        ++i;
                    } else {
                        var number = input.substring(start, i);
                        try {
                            tokens.add(new Token(Token.Type.NUMBER, Integer.parseInt(number),
                                    start, i));
                        } catch (NumberFormatException e) {
                            tokens.add(new Token(Token.Type.NUMBER, Long.parseLong(number),
                                    start, i));
                        }

                        nState = NumberState.END;
                    }
                }

                case DECIMAL_POINT -> {
                    if (c >= '0' && c <= '9') {
                        nState = NumberState.FRACTION;
                        ++i;
                    } else {
                        throw syntaxError("unexpected end of number");
                    }
                }

                case FRACTION -> {
                    if (c >= '0' && c <= '9') {
                        ++i;
                    } else if (c == 'e' || c == 'E') {
                        nState = NumberState.EXPONENT;
                        ++i;
                    } else {
                        tokens.add(new Token(Token.Type.NUMBER, Double.parseDouble(input.substring(start, i)),
                                start, i));
                        nState = NumberState.END;
                    }
                }

                case EXPONENT -> {
                    if (c == '+' || c == '-') {
                        nState = NumberState.EXPONENT_SIGN;
                        ++i;
                    } else if (c >= '0' && c <= '9') {
                        nState = NumberState.EXPONENT_INTEGER;
                        ++i;
                    } else {
                        throw syntaxError("unexpected end of number");
                    }
                }

                case EXPONENT_SIGN -> {
                    if (c >= '0' && c <= '9') {
                        nState = NumberState.EXPONENT_INTEGER;
                        ++i;
                    } else {
                        throw syntaxError("unexpected end of number");
                    }
                }

                case EXPONENT_INTEGER -> {
                    if (c >= '0' && c <= '9') {
                        ++i;
                    } else {
                        tokens.add(new Token(Token.Type.NUMBER, Double.parseDouble(input.substring(start, i)),
                                start, i));
                        nState = NumberState.END;
                    }
                }

                case END -> {

                }
            }
        }

        if (nState != NumberState.END && nState != NumberState.EXPONENT_INTEGER && nState != NumberState.INTEGER
                && nState != NumberState.ZERO && nState != NumberState.FRACTION) {
            throw syntaxError("unexpected end of number");
        }
    }

    private void readKeyword() {
        int start = i;

        while (i < input.length()) {
            var c = input.charAt(i);
            if (c >= 'a' && c <= 'z') {
                ++i;
            } else {
                break;
            }
        }

        var keyword = input.substring(start, i);

        if (keyword.length() < 4 || keyword.length() > 5) {
            throw syntaxError("unexpected token", keyword);
        }

        switch (keyword) {
            case "true" -> tokens.add(new Token(Token.Type.BOOLEAN, true, start, i));
            case "false" -> tokens.add(new Token(Token.Type.BOOLEAN, false, start, i));
            case "null" -> tokens.add(new Token(Token.Type.NULL, null, start, i));
            default -> throw syntaxError("unexpected token", keyword);
        }
    }

    private int getLine() {
        return lineOffsets.size();
    }

    private int getColumn() {
        return i - lineOffsets.getLast();
    }

    private JsonSyntaxError syntaxError(String message, String token) {
        var error = "Syntax Error: " + message + " at line " + getLine() + ", column " + getColumn();
        if (token != null) {
            return new JsonSyntaxError(error + ": " + token);
        }
        return new JsonSyntaxError(error);
    }

    private JsonSyntaxError syntaxError(String message, char c) {
        return new JsonSyntaxError(
                "Syntax Error: " + message + " at line " + getLine() + ", column " + getColumn() + ": " + c);
    }

    private JsonSyntaxError syntaxError(String message) {
        return syntaxError(message, null);
    }

}
