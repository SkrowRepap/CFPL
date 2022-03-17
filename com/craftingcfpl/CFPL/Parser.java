package com.craftingcfpl.CFPL;

import java.util.ArrayList;
import java.util.List;

import static com.craftingcfpl.CFPL.TokenType.*;


public class Parser {
    private static class ParseError extends RuntimeException {
    }

    private final List<Token> tokens;
    private int current = 0;

    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    List<Stmt> parse() {
        List<Stmt> statements = new ArrayList<>();
        while (!isAtEnd()) {
            List<Stmt> declarations = declarations();
            if (declarations != null && declarations.size() > 0)
              statements.addAll(declarations);
        }

        return statements;
    }

    private Expr expression() {
        return assignment();
    }

    private Stmt declaration() {
        try {
            if (match(VAR))
                return varDeclaration();

            return statement();
        } catch (ParseError error) {
            synchronize();
            return null;
        }
    }

    private Stmt varDeclaration() {
        Token name = consume(IDENTIFIER, "Expected variable name");

        Expr iniExpr = null;

        if (match(EQUAL)) {
            iniExpr = expression();
        }

        consume(AS, "Expected 'AS' after variable name");

        Token dataType = consume(getDataType(peek()), "Expected / Invalid Data Type");

        return new Stmt.Var(name, iniExpr, dataType);
    }

    private List<Stmt> declarations() {
        List<Stmt> stmts = new ArrayList<>();
        try {
            if (match(TokenType.VAR)) {
                List<Stmt.Var> vStmts = varDeclarations();
                for (Stmt.Var var : vStmts) {
                    stmts.add(var);
                }
            } else {
                stmts.add(statement());
            }
            return stmts;
        } catch (ParseError error) {
            synchronize();
            return null;
        }
    }

    private List<Stmt.Var> varDeclarations() {
        Token name = consume(IDENTIFIER, "Expect variable name.");
        List<Stmt.Var> stmts = new ArrayList<>();

        Expr initializer = null;
        if (match(EQUAL)) {
            initializer = expression();
        }

        stmts.add(new Stmt.Var(name, initializer, null));

        while(match(COMMA)) {
            name = consume(IDENTIFIER, "Expect variable name.");
            initializer = null;

            if (match(EQUAL)) {
                initializer = expression();
            }

            stmts.add(new Stmt.Var(name, initializer, null));
            
        }


        consume(AS, "Expect 'AS' after variable declaration.");

        Token dataType = consume(getDataType(peek()), "Expected Data Type");

        for (Stmt.Var stmt : stmts) {
            stmt.setDataType(dataType);
        }

        return stmts;
    }
    private Stmt statement() {
        if (match(PRINT))
            return printStatement();


        return expressionStatement();
    }

    private Stmt printStatement() {
        Expr value = expression();
        return new Stmt.Print(value);
    }

    private Stmt expressionStatement() {
        Expr expr = expression();
        return new Stmt.Expression(expr);
    }

    private List<Stmt> block() {
        List<Stmt> statements = new ArrayList<>();

        while (!check(STOP) && !isAtEnd()) {
            statements.add(declaration());
        }

        consume(STOP, "Out of bounds");
        return statements;
    }

    private Expr assignment() {
        Expr expr = equality();

        if (match(EQUAL)) {
            Token equals = previous();
            Expr value = assignment();

            if (expr instanceof Expr.Variable) {
                Token name = ((Expr.Variable) expr).name;
                return new Expr.Assign(name, value);
            }

            error(equals, "Invalid assignment target.");
        }

        return expr;
    }

    private Expr equality() {
        Expr expr = comparison();

        while (match(BANG_EQUAL, EQUAL_EQUAL)) {
            Token operator = previous();
            Expr right = comparison();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr comparison() {
        Expr expr = term();

        while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL, AND, OR, AMPERSAND)) {
            Token operator = previous();
            Expr right = term();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr term() {
        Expr expr = factor();

        while (match(MINUS, PLUS)) {
            Token operator = previous();
            Expr right = factor();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr factor() {
        Expr expr = unary();

        while (match(SLASH, STAR)) {
            Token operator = previous();
            Expr right = unary();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr unary() {
        if (match(BANG, MINUS,NOT)) {
            Token operator = previous();
            Expr right = unary();
            return new Expr.Unary(operator, right);
        }

        return primary();
    }

    private Expr primary() {
        if (match(FALSE))
            return new Expr.Literal(false);
        if (match(TRUE))
            return new Expr.Literal(true);
        if (match(NIL))
            return new Expr.Literal(null);

        if (match(NUMBER, STRING, CHAR)) {
            return new Expr.Literal(previous().literal);
        }
        if (match(IDENTIFIER)) {
            return new Expr.Variable(previous());
        }
        if (match(LEFT_PAREN)) {
            Expr expr = expression();
            consume(RIGHT_PAREN, "Expect ')' after expression.");
            return new Expr.Grouping(expr);
        }

        

        throw error(peek(), "Expect expression.");

    }

    private Token consume(TokenType type, String message) {
        if (check(type))
            return advance();

        throw error(peek(), message);
    }

    private Token advance() {
        if (!isAtEnd())
            ++current;
        return previous();
    }

    private Token previous() {
        return tokens.get(current - 1);
    }

    private boolean isAtEnd() {
        return peek().type == EOF;
    }

    private ParseError error(Token token, String message) {
        CFPL.error(token, message);
        return new ParseError();
    }

    private boolean match(TokenType... types) {
        for (TokenType tokenType : types) {
            if (check(tokenType)) {
                advance();
                return true;
            }
        }
        return false;
    }

    private boolean check(TokenType types) {
        if (isAtEnd())
            return false;

        return peek().type == types;
    }

    private Token peek() {
        return tokens.get(current);
    }

    

    private TokenType getDataType (Token token) {
        switch (token.type) {
            case INT:
                return INT;
            case CHAR:
                return CHAR;
            case BOOL:
                return BOOL;
            case FLOAT:
                return FLOAT;
            case STRING:
                return STRING;
            default:
            break;
        }

        return null;
    }

    private void synchronize() {
        advance();

        while (!isAtEnd()) {
            switch (peek().type) {
                case CLASS:
                case FUN:
                case FOR:
                case IF:
                case WHILE:
                case RETURN:
                    return;
            }

            advance();
        }
    }

    
 
}
