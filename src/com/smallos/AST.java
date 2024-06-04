package com.smallos;
import java.util.List;
import java.util.Map;

public interface AST {
    interface Node {}
    
    interface Value extends Expr {}
    record Bool(boolean value) implements Value {}
    record Nil() implements Value {}
    record Symbol(String value) implements Value {}
    record Num(Number value) implements Value {}
    record Str(String value) implements Value {}
    record ByteBlock(Byte[] value) implements Value {}
    record Identifier(String name) implements Value {}
    record Chain(List<Expr> values) implements Value {}
    record Block(List<Identifier> args, List<Stmt> statements) implements Value {}
    record Array(List<Expr> values) implements Value {}
    record NestedExpr(Expr expr) implements Value {}
    
    interface Message extends Node {}
    record UnaryMessage(String name) implements Message {}
    record BinaryMessage(String name, Expr argument) implements Message {}
    record KeywordMessage(String name, Map<String, Expr> arguments) implements Message {}
    
    interface Expr extends Stmt {}
    record UnaryExpression(Value receiver, List<UnaryMessage> messages) implements Expr {}
    record BinaryExpression(Expr receiver, List<BinaryMessage> message) implements Expr {}
    record KeywordExpression(Expr receiver, KeywordMessage message) implements Expr {}
    record Cascade(AST.Expr primary, List<Message> messages) implements Expr {}
    
    interface Member extends Node {}
    record Requirement(Signature signature) implements Member {}
    record Method(boolean isStatic, Signature signature, List<Stmt> statements) implements Member {}
    record Field(boolean isStatic, String name, Expr value) implements Member {}
    
    interface Signature extends Node {}
    record UnarySignature(String name) implements Signature {}
    record BinarySignature(String name, Identifier argument) implements Signature {}
    record KeywordSignature(String name, Map<String, Identifier> arguments) implements Signature {}
    
    interface Stmt extends Node {}
    record Pragma(Message value) implements Stmt, Member {}
    record Answer(Expr value) implements Stmt {}
    record TraitDef(Identifier name, Identifier parent, List<Member> members) implements Stmt {}
    record ClassDef(Identifier name, Identifier parent, List<Identifier> traits, List<Member> members) implements Stmt {}
    record Assignment(Identifier name, Expr value) implements Stmt {}
    record TempDecl(Identifier name, Expr value) implements Stmt {}
    
    record Program(List<Stmt> statements) implements Node {}
}