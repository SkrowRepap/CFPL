package com.craftingcfpl.CFPL;

import java.util.HashMap;
import java.util.Map;

class Environment {

    final Environment enclosing;

    private final Map<String, Object> values = new HashMap<>();


    // Global or local
    Environment() {
        enclosing = null;
    }

    Environment(Environment enclosing) {
        this.enclosing = enclosing;
    }

    void define(String name, Object value) {
        values.put(name, value);
    }

    Object get(Token name) {
        if (values.containsKey(name.lexeme)) {
            return values.get(name.lexeme);
        }
        

        if (enclosing != null) {
            return enclosing.get(name);
        }

        throw new RuntimeError(name,
                "Undefined variable '" + name.lexeme + "'.");
    }


    void assign(Token name, Object value) {
        if (values.containsKey(name.lexeme)) {
            Object ob = values.get(name.lexeme);
            // System.out.println("debug: " + value );
            // System.out.println("debug: " + values );
            
            if (value != null) {
                if (!ob.getClass().getSimpleName().equals(value.getClass().getSimpleName())) 
                    throw new RuntimeError(name,
                            name.lexeme + " expects " + ob.getClass().getSimpleName() + " but received " + value.getClass().getSimpleName() + " instead.");
            }
    
            
            values.put(name.lexeme, value);
            return;
        }
        

        if (enclosing != null) {
            enclosing.assign(name, value);
            return;
        }

        throw new RuntimeError(name,
                "Undefined variable '" + name.lexeme + "'.");
    }

    
}