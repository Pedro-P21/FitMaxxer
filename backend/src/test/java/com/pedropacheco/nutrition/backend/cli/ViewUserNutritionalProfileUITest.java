package com.pedropacheco.nutrition.backend.cli;

import com.pedropacheco.nutrition.backend.userNutritionalProfile.dto.UserProfileResponseDTO;
import com.pedropacheco.nutrition.backend.userNutritionalProfile.service.UserProfileNotFoundException;
import com.pedropacheco.nutrition.backend.userNutritionalProfile.service.ViewUserNutritionalProfileService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import java.util.Scanner;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.*;

@ExtendWith(OutputCaptureExtension.class)
class ViewUserNutritionalProfileUITest {
    @Test
    void reservedMainMenuEntryDisplaysTheSavedProfile(CapturedOutput output) {
        var service = mock(ViewUserNutritionalProfileService.class);
        when(service.getProfile("owner")).thenReturn(new UserProfileResponseDTO(1L, "owner", 30,
                70.5, 175.5, "MALE", null, "SEDENTARY", 1.2, 2000, 2000, 120, 220, 70));
        var ui = new ViewUserNutritionalProfileUI(service);
        var menu = new FitMaxxerMainMenu(mock(UpdateUserNutritionalProfileUI.class), mock(FoodCatalogSearchUI.class),
                mock(DailyLogUI.class), mock(DailyHistoryUI.class), mock(CustomRecipeUI.class), ui);
        menu.show("owner", new Scanner("1\n0\n"));
        verify(service).getProfile("owner");
        for (String expected : new String[]{"User ID: owner", "Age: 30", "Gender: MALE",
                "Weight: 70.5 kg", "Height: 175.5 cm", "Body fat: Not recorded", "Activity level: SEDENTARY",
                "Maintenance calories: 2000 kcal", "Calorie target: 2000 kcal",
                "Protein target: 120 g", "Carbohydrate target: 220 g", "Fat target: 70 g"})
            assertTrue(output.getOut().contains(expected), expected);
        assertFalse(output.getOut().contains("Profile ID:"));
        assertFalse(output.getOut().contains("Activity multiplier:"));
    }

    @Test
    void displaysInformationalMessageWhenProfileIsMissing(CapturedOutput output) {
        var service = mock(ViewUserNutritionalProfileService.class);
        when(service.getProfile("owner")).thenThrow(new UserProfileNotFoundException());
        new ViewUserNutritionalProfileUI(service).show("owner");
        assertTrue(output.getOut().contains("[INFO] No nutritional profile found. Create your profile first."));
    }
}
