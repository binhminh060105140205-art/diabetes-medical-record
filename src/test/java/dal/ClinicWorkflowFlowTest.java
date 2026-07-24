package dal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ClinicWorkflowFlowTest {

    @Test
    void allowsTheCompleteHappyPathWithoutLaboratoryTests() {
        assertTrue(ClinicWorkflowDAO.validEncounterTransition(
                "WAITING_TRIAGE", "WAITING_DOCTOR"));
        assertTrue(ClinicWorkflowDAO.validEncounterTransition(
                "WAITING_DOCTOR", "IN_CONSULTATION"));
        assertTrue(ClinicWorkflowDAO.validEncounterTransition(
                "IN_CONSULTATION", "COMPLETED"));
    }

    @Test
    void allowsTheCompleteHappyPathWithLaboratoryTests() {
        assertTrue(ClinicWorkflowDAO.validEncounterTransition(
                "IN_CONSULTATION", "WAITING_LAB"));
        assertTrue(ClinicWorkflowDAO.validEncounterTransition(
                "WAITING_LAB", "LAB_COMPLETED"));
        assertTrue(ClinicWorkflowDAO.validEncounterTransition(
                "LAB_COMPLETED", "IN_CONSULTATION"));
        assertTrue(ClinicWorkflowDAO.validEncounterTransition(
                "IN_CONSULTATION", "COMPLETED"));
    }

    @Test
    void blocksConclusionWhenTheDoctorHasNotStartedOrResumedTheVisit() {
        assertFalse(ClinicWorkflowDAO.validEncounterTransition(
                "WAITING_TRIAGE", "COMPLETED"));
        assertFalse(ClinicWorkflowDAO.validEncounterTransition(
                "WAITING_DOCTOR", "COMPLETED"));
        assertFalse(ClinicWorkflowDAO.validEncounterTransition(
                "WAITING_LAB", "COMPLETED"));
        assertFalse(ClinicWorkflowDAO.validEncounterTransition(
                "LAB_COMPLETED", "COMPLETED"));
    }
}
