package com.pedropacheco.nutrition.backend.cli;

import com.pedropacheco.nutrition.backend.userAuth.service.UserAuthService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Scanner;

@Component
public class TerminalRunner implements CommandLineRunner {

    private final UserAuthService authService;
    private final FitMaxxerMainMenu mainMenu;

    public TerminalRunner(UserAuthService authService, FitMaxxerMainMenu mainMenu) {
        this.authService = authService;
        this.mainMenu = mainMenu;
    }

    @Override
    public void run(String... args) {
        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        System.out.println("----------------------------------------");
        System.out.println(" FitMaxxer - Auth Terminal Runner");
        System.out.println("----------------------------------------");

        while (running) {
            System.out.println("\nSelect an option:");
            System.out.println("1. Register User");
            System.out.println("2. Login");
            System.out.println("3. Exit Application");
            System.out.print("Choice: ");

            String choice = scanner.nextLine();

            try {
                switch (choice) {
                    case "1":
                        System.out.print("Enter email: ");
                        String regEmail = scanner.nextLine();
                        System.out.print("Enter password: ");
                        String regPass = scanner.nextLine();
                        System.out.print("Enter user ID: ");
                        String regUserId = scanner.nextLine();

                        authService.registerUser(regEmail, regPass, regUserId);
                        System.out.println("[SUCCESS] User registered successfully.");
                        break;

                    case "2":
                        System.out.print("Enter email or user ID: ");
                        String identifier = scanner.nextLine();
                        System.out.print("Enter password: ");
                        String loginPass = scanner.nextLine();

                        String token = authService.authenticate(identifier, loginPass);
                        System.out.println("[SUCCESS] Login successful.");

                        // Transition to the Main Menu after successful login
                        mainMenu.show(token);

                        System.out.println("\nReturned to authentication menu.");
                        break;

                    case "3":
                        running = false;
                        System.out.println("Shutting down application.");
                        break;

                    default:
                        System.out.println("[ERROR] Invalid option. Please choose 1, 2, or 3.");
                }
            } catch (Exception e) {
                System.out.println("[ERROR] Operation failed: " + e.getMessage());
            }
        }
    }
}