import java.util.Scanner;

public class Login {
    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in);

        String correctUsername = "Sajinthini";
        String correctPassword = "1234";

        System.out.print("Enter Username: ");
        String username = sc.nextLine();

        System.out.print("Enter Password: ");
        String password = sc.nextLine();

        if (username.equals(correctUsername) && password.equals(correctPassword)) {
            System.out.println("Login Successful!");
            System.out.println("Welcome, " + username);
        } else {
            System.out.println("Invalid Username or Password!");
        }

        sc.close();
    }
}
