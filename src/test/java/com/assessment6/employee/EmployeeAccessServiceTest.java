package com.assessment6.employee;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class EmployeeAccessServiceTest {

    private EmployeeAccessService service;

    @BeforeEach
    void setUp() {
        service = new EmployeeAccessService();
    }

    @Test
    void shouldBeEligibleWhenAllRulesPass() throws InvalidEmployeeException {
        EligibilityResult result = service.checkEligibility(employee(30, "IT", "ACTIVE", "SECRET", true), "SECRET");

        assertEquals(EligibilityStatus.ELIGIBLE, result.getStatus());
        assertTrue(result.getReasons().isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"IT", "HR", "FINANCE", "ADMINISTRATION", "it", "Hr", " finance ", " administration "})
    void shouldAcceptAuthorizedDepartments(String department) throws InvalidEmployeeException {
        EligibilityResult result = service.checkEligibility(employee(30, department, "ACTIVE", "SECRET", true), "PUBLIC");

        assertNotDepartmentFailure(result);
    }

    @ParameterizedTest
    @ValueSource(strings = {"SALES", "MARKETING", "DESIGN", "LEGAL"})
    void shouldRejectUnauthorizedDepartment(String department) throws InvalidEmployeeException {
        EligibilityResult result = service.checkEligibility(employee(30, department, "ACTIVE", "SECRET", true), "PUBLIC");

        assertEquals(EligibilityStatus.NOT_ELIGIBLE, result.getStatus());
        assertTrue(result.getReasons().contains("Department is not authorized"));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 10, 18, 19, 20})
    void shouldRejectAgeBelow21(int age) throws InvalidEmployeeException {
        EligibilityResult result = service.checkEligibility(employee(age, "IT", "ACTIVE", "SECRET", true), "PUBLIC");

        assertTrue(result.getReasons().contains("Employee must be at least 21 years old"));
        assertEquals(EligibilityStatus.NOT_ELIGIBLE, result.getStatus());
    }

    @ParameterizedTest
    @ValueSource(ints = {21, 22, 25, 30, 50, 100, 120})
    void shouldAcceptValidAgeRange(int age) throws InvalidEmployeeException {
        EligibilityResult result = service.checkEligibility(employee(age, "IT", "ACTIVE", "SECRET", true), "PUBLIC");

        assertFalse(result.getReasons().contains("Employee must be at least 21 years old"));
    }

    @ParameterizedTest
    @ValueSource(ints = {-100, -5, -1, 121, 150, 200})
    void shouldThrowExceptionForInvalidAge(int age) {
        assertThrows(InvalidEmployeeException.class,
                () -> service.checkEligibility(employee(age, "IT", "ACTIVE", "SECRET", true), "PUBLIC"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ACTIVE", "active", " Active "})
    void shouldAcceptActiveEmployment(String employmentType) throws InvalidEmployeeException {
        EligibilityResult result = service.checkEligibility(employee(30, "IT", employmentType, "SECRET", true), "PUBLIC");

        assertFalse(result.getReasons().contains("Employment status is not active"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"INACTIVE", "CONTRACTOR", "TERMINATED", "inactive"})
    void shouldRejectNonActiveEmployment(String employmentType) throws InvalidEmployeeException {
        EligibilityResult result = service.checkEligibility(employee(30, "IT", employmentType, "SECRET", true), "PUBLIC");

        assertTrue(result.getReasons().contains("Employment status is not active"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"UNKNOWN", "PART_TIME", "RETIRED"})
    void shouldThrowExceptionForInvalidEmploymentType(String employmentType) {
        assertThrows(InvalidEmployeeException.class,
                () -> service.checkEligibility(employee(30, "IT", employmentType, "SECRET", true), "PUBLIC"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void shouldThrowExceptionForBlankEmployeeId(String id) {
        assertThrows(InvalidEmployeeException.class,
                () -> service.checkEligibility(new Employee(id, "Name", 30, "IT", "ACTIVE", "SECRET", true), "PUBLIC"));
    }

    @Test
    void shouldRejectInvalidEmployeeId() throws InvalidEmployeeException {
        EligibilityResult result = service.checkEligibility(employee(30, "IT", "ACTIVE", "SECRET", false), "PUBLIC");

        assertTrue(result.getReasons().contains("Employee ID is invalid"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void shouldThrowExceptionForBlankName(String name) {
        assertThrows(InvalidEmployeeException.class,
                () -> service.checkEligibility(new Employee("ID", name, 30, "IT", "ACTIVE", "SECRET", true), "PUBLIC"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void shouldThrowExceptionForBlankDepartment(String department) {
        assertThrows(InvalidEmployeeException.class,
                () -> service.checkEligibility(employee(30, department, "ACTIVE", "SECRET", true), "PUBLIC"));
    }

    @ParameterizedTest
    @MethodSource("sufficientClearanceCases")
    void shouldAcceptSufficientClearance(String clearance, String access) throws InvalidEmployeeException {
        EligibilityResult result = service.checkEligibility(employee(30, "IT", "ACTIVE", clearance, true), access);

        assertFalse(result.getReasons().contains("Security clearance is insufficient for requested access level"));
    }

    private static Stream<Arguments> sufficientClearanceCases() {
        return Stream.of(
                Arguments.of("NONE", "PUBLIC"), Arguments.of("BASIC", "PUBLIC"),
                Arguments.of("BASIC", "INTERNAL"), Arguments.of("CONFIDENTIAL", "PUBLIC"),
                Arguments.of("CONFIDENTIAL", "INTERNAL"), Arguments.of("CONFIDENTIAL", "CONFIDENTIAL"),
                Arguments.of("SECRET", "PUBLIC"), Arguments.of("SECRET", "INTERNAL"),
                Arguments.of("SECRET", "CONFIDENTIAL"), Arguments.of("SECRET", "SECRET"));
    }

    @ParameterizedTest
    @CsvSource({"NONE, INTERNAL", "NONE, CONFIDENTIAL", "NONE, SECRET", "BASIC, CONFIDENTIAL", "BASIC, SECRET", "CONFIDENTIAL, SECRET"})
    void shouldReportInsufficientClearance(String clearance, String access) throws InvalidEmployeeException {
        EligibilityResult result = service.checkEligibility(employee(30, "IT", "ACTIVE", clearance, true), access);

        assertEquals(EligibilityStatus.CONDITIONALLY_ELIGIBLE, result.getStatus());
        assertEquals(List.of("Security clearance is insufficient for requested access level"), result.getReasons());
    }

    @ParameterizedTest
    @ValueSource(strings = {"INVALID", "TOP_SECRET"})
    void shouldThrowExceptionForInvalidClearance(String clearance) {
        assertThrows(InvalidEmployeeException.class,
                () -> service.checkEligibility(employee(30, "IT", "ACTIVE", clearance, true), "PUBLIC"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void shouldThrowExceptionForBlankClearance(String clearance) {
        assertThrows(InvalidEmployeeException.class,
                () -> service.checkEligibility(employee(30, "IT", "ACTIVE", clearance, true), "PUBLIC"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"PUBLIC", "internal", " Confidential ", "SECRET"})
    void shouldAcceptValidAccessLevelsCaseInsensitively(String access) throws InvalidEmployeeException {
        EligibilityResult result = service.checkEligibility(employee(30, "IT", "ACTIVE", "SECRET", true), access);

        assertEquals(EligibilityStatus.ELIGIBLE, result.getStatus());
    }

    @ParameterizedTest
    @ValueSource(strings = {"INVALID", "TOP_SECRET"})
    void shouldThrowExceptionForInvalidAccessLevel(String access) {
        assertThrows(InvalidEmployeeException.class,
                () -> service.checkEligibility(employee(30, "IT", "ACTIVE", "SECRET", true), access));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void shouldThrowExceptionForBlankAccessLevel(String access) {
        assertThrows(InvalidEmployeeException.class,
                () -> service.checkEligibility(employee(30, "IT", "ACTIVE", "SECRET", true), access));
    }

    @Test
    void shouldBeConditionallyEligibleWhenOnlyClearanceFails() throws InvalidEmployeeException {
        EligibilityResult result = service.checkEligibility(employee(30, "IT", "ACTIVE", "BASIC", true), "SECRET");

        assertEquals(EligibilityStatus.CONDITIONALLY_ELIGIBLE, result.getStatus());
        assertEquals(1, result.getReasons().size());
    }

    @Test
    void shouldReturnAllReasonsForMultipleFailures() throws InvalidEmployeeException {
        EligibilityResult result = service.checkEligibility(employee(20, "SALES", "INACTIVE", "BASIC", false), "SECRET");

        assertEquals(EligibilityStatus.NOT_ELIGIBLE, result.getStatus());
        assertEquals(5, result.getReasons().size());
        assertTrue(result.getReasons().containsAll(List.of(
                "Employee must be at least 21 years old", "Department is not authorized",
                "Employment status is not active", "Employee ID is invalid",
                "Security clearance is insufficient for requested access level")));
    }

    @Test
    void shouldThrowExceptionForNullEmployee() {
        assertThrows(InvalidEmployeeException.class, () -> service.checkEligibility(null, "PUBLIC"));
    }

    private Employee employee(int age, String department, String employmentType,
                              String clearance, boolean idValid) {
        return new Employee("EMP001", "Test Employee", age, department,
                employmentType, clearance, idValid);
    }

    private void assertNotDepartmentFailure(EligibilityResult result) {
        assertFalse(result.getReasons().contains("Department is not authorized"));
    }
}
