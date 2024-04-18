package com.smallos;
import java.util.List;
import java.util.ArrayList;


public class Parser {
    private static class Context {
        List<Lexer.Token> tokens;
        int pos;
        
        public Context(List<Lexer.Token> tokens) {
            this.tokens = tokens;
            this.pos = 0;
        }
        
        public boolean check(String type) {
            return peek().type() == type;
        }
        public boolean lookahead(String type) {
            return tokens.get(pos + 1).type() == type;
        }
        
        public Lexer.Token accept(String type) {
            if(check(type)) {
                return advance();
            } else {
                return null;
            }
        }
        public Lexer.Token expect(String type) {
            if(check(type)) {
                return advance();
            }
            error("Expected " + type + ", got " + peek().type());
        }
        public Lexer.Token expect(String type, String message) {
            if(check(type)) {
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
            throw new Error("Error while parsing line " + peek().lineNum() + ": " + message);
        }
    }
    
    // Parser functions
    
    // Values
    private static AST.Value literal(Context ctx) {
        return null;
    }
    
    private static AST.Identifier identifier(Context ctx) {
        return null;
    }
    
    private static AST.Chain chain(Context ctx) {
        return null;
    }
    
    private static AST.ByteArray byteArray(Context ctx) {
        return null;
    }
    
    private static AST.Block block(Context ctx) {
        return null;
    }
    
    private static AST._Array array(Context ctx) {
        return null;
    }
    
    private static AST.Value value(Context ctx) {
        return null;
    }
    
    // Messages
    private static AST.UnaryMessage unaryMessage(Context ctx) {
        return null;
    }
    
    private static AST.BinaryMessage binaryMessage(Context ctx) {
        return null;
    }
    
    private static AST.KeywordMessage keywordMessage(Context ctx) {
        return null;
    }
    
    private static AST.Message message(Context ctx) {
        return null;
    }
    
    // Expressions
    private static AST.Expr unaryExpression(Context ctx) {
        return null;
    }
    
    private static AST.Expr binaryExpression(Context ctx) {
        return null;
    }
    
    private static AST.Expr keywordExpression(Context ctx) {
        return null;
    }
    
    private static AST.Cascade cascade(Context ctx) {
        return null;
    }
    
    private static AST.Expr expression(Context ctx) {
        return null;
    }
    
    // Members
    private static AST.Signature signature(Context ctx) {
        return null;
    }
    
    private static AST.Requirement requirement(Context ctx) {
        ctx.expect("REQUIRE");
        AST.Signature sig = signature(ctx);
        ctx.expect("PERIOD");
        return AST.Requirement(sig);
    }
    
    private static AST.Method method(Context ctx) {
        boolean isStatic = ctx.accept("STATIC") != null;
        ctx.expect("DEF");
        AST.Signature sig = signature(ctx);
        ctx.expect("AS");
        List<AST.Stmt> statements = new ArrayList<>();
        while(ctx.accept("END") == null) {
            statements.add(statement(ctx));
        }
        return new AST.Method(isStatic, sig, statements);
    }
    
    private static AST.Field field(Context ctx) {
        boolean isStatic = ctx.accept("STATIC") != null;
        ctx.expect("VAR");
        String name = ctx.expect("ID").value();
        AST.Expr val = new AST.Nil();
        if(ctx.accept("ASSIGN") != null) val = expression(ctx);
        ctx.expect("PERIOD", "Statements must be ended with a period.");
        return new AST.Field(isStatic, name, val);
    }
    
    private static AST.Member member(Context ctx) {
        if(ctx.check("STATIC")) {
            if(ctx.lookahead("DEF")) return method(ctx);
            else if(ctx.lookahead("VAR")) return field(ctx);
            else if(ctx.lookahead("REQUIRE")) ctx.error("Requirements cannot be static.");
            else ctx.error("Expected method or field after 'static' token.");
        } 
        else if(ctx.check("DEF")) return method(ctx);
        else if(ctx.check("VAR")) return field(ctx);
        else if(ctx.check("REQUIRE")) return requirement(ctx);
        else if(ctx.check("AT")) return pragma(ctx);
        else ctx.error("Expected one of: method, field, requirement, pragma in class/trait body.");
    }
    
    // Statements
    private static AST.Pragma pragma(Context ctx) {
        ctx.expect("AT");
        AST.Message val = message(ctx);
        return new AST.Pragma(val);
    }
    
    private static AST.Answer answer(Context ctx) {
        ctx.expect("ANSWER");
        AST.Expr val = expression(ctx);
        ctx.expect("PERIOD", "Statements must be ended with a period.");
    }
    
    private static AST.TraitDef traitDef(Context ctx) {
        ctx.expect("TRAIT");
        String name = ctx.expect("ID").value();
        AST.Identifier parent = null;
        if(ctx.accept("EXTENDING") != null) {
            parent = identifier(ctx);
        }
        List<AST.Member> members = new ArrayList<>();
        while(ctx.accept("END") == null) {
            AST.Member val = member(ctx);
            if(val instanceof AST.Field) {
                ctx.error("Traits cannot contain fields.");
            }
            members.add(member(ctx));
        }
    }
    
    private static AST.ClassDef classDef(Context ctx) {
        ctx.expect("CLASS");
        String name = ctx.expect("ID").value();
        AST.Identifier parent = null;
        if(ctx.accept("EXTENDING") != null) {
            parent = identifier(ctx);
        }
        List<AST.Identifier> traits = new ArrayList<>();
        if(ctx.accept("IMPLEMENTING") != null) {
            traits.add(identifier(ctx));
            while(ctx.accept("COMMA") != null) {
                traits.add(identifier(ctx));
            }
        }
        ctx.expect("IS");
        List<AST.Member> members = new ArrayList<>();
        while(ctx.accept("END") == null) {
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
        AST.Identifier name = identifier(ctx);
        AST.Expr val = new AST.Nil();
        if(ctx.accept("ASSIGN") != null) val = expression(ctx);
        ctx.expect("PERIOD", "Statements must be ended with a period.");
        return new AST.TempDecl(name, val);
    }
    
    private static AST.Assignment assignment(Context ctx) {
        AST.Identifier name = identifier(ctx);
        ctx.expect("ASSIGN");
        AST.Expr val = expression(ctx);
        ctx.expect("PERIOD", "Statements must be ended with a period.");
        return new AST.Assignment(name, val);
    }
    
    private static AST.Stmt statement(Context ctx) {
        if(ctx.check("ID") && ctx.lookahead("ASSIGN")) return assignment(ctx);
        else if(ctx.check("VAR")) return tempDecl(ctx);
        else if(ctx.check("CLASS")) return classDef(ctx);
        else if(ctx.check("TRAIT")) return traitDef(ctx);
        else if(ctx.check("ANSWER")) return answer(ctx);
        else if(ctx.check("AT")) return pragma(ctx);
        else if(ctx.check("ID") || ctx.check("LBRACE") || ctx.check("LBRACKET") || ctx.check("HASH") || ctx.check("STRING") || ctx.check("NUMBER") || ctx.check("SYMBOL") || ctx.check("TRUE") || ctx.check("FALSE") || ctx.check("NIL") || ctx.check("LPAREN")) {
            AST.Expr val = expression(ctx);
            ctx.expect("PERIOD", "Statements must end with a period.");
            return val;
        } else {
            ctx.error("Expected statement.");
        }
    }
    
    private static AST.Program program(Context ctx) {
        List<AST.Stmt> statements = new ArrayList<>();
        while(ctx.accept("EOF") == null) {
            statements.add(statement(ctx));
        }
        return new AST.Program(statements);
    }
    
    public static AST.Program parse(List<Lexer.Token> tokens) {
        Context ctx = new Context(tokens);
        return program(ctx);
    }
}
