package com.craftingcfpl.CFPL;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.craftingcfpl.CFPL.TokenType.*;

class Scanner {
    private final String source;
    
    private final List<Token> tokens = new ArrayList<>();
    private int start = 0;
    private int current = 0;
    private int line = 1;
    private int newline = 0; // added -ian

    private static final Map<String, TokenType> keywords;

    static {
        keywords = new HashMap<String, TokenType>();
        // keywords.put("and", AND);
        // keywords.put("class", CLASS);
        // keywords.put("else", ELSE);
        // keywords.put("false", FALSE);
        // keywords.put("for", FOR);
        // keywords.put("fun", FUN);
        // keywords.put("if", IF);
        // keywords.put("nil", NIL);
        // keywords.put("or", OR);
        // keywords.put("print", PRINT);
        // keywords.put("return", RETURN);
        // keywords.put("super", SUPER);
        // keywords.put("this", THIS);
        // keywords.put("true", TRUE);
        // keywords.put("while", WHILE);

        //CFPL

        //DataTypes
        keywords.put("INT", INT);
        keywords.put("CHAR", CHAR);
        keywords.put("BOOL", BOOL);
        keywords.put("FLOAT", FLOAT);
        keywords.put("STRING", STRING);
        

        //BLOCK
        keywords.put("START", START);
        keywords.put("STOP", STOP);

        //RESERVED WORDS
        keywords.put("VAR", VAR);
        keywords.put("OUTPUT:", PRINT);
        keywords.put("INPUT:", INPUT);
        keywords.put("AS", AS);
        

        //LOGICAL
        keywords.put("TRUE", TRUE);
        keywords.put("FALSE", FALSE);
        keywords.put("AND", TokenType.AND);
        keywords.put("OR", OR);
        keywords.put("NOT", NOT);
        keywords.put("IF", IF);
        keywords.put("ELSE", ELSE);
        keywords.put("WHILE", WHILE);
        keywords.put("FOR", FOR);
        keywords.put("#", NEXT_LINE);

    }

    Scanner(String source) {
        this.source = source;
    }

    private boolean shouldAddNewLine() {
        int size = tokens.size();
        return  size >= 1 
                && tokens.get(size - 1).type 
                != TokenType.NEWLINE;
    }
    
    List<Token> scanTokens() {
        while (!isAtEnd()) {
            // We are at the beginning of the next lexeme.
            start = current;
            scanToken();
        }

        tokens.add(new Token(EOF, "", null, line));
        return tokens;
    }   
    
    private void scanToken() {
        char c = advance();
        switch (c) {
            case '(':
                addToken(LEFT_PAREN);
                break;
            case ')':
                addToken(RIGHT_PAREN);
                break;
            case '{':
                addToken(LEFT_BRACE);
                break;
            case '}':
                addToken(RIGHT_BRACE);
                break;
            case ',':
                addToken(COMMA);
                break;
            case '-':
                addToken(match('-') ? DECREMENT : MINUS);
                break;
            case '+':
                addToken(match('+') ? INCREMENT: PLUS);
                break;
            case ';':
                addToken(SEMICOLON);
                break;
            case '%':
                addToken(MODULO);
                break;
            case '*':
                if (allowComment() == true) { // added -ian
                    while (peek() != '\n' && !isAtEnd()) // added -ian
                        advance(); // added -ian
                } else { // added -ian
                    addToken(STAR); // added -ian
                }

                break;
            case '!':
                addToken(match('=') ? BANG_EQUAL : BANG);
                break;
            case '&':
                addToken(AMPERSAND);
                break;
            case '=': // == ? =
                addToken(match('=') ? EQUAL_EQUAL : EQUAL);
                break;
            case '<': // <= ? <
                addToken(match('=') ? LESS_EQUAL :  match('>') ? BANG_EQUAL : LESS);
                break;
            case '>':
                addToken(match('=') ? GREATER_EQUAL : GREATER);
                break;
            case '/':
                if (match('/')) {
                    // A comment goes until the end of the line.
                    while (peek() != '\n' && !isAtEnd())
                        advance();
                } else {
                    addToken(SLASH);
                }
                break;

            case ' ':
            case '\r':
            case '\t':
                // Ignore whitespace.
                break;

            case '\n':

                if(shouldAddNewLine()) {
                    addToken(NEWLINE);
                }
                newline = line;

                line++;
                break;
            
            case '"':
                if (peek() == '[')  {
                    escapeCode();
                }
                else { 
                    string();
                }
                break;
            case '#':
                addToken(NEXT_LINE);
                break;
            case '\'':
                charac();
                break;

            default:
                if (isDigit(c)) {
                    number();
                } else if (isAlpha(c)) {
                    identifier();
                }
                else {
                    CFPL.error(line, "Unexpected character.");
                }
            break;
        }
    }

    // public static Map<String, TokenType> getReservedWords() {
    //     return keywords;
    // }
    private boolean isAtStart() { // added -ian
        return current == 1; // added -ian
    }

    private boolean isAtEnd() {
        return current >= source.length();
    }

    private char advance() { // forward
        return source.charAt(current++);
    }

    private void addToken(TokenType type) {
        addToken(type, null);
    }

    private void addToken(TokenType type, Object literal) {
        String text = source.substring(start, current);
        tokens.add(new Token(type, text, literal, line));
    }

    private boolean match(char expected) {
        if (isAtEnd())
            return false;
        if (source.charAt(current) != expected)
            return false;

        current++;
        return true;
    }

    private char peek() { // mangniid "ty" (read)
        if (isAtEnd())
            return '\0';
        return source.charAt(current);
    }

    private boolean allowComment() { // added -ian
        if (isAtStart() || notAlphaPrev() || justNewlined()) { // added -ian
            // System.out.println("I WAS HERE"); //added -ian
            return true; // added -ian
        } else // added -ian
            return false; // added -ian
    } // added -ian

    private boolean justNewlined() { // added -ian
        if (newline < line && source.charAt(current - 1) == '\n') // added -ian
            return true; // added -ian
        else // added -ian
            return false; // added -ian
    } // added -ian

    private boolean notAlphaPrev() { // added -ian
        int ctr = current - 1; // added -ian
        // System.out.println(ctr); //added -ian
        while (!isAlphaNumeric(source.charAt(ctr)) && source.charAt(ctr) != '\n') { // added -ian
            ctr--; // added -ian
        } // added -ian
        if (isAlphaNumeric(source.charAt(ctr))) { // added -ian
            // System.out.println(ctr); //added -ian
            // System.out.println(source.charAt(ctr)); //added -ian
            return false; // added -ian
        } else { // added -ian
            // System.out.println("ALLOWED"); //added -ian
            return true; // added -ian
        } // added -ian
    } // added -ian

    private void string() {
        while (peek() != '"' && !isAtEnd()) {
            if (peek() == '\n') line++;
            advance();
        }

        if (isAtEnd()) {
            CFPL.error(line, "Unterminated string.");
            return;
        }

        //If closing tag '"' is found
        advance();

        String value = source.substring(start + 1, current-1);
        TokenType isBool = keywords.get(value);

        try {
            switch (isBool) {
            case TRUE:
                addToken(TRUE);
                break;
            case FALSE:
                addToken(FALSE);
                break;
            case NEXT_LINE:
                addToken(NEXT_LINE);
                break;
            default:
                addToken(STRING, value);
                break;
        }
        } catch (Exception e) {
            addToken(STRING, value);
        }
        
       
    }

    private void charac() {
        while (peek() != '\'' && !isAtEnd()) {
            advance();
        }

        if (isAtEnd()) {
            CFPL.error(line, "Unterminated character.");
            return;
        }

        // If closing tag '"' is found
        advance();

        String value = source.substring(start + 1, current - 1);

        if (value.length() != 1) {
            CFPL.error(line, value + " is not a character");
        }
        addToken(CHAR, (char)value.charAt(0));
    }

    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private void number() {
        while (isDigit(peek()))
            advance();
        
        //If fraction is found
        if (peek() == '.' && isDigit(peekNext())) {

            //consume '.'
            advance();

            while(isDigit(peek()))
                advance();

            
        }

       String value = source.substring(start, current);
        if (value.indexOf(".") != -1) {
            addToken(NUMBER, Double.parseDouble(value));
        } else {
            addToken(NUMBER, Integer.parseInt(value));
        }
    
        
    }

    private char peekNext() { 
        if (current + 1 >= source.length()) {
            return '\0';
        } 
        return source.charAt(current + 1);

    }

    private boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') ||
                (c >= 'A' && c <= 'Z') ||
                c == '_';
    }

    private boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isDigit(c);
    }

    private void identifier() {
        while (isAlphaNumeric(peek())) {
            advance();
        }
        
        if (peek() == ':') {
            advance();
        }

        String text = source.substring(start, current);

        TokenType type = keywords.get(text);

        if (type == null)
            type = IDENTIFIER; //x , y, name
        addToken(type);
    }

    private void escapeCode() {
        while (peek() != ']' && !isAtEnd()) {
            advance();
        }

        if (peekNext() == ']')
            advance();

        if (isAtEnd()) {
            CFPL.error(line, "Unterminated escape code.");
            return;
        }
        
        advance();

        String value = source.substring(start + 2, current - 1);
        // If closing tag '"' is found
        if (peek() == '"')
            advance();


        if (value.length() != 1) {
            CFPL.error(line, value + " is not a character");
        }
        addToken(STRING, value);
    }
}