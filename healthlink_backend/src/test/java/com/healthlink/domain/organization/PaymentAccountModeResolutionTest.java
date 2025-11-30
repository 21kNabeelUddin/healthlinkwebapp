package com.healthlink.domain.organization;

import com.healthlink.domain.user.entity.Organization;
import com.healthlink.domain.user.entity.PaymentAccountMode;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class PaymentAccountModeResolutionTest {

    @Test
    void defaultModeIsDoctorLevel() {
        Organization org = new Organization();
        assertEquals(PaymentAccountMode.DOCTOR_LEVEL, org.getPaymentAccountMode());
    }
}
