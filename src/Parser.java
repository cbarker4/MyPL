/*
 * File: Parser.java
 * Date: Spring 2022
 * Auth:
 * Desc:
 */


public class Parser {

    private Lexer lexer = null;
    private Token currToken = null;
    private final boolean DEBUG = false;


    // constructor
    public Parser(Lexer lexer) {
        this.lexer = lexer;
    }

    // do the parse
    public void parse() throws MyPLException
    {
        // <program> ::= (<tdecl> | <fdecl>)*
        advance();
        System.out.println("LExer");
        while (!match(TokenType.EOS)) {
            if (match(TokenType.TYPE))
                tdecl();
            else
                fdecl();
        }
        advance(); // eat the EOS token
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

    // return true if current token is a (non-id) primitive type
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

    // return true if current token starts an expression
    private boolean isExpr() {
        return match(TokenType.NOT) || match(TokenType.LPAREN) ||
                match(TokenType.NIL) || match(TokenType.NEW) ||
                match(TokenType.ID) || match(TokenType.NEG) ||
                match(TokenType.INT_VAL) || match(TokenType.DOUBLE_VAL) ||
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


    void tdecl() throws MyPLException{
        eat(TokenType.TYPE,"expecting Type ");
        eat (TokenType.ID,"Expecting ID");
        eat (TokenType.LBRACE,"Expecting left Brace");
        vdecls();
        eat (TokenType.RBRACE,"Expecting }");
    }


    void vdecls() throws MyPLException{
        while (match(TokenType.VAR)){
            vdecl_stmt();
        }
    }


    void fdecl() throws MyPLException {
        eat(TokenType.FUN, "Expecting fun");
        if (isPrimitiveType() | match(TokenType.VOID_TYPE)||match(TokenType.ID)){
            advance();
        }
        else{
            error("expected a type");
        }
        eat(TokenType.ID, "Expecting ID");
        eat (TokenType.LPAREN,"Expecting ( ");
        params();
        eat(TokenType.RPAREN, "Expecting )");
        eat (TokenType.LBRACE,"Expecting {");
        stmts();
        eat(TokenType.RBRACE, "Expecting }");
    }
    void params() throws MyPLException{
        if (!match(TokenType.RPAREN)) {
            dtype();
            eat(TokenType.ID, "Expecting ID");
            while (match(TokenType.COMMA)) {
                eat(TokenType.COMMA, "Expecting Comma");
                dtype();
                eat(TokenType.ID, "Expecting ID");
            }
        }
    }

    void dtype() throws MyPLException {
        if (isPrimitiveType() || match(TokenType.ID)){advance();}
        else{error("Expected Primative type ");}

    }
    void stmts() throws MyPLException {
        while(!match(TokenType.RBRACE)){
            stmt();
            if (match(TokenType.EOS)){error("Missing }");}
        }
    }

    void stmt() throws MyPLException {
        if (match(TokenType.VAR)) vdecl_stmt();
        if (match(TokenType.ID)) {
            advance();
            if (match(TokenType.DOT)|| match(TokenType.ASSIGN))
                System.out.println("Here");
                assign_stmt();
            if (match(TokenType.LPAREN))
                call_expr();
        }
        if (match(TokenType.ASSIGN)){assign_stmt();}
        if (match(TokenType.IF)) cond_stmt();
        if (match(TokenType.WHILE)) while_stmt();
        if (match(TokenType.FOR))for_stmt();
        if (match(TokenType.RETURN))ret_stmt();
        if (match(TokenType.DELETE))delete_stmt();
    }




    void vdecl_stmt()throws MyPLException{
        eat(TokenType.VAR,"expecting Var");
        if (isPrimitiveType()){advance();}
        eat (TokenType.ID,"expecating Id");
        if (match(TokenType.ID)){advance();}
        eat (TokenType.ASSIGN,"expecating Assign");
        expr();
    }

    private void assign_stmt() throws MyPLException {
        lvalue();
        eat(TokenType.ASSIGN,"Expected =");
        expr();


    }

    private void lvalue() throws MyPLException {
        while (match(TokenType.DOT)){
            eat(TokenType.DOT,"Expected .");
            eat(TokenType.ID,"Expected ID");
        }
    }

    private void cond_stmt() throws MyPLException {
        eat(TokenType.IF,"Expected if");
        expr();
        eat(TokenType.LBRACE,"Expected {");
        stmts();
        eat(TokenType.RBRACE,"Expected }");
        condt();

    }

    private void condt() throws MyPLException {
        if (match(TokenType.ELIF)) {
            advance();
            expr();
            eat(TokenType.LBRACE,"Expected {");
            stmts();
            eat(TokenType.RBRACE,"Expected }");
            condt();
        }
        if (match(TokenType.ELSE)){
            advance();
            eat(TokenType.LBRACE,"Expected {");
            stmts();
            eat(TokenType.RBRACE,"Expected }");
            condt();
        }
    }
    private void while_stmt() throws MyPLException {
        advance();
        expr();
        eat(TokenType.LBRACE,"Expected {");
        stmts();
        eat(TokenType.RBRACE,"Expected }");
    }

    private void for_stmt() throws MyPLException {
        advance();
        eat(TokenType.ID,"Expected ID");
        eat(TokenType.FROM,"Expected from");
        expr();
        if (match(TokenType.UPTO) | match(TokenType.DOWNTO)){
            advance();
        }
        else{
            error("Expected upto or downto");
        }
        expr();
        eat(TokenType.LBRACE,"Expected {");
        stmts();
        eat(TokenType.RBRACE,"Expected }");
    }

    private void call_expr() throws MyPLException {
        eat(TokenType.LPAREN,"Expected (");
        args();
        eat(TokenType.RPAREN,"Expected )");
    }

    private void args() throws MyPLException {
        expr();
        while (match(TokenType.COMMA)){
            advance();
            expr();
        }
    }

    private void ret_stmt() throws MyPLException {
        eat(TokenType.RETURN,"Expected return");
        expr();
    }

    private void delete_stmt() throws MyPLException {
        eat(TokenType.DELETE,"Expected delete");
        eat(TokenType.ID,"Expected Token");
    }

    private void expr() throws MyPLException {
//        if (isrvalue()){rvalue();}
//        else if (match(TokenType.NOT)){advance();expr();}
//        else if (match(TokenType.LPAREN)){
//            advance();
//            expr();
//            eat(TokenType.RPAREN,"expected )");
//        }
//        if (isOperator()){
//            advance();
//            expr();
//        }


        if (match(TokenType.NOT)){
            advance();

            expr();

        }

        else if (match(TokenType.LPAREN)){
            Expr exr = new Expr();
            advance();
            expr();
            eat(TokenType.RPAREN,"expected )");

        }

        if (isrvalue()){
            SimpleTerm C = new SimpleTerm();
            rvalue();
        }

        if (isOperator()){
            advance();
            expr();
        }

    }

    private void rvalue() throws MyPLException {
        //if (isPrimitiveValue()){advance();}
        if(match(TokenType.NEG)){advance();expr();return;}
        advance();
        if (match(TokenType.ID)){advance();}
        if (match(TokenType.DOT)){lvalue();}
        if (match(TokenType.LPAREN)){call_expr();}

    }






    /* TODO: Add the recursive descent functions below */


}
