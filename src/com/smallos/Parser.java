package com.smallos;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

public class Parser {
    private static class Context {
        List<Lexer.Token> tokens;
        int pos;
        
        public Context(List<Lexer.Token> tokens) {
            this.tokens = tokens;
            this.pos = 0;
        }
        
        public boolean check(String type) {
            return peek().type().equals(type);
        }
        public boolean lookahead(String type) {
            return tokens.get(pos + 1).type().equals(type);
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
            return null;
        }
        public Lexer.Token expect(String type, String message) {
            if(check(type)) {
                return advance();
            }
            error(message);
            return null;
        }
        
        public Lexer.Token peek() {
            return tokens.get(pos);
        }
        public Lexer.Token advance() {
            return tokens.get(pos++);
        }
        
        public void error(String message) {
            throw new SyntaxError("Error while parsing line " + peek().lineNum() + ": " + message + " latest token:" + peek());
        }
    }
    
    // Parser functions
    
    // Values
    private static AST.Value literal(Context ctx) {
        if(ctx.check("NUMBER")) return new AST.Num(Double.parseDouble(ctx.expect("NUMBER").value()));
        else if(ctx.check("STRING")) return new AST.Str(ctx.expect("STRING").value());
        else if(ctx.check("SYMBOL")) return new AST.Symbol(ctx.expect("SYMBOL").value());
        else if(ctx.check("TRUE")) return new AST.Bool(true);
        else if(ctx.check("FALSE")) return new AST.Bool(false);
        else if(ctx.check("NIL")) return new AST.Nil();

        return null;
    }
    
    private static AST.Identifier identifier(Context ctx) {
        return new AST.Identifier(ctx.expect("ID").value());
    }
    
    private static AST.ByteBlock byteBlock(Context ctx) {
        ctx.expect("LBRACKET");
        List<Byte> bytes = new ArrayList<>();
        while(ctx.check("BYTE")) {
            bytes.add(Byte.parseByte(ctx.accept("BYTE").value()));
        }
        return new AST.ByteBlock(bytes.toArray(new Byte[bytes.size()]));
    }
    
    private static AST.Block block(Context ctx) {
        ctx.expect("LBRACKET");
        if(ctx.check("COLON")) {
            ctx.expect("COLON");
            List<AST.Identifier> args = new ArrayList<>();
            while(ctx.check("ID")) {
                args.add(identifier(ctx));
            }
            ctx.expect("PIPE");
            List<AST.Stmt> statements = new ArrayList<>();
            while(ctx.check("NEWLINE")) {
                statements.add(statement(ctx));
            }
            ctx.expect("RBRACKET");
            return new AST.Block(args, statements);
        } else {
            List<AST.Stmt> statements = new ArrayList<>();
            while(ctx.check("NEWLINE")) {
                statements.add(statement(ctx));
            }
            ctx.expect("RBRACKET");
            return new AST.Block(null, statements);
        }
    }
    
    private static AST.Array array(Context ctx) {
        List<AST.Expr> values = new ArrayList<>();
        ctx.expect("LBRACE");
        while(!ctx.check("RBRACE")) {
            values.add(expression(ctx));
            ctx.expect("COMMA");
        }
        ctx.expect("RBRACE");
        return new AST.Array(values);
    }
    
    private static AST.Value value(Context ctx) {
        if(ctx.check("ID")) {
            return identifier(ctx);
        } else if(ctx.check("LBRACE")) {
            return array(ctx);
        } else if(ctx.check("LBRACKET")) {
            return block(ctx);
        } else if(ctx.check("HASH") && ctx.lookahead("LBRACKET")) {
            return byteBlock(ctx);
        } else if(ctx.check("STRING") || ctx.check("NUMBER") || ctx.check("SYMBOL") || ctx.check("TRUE") || ctx.check("FALSE") || ctx.check("NIL")) {
            return literal(ctx);
        } else if(ctx.check("LPAREN")) {
            ctx.accept("LPAREN");
            AST.Expr expr = expression(ctx);
            ctx.expect("RPAREN");
            return new AST.NestedExpr(expr);
        } else {
            ctx.error("Value expected.");
            return null;
        }
    }
    
    // Messages
    private static AST.UnaryMessage unaryMessage(Context ctx) {
        String name = ctx.accept("ID").value();
        return new AST.UnaryMessage(name);
    }
    
    private static AST.BinaryMessage binaryMessage(Context ctx) {
        String name = ctx.accept("BINOP").value();
        AST.Expr argument = unaryExpression(ctx);

        return new AST.BinaryMessage(name, argument);
    }
    
    private static AST.KeywordMessage keywordMessage(Context ctx) {
        StringBuilder bob = new StringBuilder();
        Map<String,AST.Expr> arguments = new HashMap<>();

        if(ctx.check("ID") && ctx.lookahead("COLON")) {
            while(ctx.check("ID")) {
                String key = ctx.expect("ID").value();
                ctx.expect("COLON");
                AST.Expr argument = (AST.Expr)binaryExpression(ctx);
    
                bob.append(key).append(":");
                arguments.put(key, argument);
            }
    
            return new AST.KeywordMessage(bob.toString(), arguments);
        }
        return null;
    }
    
    private static AST.Message message(Context ctx) {
        if(ctx.check("ID") && ctx.lookahead("COLON")) return keywordMessage(ctx);
        else if(ctx.check("ID")) return unaryMessage(ctx);
        else if(ctx.check("BINOP")) return binaryMessage(ctx);
        else {
            ctx.error("Expected message, got " + ctx.peek().type());
            return null;
        }
    }
    
    // Expressions
    private static AST.Expr unaryExpression(Context ctx) {
        AST.Value receiver = value(ctx);
        List<AST.UnaryMessage> messages = new ArrayList<AST.UnaryMessage>();
        while(ctx.check("ID") && !ctx.lookahead("COLON")) {
            messages.add(unaryMessage(ctx));
        }
        if(messages.isEmpty()) {
            return receiver;
        }
        return new AST.UnaryExpression(receiver, messages);
    }
    
    private static AST.Expr binaryExpression(Context ctx) {
        AST.Expr receiver = unaryExpression(ctx);
        List<AST.BinaryMessage> messages = new ArrayList<AST.BinaryMessage>();
        while(ctx.check("BINOP")) {
            messages.add(binaryMessage(ctx));
        }
        if(messages.isEmpty()) {
            return receiver;
        }
        return new AST.BinaryExpression(receiver, messages);
    }
    
    private static AST.Expr keywordExpression(Context ctx) {
        AST.Expr receiver = binaryExpression(ctx);
        AST.KeywordMessage message = keywordMessage(ctx);
        if(message == null) {
            return receiver;
        }
        return new AST.KeywordExpression(receiver, message);
    }
    
    private static AST.Expr expression(Context ctx) {
        AST.Expr receiver = keywordExpression(ctx);
        List<AST.Message> messages = new ArrayList<AST.Message>();

        if(ctx.check("SEMICOLON")) {
            while(ctx.check("SEMICOLON")) {
                ctx.expect("SEMICOLON");
                messages.add(message(ctx));
            }
            return new AST.Cascade(receiver, messages);
        }
        return receiver;
    }
    
    // Members
    private static AST.Signature signature(Context ctx) {
        String name;
        if(ctx.check("BINOP")) {
            name = ctx.expect("BINOP").value();
            AST.Identifier arg = identifier(ctx);
            return new AST.BinarySignature(name, arg);
        } else if(ctx.check("ID")) {
            if(ctx.lookahead("COLON")) {
                StringBuilder bob = new StringBuilder();
                Map<String,AST.Identifier> args = new HashMap<>();
                
                while(ctx.check("ID")) {
                    String key = ctx.expect("ID").value();
                    ctx.expect("COLON");
                    AST.Identifier val = identifier(ctx);
                    
                    bob.append(key).append(":");
                    args.put(key, val);
                }
                return new AST.KeywordSignature(bob.toString(), args);
            } else {
                return new AST.UnarySignature(ctx.expect("ID").value());
            }
        } else {
            ctx.error("Expected signature.");
            return null;
        }
    }
    
    private static AST.Requirement requirement(Context ctx) {
        ctx.expect("REQUIRE");
        AST.Signature sig = signature(ctx);
        ctx.expect("PERIOD");
        return new AST.Requirement(sig);
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

        return null;
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
        return new AST.Answer(val);
    }
    
    private static AST.TraitDef traitDef(Context ctx) {
        ctx.expect("TRAIT");
        String name = ctx.expect("ID").value();
        AST.Identifier parent = null;
        if(ctx.accept("EXTENDING") != null) {
            parent = identifier(ctx);
        }
        ctx.expect("IS");
        List<AST.Member> members = new ArrayList<>();
        while(!ctx.check("END")) {
            AST.Member val = member(ctx);
            if(val instanceof AST.Field) {
                ctx.error("Traits cannot contain fields.");
            }
            members.add(val);
        }
        ctx.expect("END");
        return new AST.TraitDef(name, parent, members);
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
        while(!ctx.check("END")) {
            AST.Member val = member(ctx);
            if(val instanceof AST.Requirement) {
                ctx.error("Classes cannot contain requirements.");
            }
            members.add(val);
        }
        ctx.expect("END");
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
        return new AST.Assignment(name, val);
    }
    
    private static AST.Stmt statement(Context ctx) {
        if(ctx.check("CLASS")) {
            return classDef(ctx);
        } else if(ctx.check("TRAIT")) {
            return traitDef(ctx);
        } else if(ctx.check("VAR")) {
            return tempDecl(ctx);
        } else if(ctx.check("ANSWER")) {
            return answer(ctx);
        } else if(ctx.check("AT")) {
            return pragma(ctx);
        } else if(ctx.check("ID") && ctx.lookahead("ASSIGN")) {
            AST.Assignment val = assignment(ctx);
            ctx.expect("PERIOD", "Statements must end with a period.");
            return val;
        } else if(ctx.check("ID") || ctx.check("LBRACE") || ctx.check("LBRACKET") || ctx.check("HASH") || ctx.check("STRING") || ctx.check("NUMBER") || ctx.check("SYMBOL") || ctx.check("TRUE") || ctx.check("FALSE") || ctx.check("NIL") || ctx.check("LPAREN")) {
            AST.Expr val = expression(ctx);
            ctx.expect("PERIOD", "Statements must end with a period.");
            return val;
        } else {
            ctx.error("Expected statement.");
            return null;
        }
    }
    
    private static AST.Program program(Context ctx) {
        List<AST.Stmt> statements = new ArrayList<>();
        while(!ctx.check("EOF")) {
            statements.add(statement(ctx));
        }
        return new AST.Program(statements);
    }
    
    public static AST.Program parse(List<Lexer.Token> tokens) {
        Context ctx = new Context(tokens);
        return program(ctx);
    }
}
