package com.smallos;
import java.util.regex.*;
import java.util.ArrayList;
import java.util.Arrays;

public class Lexer {
    public static record Token(int lineNum, String type, Object value) {};
    
    final String regex = "(?<COMMENT>\\/\\/.*$)|(?<SYMBOL>#[a-zA-Z_$][a-zA-Z_$0-9]*)|(?<ASSIGN>:=)|(?<PERIOD>\\.)|(?<COLON>:)|(?<SEMICOLON>;)|(?<COMMA>,)|(?<HASH>#)|(?<LPAREN>\\()|(?<RPAREN>\\))|(?<LBRACKET>\\[)|(?<RBRACKET>\\])|(?<LBRACE>\\{)|(?<RBRACE>\\})|(?<ANSWER>\\^)|(?<PIPE>\\|)|(?<AT>@)|(?<STRING>\\\"(?:[^\\\"]|\\\"\\\")+\\\")|(?<BYTE>x[0-9A-Fa-f]{2})|(?<NUMBER>[-+]?\\d+(?:\\.\\d+)?)|(?<ID>[a-zA-Z_][a-zA-Z0-9_]*)|(?<BINOP>[-+/*=<>!]+)|(?<NEWLINE>\\n)|(?<SKIP>[ \\t]+)|(?<MISMATCH>.)";
    final Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);

    final String[] keywords = {"class", "trait", "extending", "implementing", "is", "as", "static", "var", "def", "end", "require", "true", "false", "nil"};
    
    private static String findGroupName(Matcher matcher) {
        for (String groupName : new String[] {"COMMENT", "SYMBOL", "ASSIGN", "PERIOD", "COLON", "SEMICOLON", "COMMA", "HASH", "LPAREN", "RPAREN", "LBRACKET", "RBRACKET", "LBRACE", "RBRACE", "ANSWER", "PIPE", "AT", "STRING", "BYTE", "NUMBER", "ID", "BINOP", "NEWLINE", "SKIP", "MISMATCH"}) {
            if (matcher.group(groupName) != null) {
                return groupName;
            }
        }
    }
    
    public static Token[] tokenize(String text) {
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
        
        return tokens.toArray();
    }
}
