package io.github.zoltan_sipeki.json_parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Parser {

    private int i = 0;

    private List<Token> tokens;

    private List<Integer> lineOffsets;

    public Parser(Tokenizer.Result tokenizerResult) {
        this.tokens = tokenizerResult.tokens();
        this.lineOffsets = tokenizerResult.lineOffsets();
    }

    public Object parse() {
        var token = tokens.get(i);
        switch (token.getType()) {
            case OBJECT_START -> {
                ++i;
                return parseObject();
            }
            case ARRAY_START -> {
                ++i;
                return parseArray();
            }
            case STRING -> {
                ++i;
                return token.getValue();
            }
            case NUMBER -> {
                ++i;
                return token.getValue();
            }
            case BOOLEAN -> {
                ++i;
                return token.getValue();
            }
            case NULL -> {
                ++i;
                return token.getValue();
            }

            default -> {
                throw syntaxError("unexpected token");
            }
        }
    }

    private Map<String, Object> parseObject() {
        var object = new HashMap<String, Object>();

        if (tokens.get(i).getType() == Token.Type.OBJECT_END) {
            ++i;
            return object;
        }

        parseEntries(object);

        if (tokens.get(i).getType() != Token.Type.OBJECT_END) {
            throw syntaxError("expected comma or '}'");
        }

        ++i;

        return object;
    }

    private void parseEntries(Map<String, Object> object) {
        parseEntry(object);

        if (tokens.get(i).getType() == Token.Type.COMMA) {
            ++i;
            parseEntries(object);
        }
    }

    private void parseEntry(Map<String, Object> object) {
        var token = tokens.get(i);
        if (token.getType() != Token.Type.STRING) {
            throw syntaxError("expected property name");
        }

        ++i;

        if (tokens.get(i).getType() != Token.Type.COLON) {
            throw syntaxError("expected ':'");
        }
        ++i;

        var value = parse();
        object.put((String) token.getValue(), value);
    }

    private List<Object> parseArray() {
        var list = new ArrayList<Object>();
        if (tokens.get(i).getType() == Token.Type.ARRAY_END) {
            return list;
        }

        parseItems(list);

        if (tokens.get(i).getType() != Token.Type.ARRAY_END) {
            throw syntaxError("expected comma or ']'");
        }

        ++i;

        return list;
    }

    private void parseItems(List<Object> list) {
        var item = parse();
        list.add(item);

        if (tokens.get(i).getType() == Token.Type.COMMA) {
            ++i;
            parseItems(list);
        }
    }

    private int getLine(int offset) {
        int index = Collections.binarySearch(lineOffsets, offset);
        return index > 0 ? index + 1 : -index - 1;
    }

    private int getColumn(int line, int offset) {
        return offset - lineOffsets.get(line - 1);
    }

    private JsonSyntaxError syntaxError(String message) {
        var token = tokens.get(i - 1);
        int line = getLine(token.getEndOffset());
        int column = getColumn(line, token.getEndOffset());

        return new JsonSyntaxError("Syntax Error: " + message + " at line " + line + ", column " + column);
    }
}
