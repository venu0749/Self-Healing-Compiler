import java.util.*;

public class Parser {

    private List<Token> tokens;
    private int position = 0;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    private Token currentToken() {
        if (position < tokens.size())
            return tokens.get(position);
        return null;
    }

    private void consume() {
        position++;
    }

    public void expect(String type) {
        Token token = currentToken();
        if(token == null || !token.type.equals(type)) {
            throw new  RuntimeException( "syntax error near : " + (token != null ? token.value : "EOF")) ;
        }
        consume();
    }


    public void expectSymbol(String symbol){
        Token token = currentToken();
        if (token == null || !token.type.equals("SYMBOL") || !token.value.equals(symbol)){
            throw new RuntimeException("Syntax Error near : " + ( token != null ? token.value : "EOF"));
        }
        consume();
    }
    public List<ASTNode> parse() {
        List<ASTNode> nodes = new ArrayList<>();
 while (currentToken() != null) {

            Token token = currentToken();

            // Parse Declaration
            if (token.type.equals("INT")) {

                consume(); // int

                Token var = currentToken();
                expect("IDENTIFIER");

                expectSymbol("=");

                Token value = currentToken();
                expect("NUMBER");

                expectSymbol(";");

                nodes.add(new DeclarationNode(var.value, value.value));
            }

            // Parse Print
            else if (token.type.equals("PRINTF")) {

                consume(); // printf

                expectSymbol("(");

                expect("STRING");

                expectSymbol(",");

                Token var = currentToken();
                expect("IDENTIFIER");

                expectSymbol(")");

                expectSymbol(";");

                nodes.add(new PrintNode(var.value));
            }

            else {
                throw new RuntimeException("Syntax Error near: " + token.value);
            }
        }

        return nodes;
    }
}
