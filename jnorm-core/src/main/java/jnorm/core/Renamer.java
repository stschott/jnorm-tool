package jnorm.core;

public class Renamer {
    String content;

    public Renamer(String content) {
        this.content = content;
    }

    public String rename() {
        // normalize labels
        this.content = this.content.replaceAll("(\\s*)label\\d+", "$1label");
        // replace parameters
        this.content = this.content.replaceAll("@parameter\\d+", "@parameter");
        // normalize variables with a $ in front
        this.content = this.content.replaceAll("\\$([a-z])\\d+_\\d+", "\\$$1");
        this.content = this.content.replaceAll("\\$([a-z])\\d+", "\\$$1");
        // normalize variables without $ in front
        this.content = this.content.replaceAll("([a-z])\\d+_\\d+([\\.\\s+\\)\\;\\]\\[\\,])", "$1$2");
        this.content = this.content.replaceAll("([a-z])\\d+([\\.\\s+\\)\\;\\]\\[\\,])", "$1$2");

        return this.content;
    }
}
