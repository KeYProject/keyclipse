
public class LoopInvariantExample {
	/*@ requires array != null;
	  @ ensures \result == (\sum int j; 0 <= j && j < array.length; array[j]);
	  @*/
	public static int sum(int[] array) {
		int result = 0;
		int i = 0;
		/*@ loop_invariant i >= 0 && i <= array.length && result == (\sum int j; 0 <= j && j < i; array[j]);
		  @ decreasing array.length - i;
		  @ assignable \nothing;
		  @*/
		while (i < array.length) {
			result += array[i];
			i++;
		}
		return result;
	}
}
