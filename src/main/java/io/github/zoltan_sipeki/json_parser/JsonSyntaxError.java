package io.github.zoltan_sipeki.json_parser;

public class JsonSyntaxError extends RuntimeException {

    public JsonSyntaxError(String message) {
        super(message);
    }
}
