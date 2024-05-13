package com.smallos;
import java.util.List;
import java.util.Map;

public interface AST {
    public static interface Node {}
    
    public static interface Value extends Expr {}
    public static record Bool(boolean value) implements Value {}
    public static record Nil() implements Value {}
    public static record Symbol(String value) implements Value {}
    public static record Num(Number value) implements Value {}
    public static record Str(String value) implements Value {}
    public static record ByteBlock(Byte[] value) implements Value {}
    public static record Identifier(String name) implements Value {}
    public static record Chain(List<Expr> values) implements Value {}
    public static record Block(List<Identifier> args, List<Stmt> statements) implements Value {}
    public static record Array(List<Expr> values) implements Value {}
    public static record NestedExpr(Expr expr) implements Value {}
    
    public static interface Message extends Node {}
    public static record UnaryMessage(String name) implements Message {}
    public static record BinaryMessage(String name, Expr argument) implements Message {}
    public static record KeywordMessage(String name, Map<String, Expr> arguments) implements Message {}
    
    public static interface Expr extends Stmt {}
    public static record UnaryExpression(Value receiver, List<UnaryMessage> messages) implements Expr {}
    public static record BinaryExpression(Expr receiver, List<BinaryMessage> message) implements Expr {}
    public static record KeywordExpression(Expr receiver, KeywordMessage message) implements Expr {}
    public static record Cascade(AST.Expr primary, List<Message> messages) implements Expr {}
    
    public static interface Member extends Node {}
    public static record Requirement(Signature signature) implements Member {}
    public static record Method(boolean isStatic, Signature signature, List<Stmt> statements) implements Member {}
    public static record Field(boolean isStatic, String name, Expr value) implements Member {}
    
    public static interface Signature extends Node {}
    public static record UnarySignature(String name) implements Signature {}
    public static record BinarySignature(String name, Identifier argument) implements Signature {}
    public static record KeywordSignature(String name, Map<String, Identifier> arguments) implements Signature {}
    
    public static interface Stmt extends Node {}
    public static record Pragma(Message value) implements Stmt, Member {}
    public static record Answer(Expr value) implements Stmt {}
    public static record TraitDef(Identifier name, Identifier parent, List<Member> members) implements Stmt {}
    public static record ClassDef(Identifier name, Identifier parent, List<Identifier> traits, List<Member> members) implements Stmt {}
    public static record Assignment(Identifier name, Expr value) implements Stmt {}
    public static record TempDecl(Identifier name, Expr value) implements Stmt {}
    
    public static record Program(List<Stmt> statements) implements Node {}
}