import re
from typing import NamedTuple

class Token(NamedTuple):
    line: int
    kind: str
    value: any
    
tokenDef = {
    "COMMENT": r'\/\/.*$',
    "SYMBOL": r'#[a-zA-Z_$][a-zA-Z_$0-9]*',
    
    # Symbols
    "ASSIGN": r':=',
    "PERIOD": r'\.',
    "COLON": r':',
    "SEMICOLON": r';',
    "COMMA": r',',
    "HASH": r'#',
    "LPAREN": r'\(',
    "RPAREN": r'\)',
    "LBRACKET": r'\[',
    "RBRACKET": r'\]',
    "LBRACE": r'\{',
    "RBRACE": r'\}',
    "ANSWER": r'\^',
    "PIPE": r'\|',
    "AT": r'@',
    
    # Values (lower precedence)
    "STRING": r'"(?:[^"]|"")+"',
    "BYTE": r'x[0-9A-Fa-f]{2}',
    "NUMBER": r'[-+]?\d+(?:\.\d+)?',
    "ID": r'[a-zA-Z_][a-zA-Z0-9_]*',
    "BINOP": r'[-+/*=<>!]+',

    "NEWLINE": r'\n',
    "SKIP": r'[ \t]+',
    "MISMATCH": r'.',
}
regex = "|".join(f"(?P<{name}>{value})" for name, value in tokenDef.items())

keywords = ["class", "trait", "extending", "implementing", "is", "as", "static", "var", "def", "end", "require", "true", "false", "nil"]

def tokenize(code: str):
    tokens = []
    line = 1
    for mo in re.finditer(regex, code, re.MULTILINE):
        kind = mo.lastgroup
        value = mo.group()
        if kind == 'NUMBER':
            value = float(value) if '.' in value else int(value)
        elif kind == 'BYTE':
            value = int(value[1:])
        elif kind == 'ID' and value in keywords:
            kind = value.upper()
        elif kind == 'COMMENT':
            continue
        elif kind == 'NEWLINE':
            line += 1
            continue
        elif kind == 'SKIP':
            continue
        elif kind == 'MISMATCH':
            raise SyntaxError(f"Unexpected character >>{value}<< in line {line}")
        tokens.append(Token(line, kind, value))
    tokens.append(Token(line, 'EOF', ''))
    return tokens
    
if __name__ == '__main__':
    while True:
        text = input('PyParser> ')
        tokens = tokenize(text)
        out = "\n".join(f"{i}: {token.kind} ('{token.value}')" for i, token in enumerate(tokens))
        print(out)
