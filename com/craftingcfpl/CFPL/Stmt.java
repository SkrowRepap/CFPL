package com.craftingcfpl.CFPL;

import java.util.List;

abstract class Stmt {
  interface Visitor<R> {
    R visitExpressionStmt(Expression stmt);
    R visitPrintStmt(Print stmt);
    R visitVarStmt(Var stmt);
    R visitBlockStmt(Block stmt);
    R visitExecutableStmt(Executable stmt);
    R visitInputStmt(Input stmt);
  }

  static class Expression extends Stmt {
    Expression(Expr expression) {
      this.expression = expression;
    }

    final Expr expression;

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitExpressionStmt(this);
    }
  }

   static class Block extends Stmt {
     Block(List<Stmt> statements) {
      this.statements = statements;
    }

    final List<Stmt> statements;
    
    @Override
    public <R> R accept(Visitor<R> visitor) {
      return visitor.visitBlockStmt(this);
    }
  }

  static class Print extends Stmt {
    Print(Expr expression) {
      this.expression = expression;
    }

    final Expr expression;

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitPrintStmt(this);
    }
  }

  static class Var extends Stmt {
    Var(Token name, Expr initializer, Token dataType) {
      this.name = name;
      this.initializer = initializer;
      this.dataType = dataType;
    }

    void setDataType(Token dataType) {
        this.dataType = dataType;
    }

    final Token name;
    final Expr initializer;
    Token dataType;

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitVarStmt(this);
    }

  }

  public static class Executable extends Stmt {
    public Executable(List<Stmt> statements) {
      this.statements = statements;
    }

    @Override
    public <R> R accept(Visitor<R> visitor) {
      return visitor.visitExecutableStmt(this);
    }

    public final List<Stmt> statements;
  }

  public static class Input extends Stmt {
    public Input(List<Token> tokens) {
      this.tokens = tokens;
    }

    @Override
    public <R> R accept(Visitor<R> visitor) {
      return visitor.visitInputStmt(this);
    }

    public final List<Token> tokens;
  }

  abstract <R> R accept(Visitor<R> visitor);
}
