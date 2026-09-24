package io.github.zoltan_sipeki.json_parser;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@SuppressWarnings("unchecked")
public class Json {

    private Object object;

    public Json() {
    }

    Json(Object object) {
        this.object = object;
    }

    public static Json parse(String content) {
        var tokenizer = new Tokenizer(content);
        var result = tokenizer.tokenize();
        var parser = new Parser(result);
        return new Json(parser.parse());
    }

    public static String stringify(Json json) {
        return json.stringify();
    }

    public List<Json> asList() {
        return convert(List.class).stream().map(Json::new).toList();
    }

    public int asInt() {
        return convert(int.class).intValue();
    }

    public double asDouble() {
        return convert(double.class).doubleValue();
    }

    public float asFloat() {
        return convert(float.class).floatValue();
    }

    public long asLong() {
        return convert(long.class).longValue();
    }

    public String asString() {
        return convert(String.class);
    }

    public boolean asBoolean() {
        return convert(boolean.class);
    }

    public UUID asUUID() {
        if (object instanceof UUID) {
            return (UUID) object;
        }
        try {
            return UUID.fromString((String) object);
        } catch (IllegalArgumentException | ClassCastException e) {
            throw new JsonException("Json (" + object.getClass().getTypeName() + ") is not a UUID");
        }
    }

    public Instant asInstant() {
        return convertTime(Instant.class);
    }

    public OffsetDateTime asOffsetDateTime() {
        return convertTime(OffsetDateTime.class);
    }

    public LocalDate asLocalDate() {
        return convertTime(LocalDate.class);
    }

    public LocalDateTime asLocalDateTime() {
        return convertTime(LocalDateTime.class);
    }

    public String getString(String key) {
        var map = (Map<String, Object>) convert(Map.class);
        return convert(String.class, key, map.get(key));
    }

    public boolean getBoolean(String key) {
        var map = (Map<String, Object>) convert(Map.class);
        return convert(boolean.class, key, map.get(key));
    }

    public Json get(String key) {
        var map = (Map<String, Object>) convert(Map.class);
        return new Json(map.get(key));
    }

    public int getInt(String key) {
        var map = (Map<String, Object>) convert(Map.class);
        return convert(int.class, key, map.get(key)).intValue();
    }

    public long getLong(String key) {
        var map = (Map<String, Object>) convert(Map.class);
        return convert(long.class, key, map.get(key)).longValue();
    }

    public double getDouble(String key) {
        var map = (Map<String, Object>) convert(Map.class);
        return convert(long.class, key, map.get(key)).doubleValue();
    }

    public float getFloat(String key) {
        var map = (Map<String, Object>) convert(Map.class);
        return convert(long.class, key, map.get(key)).floatValue();
    }

    public UUID getUUID(String key) {
        var map = (Map<String, Object>) convert(Map.class);
        Object value = null;
        try {
            value = map.get(key);
            if (value instanceof UUID) {
                return (UUID) value;
            }

            return UUID.fromString((String) value);
        } catch (IllegalArgumentException | ClassCastException e) {
            throw new JsonException("Json (" + value.getClass().getTypeName() + ") is not a UUID");
        }
    }

    public LocalDate getLocalDate(String key) {
        var map = (Map<String, Object>) convert(Map.class);
        return convertTime(LocalDate.class, key, map.get(key));
    }

    public LocalDateTime getLocalDateTime(String key) {
        var map = (Map<String, Object>) convert(Map.class);
        return convertTime(LocalDateTime.class, key, map.get(key));
    }

    public OffsetDateTime getOffsetDateTime(String key) {
        var map = (Map<String, Object>) convert(Map.class);
        return convertTime(OffsetDateTime.class, key, map.get(key));
    }

    public Instant getInstant(String key) {
        var map = (Map<String, Object>) convert(Map.class);
        return convertTime(Instant.class, key, map.get(key));
    }

    public Json put(Object object) {
        if (object instanceof Json j) {
            this.object = j.object;
        } else {
            this.object = object;
        }
        return this;
    }

    public Json put(String key, Object object) {
        if (!(object instanceof Map)) {
            object = new HashMap<String, Object>();
        }

        var map = (Map<String, Object>) object;
        if (object instanceof Json j) {
            map.put(key, j.object);
        } else {
            map.put(key, object);
        }
        return this;
    }

    private String stringify() {
        return stringify(object);
    }

    private String stringify(Object o) {
        var sb = new StringBuilder();

        if (o instanceof String s) {
            sb.append('\"');
            sb.append(escape(s));
            sb.append('\"');
        } else if (o instanceof Map) {
            var m = (Map<String, Object>) o;
            sb.append("{");

            int i = 0;
            for (var entry : m.entrySet()) {
                sb.append('\"');
                sb.append(escape(entry.getKey()));
                sb.append('\"');
                sb.append(":");
                sb.append(stringify(entry.getValue()));
                if (i++ < m.size() - 1) {
                    sb.append(",");
                }
            }

            sb.append("}");
        } else if (o instanceof Collection c) {
            sb.append("[");

            int i = 0;
            for (var item : c) {
                sb.append(stringify(item));
                if (i++ < c.size() - 1) {
                    sb.append(",");
                }
            }

            sb.append("]");
        } else if (o == null) {
            sb.append("null");
        } else if (!(o instanceof Number n) || Double.isFinite(n.doubleValue())) {
            sb.append(o);
        } else {
            throw new JsonException("Cannot serialize non-finite number");
        }

        return sb.toString();

    }

    private StringBuilder escape(String str) {
        var sb = new StringBuilder();

        for (int i = 0; i < str.length(); i++) {
            var c = str.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append("\\u" + String.format("%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }

        return sb;
    }

    private <T> T convert(Class<T> clazz) {
        try {
            return (T) object;
        } catch (ClassCastException e) {
            throw new JsonException(
                    "Json (" + object.getClass().getTypeName() + ") is not a " + clazz.getTypeName());
        }
    }

    private <T> T convert(Class<T> clazz, String fieldName, Object value) {
        try {
            return (T) value;
        } catch (ClassCastException e) {
            throw new JsonException(
                    "Field '" + fieldName + "' (" + value.getClass().getTypeName() + ") is not a "
                            + clazz.getTypeName());
        }
    }

    private <T> T convertTime(Class<T> clazz) {
        try {
            return (T) clazz.getDeclaredMethod("parse", String.class).invoke(null, (String) object);
        } catch (DateTimeParseException | ClassCastException e) {
            throw new JsonException(
                    "Json (" + object.getClass().getTypeName() + ") is not a " + clazz.getTypeName());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private <T> T convertTime(Class<T> clazz, String fieldName, Object value) {
        try {
            return (T) clazz.getDeclaredMethod("parse", CharSequence.class).invoke(null,
                    (String) object);

        } catch (DateTimeParseException | ClassCastException e) {
            throw new JsonException(
                    "Field '" + fieldName + "' (" + value.getClass().getTypeName() + ") is not a "
                            + clazz.getTypeName());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}
