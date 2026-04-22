public class StructNode extends ASTNode {
    public String structName;

    public StructNode(String structName) {
        this.structName = structName;
    }

    @Override
    public String toString() {
        return "Struct: " + structName;
    }

    @Override
    public boolean isValid() {
        return structName != null;
    }
}
