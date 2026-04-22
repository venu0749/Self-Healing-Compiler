public class AssignmentNode extends ASTNode {

    String variable;
    String value;

    public AssignmentNode(String variable, String value) {
        this.variable = variable;
        this.value = value;
    }

    @Override
    public String toString() {
        return "Assignment: " + variable + " = " + value;
    }
     @Override
public boolean isValid() {
    return variable != null && value != null;
}
}