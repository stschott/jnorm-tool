package jnorm.core;

public class PrettyPrinter {
    String content;

    public PrettyPrinter(String content) {
        this.content = content;
    }

    public String prettyPrint() {
        this.content = this.content.replaceAll("[\\r\\n]+","\n");
        this.content = this.content.replaceAll("(?m)^\\s+|\\s+$", "");
        return this.content;
    }
}
