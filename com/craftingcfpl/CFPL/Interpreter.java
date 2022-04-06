package com.craftingcfpl.CFPL;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import javax.lang.model.util.ElementScanner14;

import com.craftingcfpl.CFPL.Stmt.While;



public class Interpreter implements
        Expr.Visitor<Object>, Stmt.Visitor<Void> {

    private Environment environment = new Environment();
    
    void interpret(List<Stmt> statements) {
        try {
            for (Stmt statement : statements) {
                execute(statement);
            }
        } catch (RuntimeError error) {
            CFPL.runtimeError(error);
        }
    }

    @Override
    public Void visitExecutableStmt(Stmt.Executable stmt) {
        executeExecutable(stmt.statements);
        return null;
    }

    void executeExecutable(List<Stmt> statements) {
        for (Stmt statement : statements) {
            execute(statement);
        }
    }

    private void execute(Stmt stmt) {
        stmt.accept(this);
    }
    
    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        executeBlock(stmt.statements, new Environment(environment));
        return null;
    }

    void executeBlock(List<Stmt> statements,
            Environment environment) {
        Environment previous = this.environment;
        try {
            this.environment = environment;

            for (Stmt statement : statements) {
                execute(statement);
            }
        } finally {
            this.environment = previous;
        }
    }

    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        while(isTruthy(evaluate(stmt.condition))) {
            execute(stmt.body);
        }
        return null;
    }

    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        if (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.thenBranch);
        } else if (stmt.elseBranch != null) {
            execute(stmt.elseBranch);
        }
        return null;
    }

    @Override
    public Object visitLogicalExpr(Expr.Logical expr) {
        Object left = evaluate(expr.left);

        if (expr.operator.type == TokenType.OR) {
            if (isTruthy(left))
                return left;
        } 
        
        if (expr.operator.type == TokenType.AND) {
            if (!isTruthy(left))
                return left;
        }

        return evaluate(expr.right);
    }



    @Override
    public Object visitAssignExpr(Expr.Assign expr) {
        Object value = evaluate(expr.value);
        environment.assign(expr.name, value);
        return value;
    }

    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        evaluate(stmt.expression);
        return null;
    }

    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        Object value = (evaluate(stmt.expression));
        System.out.println(stringify(value));
        return null;
    }

    @Override
    public Object visitVariableExpr(Expr.Variable expr) {
        return environment.get(expr.name);
    }

    //Bugs
    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        Object value = null;
        // if (stmt.initializer != null) {
            try {
                switch (stmt.dataType.type) {
                    case INT:
                        // value =  stmt.initializer == null ? (int)0 : (int) Double.valueOf((double)evaluate(
                        //         stmt.initializer)).intValue();
                        value = stmt.initializer == null ? (int) 0
                                : evaluate(stmt.initializer);

                        if (!value.getClass().getSimpleName().equals("Integer")) {
                            throw new RuntimeError(stmt.name,
                                    stmt.name.lexeme + " expects " + stmt.dataType.type + " but received "
                                            + value.getClass().getSimpleName() + " instead.");
                        }
                        break;
                    case CHAR:
                        value = stmt.initializer == null ? (char)' ' : (char) evaluate(stmt.initializer);
                        break;

                    case BOOL:
                        value = stmt.initializer == null ? (boolean)false : (boolean) evaluate(stmt.initializer);
                        break;
                    case FLOAT:
                        value = stmt.initializer == null ? (double)0.0 : (double) evaluate(stmt.initializer);
                        break;

                    case STRING:
                        value = (String) evaluate(stmt.initializer);
                        break;
                    default:
                        value = null;
                        break;
                }
            } catch (ClassCastException e) {
                CFPL.error(stmt.dataType, "Incorrect Datatype");
            }
        // }

        environment.define(stmt.name.lexeme, value, stmt.name.line);
        return null;
    }

    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        return expr.value;
    }

    @Override
    public Object visitUnaryExpr(Expr.Unary expr) {
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case NOT:
            case BANG:
                return !isTruthy(right);
            case MINUS:
                if (right instanceof Integer) 
                    return - (int) right;
                if (right instanceof Double)
                    return - (double) right;
            
        }

        // Unreachable.
        return null;
    }

    @Override
    public Object visitBinaryExpr(Expr.Binary expr) {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);
        // System.out.println(left.getClass().getSimpleName());
        // System.out.println(right.getClass().getSimpleName());
        switch (expr.operator.type) {

            case BANG_EQUAL:
                return !isEqual(left, right); 

            case EQUAL_EQUAL:
                return isEqual(left, right);

            case MODULO:
                if (left instanceof Integer && right instanceof Integer) {
                    return (int) left % (int) right;
                }
                throw new RuntimeError(expr.operator, "Modulo only accepts two integers!");

            case GREATER:
                checkNumberOperand(expr.operator, right);
                return ((Number)left).doubleValue() > ((Number)right).doubleValue();

            case GREATER_EQUAL:
                checkNumberOperand(expr.operator, right);
                return ((Number) left).doubleValue() >= ((Number) right).doubleValue();

            case LESS:
                checkNumberOperand(expr.operator, right);
                return ((Number)left).doubleValue() < ((Number) right).doubleValue();

            case LESS_EQUAL:
                checkNumberOperand(expr.operator, right);
                return ((Number) left).doubleValue() <= ((Number) right).doubleValue();

            case MINUS:
                checkNumberOperand(expr.operator, right);
                if (left instanceof Integer && right instanceof Integer) {
                    return (int) left - (int) right;
                }
                return ((Number) left).doubleValue() - ((Number) right).doubleValue();

            case SLASH:
                checkNumberOperand(expr.operator, right);
                if (left instanceof Integer && right instanceof Integer) {
                    return (int) left / (int) right;
                }
                return ((Number) left).doubleValue() / ((Number) right).doubleValue();

            case STAR:
                checkNumberOperand(expr.operator, right);
                if (left instanceof Integer && right instanceof Integer) {
                    return (int) left * (int) right;
                }
                
                return ((Number) left).doubleValue() * ((Number) right).doubleValue();
                
            case PLUS:
                if (left instanceof Integer && right instanceof Integer) {
                    return (int) left + (int) right;
                }
                if (left instanceof Double || right instanceof Double) {
                    return  ((Number)left).doubleValue() + ((Number)right).doubleValue();
                }
               if (left instanceof String && right instanceof String) {
                    return (String) left + (String) right;
                } 
                new RuntimeError(expr.operator, "Operands must be a number or a string.");
                break;

            case AMPERSAND:
                return left.toString() + "" + right.toString();

        }

        // Unreachable.
        return null;
    }

    private boolean isEqual(Object left, Object right) {
        if (left == null && right == null)
            return false;
        if (left == null)
            return false;

        return left.equals(right);
    }


    private boolean isTruthy(Object right) {
        if (right == null)
            return false;
        if (right instanceof Boolean)
            return (boolean) right;

        return true;
    }

    @Override
    public Object visitGroupingExpr(Expr.Grouping expr) {
        return evaluate(expr.expression);
    }

    private void checkNumberOperand(Token operator, Object operand) {
        if (operand instanceof Number)
            return;
        throw new RuntimeError(operator, "Operand must be a number.");
    }

    private Object evaluate(Expr expr) {
        return expr.accept(this);
    }


    private String stringify(Object object) {
        if (object == null)
            return "nil";

        if (object instanceof Double) {
            String text = object.toString();
            if (text.endsWith(".0")) {
                text = text.substring(0, text.length() - 2);
            }
            return text.toString();
        }

        return object.toString();
    }

    @Override
    public Void visitInputStmt(Stmt.Input input) {

        System.out.println("[Input]");

        InputStreamReader inputStreamReader = new InputStreamReader(System.in);
        BufferedReader reader = new BufferedReader(inputStreamReader);

        String inputs;

        try {
            // [1, 2]
            inputs = reader.readLine();
            String[] values = inputs.split(",");
        
            if (inputs == null || values.length != input.tokens.size()) {
                CFPL.error(input.tokens.get(0), "Error you did not enter values");

                return null;
            }

            for (int i = 0; i < input.tokens.size(); i++) {
                Object fValue = values[i]; // int
                String value = values[i];
                Token t = input.tokens.get(i);
                Expr.Variable var = new Expr.Variable(t); // variable a 
                Object currValue = environment.get(var.name); // 

                

                if (currValue != null) {
                    String dataType = currValue.getClass().getSimpleName();
                    try {
                        if (currValue instanceof Integer) {
                            fValue = Integer.valueOf(value);
                        } else if (currValue instanceof Double) {
                            fValue = Double.parseDouble(value);
                        } else if (currValue instanceof Character) {
                            if (value.length() > 1) {
                                throw new RuntimeError(var.name, "Expected a character");
                            } else if (value.length() == 1) {
                                fValue = value.charAt(0);
                            }
                        } else if (currValue instanceof Boolean) {
                            if (value.contains("TRUE"));
                                fValue = true;
                            if (value.contains("FALSE"));
                                fValue = false;
                        }
                    } catch (ClassCastException | NumberFormatException e) {
                        CFPL.runtimeError(new RuntimeError(var.name, "Error: Incorrect Datatype"));
                        fValue = getDataType(fValue);
                        
                    }
                }
                // System.out.println("debug cV: " + currValue);
                // System.out.println("debug fV: " + fValue);
                // System.out.println("debug V: " + value);
                environment.assign(var.name, fValue); 
            }
        } catch (NullPointerException | IOException e) {
            // TODO Auto-generated catch block
            System.out.println(e);
        }

        

        
        return null;
    }

    private Object getDataType (Object value) {
        String objStr = value.toString();
        if (objStr.length() == 1)
            return objStr.charAt(0);
        if (objStr.toLowerCase().contains("true"))
            return true;
        if (objStr.toLowerCase().contains("false"))
            return false;

        return ((Number)value);
    }
   
  
}
