package com.smallos;
import java.util.regex.*;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import static java.util.Map.entry;

public class Lexer {
    public static record Token(int lineNum, String type, String value) {
        public String toString() {
            return String.format("(<%s> %s @ %s)", type, value, lineNum);
        }
    };
    
    final static List<Map.Entry<String,String>> dict = List.of(
        entry("COMMENT", "//.*$"),
        entry("SYMBOL", "#[a-zA-Z_$][a-zA-Z_$0-9]*"),
        entry("ASSIGN", ":="),
        entry("PERIOD", "\\."),
        entry("COLON", ":"),
        entry("SEMICOLON", ";"),
        entry("COMMA", ","),
        entry("HASH", "#"),
        entry("LPAREN", "\\("),
        entry("RPAREN", "\\)"),
        entry("LBRACKET", "\\["),
        entry("RBRACKET", "\\]"),
        entry("LBRACE", "\\{"),
        entry("RBRACE", "\\}"),
        entry("ANSWER", "\\^"),
        entry("PIPE", "\\|"),
        entry("AT", "@"),
        entry("STRING", "\"(?:[^\\\"]|\\\"\\\")+\""),
        entry("BYTE", "x[0-9A-Fa-f]{2}"),
        entry("NUMBER", "[-+]?\\d+(?:\\.\\d+)?"),
        entry("ID", "[a-zA-Z_][a-zA-Z0-9_]*"),
        entry("BINOP", "[-+/*=<>!]+"),
        entry("NEWLINE", "\\n"),
        entry("SKIP", "[ \\t]+"),
        entry("MISMATCH", ".")
    );
    
    final static Pattern pattern = Pattern.compile(dict.stream()
        .map(entry -> String.format("(?<%s>%s)", entry.getKey(), entry.getValue()))
        .collect(Collectors.joining("|")), 
        Pattern.MULTILINE
    );
    final static String[] groups = dict.stream().map(entry -> entry.getKey()).toArray(String[]::new);
    final static String[] keywords = {"class", "trait", "extending", "implementing", "is", "as", "static", "var", "def", "end", "require", "true", "false", "nil"};
    
    private static String findGroupName(Matcher matcher) {
        for (String group : groups) {
            if (matcher.group(group) != null) {
                return group;
            }
        }
        throw new IllegalStateException("Unknown lexer state.");
    }
    
    public static List<Token> tokenize(String text) {
        ArrayList<Token> tokens = new ArrayList<>();
        Matcher m = pattern.matcher(text);
        
        int line = 1;
        while(m.find()) {
            String type = findGroupName(m);
            String value = m.group();
            
            if (type == "ID" && Arrays.asList(keywords).contains(value)) {
                type = value.toUpperCase();
            } else if(type == "COMMENT") {
                continue;
            } else if(type == "NEWLINE") {
                line++;
                continue;
            } else if(type == "SKIP") {
                continue;
            } else if(type == "MISMATCH") {
                throw new SyntaxError("Unknown character at line " + line + ":" + value);
            }
            tokens.add(new Token(line, type, value));
        }
        tokens.add(new Token(line, "EOF", ""));
        
        return tokens;
    }
}
