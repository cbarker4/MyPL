/*
 * File: PrintVisitor.java
 * Date: Spring 2022
 * Auth:
 * Desc:
 */

import java.io.PrintStream;


public class PrintVisitor implements Visitor {

    // output stream for printing
    private PrintStream out;
    // current indent level (number of spaces)
    private int indent = 0;
    // indentation amount
    private final int INDENT_AMT = 2;

    //------------------------------------------------------------
    // HELPER FUNCTIONS
    //------------------------------------------------------------

    private String getIndent() {
        return " ".repeat(indent);
    }

    private void incIndent() {
        indent += INDENT_AMT;
    }

    private void decIndent() {
        indent -= INDENT_AMT;
    }

    //------------------------------------------------------------
    // VISITOR FUNCTIONS
    //------------------------------------------------------------

    // Hint: To help deal with call expressions, which can be statements
    // or expressions, statements should not indent themselves and add
    // newlines. Instead, the function asking statements to print
    // themselves should add the indent and newlines.


    // constructor
    public PrintVisitor(PrintStream printStream) {
        out = printStream;
    }


    // top-level nodes

    @Override
    public void visit(Program node) throws MyPLException {
        System.out.println("HI");
        // print type decls first
        for (TypeDecl d : node.tdecls)
            d.accept(this);
        // print function decls second
        for (FunDecl d : node.fdecls)
            d.accept(this);
    }

    @Override
    public void visit(TypeDecl node) throws MyPLException {
        System.out.print("type ");
        System.out.print(node.typeName.lexeme());
        System.out.println("{");
        indent = indent + INDENT_AMT;
        for (VarDeclStmt d : node.vdecls){
            d.accept(this);
        }
        indent = indent - INDENT_AMT;
        System.out.println(getIndent()+"}");
        System.out.println("");


    }

    @Override
    public void visit(FunDecl node) throws MyPLException {
        System.out.print("fun ");
        System.out.print(node.returnType.lexeme() + " ");
        System.out.print(node.funName.lexeme());
        System.out.print("(");
        for (int i =0; i <node.params.size();i++){
            System.out.print(node.params.get(i).paramType.lexeme()+ " ");
            System.out.print(node.params.get(i).paramName.lexeme());
            if (i < node.params.size()-1)
                System.out.print(",");
        }
        System.out.println(") {");
        indent = indent + INDENT_AMT;
        for (Stmt d : node.stmts){
            d.accept(this);

        }

        indent = indent - INDENT_AMT;
        System.out.println(getIndent()+"}");
    }

    @Override
    public void visit(VarDeclStmt node) throws MyPLException {
        System.out.print(getIndent()+"var ");
        if (node.typeName != null){
            System.out.print(node.typeName.lexeme());}
        System.out.print(" " + node.varName.lexeme()+" = ");
        node.expr.accept(this);
        System.out.println("");


    }

    @Override
    public void visit(AssignStmt node) throws MyPLException {
        int i = 0;
        //System.out.print(node.lvalue.get(0).lexeme());
        System.out.print(getIndent());
        while (i<node.lvalue.size()){
            if (i != node.lvalue.size()-1)
                System.out.print(node.lvalue.get(i).lexeme()+".");
            else {
                System.out.print(node.lvalue.get(i).lexeme());
                if (i != node.lvalue.size()-1)
                System.out.print(".");
            }
            i =i + 1;
        }
        System.out.print(" = ");
        node.expr.accept(this);
        System.out.println("");
    }

    @Override
    public void visit(CondStmt node) throws MyPLException {
        System.out.print(getIndent()+"if ");
        node.ifPart.cond.accept(this);
        System.out.println("{");
        indent =INDENT_AMT + indent;
        for (Stmt d :node.ifPart.stmts) {
            d.accept(this);
        }
        indent = indent - INDENT_AMT;
        System.out.println(getIndent()+"}");

    }

    @Override
    public void visit(WhileStmt node) throws MyPLException {
        System.out.print(getIndent()+"while ");
        node.cond.accept(this);
        System.out.println("{");
        indent =INDENT_AMT + indent;
        for (Stmt d :node.stmts) {
            d.accept(this);
        }
        indent = indent - INDENT_AMT;
        System.out.println(getIndent()+"}");


    }

    @Override
    public void visit(ForStmt node) throws MyPLException {
        System.out.print(getIndent()+"for ");
        System.out.print(node.varName.lexeme() + " from ");
        node.start.accept(this);
        if (node.upto){
            System.out.print(" upto ");
        }
        else {
            System.out.print(" downto ");
        }
        node.end.accept(this);
        System.out.println("{");
        indent =INDENT_AMT + indent;

        for (Stmt d :node.stmts) {
            d.accept(this);
        }
        indent = indent - INDENT_AMT;
        System.out.println(getIndent()+"}");

    }

    @Override
    public void visit(ReturnStmt node) throws MyPLException {
        System.out.print(getIndent()+"return ");
        if (node.expr!=null) {
            node.expr.accept(this);

        }
        System.out.println();

    }

    @Override
    public void visit(DeleteStmt node) throws MyPLException {
        System.out.print(getIndent()+"delete " + node.varName.lexeme());
    }

    @Override
    public void visit(CallExpr node) throws MyPLException {
        System.out.print(getIndent()+node.funName.lexeme() + "(" );

        if (node.args != null) {
            for (Expr d :node.args){
                d.accept(this);
                if (node.args.get(node.args.size()-1) != d)
                    System.out.print(", ");
            }
//            if (node.args.size() == 1) {
//                node.args.get(0).first.accept(this);
//            } else if (node.args.size() > 1) {
//                node.args.get(0).first.accept(this);
//                int i = 1;
//                while (i < node.args.size()) {
//                    node.args.get(i).first.accept(this);
//                    i = i + 1;
//                    System.out.print(",");
//                }
//            }
       }
        System.out.println(")");

    }

    @Override
    public void visit(SimpleRValue node) throws MyPLException {
        if (node.value.type() == TokenType.STRING_VAL) {
            System.out.print("\"");
            System.out.print(node.value.lexeme());
            System.out.print("\"");
        }
        else if (node.value.type() == TokenType.CHAR_VAL) {
            System.out.print("'");
            System.out.print(node.value.lexeme());
            System.out.print("'");
        }
        else
            System.out.print(node.value.lexeme());
    }

    @Override
    public void visit(NewRValue node) throws MyPLException {
        System.out.print("new ");
        System.out.print(node.typeName.lexeme());

    }

    @Override
    public void visit(IDRValue node) throws MyPLException {
        for (Token d :node.path){
            String temp = d.lexeme();
            System.out.print(d.lexeme() );
            if (node.path.get(node.path.size()-1)!=d){
                if (!temp.equals("neg"))
                    System.out.print(".");
                else{
                    System.out.print(" ");
                }
            }
        }

    }

    @Override
    public void visit(NegatedRValue node) throws MyPLException {
        node.expr.logicallyNegated = true;
        node.expr.accept(this);

    }

    @Override
    public void visit(Expr node) throws MyPLException {
        if (node.logicallyNegated){
            System.out.print("not ");
        }

        if (node.rest!=null){
            System.out.print("(");}

        if (node.first!=null){
            //System.out.print(node.first);
            node.first.accept(this);}
            //System.out.print("___");

        if (node.op!=null){
            System.out.print(" "+node.op.lexeme()+" ");

        }
       if (node.rest!=null){
           node.rest.accept(this);
           System.out.print(")");
       }

    }

    @Override
    public void visit(SimpleTerm node) throws MyPLException {
        if (node.rvalue!=null){
        node.rvalue.accept(this);}

    }

    @Override
    public void visit(ComplexTerm node) throws MyPLException {
        node.expr.accept(this);

    }


    // TODO: Finish the rest of the visitor functions ...


}
