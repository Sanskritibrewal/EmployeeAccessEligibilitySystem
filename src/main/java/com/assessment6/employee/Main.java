package com.assessment6.employee;

import java.util.List;

public class Main {

    public static void main(String[] args) {
        EmployeeAccessService service = new EmployeeAccessService();
        List<Employee> employees = List.of(
                new Employee("EMP001", "Rahul", 30, "IT", "ACTIVE", "SECRET", true),
                new Employee("EMP002", "Priya", 28, "HR", "ACTIVE", "BASIC", true),
                new Employee("EMP003", "Arjun", 20, "SALES", "INACTIVE", "BASIC", false));

        for (Employee employee : employees) {
            System.out.println("Employee: " + employee.getName());
            try {
                EligibilityResult result = service.checkEligibility(employee, "CONFIDENTIAL");
                System.out.println("Status: " + result.getStatus());
                printReasons(result);
            } catch (InvalidEmployeeException exception) {
                System.out.println("Invalid employee: " + exception.getMessage());
            }
            System.out.println();
        }
    }

    private static void printReasons(EligibilityResult result) {
        if (!result.getReasons().isEmpty()) {
            System.out.println("Reasons:");
            for (String reason : result.getReasons()) {
                System.out.println("- " + reason);
            }
        }
    }
}
