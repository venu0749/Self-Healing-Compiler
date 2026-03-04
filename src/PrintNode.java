public class PrintNode extends ASTNode {
    public String variable;
    public String variableName;
    public PrintNode(String variable) {
        this.variable = variable;
    }
    @Override 
    public String tostring(){
        return "printnode -> printf(\"%d\", " + variable + " ) "; //overridinng the method from ASTnode 
    }
    @Override
public boolean isValid() {
    return variableName != null && !variableName.isEmpty();
}
}
