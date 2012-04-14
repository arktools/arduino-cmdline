package processing.app;

import junit.framework.*;


public class TestCommander extends TestCase {

    protected void setUp() {
        // common setup code
    }

    protected void tearDown() {
        // common cleanup code
    }

    public void testAdd() {
        int num1 = 3;
        int num2 = 2;
        int total = 5;
        int sum = 0;
        sum = num1 + num2;
        assertEquals(sum, total);
    }
      
    public void testMulitply() {
        
        int num1 = 3; 
        int num2 = 7; 
        int total = 21;
        int sum = 0;
        sum = num1 * num2;
        assertEquals("Problem with multiply", sum, total);
        
        num1 = 5;
        num2 = 4;
        total = 20;
        sum = num1*num2;
        assertEquals("Problem with multiply", sum, total);
    }
}
