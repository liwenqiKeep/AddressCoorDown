package org.example;

/**
 * @author Liwq
 */
public class Test {

    public static void main(String[] args) throws InterruptedException {


        long s = System.currentTimeMillis();
        String[] args1 = {"-s","D:\\desk\\test.csv","-f","contact_addr","-t","D:\\desk\\test2.csv"};
        Main.main(args1);

        System.out.println("执行时长：" + (System.currentTimeMillis() - s) + "ms");
    }
}
