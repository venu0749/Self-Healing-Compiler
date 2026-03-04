public class DeclarationNode extends ASTNode {

    public String variableName;
    public String type;

    public DeclarationNode(String type, String variableName) {
        this.type = type;
        this.variableName = variableName;
    }

    @Override
    public String tostring() {
        return type + " " + variableName + ";";
    }
    @Override
public boolean isValid() {
    return variableName != null && !variableName.isEmpty();
}
}