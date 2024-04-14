from AST import *
from lexer import Token, tokenize
from sys import argv, exit

# Utility class for managing parser state
class Context:
    tokens: list[Token]
    pos: int
    
    def __init__(self, tokens: list[Token]):
        self.tokens = tokens
        self.pos = 0

    def advance(self):
        self.pos += 1
    
    def full_peek(self):
        return self.tokens[self.pos]
    
    def peek(self):
        return self.tokens[self.pos].kind
    
    def lookahead(self):
        return self.tokens[self.pos+1].kind
    
    def pop(self):
        c = self.full_peek()
        self.advance()
        return c
    
    def accept(self, kind: str):
        if self.peek() == kind:
            return self.pop()
        return None
    
    def expect(self, kind: str, message=None):
        if self.peek() == kind:
            return self.pop()
        self.error(message or f"Expected {kind}, got: {self.peek()}")
    
    def error(self, message):
        try:
            raise SyntaxError(f"Error while parsing near line {self.full_peek().line}: {message}")
        except Exception as e:
            print(str(e))
            exit(1)

# Parser functions -- Values
def literal(ctx: Context):
    if number := ctx.accept('NUMBER'):
        return Number(number.value)
    elif string := ctx.accept('STRING'):
        return String(string.value)
    elif symbol := ctx.accept('SYMBOL'):
        return Symbol(symbol.value)
    elif symbol := ctx.accept('TRUE'):
        return Boolean(True)
    elif symbol := ctx.accept('FALSE'):
        return Boolean(False)
    elif symbol := ctx.accept('NIL'):
        return Nil()
    return None

def identifier(ctx: Context):
    if ID := ctx.accept('ID'):
        return Identifier(ID.value)
    return None

def chain(ctx: Context):
    ctx.expect('HASH')
    ctx.expect('LPAREN')
    expressions = []
    first = expression(ctx)
    if not first:
        ctx.error('Chains must contain at least one expression.')
    else:
        expressions.append(first)
    
    while ctx.accept('COMMA'):
        expressions.append(expression(ctx))
    ctx.expect('RPAREN')
    return Chain(expressions)
        
def byteArray(ctx: Context):
    ctx.expect('HASH')
    ctx.expect('LBRACKET')
    values = []
    while val := ctx.accept('BYTE'):
        values.append(val)
    ctx.expect('RBRACKET')
    return ByteArray(values)

def block(ctx: Context):
    ctx.expect('LBRACKET')
    # Parsing the head
    args = []
    if ctx.accept('COLON'):
        while ID := identifier(ctx):
            args.append(ID)
        ctx.expect('PIPE')
    
    statements = []
    while not ctx.accept('RBRACKET'):
        statements.append(statement(ctx))
    
    return Block(args, statements)

def array(ctx: Context):
    elements = []
    ctx.expect('LBRACE')
    if not ctx.peek() == 'RBRACE':
        elements.append(expression(ctx))
        while not ctx.accept('RBRACE'):
            ctx.expect('COMMA')
            elements.append(expression(ctx))
    else: 
        ctx.accept('RBRACE')
    return Array(elements)

def value(ctx: Context):
    if ctx.peek() == 'ID':
        return identifier(ctx)
    elif ctx.peek() == 'LBRACE':
        return array(ctx)
    elif ctx.peek() == 'LBRACKET':
        return block(ctx)
    elif ctx.peek() == 'HASH':
        if ctx.lookahead() == 'LPAREN':
            return chain(ctx)
        elif ctx.lookahead() == 'LBRACKET':
            return byteArray(ctx)
        else:
            ctx.error("Expected chain or byte array after hash token.")
    elif ctx.peek() in ['STRING', 'NUMBER', 'SYMBOL', 'TRUE', 'FALSE', 'NIL']:
        return literal(ctx)
    elif ctx.peek() == 'LPAREN':
        ctx.accept('LPAREN')
        val = expression(ctx)
        ctx.expect('RPAREN')
        return val

# Messages
def unaryMessage(ctx: Context):
    if ID := ctx.accept('ID'):
        return UnaryMessage(ID.value)

def binaryMessage(ctx: Context):
    if binOp := ctx.accept('BINOP'):
        val = expression(ctx)
        if not val:
            ctx.error("Expected expression as argument to binary.")
        return BinaryMessage(binOp.value, val)

def keywordMessage(ctx: Context):
    name = ""
    args = {}
    if ctx.peek() == 'ID' and ctx.lookahead() == 'COLON':
        while ctx.peek() == 'ID':
            ID = ctx.expect('ID')
            ctx.expect('COLON')
            val = binaryExpression(ctx)
            if not val:
                ctx.error("Expected binary expression as argument to keyword message.")
                
            args[ID.value] = val
            name += f"{ID.value}:"
        return KeywordMessage(name, args)
    
def message(ctx: Context):
    if ctx.peek() == 'BINOP':
        return binaryMessage(ctx)
    elif ctx.peek() == 'ID':
        if ctx.lookahead() == 'COLON':
            return keywordMessage(ctx)
        return unaryMessage(ctx)
    
# Expressions
def unaryExpression(ctx: Context):
    receiver = value(ctx)
    messages = []
    while ctx.peek() == 'ID' and ctx.lookahead() != 'COLON':
        messages.append(unaryMessage(ctx))
    if messages == []:
        return receiver
    return UnaryExpression(receiver, messages)

def binaryExpression(ctx: Context):
    receiver = unaryExpression(ctx)
    messages = []
    while ctx.peek() == 'BINOP':
        messages.append(binaryMessage(ctx))
    if messages == []:
        return receiver
    return BinaryExpression(receiver, messages)

def keywordExpression(ctx: Context):
    val = binaryExpression(ctx)
    message = keywordMessage(ctx)
    if not message:
        return val
    return KeywordExpression(val, message)

def expression(ctx: Context):
    val = keywordExpression(ctx)
    cascade = []
    while ctx.accept('SEMICOLON'):
        cascade.append(message(ctx))
    if cascade == []:
        return val
    return Expression(val, cascade)

# Members
def signature(ctx: Context):
    name = ""
    args = []
    if ctx.peek() == 'BINOP':
        name = ctx.expect('BINOP').value
        args.append(identifier(ctx))
        return Signature(name, args)
    elif ctx.peek() == 'ID':
        if ctx.lookahead() == 'COLON':
            while ctx.peek() == 'ID':
                key = ctx.expect('ID')
                ctx.expect('COLON')
                val = identifier(ctx)
                
                name += f"{key.value}:"
                args.append(val)
            return Signature(name, args)
        else:
            return Signature(ctx.expect('ID').value, None)
    else:
        ctx.error("Expected signature.")

def requirement(ctx: Context):
    ctx.expect('REQUIRE')
    sig = signature(ctx)
    ctx.expect('PERIOD')
    return Requirement(sig)

def method(ctx: Context):
    static = bool(ctx.accept('STATIC'))
    ctx.expect('DEF')
    sig = signature(ctx)
    ctx.expect('AS')
    statements = []
    while not ctx.accept('END'):
        statements.append(statement(ctx))
    return Method(static, sig, statements)

def field(ctx: Context):
    static = bool(ctx.accept('STATIC'))
    ctx.expect('VAR')
    name = ctx.expect('ID').value
    value = None
    if ctx.accept('ASSIGN'):
        value = expression(ctx)
    ctx.expect('PERIOD', 'Fields must end with a period.')
    return Field(static, name, value)

def member(ctx: Context):
    peek = ctx.peek()
    if peek == 'STATIC':
        look = ctx.lookahead()
        if look == 'DEF':
            return method(ctx)
        elif look == 'VAR':
            return field(ctx)
        elif look == 'REQUIRE':
            ctx.error("Method requirements cannot be static.")
        else:
            ctx.error("Expected method or field after 'static' token.")
    elif peek == 'DEF':
        return method(ctx)
    elif peek == 'VAR':
        return field(ctx)
    elif peek == 'REQUIRE':
        return requirement(ctx)
    elif peek == 'AT':
        return pragma(ctx)
    else:
        ctx.error("Expected one of: method, field, requirement, pragma in class/trait body.")

# Statements
def pragma(ctx: Context):
    ctx.expect('AT')
    val = message(ctx)
    return Pragma(val)

def answer(ctx: Context):
    ctx.expect('ANSWER')
    val = expression(ctx)
    ctx.expect('PERIOD', message="Statements must be ended with a period.")
    return Answer(val)

def traitDef(ctx: Context):
    ctx.expect('TRAIT')
    name = ctx.expect('ID').value
    parent = None
    if ctx.accept('EXTENDING'):
        parent = ctx.expect('ID').value
    members = []
    ctx.expect('IS')
    while not ctx.accept('END'):
        val = member(ctx)
        if type(val) is Field:
            ctx.error('Traits cannot hold fields.')
        members.append(val)
    return TraitDef(name, parent, members)

def classDef(ctx: Context):
    ctx.expect('CLASS')
    name = ctx.expect('ID').value
    parent = None
    if ctx.accept('EXTENDING'):
        parent = ctx.expect('ID').value
    traits = None
    if ctx.accept('IMPLEMENTING'):
        traits = []
        while ID := ctx.accept('ID'):
            traits.append(ID.value)
            ctx.accept('COMMA')
    ctx.expect('IS')
    members = []
    while not ctx.accept('END'):
        val = member(ctx)
        if type(val) is Requirement:
            ctx.error("Classes cannot contain requirements.")
        members.append(val)
    return ClassDef(name, parent, traits, members)

def assignment(ctx: Context):
    var = ctx.expect('ID').value
    ctx.expect('ASSIGN')
    val = expression(ctx)
    ctx.expect('PERIOD', message="Statements must be ended with a period.")
    return Assignment(var, val)

def tempdecl(ctx: Context):
    ctx.expect('VAR')
    name = ctx.expect('ID').value
    val = None
    if ctx.accept('ASSIGN'):
        val = expression(ctx)
    ctx.expect('PERIOD', message="Statements must be ended with a period.")
    return TempDecl(name, val)

def statement(ctx: Context):
    peek = ctx.peek()
    look = ctx.lookahead()
    if peek == 'ID' and look == 'ASSIGN':
        return assignment(ctx)
    elif peek == 'VAR':
        return tempdecl(ctx)
    elif peek == 'CLASS':
        return classDef(ctx)
    elif peek == 'TRAIT':
        return traitDef(ctx)
    elif peek == 'ANSWER':
        return answer(ctx)
    elif peek == 'AT':
        return pragma(ctx)
    elif peek in ['ID', 'LBRACE', 'LBRACKET', 'HASH', 'STRING', 'NUMBER', 'SYMBOL', 'TRUE', 'FALSE', 'NIL', 'LPAREN']:
        val = expression(ctx)
        ctx.expect('PERIOD', message="Statements must be ended with a period.")
        return val
    else:
        ctx.error('Expected statement.')

def program(ctx: Context):
    statements = []
    while not ctx.accept('EOF'):
        statements.append(statement(ctx))
    return Program(statements)

# driver function
def parse(tokens: list[Token]) -> Program:
    ctx = Context(tokens)
    return program(ctx)

# Main script
if __name__ == '__main__':
    if len(argv) >= 2:
        f = open(argv[1])
        text = f.read()
        f.close()
        
        tokens = tokenize(text)
        print(parse(tokens))
    else:
        while True:
            text = input('PyParser> ')
            tokens = tokenize(text)
            print(parse(tokens))
