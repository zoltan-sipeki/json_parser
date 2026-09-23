package io.github.zoltan_sipeki;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import io.github.zoltan_sipeki.json_parser.Parser;
import io.github.zoltan_sipeki.json_parser.Tokenizer;

public class Main {

    public static void main(String[] args) throws IOException, NoSuchFieldException, SecurityException {
        var content = Files.readString(Path.of(args[0]));

        var tokenizer = new Tokenizer(content);
        var result = tokenizer.tokenize();

        var parser = new Parser(result);

        var json = parser.parse();
        System.out.println(json);

    }
}