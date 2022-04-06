package com.craftingcfpl.CFPL;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.craftingcfpl.CFPL.Stmt.Expression;

import static com.craftingcfpl.CFPL.TokenType.*;


public class Parser {

    private static class ParseError extends RuntimeException {
    }

    // BNF

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

        Token name = consume(IDENTIFIER, "Expected variable name"); //name, fname, a

        Expr iniExpr = null; // 0, 0.0

        if (match(EQUAL)) {
            iniExpr = expression();
        }

        consume(AS, "Expected 'AS' after variable name");

        Token dataType = consume(getDataType(peek()), "Expected / Invalid Data Type");

        consume(TokenType.NEWLINE, "Expected line break");

        return new Stmt.Var(name, iniExpr, dataType);
    }

    private Stmt forVarDeclaration() {

        Token name = consume(IDENTIFIER, "Expected variable name"); // name, fname, a

        Expr iniExpr = null; // 0, 0.0

        if (match(EQUAL)) {
            iniExpr = expression();
        }

        consume(AS, "Expected 'AS' after variable name");

        Token dataType = consume(getDataType(peek()), "Expected / Invalid Data Type");

        consume(TokenType.SEMICOLON, "Expected semicolon after expressions");

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

        consume(NEWLINE, "Expected newline after declaring");

        return stmts;
    }
    private Stmt statement() {
        // if (match(TokenType.INPUT))
        //     return new Stmt.Input(input());  
        if (match(START)) {
            return new Stmt.Block(executable());
        }

        return expressionStatement(); //VAR
    }

    private Stmt ifStatement() {
        consume(LEFT_PAREN, "Expect '(' after 'if'.");
        Expr condition = expression();
        consume(RIGHT_PAREN, "Expect ')' after if condition.");

        consume(NEWLINE, "Expect line break after if statement.");

        Stmt thenBranch = statement();
        Stmt elseBranch = null;
        if (match(ELSE)) {
            consume(NEWLINE, "Expect line break after else statement.");
            elseBranch = statement();
        }

        return new Stmt.If(condition, thenBranch, elseBranch);
    }

    private Stmt printStatement() {
        Expr value = expression();
        consume(TokenType.NEWLINE, "Expected line break");
        return new Stmt.Print(value);
    }

    private Stmt expressionStatement() {
        Expr expr = expression();
        consume(TokenType.NEWLINE, "Expected line break");
        return new Stmt.Expression(expr);
    }

    private Stmt forExpressionStatement() {
        Expr expr = expression();
        consume(TokenType.SEMICOLON, "Expected semicolon after expression");
        return new Stmt.Expression(expr);
    }

    private List<Stmt> executable() {
        List<Stmt> statements = new ArrayList<>();

        if (!match(TokenType.NEWLINE)) {
            throw error(peek(), "Expect break line after START");
        }

        while (!check(STOP) && !isAtEnd()) {
            statements.add(executeStatements());
        }

        consume(STOP, "Expected STOP");

        consume(NEWLINE, "Expected line break");

        return statements;
    }

    private Stmt executeStatements() {
        try {
            if (match(TokenType.INPUT))
                return new Stmt.Input(input());
       
            // if (match(TokenType.VAR))
            //     return varDeclaration();

            if (match(TokenType.PRINT))
                return printStatement();

            if (match(IF)) {
                return ifStatement();
            }
            if (match(WHILE)) {
                return whileStatement();
            }
            if (match(FOR)) 
                return forStatement();

          

            if (match(TokenType.START))
                return new Stmt.Block(executable());

            return expressionStatement();
        } catch (ParseError error) {
            synchronize();
            return null;
        }
    }

    private Stmt forStatement() {
        consume(LEFT_PAREN, "Expect '(' after FOR expression");

        Stmt iniStmt;
        if (match(SEMICOLON)) {
            iniStmt = null;
        } else if (match(VAR)) {
            iniStmt = forVarDeclaration();
        } else {
            iniStmt = forExpressionStatement();
        }


        Expr condition = null;
        if (!check(SEMICOLON)) {
            condition = expression();
        }
        consume(SEMICOLON, "Expect ';' after loop condition.");


        Expr increment = null;
        if (!check(RIGHT_PAREN)) {
            increment = expression();
        }
        consume(RIGHT_PAREN, "Expect ')' after for clauses.");

        consume(NEWLINE, "Expect line break after for statement.");


        Stmt body = statement();
 

        if (increment != null) {
            body = new Stmt.Block(
                    Arrays.asList(
                            body,
                            new Stmt.Expression(increment)));
        }

        if (condition == null)
            condition = new Expr.Literal(true);

        body = new Stmt.While(condition, body);

        if (iniStmt != null) {
            body = new Stmt.Block(Arrays.asList(iniStmt, body));
            
        }

        return body;
    }

    private Stmt whileStatement() {
        consume(LEFT_PAREN, "Expect '(' after WHILE expression");

        Expr condition = expression();
        consume(RIGHT_PAREN, "Expect ')' after expression ");
        consume(NEWLINE, "Expect line break after WHILE statement");


        Stmt body = statement();

        return new Stmt.While(condition, body);

    }

    private Expr assignment() {
        Expr expr = or();

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


    private Expr or() {
        Expr expr = and();

        while (match(OR)) {
            Token operator = previous();
            Expr right = and();
            expr = new Expr.Logical(expr, operator, right);
        }

        return expr;
    }

    private Expr and() {
        Expr expr = equality();

        while (match(AND)) {
            Token operator = previous();
            Expr right = equality();
            expr = new Expr.Logical(expr, operator, right);
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

        while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL, AMPERSAND)) {
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
        if (match(BANG, MINUS,NOT,INCREMENT,DECREMENT)) {
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
        if (match(NEXT_LINE))
            return new Expr.Literal('\n');

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

    private Token consume(TokenType type, String message) { // VAR
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
            if (previous().type == NEWLINE)
                return;

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

    private List<Token> input() {
        Token name = consume(TokenType.IDENTIFIER, "Expect variable name.");
        List<Token> tokens = new ArrayList<>();
        tokens.add(name);
        while (match(TokenType.COMMA)) {
            tokens.add(consume(TokenType.IDENTIFIER, "Expect a variable name"));
        }

        consume(TokenType.NEWLINE, "Expect new line after variable declaration.");
        return tokens;
    }



    
 
}
