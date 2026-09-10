package com.assessment6.employee;

import java.util.List;

public class EligibilityResult {

    private final EligibilityStatus status;
    private final List<String> reasons;

    public EligibilityResult(EligibilityStatus status, List<String> reasons) {
        this.status = status;
        this.reasons = List.copyOf(reasons);
    }

    public EligibilityStatus getStatus() {
        return status;
    }

    public List<String> getReasons() {
        return reasons;
    }
}
