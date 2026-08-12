package com.vetautet.app.application.user.usecase;

import com.vetautet.app.application.user.dto.AvailabilityCheckType;
import com.vetautet.app.application.user.dto.AvailabilityResult;
import com.vetautet.app.application.user.port.output.UserAvailabilityProbe;
import com.vetautet.app.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CheckUserAvailabilityUseCaseImplTest {

    @Mock
    private UserAvailabilityProbe userAvailabilityProbe;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CheckUserAvailabilityUseCaseImpl useCase;

    @Test
    void execute_synced_probeSaysExists_returnsUnavailable() {
        when(userAvailabilityProbe.isSynced()).thenReturn(true);
        when(userAvailabilityProbe.mightExist(AvailabilityCheckType.USERNAME, "john_doe")).thenReturn(true);

        AvailabilityResult result = useCase.execute(AvailabilityCheckType.USERNAME, "John_Doe");

        assertThat(result.isAvailable()).isFalse();
        assertThat(result.getType()).isEqualTo(AvailabilityCheckType.USERNAME);
        assertThat(result.getValue()).isEqualTo("John_Doe");
        verify(userRepository, never()).existsByUsernameIgnoreCase(any());
    }

    @Test
    void execute_synced_probeSaysNotExists_returnsAvailable() {
        when(userAvailabilityProbe.isSynced()).thenReturn(true);
        when(userAvailabilityProbe.mightExist(AvailabilityCheckType.EMAIL, "john@example.com")).thenReturn(false);

        AvailabilityResult result = useCase.execute(AvailabilityCheckType.EMAIL, "  John@Example.com  ");

        assertThat(result.isAvailable()).isTrue();
        verify(userRepository, never()).existsByEmailIgnoreCase(any());
    }

    @Test
    void execute_notSynced_username_fallsBackToDatabase() {
        when(userAvailabilityProbe.isSynced()).thenReturn(false);
        when(userRepository.existsByUsernameIgnoreCase("john_doe")).thenReturn(true);

        AvailabilityResult result = useCase.execute(AvailabilityCheckType.USERNAME, "john_doe");

        assertThat(result.isAvailable()).isFalse();
        verify(userAvailabilityProbe, never()).mightExist(any(), any());
    }

    @Test
    void execute_notSynced_email_fallsBackToDatabase() {
        when(userAvailabilityProbe.isSynced()).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("new@example.com")).thenReturn(false);

        AvailabilityResult result = useCase.execute(AvailabilityCheckType.EMAIL, "new@example.com");

        assertThat(result.isAvailable()).isTrue();
    }

    @Test
    void execute_nullValue_normalizesToEmptyStringAndStillChecks() {
        when(userAvailabilityProbe.isSynced()).thenReturn(true);
        when(userAvailabilityProbe.mightExist(eq(AvailabilityCheckType.USERNAME), eq(""))).thenReturn(false);

        AvailabilityResult result = useCase.execute(AvailabilityCheckType.USERNAME, null);

        assertThat(result.isAvailable()).isTrue();
        assertThat(result.getValue()).isNull();
    }
}
