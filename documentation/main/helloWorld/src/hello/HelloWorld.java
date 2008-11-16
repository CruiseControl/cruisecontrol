package hello;

public class HelloWorld {

    public String sayHello() {
        return "Hello world";
    }

    public static void main(String[] args) {
        System.out.println(new HelloWorld().sayHello());
    }

}
