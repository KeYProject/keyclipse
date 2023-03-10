package test;

public class TestClass {
    private int /*@ spec_public @*/ aNewName;
    public TestClassOther otherClass = new TestClassOther();

    /*@ normal_behavior
      @ ensures \result == otherClass.balance;
      @ assignable \nothing;
      @*/
    public int accessBalanceFromOtherClass() {
        return otherClass.balance;
    }
}
