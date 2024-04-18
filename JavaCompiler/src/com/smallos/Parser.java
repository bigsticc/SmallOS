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
    
    private static AST.Signature signature(Context ctx) {
        
    }
    
    private static AST.Method method(Context ctx) {
        boolean isStatic = ctx.accept("STATIC") != null;
        ctx.expect("DEF");
        AST.Signature sig = signature(ctx);
        ctx.expect("AS");
        List<Stmt> statements = new ArrayList<>();
        while(!ctx.accept("END")) {
            statements.add(statement(ctx));
        }
        return new AST.Method(isStatic, sig, statements);
    }
    
    private static AST.Field field(Context ctx) {
        boolean isStatic = ctx.accept("STATIC") != null;
        ctx.expect("VAR");
        String name = ctx.expect("ID").value;
        AST.Expr val = new AST.Nil();
        if(ctx.accept("ASSIGN")) val = expression(ctx);
        ctx.expect("PERIOD", "Statements must be ended with a period.");
        return new AST.Field(isStatic, name, val);
    }
    
    private static AST.Member member(Context ctx) {
        if(ctx.peek("STATIC")) {
            if(ctx.lookahead("DEF")) return method(ctx);
            else if(ctx.lookahead("VAR")) return field(ctx);
            else if(ctx.lookahead("REQUIRE")) ctx.error("Requirements cannot be static.");
            else ctx.error("Expected method or field after 'static' token.");
        } 
        else if(ctx.peek("DEF")) return method(ctx);
        else if(ctx.peek("VAR")) return field(ctx);
        else if(ctx.peek("REQUIRE")) return require(ctx);
        else if(ctx.peek("AT")) return pragma(ctx);
        else ctx.error("Expected one of: method, field, requirement, pragma in class/trait body.");
    }
    
    // Statements
    private static AST.Pragma pragma(Context ctx) {
        ctx.expect("AT");
        AST.Expr val = expression(ctx);
        return new AST.Pragma(val);
    }
    
    private static AST.Answer answer(Context ctx) {
        ctx.expect("ANSWER");
        AST.Expr val = expression(ctx);
        ctx.expect("PERIOD", "Statements must be ended with a period.");
    }
    
    private static AST.TraitDef traitDef(Context ctx) {
        ctx.expect("TRAIT");
        String name = ctx.expect("ID").value;
        AST.Identifier parent = new AST.Nil();
        if(ctx.accept("EXTENDING")) {
            parent = identifier(ctx);
        }
        List<AST.Member> members = new ArrayList<>();
        while(!ctx.accept("END")) {
            AST.Member val = member(ctx);
            if(val instanceof AST.Field) {
                ctx.error("Traits cannot contain fields.");
            }
            members.add(member(ctx));
        }
    }
    
    private static AST.ClassDef classDef(Context ctx) {
        ctx.expect("CLASS");
        String name = ctx.expect("ID").value;
        AST.Identifier parent = new AST.Nil();
        if(ctx.accept("EXTENDING")) {
            parent = identifier(ctx);
        }
        List<AST.Identifier> traits = new ArrayList<>();
        if(ctx.accept("IMPLEMENTING")) {
            traits.add(identifier(ctx));
            while(ctx.accept("COMMA")) {
                traits.add(identifier(ctx));
            }
        }
        ctx.expect("IS");
        List<AST.Member> members = new ArrayList<>();
        while(!ctx.accept("END")) {
            AST.Member val = member(ctx);
            if(val instanceof AST.Requirement) {
                ctx.error("Classes cannot contain requirements.");
            }
            members.add(member(ctx));
        }
        return new AST.ClassDef(name, parent, traits, members);
    }
    
    private static AST.TempDecl tempDecl(Context ctx) {
        ctx.expect("VAR");
        String name = ctx.expect("ID").value;
        AST.Expr val = new AST.Nil();
        if(ctx.accept("ASSIGN")) val = expression(ctx);
        ctx.expect("PERIOD", "Statements must be ended with a period.");
        return new AST.TempDecl(name, val);
    }
    
    private static AST.Assignment assignment(Context ctx) {
        String name = ctx.expect("ID").value;
        ctx.expect("ASSIGN");
        AST.Expr val = expression(ctx);
        ctx.expect("PERIOD", "Statements must be ended with a period.");
        return new AST.Assignment(name, val);
    }
    
    private static AST.Stmt statement(Context ctx) {
        if(ctx.peek("ID") && ctx.lookahead("ASSIGN")) return assignment(ctx);
        else if(ctx.peek("VAR")) return tempDecl(ctx);
        else if(ctx.peek("CLASS")) return classDef(ctx);
        else if(ctx.peek("TRAIT")) return traitDef(ctx);
        else if(ctx.peek("ANSWER")) return answer(ctx);
        else if(ctx.peek("AT")) return pragma(ctx);
        else if(ctx.peek("ID") || ctx.peek("LBRACE") || ctx.peek("LBRACKET") || ctx.peek("HASH") || ctx.peek("STRING") || ctx.peek("NUMBER") || ctx.peek("SYMBOL") || ctx.peek("TRUE") || ctx.peek("FALSE") || ctx.peek("NIL") || ctx.peek("LPAREN")) {
            Expr val = expression(ctx);
            ctx.expect("PERIOD", "Statements must end with a period.");
            return val;
        } else {
            ctx.error("Expected statement.")
        }
    }
    
    private static AST.Program program(Context ctx) {
        List<AST.Stmt> statements = new ArrayList<>();
        while(!ctx.accept("EOF")) {
            statements.add(statement());
        }
        return new AST.Program(statements);
    }
    
    public static AST.Program parse(List<Lexer.Token> tokens) {
        Context ctx = new Context(tokens);
        return program(ctx);
    }
}
