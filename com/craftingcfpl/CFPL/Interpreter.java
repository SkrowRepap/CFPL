package com.craftingcfpl.CFPL;

import java.util.List;


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

    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        Object value = null;
        if (stmt.initializer != null) {
            try {
                switch (stmt.dataType.type) {
                    case INT:
                        value =  (double) evaluate(stmt.initializer);
                        break;
                    case CHAR:
                        value = (char) evaluate(stmt.initializer);
                        break;

                    case BOOL:
                        value = (boolean) evaluate(stmt.initializer);
                        break;
                    case FLOAT:
                        value = (double) evaluate(stmt.initializer);
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
        }

        environment.define(stmt.name.lexeme, value);
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
                return -(double) right;
            
        }

        // Unreachable.
        return null;
    }

    @Override
    public Object visitBinaryExpr(Expr.Binary expr) {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case BANG_EQUAL:
                return !isEqual(left, right);
            case EQUAL_EQUAL:
                return isEqual(left, right);
            case GREATER:
                checkNumberOperand(expr.operator, right);
                return (double) left > (double) right;
            case GREATER_EQUAL:
                checkNumberOperand(expr.operator, right);
                return (double) left >= (double) right;
            case LESS:
                checkNumberOperand(expr.operator, right);
                return (double) left < (double) right;
            case LESS_EQUAL:
                checkNumberOperand(expr.operator, right);
                return (double) left <= (double) right;
            case MINUS:
                checkNumberOperand(expr.operator, right);
                return (double) left - (double) right;
            case SLASH:
                checkNumberOperand(expr.operator, right);
                return (double) left / (double) right;
            case STAR:
                checkNumberOperand(expr.operator, right);
                return (double) left * (double) right;
            case AND:
                return (boolean) left && (boolean)right;
            case OR:
                return (boolean) left || (boolean) right;
            case PLUS:
                if (left instanceof Double && right instanceof Double) {
                    return (double) left + (double) right;
                }
               if (left instanceof String && right instanceof String) {
                    return (String) left + (String) right;
                } 
                new RuntimeError(expr.operator, "Operands must be a number or a string.");
                break;
            case AMPERSAND:
                return left + "" + right;

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
        if (operand instanceof Double)
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

  
}
