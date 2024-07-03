package jnorm.core.model;

public class NormalizationStatistics {
    // JDK version
    public int sortMethod = 0;
    public int arithmetic = 0;
    public int charSequenceToString = 0;
    public int emptyTryCatch = 0;
    public int stringConstantConcat = 0;
    public int methodRefOperator = 0;
    public int bufferMethod = 0;
    public int tryWithResources = 0;
    public int duplicateCheckcast = 0;
    public int enumUsage = 0;
    
    // target level
    public int outerClassObjectCreation = 0;
    public int dynamicStringConcat = 0;
    public int nestBasedAccessControl = 0;
    public int privateMethodInvocation = 0;
    public int innerClassInstantiation = 0;
    
    // aggressive normalization
    public int typecheck = 0;

    // additional info
    public int amountOfClasses = 0;
    public int amountOfMethods = 0;

    @Override
    public String toString() {
        return "NormalizationStatistics{" +
                "sortMethod=" + sortMethod +
                ", arithmetic=" + arithmetic +
                ", charSequenceToString=" + charSequenceToString +
                ", emptyTryCatch=" + emptyTryCatch +
                ", stringConstantConcat=" + stringConstantConcat +
                ", methodRefOperator=" + methodRefOperator +
                ", bufferMethod=" + bufferMethod +
                ", tryWithResources=" + tryWithResources +
                ", duplicateCheckcast=" + duplicateCheckcast +
                ", enumUsage=" + enumUsage +
                ", outerClassObjectCreation=" + outerClassObjectCreation +
                ", dynamicStringConcat=" + dynamicStringConcat +
                ", nestBasedAccessControl=" + nestBasedAccessControl +
                ", privateMethodInvocation=" + privateMethodInvocation +
                ", innerClassInstantiation=" + innerClassInstantiation +
                ", typecheck=" + typecheck +
                ", amountOfClasses=" + amountOfClasses +
                ", amountOfMethods=" + amountOfMethods +
                '}';
    }
}
