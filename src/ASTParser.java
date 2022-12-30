/*
 * File: ASTParser.java
 * Date: Spring 2022
 * Auth:
 * Desc:
 */


public class ASTParser {

    private Lexer lexer = null;
    private Token currToken = null;
    private final boolean DEBUG = false;

    /**
     */
    public ASTParser(Lexer lexer) {
        this.lexer = lexer;
    }

    /**
     */
    public Program parse() throws MyPLException
    {
        // <program> ::= (<tdecl> | <fdecl>)*
        Program progNode = new Program();
        advance();
        while (!match(TokenType.EOS)) {
            if (match(TokenType.TYPE)){
                TypeDecl t = new TypeDecl();
                tdecl(t);
                progNode.tdecls.add(t);
            }
            else {
                FunDecl f = new FunDecl();
                fdecl(f);
                progNode.fdecls.add(f);
            }
        }
        advance(); // eat the EOS token
        return progNode;
    }


    //------------------------------------------------------------
    // Helper Functions
    //------------------------------------------------------------

    // get next token
    private void advance() throws MyPLException {
        currToken = lexer.nextToken();
    }

    // advance if current token is of given type, otherwise error
    private void eat(TokenType t, String msg) throws MyPLException {
        if (match(t))
            advance();
        else
            error(msg);
    }

    // true if current token is of type t
    private boolean match(TokenType t) {
        return currToken.type() == t;
    }

    // throw a formatted parser error
    private void error(String msg) throws MyPLException {
        String s = msg + ", found '" + currToken.lexeme() + "' ";
        s += "at line " + currToken.line();
        s += ", column " + currToken.column();
        throw MyPLException.ParseError(s);
    }

    // output a debug message (if DEBUG is set)
    private void debug(String msg) {
        if (DEBUG)
            System.out.println("[debug]: " + msg);
    }

    private boolean isPrimitiveType() {
        return match(TokenType.INT_TYPE) || match(TokenType.DOUBLE_TYPE) ||
                match(TokenType.BOOL_TYPE) || match(TokenType.CHAR_TYPE) ||
                match(TokenType.STRING_TYPE);
    }

    // return true if current token is a (non-id) primitive value
    private boolean isPrimitiveValue() {
        return match(TokenType.INT_VAL) || match(TokenType.DOUBLE_VAL) ||
                match(TokenType.BOOL_VAL) || match(TokenType.CHAR_VAL) ||
                match(TokenType.STRING_VAL);
    }

    private boolean isOperator() {
        return match(TokenType.PLUS) || match(TokenType.MINUS) ||
                match(TokenType.DIVIDE) || match(TokenType.MULTIPLY) ||
                match(TokenType.MODULO) || match(TokenType.AND) ||
                match(TokenType.OR) || match(TokenType.EQUAL) ||
                match(TokenType.LESS_THAN) || match(TokenType.GREATER_THAN) ||
                match(TokenType.LESS_THAN_EQUAL) || match(TokenType.GREATER_THAN_EQUAL) ||
                match(TokenType.NOT_EQUAL);
    }
    private boolean isrvalue(){
        return isPrimitiveValue() || match(TokenType.NIL) || match(TokenType.NEW)|| match(TokenType.ID)||match(TokenType.NEG);


    }

    //------------------------------------------------------------
    // Recursive Descent Functions
    //------------------------------------------------------------


    // TODO: Add your recursive descent functions from HW-3
    // and extend them to build up the AST

    void tdecl(TypeDecl t) throws MyPLException{
        eat(TokenType.TYPE,"expecting Type ");
        t.typeName = currToken;
        eat (TokenType.ID,"Expecting ID");
        eat (TokenType.LBRACE,"Expecting left Brace");
        vdecls(t);
        eat (TokenType.RBRACE,"Expecting }");
    }


    void vdecls(TypeDecl t) throws MyPLException{
        while (match(TokenType.VAR)){
            VarDeclStmt v = new VarDeclStmt();
            vdecl_stmt(v);
            t.vdecls.add(v);
        }
    }


    void fdecl(FunDecl f) throws MyPLException {
        eat(TokenType.FUN, "Expecting fun");
        if (isPrimitiveType() | match(TokenType.VOID_TYPE)||match(TokenType.ID)){
            f.returnType = currToken;
            advance();
        }
        else{
            error("expected a type");
        }
        f.funName = currToken;

        eat(TokenType.ID, "Expecting ID");
        eat (TokenType.LPAREN,"Expecting ( ");
        params(f);
        eat(TokenType.RPAREN, "Expecting )");
        eat (TokenType.LBRACE,"Expecting {");
        stmts(f);
        eat(TokenType.RBRACE, "Expecting }");
    }

    void params(FunDecl f) throws MyPLException{
        if (!match(TokenType.RPAREN)) {
            FunParam param = new FunParam();
            dtype(param);
            param.paramName = currToken;

            f.params.add(param);
            eat(TokenType.ID, "Expecting ID");
            while (match(TokenType.COMMA)) {
                FunParam param1 = new FunParam();
                eat(TokenType.COMMA, "Expecting Comma");
                dtype(param1);
                param1.paramName = currToken;

                eat(TokenType.ID, "Expecting ID");
                f.params.add(param1);

            }
        }
    }

    void dtype(FunParam param) throws MyPLException {
        if (isPrimitiveType() || match(TokenType.ID)){
            param.paramType = currToken;
            advance();
        }
        else{error("Expected Primative type ");}

    }
    void stmts(FunDecl f) throws MyPLException {
        while(!match(TokenType.RBRACE)){
            stmt(f);
            if (match(TokenType.EOS)){error("Missing }");}
        }
    }

    void stmt(FunDecl f) throws MyPLException {
        if (match(TokenType.VAR)){
            VarDeclStmt v =  new VarDeclStmt();
            vdecl_stmt(v);
            f.stmts.add(v);
        }
        if (match(TokenType.ID)) {
            Token temp = currToken;
            advance();

            if (match(TokenType.DOT) || match(TokenType.ASSIGN)){
                AssignStmt a = new AssignStmt();
                a.lvalue.add(temp);
                assign_stmt(a);
                f.stmts.add(a);
            }
            if (match(TokenType.LPAREN)) {
                CallExpr c = new CallExpr();
                c.funName = temp;
                call_expr(c);
                f.stmts.add(c);
            }
        }
//        if (match(TokenType.ASSIGN)){
//            AssignStmt a = new AssignStmt();
//
//            assign_stmt(a);
//            f.stmts.add(a);
//        }
        if (match(TokenType.IF)){
            CondStmt c  = new CondStmt();
            cond_stmt(c);
            f.stmts.add(c);
        }
        if (match(TokenType.WHILE)) {
            WhileStmt w = new WhileStmt();
            while_stmt(w);
            f.stmts.add(w);
        }
        if (match(TokenType.FOR)){
            ForStmt fo = new ForStmt();
            for_stmt(fo);
            f.stmts.add(fo);
        }
        if (match(TokenType.RETURN)){
            ReturnStmt r = new ReturnStmt();
            ret_stmt(r);
            f.stmts.add(r);
        }
        if (match(TokenType.DELETE)){
            DeleteStmt d = new DeleteStmt();
            delete_stmt(d);
            f.stmts.add(d);
        }
    }




    void vdecl_stmt(VarDeclStmt v)throws MyPLException{
        eat(TokenType.VAR,"expecting Var");
        Token temp = null;

         if (isPrimitiveType()| currToken.type() == TokenType.ID){
            temp = currToken;
            advance();
        }
         if (currToken.type() == TokenType.ID){
             v.typeName = temp;
             v.varName = currToken;
             advance();
         }
         else
             v.varName = temp;

            eat(TokenType.ASSIGN, "expecating Assign");
            Expr e = new Expr();
            expr(e);
            v.expr = e;

    }

    private void assign_stmt(AssignStmt a) throws MyPLException {
        lvalue(a);
        eat(TokenType.ASSIGN,"Expected =");
        Expr e = new Expr();
        expr(e);
        a.expr = e;
    }

    private void lvalue(AssignStmt a) throws MyPLException {
        while (match(TokenType.DOT)){
            eat(TokenType.DOT,"Expected .");
            a.lvalue.add(currToken);
            eat(TokenType.ID,"Expected ID");

        }
    }

    private void cond_stmt(CondStmt c) throws MyPLException {
        eat(TokenType.IF,"Expected if");
        BasicIf B = new BasicIf();
        Expr e = new Expr();
        expr(e);
        B.cond = e;
        eat(TokenType.LBRACE,"Expected {");
        FunDecl f = new FunDecl();
        stmts(f);
        B.stmts = f.stmts;
        c.ifPart = B;
        eat(TokenType.RBRACE,"Expected }");
        condt(c);

    }

    private void condt(CondStmt c) throws MyPLException {
        BasicIf B = new BasicIf();
        if (match(TokenType.ELIF)) {
            advance();
            Expr e = new Expr();
            expr(e);
            eat(TokenType.LBRACE,"Expected {");
            FunDecl f = new FunDecl();
            stmts(f);
            B.stmts = f.stmts;
            B.cond = e;
            eat(TokenType.RBRACE,"Expected }");
            c.elifs.add(B);
            condt(c);
        }
        if (match(TokenType.ELSE)){
            advance();
            eat(TokenType.LBRACE,"Expected {");
            FunDecl f = new FunDecl();
            stmts(f);
            c.elseStmts = f.stmts;
            eat(TokenType.RBRACE,"Expected }");
            condt(c);
        }
    }
    private void while_stmt(WhileStmt w) throws MyPLException {
        advance();
        Expr e = new Expr();
        expr(e);
        eat(TokenType.LBRACE,"Expected {");
        FunDecl f = new  FunDecl();
        stmts(f);
        w.cond = e;
        w.stmts = f.stmts;
        eat(TokenType.RBRACE,"Expected }");
    }

    private void for_stmt(ForStmt fo) throws MyPLException {
        advance();
        fo.varName = currToken;
        eat(TokenType.ID,"Expected ID");
        eat(TokenType.FROM,"Expected from");
        Expr e = new Expr();
        expr(e);
        fo.start = e;
        if (match(TokenType.UPTO) | match(TokenType.DOWNTO)){
            if (match(TokenType.UPTO)){
                fo.upto = false;
            }
            else{
                fo.upto = true;
            }
            advance();
        }
        else{
            error("Expected upto or downto");
        }
        Expr er = new Expr();
        expr(er);
        fo.end = er;
        eat(TokenType.LBRACE,"Expected {");
        FunDecl f = new FunDecl();
        stmts(f);
        fo.stmts = f.stmts;
        eat(TokenType.RBRACE,"Expected }");
    }

    private void call_expr(CallExpr c) throws MyPLException {

        eat(TokenType.LPAREN,"Expected (");


        args(c);
        //tem.out.println(currToken);
        eat(TokenType.RPAREN,"Expected )12");
    }

    private void args(CallExpr c) throws MyPLException {
        Expr e = new Expr();
        expr(e);

        c.args.add(e);

        if (e.thread == true){
            c.canTread.add(true);
        }
        else {
            c.canTread.add(false);
        }

        while (match(TokenType.COMMA)){

            Expr er = new Expr();
            advance();
            expr(er);
            if (er.thread == true){
                c.canTread.add(true);
            }
            else {
                c.canTread.add(false);
            }
            c.args.add(er);
        }
    }

    private void ret_stmt(ReturnStmt r) throws MyPLException {
        eat(TokenType.RETURN,"Expected return");
        Expr e = new Expr();
        expr(e);
        r.expr = e;
    }

    private void delete_stmt(DeleteStmt d) throws MyPLException {
        eat(TokenType.DELETE,"Expected delete");
        d.varName = currToken;
        eat(TokenType.ID,"Expected Token");
    }

    /*
    Got help from Zach on this function
     */
    private void expr(Expr e) throws MyPLException {

        if (match(TokenType.NOT)){
            advance();
            e.logicallyNegated = true;
            e.first = new ComplexTerm();
            ((ComplexTerm)e.first).expr = new Expr();
            expr(((ComplexTerm)e.first).expr);
        }

        else if (match(TokenType.LPAREN)){
            advance();
            e.first = new ComplexTerm();
            ((ComplexTerm)e.first).expr = new Expr();
            expr(((ComplexTerm)e.first).expr);
            eat(TokenType.RPAREN,"expected )");
        }

        else {

            SimpleTerm C = new SimpleTerm();
            rvalue(C);
            e.first = C;
            //System.out.println(C.rvalue instanceof CallExpr);
            if (C.rvalue instanceof CallExpr){
                e.thread = true;
            }


        }

        if (isOperator()){
            e.op = currToken;
            advance();
            e.rest = new Expr();
            expr(e.rest);
        }

    }




    private void rvalue(SimpleTerm S) throws MyPLException {
        IDRValue idr = new IDRValue();
        if(match(TokenType.NEG)){
            advance();
            S.rvalue = new NegatedRValue();
            ((NegatedRValue)S.rvalue).expr = new Expr();
            expr(((NegatedRValue)S.rvalue).expr);
           return;
        }
        else if (match(TokenType.NEW)){
            NewRValue d = new NewRValue();
            advance();
            d.typeName = currToken;
            S.rvalue = d;
            eat(TokenType.ID,"Expecting ID");
            return;
        }
        else if (match(TokenType.ID)) {
            Token temp = currToken;
            advance();
            if (match(TokenType.DOT)) {
                AssignStmt a = new AssignStmt();
                a.lvalue.add(temp);
                lvalue(a);
                for (int i = 0; i < a.lvalue.size(); i++)
                    idr.path.add(a.lvalue.get(i));
                S.rvalue = idr;
                return;
            }
            else if (match(TokenType.LPAREN)) {


                CallExpr c = new CallExpr();
                c.threadable = true;
                c.funName = temp;
                call_expr(c);
                S.rvalue = c;
                return;
            }
            else {
                SimpleRValue d = new SimpleRValue();
                idr.path.add(temp);
                S.rvalue = idr;
                return;
            }
        }

        else if(!currToken.lexeme.equals(")")){
           // System.out.println(currToken.lexeme);System.out.println(currToken.lexeme.equals("("));
        SimpleRValue simp = new SimpleRValue();
        simp.value = currToken;
        //System.out.println(currToken.lexeme + "g");
        advance();
        S.rvalue = simp;}
    }
}
