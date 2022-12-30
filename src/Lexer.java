/*
 * File: Lexer.java
 * Date: Spring 2022
 * Auth: Caleb Barker
 * Desc: This Assignment I created a lexer for the MYPL language
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Objects;


public class Lexer {

    private BufferedReader buffer;  // handle to input stream
    private int line = 1;           // current line number
    private int column = 0;         // current column number
    private int columntemp = 0;     // tracks dif. from reading in
    private String id ="";          // saved id
    private Token savedToken = null;
    private Boolean first = false;  // put's tokens out in correct order
    //--------------------------------------------------------------------
    // Constructor
    //--------------------------------------------------------------------

    public Lexer(InputStream instream) {
        buffer = new BufferedReader(new InputStreamReader(instream));
    }


    //--------------------------------------------------------------------
    // Private helper methods
    //--------------------------------------------------------------------

    // Returns next character in the stream. Returns -1 if end of file.
    private int read() throws MyPLException {
        try {
            return buffer.read();
        } catch(IOException e) {
            error("read error", line, column + 1);
        }
        return -1;
    }


    // Returns next character without removing it from the stream.
    private int peek() throws MyPLException {
        int ch = -1;
        try {
            buffer.mark(1);
            ch = read();
            buffer.reset();
        } catch(IOException e) {
            error("read error", line, column + 1);
        }
        return ch;
    }


    // Print an error message and exit the program.
    private void error(String msg, int line, int column) throws MyPLException {
        msg = msg + " at line " + line + ", column " + column;
        throw MyPLException.LexerError(msg);
    }



    // Checks for whitespace
    public static boolean isWhitespace(int ch) {
        return Character.isWhitespace((char)ch);
    }


    // Checks for digit
    private static boolean isDigit(int ch) {
        return Character.isDigit((char)ch);
    }


    // Checks for letter
    private static boolean isLetter(int ch) {
        return Character.isLetter((char)ch);
    }


    // Checks if given symbol
    private static boolean isSymbol(char  ch) {
        char[] temp = {'(','+','='};
        for (int i =0 ;i < temp.length;i++){
            if (temp[i] == ch){
                return (true);
            }
        }
        return false;
    }


    // Checks if end-of-file
    private static boolean isEOF(int ch) {
        return ch == -1;
    }


    //--------------------------------------------------------------------
    // Public next_token function
    //--------------------------------------------------------------------


    // returns the current id and also clears it
    String clearId(){
        String temp;
        temp = id ;
        id = "";
        return temp;
    }
    // Block of if statements to find tokens
    public Token SaveNextToken() throws MyPLException {
        int temp = read();
        column+=1;
        if(temp == -1){return new Token(TokenType.EOS, clearId(), line, column);}// returns eos
        char symbol = (char) temp;
        if (isLetter(symbol)&id.length()==0){
            if (!isLetter((char)peek())){
                if (!isDigit((char)peek()))
                    return new Token(TokenType.ID, ""+symbol, line, column);
            }}

        if(symbol!=' ') {
            id += symbol;
            if (symbol == ',') {
                return new Token(TokenType.COMMA, clearId(), line, column);
            } else if (symbol == '.') {
                return new Token(TokenType.DOT, clearId(), line, column);
            }else if (symbol == '?') {error("invalid symbol '"+symbol +  "'",line,column);}
            else if (symbol == '+') {
                return new Token(TokenType.PLUS, clearId(), line, column);
            } else if (symbol == '-') {
                return new Token(TokenType.MINUS, clearId(), line, column);
            } else if (symbol == '*') {
                return new Token(TokenType.MULTIPLY, clearId(), line, column);
            } else if (symbol == '/') {
                return new Token(TokenType.DIVIDE, clearId(), line, column);
            } else if (symbol == '%') {
                return new Token(TokenType.MODULO, clearId(), line, column);
            } else if (symbol == '{') {
                return new Token(TokenType.LBRACE, clearId(), line, column);
            } else if (symbol == '}') {
                return new Token(TokenType.RBRACE, clearId(), line, column);
            } else if (symbol == '(') {
                //first = true;
                clearId();
                return new Token(TokenType.LPAREN, "(", line, column);
            } else if (symbol == ')') {
                return new Token(TokenType.RPAREN, clearId(), line, column);
            } else if (symbol == '!') {
                if ((char) peek() == '=') {
                    read();
                    columntemp+=1;
                    clearId();
                    return new Token(TokenType.NOT_EQUAL, "!=", line, ColumnUpdate());
                }
                else {error("expecting '=', found '>'",line,column+1);}
            } else if (symbol == '=') {
                if ((char) peek() == '=') {
                    read();
                    columntemp+=1;
                    clearId();
                    return new Token(TokenType.EQUAL,"==" , line, ColumnUpdate());
                } else {
                    clearId();
                    return new Token(TokenType.ASSIGN, "=", line, column);
                }
            } else if (symbol == '>') {
                if ((char) peek() == '=') {
                    read();
                    columntemp+=1;
                    clearId();
                    return new Token(TokenType.GREATER_THAN_EQUAL, ">=", line, ColumnUpdate());
                } else {
                    clearId();
                    return new Token(TokenType.GREATER_THAN, ">", line, column);
                }
            } else if (symbol == '<') {
                if ((char) peek() == '=') {
                    read();
                    columntemp+=1;
                    clearId();
                    return new Token(TokenType.LESS_THAN_EQUAL, "<=", line, ColumnUpdate());
                } else {
                    clearId();
                    return new Token(TokenType.LESS_THAN, "<", line, column);
                }
            } else if (symbol == '"') {
                id = "";
                while ((char) peek() != '"') {
                    temp = read();
                    if ((char)temp == '\n')
                        error("found newline within string",line,column+1);
                    columntemp+=1;
                    id += (char) temp;
                    if (temp == -1){error("found end-of-file in string",line,columntemp);}
                }


                read();
                columntemp+=1;

                return new Token(TokenType.STRING_VAL, clearId(), line, ColumnUpdate());
            } else if (symbol == '\'') {
                id = "";
                if ('\''== (char)peek()) {error("empty character",line,column);}
                char other = (char)read();
                id = id + other;
                columntemp+=1;
                if (peek() == '\''){
                    read();
                    columntemp+=1;
                    return new Token(TokenType.CHAR_VAL, clearId(), line, ColumnUpdate());
                }
                else {error("expecting ' found, '"+(char)peek()+"'",line,column+1);}




            }else if (symbol == '\n' | symbol == '\r') {
                column = 0;
                if (symbol == '\n' )
                    line+=1;
                clearId();
            } else if (id.equals("and")) {
                if (!isLetter((char)peek())&&!isDigit((char)peek()))
                return new Token(TokenType.AND, clearId(), line, column -2 );
            }else if (id.equals("or")) {
                if (!isLetter((char)peek())&&!isDigit((char)peek()))
                return new Token(TokenType.OR, clearId(), line, column -1 );
            }else if (id.equals("bool")) {
                if (!isLetter((char)peek())&&!isDigit((char)peek()))
                return new Token(TokenType.BOOL_TYPE, clearId(), line, column-3);
            }else if (id.equals("not")) {
                if (!isLetter((char)peek())&&!isDigit((char)peek()))
                return new Token(TokenType.NOT, clearId(), line, column -2 );
            }else if (id.equals("neg")) {
                if (!isLetter((char)peek())&&!isDigit((char)peek()))
                return new Token(TokenType.NEG, clearId(), line, column -2 );
            }else if (id.equals("int")) {
                if (!isLetter((char)peek())&&!isDigit((char)peek()))
                return new Token(TokenType.INT_TYPE, clearId(), line, column -2 );
            }else if (id.equals("from")) {
                if (!isLetter((char)peek())&&!isDigit((char)peek()))
                return new Token(TokenType.FROM, clearId(), line, column -3 );
            }else if (id.equals("true")) {
                if (!isLetter((char)peek())&&!isDigit((char)peek()))
                return new Token(TokenType.BOOL_VAL, clearId(), line, column -3 );
            }else if (id.equals("false")) {
                if (!isLetter((char)peek())&&!isDigit((char)peek()))
                return new Token(TokenType.BOOL_VAL, clearId(), line, column -4 );
            } else if (id.equals("double")) {
                if (!isLetter((char)peek())&&!isDigit((char)peek()))
                return new Token(TokenType.DOUBLE_TYPE, clearId(), line, column-5);
            } else if (id.equals("char")) {
                if (!isLetter((char)peek())&&!isDigit((char)peek()))
                return new Token(TokenType.CHAR_TYPE, clearId(), line, column-3);
            } else if (id.equals("string")) {
                if (!isLetter((char)peek())&&!isDigit((char)peek()))
                return new Token(TokenType.STRING_TYPE, clearId(), line, column-5);
            } else if (id.equals("void")) {
                if (!isLetter((char)peek())&&!isDigit((char)peek()))
                return new Token(TokenType.VOID_TYPE, clearId(), line, column-3);
            } else if (id.equals("var")) {
                if (!isLetter((char)peek())&&!isDigit((char)peek()))
                return new Token(TokenType.VAR, clearId(), line, column-2);
            } else if (id.equals("type")) {
                if (!isLetter((char)peek())&&!isDigit((char)peek()))
                return new Token(TokenType.TYPE, clearId(), line, column-3);
            } else if (id.equals("while")) {
                if (!isLetter((char)peek())&&!isDigit((char)peek()))
                return new Token(TokenType.WHILE, clearId(), line, column-4);
            } else if (id.equals("for")) {
                if (!isLetter((char)peek())&&!isDigit((char)peek()))
                return new Token(TokenType.FOR, clearId(), line, column-2);
            } else if (id.equals("upto")) {
                if (!isLetter((char)peek())&&!isDigit((char)peek()))
                return new Token(TokenType.UPTO, clearId(), line, column-3);
            } else if (id.equals("downto")) {
                if (!isLetter((char)peek())&&!isDigit((char)peek()))
                return new Token(TokenType.DOWNTO, clearId(), line, column-5);
            } else if (id.equals("if")) {
                if (!isLetter((char)peek())&&!isDigit((char)peek()))
                return new Token(TokenType.IF, clearId(), line, column-1);
            } else if (id.equals("elif")) {
                if (!isLetter((char)peek())&&!isDigit((char)peek()))
                return new Token(TokenType.ELIF, clearId(), line, column-3);
            } else if (id.equals("else")) {
                if (!isLetter((char)peek())&&!isDigit((char)peek()))
                return new Token(TokenType.ELSE, clearId(), line, column-3);
            } else if (id.equals("fun")) {
                if (!isLetter((char)peek())&&!isDigit((char)peek()))
                    return new Token(TokenType.FUN, clearId(), line, column-2);
            } else if (id.equals("new")) {
                if (!isLetter((char)peek())&&!isDigit((char)peek()))
                return new Token(TokenType.NEW, clearId(), line, column-2);
            } else if (id.equals("delete")) {
                if (!isLetter((char)peek())&&!isDigit((char)peek()))
                return new Token(TokenType.DELETE, clearId(), line, column-5);
            } else if (id.equals("return")) {
                if (!isLetter((char)peek())&&!isDigit((char)peek()))
                return new Token(TokenType.RETURN, clearId(), line, column-5);
            } else if (id.equals("nil")) {
                if (!isLetter((char)peek())&&!isDigit((char)peek()))
                return new Token(TokenType.NIL, clearId(), line, column-2);
            } else if (id.length()>1 & isDigit(symbol)){
                while(isDigit((char)peek())&&isLetter((char)peek())){
                    id +=(char)read();
                    column += 1;
                }
                return new Token(TokenType.ID,clearId(),line, column - (id.length()+1));
            }


            else if (symbol=='#'){
                while (read()!='\r'){}
                read();
                clearId();
                line+=1;
            }
            else if (isDigit(symbol)){
                if (Objects.equals(id, "0")){
                    if (isDigit(peek())){
                        temp = read();
                        id += (char)temp;
                        if (peek() == '.'){

                            while (isDigit(peek())|peek()=='.'){
                                temp = read();
                                if(isDigit((char)temp)|(char)temp == '.')
                                    id += (char)temp;

                            }
                            error("leading zero in '" + id +"'" ,line,column);
                        }
                        error("leading zero in '" + id  +"'" ,line,column);}}


                while (isDigit(peek())){
                    id +=(char)read();
                    columntemp+= 1;
                }

                char [] array = id.toCharArray();
                for(int i =0; i < array.length;i++){
                    if (array[0] == 0){

                        if(array[1]==0){
                            error("leading zero in '" + id + (char)peek()+"'" ,line,column);}

                    }

                }



                if (peek()=='.'){
                    id += (char)read();
                    columntemp+= 1;
                    if (!isDigit((char)peek()))
                    {
                        error("missing decimal digit in double value '" + id +"'" ,line,column);
                    }
                    while (isDigit(peek())){
                        id +=(char)read();
                        columntemp+= 1;
                    }
                    if (peek()=='.'){error("too many decimal points in double value '" + id +"'" ,line,column);}
                    return new Token(TokenType.DOUBLE_VAL, clearId(), line, ColumnUpdate());
                }
                return new Token(TokenType.INT_VAL, clearId(), line, ColumnUpdate());
            }
            else
            {
                int col;
                savedToken = null;
                if ((char)peek()==' '|(char)peek()=='\n'|peek()==-1|(char)peek()=='\r'|(char)peek()==')'|
                        (char)peek()=='('|(char)peek()=='.'|(char)peek()==(',')|(char)peek()=='{'|(char)peek()=='}'|
                        (char)peek()=='*'|(char)peek()=='-'|(char)peek()=='+'|(char)peek()=='/'|(char)peek()=='%'|
                        (char)peek()=='<'|(char)peek()=='>'|(char)peek()=='='){
                    //if ((char)peek()=='\n'|(char)peek()=='\r')first = true;
                    if (column == 1){col = column;}
                    else{col = column - (id.length() - 1);}
                    //System.out.println("IT was me");
                    return new Token(TokenType.ID, clearId(), line, col);}}
            // the long weird lexme is used to save strings

            return new Token(TokenType.ID, "r1e2a3l4l5y6L7o8n9g", line, column);

        }

        return new Token(TokenType.ID, "r1e2a3l4l5y6L7o8n9g", line, column);


    }

    // function that helps track what token to return in order and returns a token
    public Token nextToken() throws MyPLException {
        while (true){

            String savedId = "";
            if (savedToken != null) {
                if (savedToken.lexeme() != ""|savedToken.lexeme() != "\n"){
                    return removeSavedToken();} // if there was a saved token outputs it first
            }

            Token temp = SaveNextToken();
            if (temp.lexeme() == "r1e2a3l4l5y6L7o8n9g") {   // mostly for getting id's
                while (temp.lexeme() == "r1e2a3l4l5y6L7o8n9g") {    // reads until it gets a new token not chars
                    savedId = id;
                    temp = SaveNextToken();
                }
                savedToken = temp;
                if (first ) {
                    first = false;

                    return new Token(TokenType.ID, savedId, line, column);  // ex. print( puts puts out Token print
                }
            } else {
                return temp;
            }
        }
    }
    // clears the saved token and returns it
    private Token removeSavedToken() {
        Token temp = savedToken;
        savedToken = null;
        return temp;
    }
    // resets column temp and returns the current expected column 
    private int ColumnUpdate(){
        int temp = column;
        column += columntemp;
        columntemp =0;
        return temp;
    }

}