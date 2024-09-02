public class Hello implements Runnable {
  private static final String MESSAGE = "Hello World";

  @Override
  public void run() {
    System.out.println(MESSAGE);
  }
}
