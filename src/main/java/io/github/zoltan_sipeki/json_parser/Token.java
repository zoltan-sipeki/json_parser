package io.github.zoltan_sipeki.json_parser;

public class Token {

    public enum Type {
        OBJECT_START,
        OBJECT_END,
        ARRAY_START,
        ARRAY_END,
        STRING,
        NUMBER,
        COMMA,
        COLON,
        BOOLEAN,
        NULL
    }

    private Type type;

    private Object value;

    private int startOffset;

    private int endOffset;

    public Token(Type type, Object value, int startOffset, int endOffset) {
        this.type = type;
        this.value = value;
        this.startOffset = startOffset;
        this.endOffset = endOffset;
    }

    public Type getType() {
        return type;
    }
    
    public Object getValue() {
        return value;
    }

    public int getStartOffset() {
        return startOffset;
    }

    public int getEndOffset() {
        return endOffset;
    }

    @Override
    public String toString() {
        return "Token{" +
                "type=" + type +
                ", value='" + value + '\'' +
                ", startOffset=" + startOffset +
                ", endOffset=" + endOffset +
                '}';
    }

}
