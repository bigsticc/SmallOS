from AST import *
from typing import Union

def trim_block(AST: BlockContext):
    new_statements = []
    if hasattr(AST, 'statements'):
        for stmt in AST.statements:
            if isinstance(stmt, Answer):
                new_statements.append(stmt)
                break  # Stop adding statements after an Answer node
            new_statements.append(stmt)
    else:
        return AST
            
    if type(AST) is Program:
        return Program(new_statements)
    elif type(AST) is Method:
        return Method(AST.static, AST.signature, new_statements)
    elif type(AST) is Block:
        return Block(AST.args, new_statements)



if __name__ == '__main__':
    from stparse import parse
    from lexer import tokenize
    from sys import argv
    
    f = open(argv[1])
    text = f.read()
    f.close()
        
    tokens = tokenize(text)
    tree = parse(tokens)
    
    print(trim_block(tree))
