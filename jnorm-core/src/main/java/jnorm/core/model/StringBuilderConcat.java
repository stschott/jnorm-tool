package jnorm.core.model;

import soot.Local;
import soot.Type;
import soot.Unit;
import soot.Value;

import java.util.ArrayList;
import java.util.List;

public class StringBuilderConcat {
    String template = "";
    List<Unit> stringBuilderCalls = new ArrayList<>();
    List<Local> aliases = new ArrayList<>();
    List<Value> dynamicValues = new ArrayList<>();
    List<Type> dynamicTypes = new ArrayList<>();
    Unit endUnit;
    Unit newStringConcatCall;

    public StringBuilderConcat(Local alias, Unit call) {
        aliases.add(alias);
        stringBuilderCalls.add(call);
    }

    public void concat(String concatString) {
        template += concatString;
    }

    public boolean aliasContained(Local alias) {
        return aliases.contains(alias);
    }

    public void addStringBuilderCall(Unit call) {
        stringBuilderCalls.add(call);
    }

    public void addAlias(Local alias) {
        aliases.add(alias);
    }

    public void addDynamicArgument(Value value, Type type) {
        dynamicValues.add(value);
        dynamicTypes.add(type);
    }

    public void setEndUnit(Unit endUnit) {
        this.endUnit = endUnit;
    }

    public void setNewStringConcatCall(Unit newStringConcatCall) {
        this.newStringConcatCall = newStringConcatCall;
    }

    public String getTemplate() {
        return template;
    }

    public List<Unit> getStringBuilderCalls() {
        return stringBuilderCalls;
    }

    public List<Value> getDynamicValues() {
        return dynamicValues;
    }

    public List<Type> getDynamicTypes() {
        return dynamicTypes;
    }

    public Unit getEndUnit() {
        return endUnit;
    }

    public Unit getNewStringConcatCall() {
        return newStringConcatCall;
    }

    @Override
    public String toString() {
        return "StringBuilderConcat{" +
                "template='" + template + '\'' +
                ", stringBuilderCalls=" + stringBuilderCalls +
                ", aliases=" + aliases +
                '}';
    }
}
