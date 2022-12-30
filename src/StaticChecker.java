/*
 * File: StaticChecker.java
 * Date: Spring 2022
 * Auth:
 * Desc:
 */

import java.lang.reflect.Array;
import java.util.*;


// NOTE: Some of the following are filled in, some partly filled in,
// and most left for you to fill in. The helper functions are provided
// for you to use as needed. 


public class StaticChecker implements Visitor {

    // the symbol table
    private SymbolTable symbolTable = new SymbolTable();
    // the current expression type
    private String currType = null;
    // the program's user-defined (record) types and function signatures
    private TypeInfo typeInfo = null;

    //--------------------------------------------------------------------
    // helper functions:
    //--------------------------------------------------------------------

    // generate an error
    private void error(String msg, Token token) throws MyPLException {
        String s = msg;
        if (token != null)
            s += " near line " + token.line() + ", column " + token.column();
        throw MyPLException.StaticError(s);
    }

    // return all valid types
    // assumes user-defined types already added to symbol table
    private List<String> getValidTypes() {
        List<String> types = new ArrayList<>();
        types.addAll(Arrays.asList("int", "double", "bool", "char", "string",
                "void"));
        for (String type : typeInfo.types())
            if (symbolTable.get(type).equals("type"))
                types.add(type);
        return types;
    }

    // return the build in function names
    private List<String> getBuiltinFunctions() {
        return Arrays.asList("print", "read", "length", "get", "stoi",
                "stod", "itos", "itod", "dtos", "dtoi");
    }

    // check if given token is a valid function signature return type
    private void checkReturnType(Token typeToken) throws MyPLException {
        if (!getValidTypes().contains(typeToken.lexeme())) {
            String msg = "'" + typeToken.lexeme() + "' is an invalid return type";
            error(msg, typeToken);
        }
    }

    // helper to check if the given token is a valid parameter type
    private void checkParamType(Token typeToken) throws MyPLException {
        if (typeToken.equals("void"))
            error("'void' is an invalid parameter type", typeToken);
        else if (!getValidTypes().contains(typeToken.lexeme())) {
            String msg = "'" + typeToken.lexeme() + "' is an invalid return type";
            error(msg, typeToken);
        }
    }


    // helpers to get first token from an expression for calls to error

    private Token getFirstToken(Expr expr) {
        return getFirstToken(expr.first);
    }

    private Token getFirstToken(ExprTerm term) {
        if (term instanceof SimpleTerm)
            return getFirstToken(((SimpleTerm)term).rvalue);
        else
            return getFirstToken(((ComplexTerm)term).expr);
    }

    private Token getFirstToken(RValue rvalue) {
        if (rvalue instanceof SimpleRValue)
            return ((SimpleRValue)rvalue).value;
        else if (rvalue instanceof NewRValue)
            return ((NewRValue)rvalue).typeName;
        else if (rvalue instanceof IDRValue)
            return ((IDRValue)rvalue).path.get(0);
        else if (rvalue instanceof CallExpr)
            return ((CallExpr)rvalue).funName;
        else
            return getFirstToken(((NegatedRValue)rvalue).expr);
    }


    //---------------------------------------------------------------------
    // constructor
    //--------------------------------------------------------------------

    public StaticChecker(TypeInfo typeInfo) {
        this.typeInfo = typeInfo;
    }


    //--------------------------------------------------------------------
    // top-level nodes
    //--------------------------------------------------------------------

    public void visit(Program node) throws MyPLException {
        // push the "global" environment
        symbolTable.pushEnvironment();

        // (1) add each user-defined type name to the symbol table and to
        // the list of rec types, check for duplicate names
        for (TypeDecl tdecl : node.tdecls) {
            String t = tdecl.typeName.lexeme();
            if (symbolTable.nameExists(t))
                error("type '" + t + "' already defined", tdecl.typeName);
            // add as a record type to the symbol table
            symbolTable.add(t, "type");
            // add initial type info (rest added by TypeDecl visit function)
            typeInfo.add(t);
        }

        // TODO: (2) add each function name and signature to the symbol
        // table check for duplicate names
        for (FunDecl fdecl : node.fdecls) {
            String funName = fdecl.funName.lexeme();
            // make sure not redefining built-in functions
            if (getBuiltinFunctions().contains(funName)) {
                String m = "cannot redefine built in function " + funName;
                error(m, fdecl.funName);
            }
            // check if function already exists
            if (symbolTable.nameExists(funName))
                error("function '" + funName + "' already defined", fdecl.funName);

            symbolTable.add(fdecl.funName.lexeme(),fdecl.returnType.lexeme());
//            typeInfo.add()
            // TODO: Build the function param names and signature.
            // ...

            for(FunParam param: fdecl.params){
                if (symbolTable.nameExists(param.paramName.lexeme())){
                    //error("Param name already used",param.paramName);
                }
                symbolTable.add(param.paramName.lexeme(),param.paramType.lexeme());
            }




            // make sure the return type is a valid type
            checkReturnType(fdecl.returnType);
            // add to the symbol table as a function
            symbolTable.add(funName, "fun");
            // add to typeInfo
            typeInfo.add(funName);
            for (FunParam param : fdecl.params){
                //System.out.println("help "+param.paramType.lexeme());
                typeInfo.add(funName,param.paramName.lexeme(),param.paramType.lexeme());
                //System.out.println("hi:"+typeInfo.get("f", "x")+"end");
            }

            // TODO: add each formal parameter as a component type
            // ...

            // add the return type
            typeInfo.add(funName, "return", fdecl.returnType.lexeme());
        }

        // TODO: (3) ensure "void main()" defined and it has correct
        // signature
        // ...
            symbolTable.nameExists("main");
            if (!symbolTable.nameExists("main")){
               String m = "main() not declared";
               Token T = null;
                error(m,T);
            }
            if(!typeInfo.get("main","return").equals("void")){
                Token T = null;
                error("main must return type void",T);
            }
            if (typeInfo.components("main").size()>1){
                Token T = null;
                error("main takes in zero arguments",T);
            }

        // check each type and function
        for (TypeDecl tdecl : node.tdecls)
            tdecl.accept(this);
        for (FunDecl fdecl : node.fdecls)
            fdecl.accept(this);

        // all done, pop the global table
        symbolTable.popEnvironment();
    }


    public void visit(TypeDecl node) throws MyPLException {
        for (int i =0; i < node.vdecls.size();i++) {
            node.vdecls.get(i).accept(this);
            //System.out.println("in: "+currType);
            typeInfo.add(node.typeName.lexeme(),node.vdecls.get(i).varName.lexeme(),currType);
        }
    }


    public void visit(FunDecl node) throws MyPLException {
        symbolTable.pushEnvironment();
        //System.out.println(symbolTable.environments);
        for (FunParam p : node.params){
            symbolTable.add(p.paramName.lexeme(),p.paramType.lexeme());
            typeInfo.add(node.funName.lexeme(),p.paramName.lexeme(),p.paramType.lexeme());
        }
        for (Stmt stmt : node.stmts){
            stmt.accept(this);
        }
        symbolTable.popEnvironment();

    }


    //--------------------------------------------------------------------
    // statement nodes
    //--------------------------------------------------------------------

    public void visit(VarDeclStmt node) throws MyPLException {

        node.expr.accept(this);
        //if (node.typeName.lexeme()!=null)
        //System.out.println(node.typeName.lexeme());
        //System.out.println(node.varName.lexeme());
        if (symbolTable.nameExistsInCurrEnv(node.varName.lexeme())){
            error("ReDeclaring " + node.varName.lexeme(),node.varName);
        }
        if (node.typeName!=null){
            if (!node.typeName.lexeme().equals(currType) & !currType.equals("void")){
                String m = "Expecting " + node.typeName.lexeme()+
                        " Got "+ currType;
                error(m, node.varName);

            }
            //System.out.println(node.typeName.lexeme());
            symbolTable.add(node.varName.lexeme(),node.typeName.lexeme());
            currType = node.typeName.lexeme();
        }
        else {
            if (currType=="void"){
               // error("implcit decleration canot be used with type void",node.varName);
            }
            symbolTable.add(node.varName.lexeme(),currType);
        }
    }


    public void visit(AssignStmt node) throws MyPLException {
        node.expr.accept(this);
        String rhsType = currType;
        String varName = "";
        if (node.lvalue.size()==1){
            varName = node.lvalue.get(0).lexeme();


        if(!symbolTable.nameExists((varName))){
            String m = varName + " is not defined";
            error(m, node.lvalue.get(0));
        }
        String lhsType = symbolTable.get(varName);
        if (rhsType.equals("void") && !lhsType.equals(rhsType)){
            String m = "expecting " + lhsType + ", found "+ rhsType;
            error(m,getFirstToken(node.expr));
        }
        if (!lhsType.equals(rhsType)){
            String m = "expecting " + lhsType + ", found "+ rhsType;
            error(m,getFirstToken(node.expr));
        }
        }
        else {
            int i = 1;
            int j = node.lvalue.size();
            String type = null;
            while (i < node.lvalue.size()) {
                varName = node.lvalue.get(0).lexeme();
                type = symbolTable.get(varName);

//                System.out.println("path "+typeInfo.get(type,node.lvalue.get(i).lexeme()));
//                System.out.println(typeInfo.get(type,node.lvalue.get(node.lvalue.size()-j).lexeme()));
//                System.out.println(rhsType);
                //type = typeInfo.get(lhsType,node.lvalue.get(i).lexeme())

                j -= 1;
                i = i + 1;
            }
            if (typeInfo.get(type, node.lvalue.get(node.lvalue.size() - 1).lexeme()) != rhsType) {
                // System.out.println(rhsType + " "+ node.lvalue.get(i).lexeme());
                //error("Not declared in type", node.lvalue.get(0));
            }

            currType = symbolTable.get(node.lvalue.get(node.lvalue.size() - 1).lexeme());
        }


        }



    private void if_help(BasicIf basic ) throws MyPLException {
        symbolTable.pushEnvironment();
        basic.cond.accept(this);
        if(!currType.equals("bool")){
            error("Expecting Boolean",getFirstToken(basic.cond));
        }
        for (Stmt stmt : basic.stmts){
            stmt.accept(this);
        }
        symbolTable.popEnvironment();
    }
    public void visit(CondStmt node) throws MyPLException {
        if_help(node.ifPart);

        for(BasicIf basicIf :node.elifs){
            if_help(basicIf);
        }

        if (node.elseStmts != null){
            for(Stmt stmt :node.elseStmts){
                symbolTable.pushEnvironment();
                stmt.accept(this);
                symbolTable.popEnvironment();
            }
        }
    }


    public void visit(WhileStmt node) throws MyPLException {
        symbolTable.pushEnvironment();
            node.cond.accept(this);
            if (!currType.equals("bool")) {
               // error("not A Boolean ",getFirstToken(node.cond));
            }
            for (Stmt stmt:node.stmts){
                stmt.accept(this);
            }
        symbolTable.popEnvironment();
    }


    public void visit(ForStmt node) throws MyPLException {
        symbolTable.pushEnvironment();
        node.start.accept(this);
        if (currType != "int"){
            error("Expecting int",getFirstToken(node.start));
        }
        symbolTable.add(node.varName.lexeme(),"int");
        for (Stmt stmt:node.stmts){
            stmt.accept(this);
        }
        node.end.accept(this);
        if (currType!= "int"){
            error("Expecting int",getFirstToken(node.end));
        }
        symbolTable.popEnvironment();
    }


    public void visit(ReturnStmt node) throws MyPLException {


         node.expr.accept(this);
         Set<String> e = typeInfo.types();

         List<Map<String, String>> tp = symbolTable.environments;
         String type = typeInfo.get(e.iterator().next(),"return");


         if (!type.equals(currType)){

             if (currType == null)
                 currType = "void";
            if(!currType.equals("void")) {
                Token t = new Token(TokenType.RETURN,"",1,2);
                //error(type + " Returning wrong type", t);
            }
         }
    }

    public void visit(DeleteStmt node) throws MyPLException {

        String type = symbolTable.get(node.varName.lexeme());

        List<String> types = Arrays.asList("int","void","double","string","bool");
        for (String s : types){
            if (s.equals(type)|type.equals("fun")){
                error("can only delete Structured types",node.varName);
            }
        }


    }


    //----------------------------------------------------------------------
    // statement and rvalue node
    //----------------------------------------------------------------------

    private void checkBuiltIn(CallExpr node) throws MyPLException {
        //
        //for (Expr e : node.args)
        //System.out.println(getFirstToken(e).lexeme);
        String funName = node.funName.lexeme();
        if (funName.equals("print")) {
            // has to have one argument, any type is allowed
            if (node.args.size() != 1)
                error("print expects one argument", node.funName);
            currType = "void";
        }
        else if (funName.equals("read")) {
            // no arguments allowed
            if (node.args.size() != 0)
                error("read takes no arguments", node.funName);
            currType = "string";
        }
        else if (funName.equals("length")) {
            // one string argument
            if (node.args.size() != 1)
                error("length expects one argument", node.funName);
            Expr e = node.args.get(0);
            e.accept(this);
            if (!currType.equals("string"))
                error("expecting string in length", getFirstToken(e));
            currType = "int";
        }
        else if (funName.equals("get")) {
            node.args.get(0).accept(this);
            String get1 = currType;
            node.args.get(1).accept(this);
            String second = currType;
            if(node.args.size()!= 2)
                error("expecting 2 arguments",node.funName);
            if(!(get1.equals("int")&second.equals("string"))){
                error("Get takes args int and string",node.args.get(0).op);
            }
            currType = "char";
        }
        else if (funName.equals("stoi")) {

            if (node.args.size() != 1)
                error("stoi expects one argument", node.funName);
            Expr e = node.args.get(0);
            e.accept(this);
            if (!currType.equals("string"))
                error("expecting string in stoi", getFirstToken(e));
            currType = "int";

        }
        else if (funName.equals("stod")) {

            if(node.args.size()!=1)
                error("stod expects one argument", node.funName);
            Expr e = node.args.get(0);
            e.accept(this);
            if (!currType.equals("string"))
                error("expecting string in stod", getFirstToken(e));
            currType = "double";

        }
        else if (funName.equals("itos")) {

            if(node.args.size()!=1)
                error("itos expects one argument", node.funName);
            Expr e = node.args.get(0);
            e.accept(this);
            if (!currType.equals("int"))
                error("expecting string in itos", getFirstToken(e));
            currType = "string";

        }
        else if (funName.equals("itod")) {

            if(node.args.size()!=1)
                error("itod expects one argument", node.funName);
            Expr e = node.args.get(0);
            e.accept(this);
            if (!currType.equals("int"))
                error("expecting string in itod", getFirstToken(e));
            currType = "double";

        }
        else if (funName.equals("dtos")) {

            if(node.args.size()!=1)
                error("dtos expects one argument", node.funName);
            Expr e = node.args.get(0);
            e.accept(this);
            if (!currType.equals("double"))
                error("expecting string in itod", getFirstToken(e));
            currType = "string";

        }
        else if (funName.equals("dtoi")) {

            if(node.args.size()!=1)
                error("dtoi expects one argument", node.funName);
            Expr e = node.args.get(0);
            e.accept(this);
            if (!currType.equals("double"))
                error("expecting string in dtoi", getFirstToken(e));
            currType = "int";

        }
    }


    public void visit(CallExpr node) throws MyPLException {

        for (String s : getBuiltinFunctions()){
            //System.out.println(s);
            if (s.equals(node.funName.lexeme())){
                checkBuiltIn(node);
                return;
            }
        }
        if (!symbolTable.nameExists(node.funName.lexeme())){
            error("Function "+ node.funName.lexeme() + " Dosen't exist",node.funName);
        }

        String name =node.funName.lexeme();
        //String type = typeInfo.get(name,"return");

        //TODO: need to check if same amount of params
       // System.out.println(name);
        Set<String> temp = typeInfo.components(name);
       // System.out.println("hi");
        //System.out.println(temp.size());
        //System.out.println(node.args.size());
        if (temp.size()-1!=node.args.size()){
            error("Mismatched arg sizes",getFirstToken(node.args.get(0)));
        }

        int i = 0;
        for (Expr expr : node.args){
          //  System.out.println();
            expr.accept(this);

        }
        currType =  symbolTable.get(node.funName.lexeme());

    }


    //----------------------------------------------------------------------
    // rvalue nodes
    //----------------------------------------------------------------------

    public void visit(SimpleRValue node) throws MyPLException {
        TokenType tokenType = node.value.type();
       // System.out.println("here 22");
        if (node.value.type() == TokenType.ID){
            if (!symbolTable.nameExistsInCurrEnv(node.value.lexeme())){
                error("called before declared",node.value);
            }
        }
        if (tokenType == TokenType.INT_VAL)
            currType = "int";
        else if (tokenType == TokenType.DOUBLE_VAL)
            currType = "double";
        else if (tokenType == TokenType.BOOL_VAL)
            currType = "bool";
        else if (tokenType == TokenType.CHAR_VAL)
            currType = "char";
        else if (tokenType == TokenType.STRING_VAL)
            currType = "string";
        else if (tokenType == TokenType.NIL)
            currType = "void";
    }


    public void visit(NewRValue node) throws MyPLException {
        boolean e = true;

        String sn = node.typeName.lexeme();
        //System.out.println(sn);
        currType = node.typeName.lexeme();
        for( String s: typeInfo.types()){
            if (s.equals(sn)){
                e = false;
            }
        }
        if (e){
            error("Type not declared",node.typeName);
        }
        if (!symbolTable.get(node.typeName.lexeme()).equals("type")){
            error("Expecting type",node.typeName);
        }
    }


    public void visit(IDRValue node) throws MyPLException {
        int i = 1;
        String type = symbolTable.get(node.path.get(0).lexeme());
        while (i < node.path.size()){
              //  System.out.println("path"+typeInfo.get(type,node.path.get(i).lexeme()));
            if(typeInfo.get(type,node.path.get(i).lexeme())==null){
                //error("Not declared in type",node.path.get(0));
            }
            i= i+1;
        }

        currType = symbolTable.get(node.path.get(node.path.size()-1).lexeme());
    }


    public void visit(NegatedRValue node) throws MyPLException {
        node.expr.accept(this);
    }


    //----------------------------------------------------------------------
    // expression node
    //----------------------------------------------------------------------

    public void visit(Expr node) throws MyPLException {
        String lhs = null;
        String rhs = null;


        if (node.first != null) {
            node.first.accept(this);
            lhs = currType;
        }
        if (node.rest != null){
            node.rest.accept(this );
            rhs = currType;
        }


        if (node.op != null) {
            String op = node.op.lexeme();
            if (op.equals("+")){
                if (lhs.equals("string")||lhs.equals("char")){
                    if (rhs.equals("string")||lhs.equals("char")){
                        currType = "string";
                    }
                    else{
                        //error("Strings and chars can only be added to themselves",getFirstToken(node));
                    }
                }
            }
            if (op.equals("or")||op.equals("and")){
                if (!(lhs.equals("bool") & rhs.equals("bool"))){
                    error("or opperands must be booleans",getFirstToken(node));
                }
            }

            if (is_PMD(node.op.lexeme())) {
                if (op.equals("-")){
                    if(lhs.equals("string")|lhs.equals("char")|rhs.equals("string")|rhs.equals("char")){
                        error("invalid srithmetic",getFirstToken(node));
                    }
                    currType = rhs;
                }
                if (op.equals("+")){
                    if (lhs.equals(rhs)&& lhs.equals("char")){
                        error("can't add chars",getFirstToken(node));
                    }
                }
               if (rhs!=lhs){
                   //error("diffrent opperands",getFirstToken(node));
               }
               if (rhs.equals("bool")|lhs.equals("bool")){
                   error("can't add type bool",getFirstToken(node));
               }
               currType = lhs;
            }
            if (node.op.lexeme().equals("%")){
                if (!lhs.equals("int")|!rhs.equals("int")){
                    error("modulo only accepts ints",getFirstToken(node));
                }
                currType = "int";
            }
            if(node.op.lexeme().equals("<")||node.op.lexeme().equals(">")||node.op.lexeme().equals("<=")||
                    node.op.lexeme().equals(">=")){
                if (!rhs.equals(lhs)){
                    error("types must match",getFirstToken(node));
                }
                if (!is_idcs(lhs) || !is_idcs(rhs)){
                    error("Comparitors can compare 'int' 'string' 'char' 'double'",getFirstToken(node));
                }
                currType = "bool";
            }
            if(op.equals("==")||op.equals("!=")){
                if(!(rhs.equals("void")|lhs.equals("void"))){
                if (!rhs.equals(lhs)){
                    error("Must compare same types",getFirstToken(node));
                }}
                currType = "bool";
            }
        }

        if (node.logicallyNegated == true){
            if(!(currType.equals("bool")|currType.equals("int")|currType.equals("double"))){
                //error("not can only be used on boolean expr"+currType,node.op);
            }
        }

    }
    private boolean is_idcs(String s){
        if (s.equals("int")||s.equals("double")||s.equals("char")||s.equals("string")){
            return  true;
        }
        return false;
    }
    private boolean is_PMD(String lexme ) {
        if (lexme.equals("+") || lexme.equals("-")||lexme.equals("*")|| lexme.equals("/")){
            return true;
        }
        return  false;
    }


    //----------------------------------------------------------------------
    // terms
    //----------------------------------------------------------------------

    public void visit(SimpleTerm node) throws MyPLException {
        node.rvalue.accept(this);
    }


    public void visit(ComplexTerm node) throws MyPLException {
        node.expr.accept(this);
    }
}
