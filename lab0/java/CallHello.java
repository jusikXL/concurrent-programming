public class CallHello {
  public static void main(String[] args) {
    Hello hello = new Hello();

    Thread thread = new Thread(hello);
    thread.start();

    try {
      thread.join(); // wait for the thread to finish
    } catch (InterruptedException e) {
      System.out.println(e);
    }
  }
}
