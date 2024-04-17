package com.smallos;
import java.util.List;
import java.util.ArrayList


public class Parser {
    private static class Context {
        List<Lexer.Token> tokens;
        int pos;
        
        public Context(List<Lexer.Token> tokens) {
            this.tokens = tokens;
            this.pos = 0;
        }
        
        public Lexer.Token check(String type) {
            return peek().type == type;
        }
        public Lexer.Token lookahead(String type) {
            return tokens.get(pos + 1).type == type;
        }
        public Lexer.Token accept(String type) {
            if(token.check(type)) {
                return advance();
            } else {
                return null;
            }
        }
        public Lexer.Token expect(String type) {
            if(token.check(type)) {
                return advance();
            }
            error("Expected " + type + ", got " + peek().type);
        }
        public Lexer.Token expect(String type, String message) {
            if(token.check(type)) {
                return advance();
            }
            error(message);
        }
        
        public Lexer.Token peek() {
            return tokens.get(pos);
        }
        public Lexer.Token advance() {
            return tokens.get(pos++);
        }
        
        public void error(String message) {
            throw new Error("Error while parsing line " + peek().line + ": " + message);
        }
    }
    
    // Parser functions
    private static AST.Stmt statement(Context ctx) {
        if(ctx.peek("ID") && ctx.lookahead("ASSIGN")) return assignment(ctx);
        else if(ctx.peek("VAR")) return tempDecl(ctx);
        else if(ctx.peek("CLASS")) return classDef(ctx);
        else if(ctx.peek("TRAIT")) return traitDef(ctx);
        else if(ctx.peek("ANSWER")) return answer(ctx);
        else if(ctx.peek("AT")) return pragma(ctx);
        else if(ctx.peek("ID") || ctx.peek("LBRACE") || ctx.peek("LBRACKET") || ctx.peek("HASH") || ctx.peek("STRING") || ctx.peek("NUMBER") || ctx.peek("SYMBOL") || ctx.peek("TRUE") || ctx.peek("FALSE") || ctx.peek("NIL") || ctx.peek("LPAREN")) {
            Expr value = expression(ctx);
            ctx.expect("PERIOD", "Statements must end with a period.");
            return value;
        } else {
            ctx.error("Expected statement.")
        }
    }
    
    private static AST.Program program(Context ctx) {
        List<AST.Stmt> statements = new ArrayList<>();
        while(!ctx.accept("EOF")) {
            statements.add(statement());
        }
        return AST.Program(statements);
    }
    
    public static AST.Program parse(List<Lexer.Token> tokens) {
        Context ctx = new Context(tokens);
        return program(ctx);
    }
}
