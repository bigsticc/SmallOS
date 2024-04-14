from typing import Union
from collections import namedtuple

Boolean = namedtuple('Boolean', 'value')
Nil = namedtuple('Nil', '')
Symbol = namedtuple('Symbol', 'value')
Number = namedtuple('Number', 'value')
String = namedtuple('String', 'value')
Chain = namedtuple('Chain', 'values')
ByteArray = namedtuple('ByteArray', 'values')
Block = namedtuple('Block', 'args statements')
Array = namedtuple('Array', 'elements')
Identifier = namedtuple('Identifier', 'name')
Value = Union[Identifier, Array, Block, ByteArray, Chain, String, Number, Symbol]

UnaryMessage = namedtuple('UnaryMessage', 'name')
BinaryMessage = namedtuple('BinaryMessage', 'name argument')
KeywordMessage = namedtuple('KeywordMessage', 'name argdict')
Message = Union[KeywordMessage, BinaryMessage, UnaryMessage]

UnaryExpression = namedtuple('UnaryExpression', 'receiver messages')
BinaryExpression = namedtuple('BinaryExpression', 'receiver messages')
KeywordExpression = namedtuple('KeywordExpression', 'receiver message')
Expression = namedtuple('Expression', 'value cascade')

Signature = namedtuple('Signature', 'name arguments')
Requirement = namedtuple('Requirement', 'signature')
Method = namedtuple('Method', 'static signature statements')
Field = namedtuple('Field', 'static name value')
Member = Union[Field, Method, Requirement]

Pragma = namedtuple('Pragma', 'value')
Answer = namedtuple('Answer', 'value')
TraitDef = namedtuple('TraitDef', 'name parent_trait members')
ClassDef = namedtuple('ClassDef', 'name parent_class traits members')
Assignment = namedtuple('Assignment', 'name value')
TempDecl = namedtuple('TempDecl', 'name value')
Statement = Union[Assignment, ClassDef, TraitDef, Answer, Pragma]

Program = namedtuple('Program', 'statements')

AST = Union[Program, Statement, Member, Expression, Message, Value]
BlockContext = Union[Program, Method, Block]

def inUnion(_type, union):
    print("In Union called with:", _type)
    return _type in union.__args__

