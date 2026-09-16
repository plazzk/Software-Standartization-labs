import java.util.*;
import java.util.regex.*;

/**
 * Лексический анализатор (токенизатор) для программ на языке C#.
 * Каждая лексема классифицируется как ОПЕРАТОР или ОПЕРАНД
 * в соответствии с интерпретацией Холстеда.
 */
public class CSharpTokenizer {

    public enum TokenType { OPERATOR, OPERAND }

    public static class Token {
        public final String text;
        public final TokenType type;
        Token(String text, TokenType type) { this.text = text; this.type = type; }
    }

    // Ключевые слова C#, трактуемые как операторы Холстеда
    private static final Set<String> KEYWORDS = new HashSet<>(Arrays.asList(
            "abstract", "as", "base", "bool", "break", "byte", "case", "catch",
            "char", "checked", "class", "const", "continue", "decimal", "default",
            "delegate", "do", "double", "else", "enum", "event", "explicit",
            "extern", "finally", "fixed", "float", "for", "foreach", "goto",
            "if", "implicit", "in", "int", "interface", "internal", "is",
            "lock", "long", "namespace", "new", "object", "operator", "out",
            "override", "params", "private", "protected", "public", "readonly",
            "ref", "return", "sbyte", "sealed", "short", "sizeof", "stackalloc",
            "static", "string", "struct", "switch", "this", "throw", "try",
            "typeof", "uint", "ulong", "unchecked", "unsafe", "ushort", "using",
            "virtual", "void", "volatile", "while", "var", "async", "await",
            "yield", "partial", "get", "set", "nameof"
    ));

    // Слова-литералы (являются операндами, хотя выглядят как идентификаторы)
    private static final Set<String> LITERAL_WORDS = new HashSet<>(Arrays.asList(
            "true", "false", "null"
    ));

    // Парные скобки считаются ОДНИМ оператором (правило Холстеда)
    private static final Map<String, String> PAIR_OPEN_NAME = new HashMap<>();
    static {
        PAIR_OPEN_NAME.put("(", "( )");
        PAIR_OPEN_NAME.put("[", "[ ]");
        PAIR_OPEN_NAME.put("{", "{ }");
    }
    private static final Set<String> PAIR_CLOSE = new HashSet<>(Arrays.asList(")", "]", "}"));

    private static final String COMMENT_LINE = "//[^\\n]*";
    private static final String COMMENT_BLOCK = "/\\*.*?\\*/";
    private static final String VERBATIM_STRING = "@\"(?:[^\"]|\"\")*\"";
    private static final String INTERP_STRING = "\\$\"(?:\\\\.|[^\"\\\\])*\"";
    private static final String STRING_LIT = "\"(?:\\\\.|[^\"\\\\])*\"";
    private static final String CHAR_LIT = "'(?:\\\\.|[^'\\\\])'";
    private static final String NUMBER_LIT =
            "\\d+\\.\\d+(?:[eE][+-]?\\d+)?[fFdDmM]?|0[xX][0-9a-fA-F]+[UuLl]*|\\d+(?:[eE][+-]?\\d+)?[UuLlFfDdMm]*";
    private static final String MULTI_OP =
            "<<=|>>=|<<|>>|<=|>=|==|!=|&&|\\|\\||\\+\\+|--|\\+=|-=|\\*=|/=|%=|&=|\\|=|\\^=|=>|->|\\?\\?|\\?\\.|::";
    private static final String IDENT = "[A-Za-z_@][A-Za-z0-9_]*";
    private static final String SINGLE_OP = "[-+*/%=<>()\\[\\]{},;:|^!&.~?]";

    private static final Pattern TOKEN_PATTERN = Pattern.compile(
            COMMENT_LINE + "|" + COMMENT_BLOCK + "|" +
            VERBATIM_STRING + "|" + INTERP_STRING + "|" + STRING_LIT + "|" + CHAR_LIT + "|" +
            NUMBER_LIT + "|" +
            MULTI_OP + "|" +
            IDENT + "|" +
            SINGLE_OP,
            Pattern.DOTALL);

    public static List<Token> tokenize(String source) {
        List<Token> tokens = new ArrayList<>();
        Matcher m = TOKEN_PATTERN.matcher(source);

        while (m.find()) {
            String t = m.group();

            // пропускаем комментарии
            if (t.startsWith("//") || t.startsWith("/*")) {
                continue;
            }

            // строковые и символьные литералы — операнды
            if (t.startsWith("\"") || t.startsWith("@\"") || t.startsWith("$\"") || t.startsWith("'")) {
                tokens.add(new Token(t, TokenType.OPERAND));
                continue;
            }

            // числовые литералы — операнды
            if (Character.isDigit(t.charAt(0))) {
                tokens.add(new Token(t, TokenType.OPERAND));
                continue;
            }

            // парные скобки — один оператор на пару
            if (PAIR_OPEN_NAME.containsKey(t)) {
                tokens.add(new Token(PAIR_OPEN_NAME.get(t), TokenType.OPERATOR));
                continue;
            }
            if (PAIR_CLOSE.contains(t)) {
                continue; // закрывающая скобка уже учтена при открывающей
            }

            // идентификаторы / ключевые слова
            if (t.matches(IDENT)) {
                String low = t.toLowerCase(Locale.ROOT);
                if (KEYWORDS.contains(low)) {
                    tokens.add(new Token(t, TokenType.OPERATOR));
                    continue;
                }
                if (LITERAL_WORDS.contains(low)) {
                    tokens.add(new Token(t, TokenType.OPERAND));
                    continue;
                }
                // если сразу после идентификатора следует '(' — это вызов
                // метода/конструктора => оператор, иначе — операнд (переменная)
                int idx = m.end();
                while (idx < source.length() && Character.isWhitespace(source.charAt(idx))) {
                    idx++;
                }
                if (idx < source.length() && source.charAt(idx) == '(') {
                    tokens.add(new Token(t, TokenType.OPERATOR));
                } else {
                    tokens.add(new Token(t, TokenType.OPERAND));
                }
                continue;
            }

            // всё остальное (операторные знаки) — операторы
            tokens.add(new Token(t, TokenType.OPERATOR));
        }
        return tokens;
    }
}
