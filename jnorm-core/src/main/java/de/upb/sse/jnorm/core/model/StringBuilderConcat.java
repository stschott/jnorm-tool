package de.upb.sse.jnorm.core.model;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import sootup.core.jimple.common.Immediate;
import sootup.core.jimple.common.Local;
import sootup.core.jimple.common.stmt.Stmt;
import sootup.core.types.Type;

public class StringBuilderConcat {
  @Getter String template = "";
  @Getter List<Stmt> stringBuilderCalls = new ArrayList<>();
  List<Local> aliases = new ArrayList<>();
  @Getter List<Immediate> dynamicValues = new ArrayList<>();
  @Getter List<Type> dynamicTypes = new ArrayList<>();
  @Setter @Getter Stmt endStmt;
  @Setter @Getter Stmt newStringConcatCall;

  public StringBuilderConcat(Local alias, Stmt call) {
    aliases.add(alias);
    stringBuilderCalls.add(call);
  }

  public void concat(String concatString) {
    template += concatString;
  }

  public boolean aliasContained(Local alias) {
    return aliases.contains(alias);
  }

  public void addStringBuilderCall(Stmt call) {
    stringBuilderCalls.add(call);
  }

  public void addAlias(Local alias) {
    aliases.add(alias);
  }

  public void addDynamicArgument(Immediate value, Type type) {
    dynamicValues.add(value);
    dynamicTypes.add(type);
  }

  @Override
  public String toString() {
    return "StringBuilderConcat{"
        + "template='"
        + template
        + '\''
        + ", stringBuilderCalls="
        + stringBuilderCalls
        + ", aliases="
        + aliases
        + '}';
  }
}
