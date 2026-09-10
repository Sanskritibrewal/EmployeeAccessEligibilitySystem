package com.assessment6.employee;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class EmployeeAccessService {

    private static final Set<String> AUTHORIZED_DEPARTMENTS = Set.of(
            "IT", "HR", "FINANCE", "ADMINISTRATION");
    private static final Set<String> VALID_EMPLOYMENT_TYPES = Set.of(
            "ACTIVE", "INACTIVE", "CONTRACTOR", "TERMINATED");
    private static final Set<String> VALID_CLEARANCE_LEVELS = Set.of(
            "NONE", "BASIC", "CONFIDENTIAL", "SECRET");
    private static final Set<String> VALID_ACCESS_LEVELS = Set.of(
            "PUBLIC", "INTERNAL", "CONFIDENTIAL", "SECRET");

    public EligibilityResult checkEligibility(Employee employee,
                                               String requestedAccessLevel)
            throws InvalidEmployeeException {
        validateEmployee(employee);
        String employmentType = normalizeRequired(employee.getEmploymentType(),
                "Employment type cannot be empty");
        String clearance = normalizeRequired(employee.getSecurityClearanceLevel(),
                "Security clearance level cannot be empty");
        String requestedAccess = normalizeRequired(requestedAccessLevel,
                "Requested access level cannot be empty");

        if (!VALID_EMPLOYMENT_TYPES.contains(employmentType)) {
            throw new InvalidEmployeeException("Invalid employment type");
        }
        if (!VALID_CLEARANCE_LEVELS.contains(clearance)) {
            throw new InvalidEmployeeException("Invalid security clearance level");
        }
        if (!VALID_ACCESS_LEVELS.contains(requestedAccess)) {
            throw new InvalidEmployeeException("Invalid requested access level");
        }

        List<String> reasons = new ArrayList<>();
        if (employee.getAge() < 21) {
            reasons.add("Employee must be at least 21 years old");
        }
        if (!AUTHORIZED_DEPARTMENTS.contains(normalize(employee.getDepartment()))) {
            reasons.add("Department is not authorized");
        }
        if (!"ACTIVE".equals(employmentType)) {
            reasons.add("Employment status is not active");
        }
        if (!employee.isIdValid()) {
            reasons.add("Employee ID is invalid");
        }

        if (clearanceValue(clearance) < accessValue(requestedAccess)) {
            reasons.add("Security clearance is insufficient for requested access level");
        }

        EligibilityStatus status;
        if (reasons.isEmpty()) {
            status = EligibilityStatus.ELIGIBLE;
        } else if (reasons.size() == 1
                && reasons.contains("Security clearance is insufficient for requested access level")) {
            status = EligibilityStatus.CONDITIONALLY_ELIGIBLE;
        } else {
            status = EligibilityStatus.NOT_ELIGIBLE;
        }
        return new EligibilityResult(status, reasons);
    }

    private void validateEmployee(Employee employee) throws InvalidEmployeeException {
        if (employee == null) {
            throw new InvalidEmployeeException("Employee cannot be null");
        }
        normalizeRequired(employee.getEmployeeId(), "Employee ID cannot be empty");
        normalizeRequired(employee.getName(), "Employee name cannot be empty");
        if (employee.getAge() < 0 || employee.getAge() > 120) {
            throw new InvalidEmployeeException("Invalid age");
        }
        normalizeRequired(employee.getDepartment(), "Department cannot be empty");
    }

    private String normalizeRequired(String value, String message)
            throws InvalidEmployeeException {
        if (value == null || value.isBlank()) {
            throw new InvalidEmployeeException(message);
        }
        return normalize(value);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private int clearanceValue(String clearance) {
        return switch (clearance) {
            case "NONE" -> 0;
            case "BASIC" -> 1;
            case "CONFIDENTIAL" -> 2;
            case "SECRET" -> 3;
            default -> throw new IllegalArgumentException("Unexpected clearance");
        };
    }

    private int accessValue(String access) {
        return switch (access) {
            case "PUBLIC" -> 0;
            case "INTERNAL" -> 1;
            case "CONFIDENTIAL" -> 2;
            case "SECRET" -> 3;
            default -> throw new IllegalArgumentException("Unexpected access level");
        };
    }
}
