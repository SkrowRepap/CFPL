package com.craftingcfpl.CFPL;

enum TokenType {
    // Single-character tokens.
    LEFT_PAREN, RIGHT_PAREN, LEFT_BRACE, RIGHT_BRACE,
    COMMA, DOT, MINUS, PLUS, SEMICOLON, SLASH, STAR, MODULO,

    // One or two character tokens.
    BANG, BANG_EQUAL,
    EQUAL, EQUAL_EQUAL,
    GREATER, GREATER_EQUAL,
    LESS, LESS_EQUAL, INCREMENT, DECREMENT,


    // Literals.
    IDENTIFIER, STRING, NUMBER, AMPERSAND, INPUT, NEWLINE,NEXT_LINE,LEFT_BRACKET,

    // Keywords.
    AND, CLASS, ELSE, FALSE, FUN, FOR, IF, NIL, OR,
    PRINT, RETURN, SUPER, THIS, TRUE, VAR, WHILE, NOT,

    // Data Types
    INT, CHAR, BOOL, FLOAT, AS, 

    //SCOPE
    START, STOP,


    EOF
}
