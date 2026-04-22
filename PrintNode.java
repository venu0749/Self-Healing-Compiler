public class PrintNode extends ASTNode {
    public String variable;
    public String variableName;
    public PrintNode(String variable) {
        this.variable = variable;
    }
    @Override 
    public String toString(){
        return "Output: " + variable;
    }
    @Override
public boolean isValid() {
    return variable != null && !variable.isEmpty();
}
}
