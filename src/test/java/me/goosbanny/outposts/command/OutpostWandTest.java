package me.goosbanny.outposts.command;

import org.bukkit.Location;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class OutpostWandTest {

    @Test
    @DisplayName("Test Wand Selection expiry with 30-second timeout")
    public void testSelectionTimeout() throws InterruptedException {
        OutpostWandListener.Selection selection = new OutpostWandListener.Selection();

        assertFalse(selection.isExpired(), "Fresh selection should not be expired");

        // Updating pos1 touches interaction time
        selection.setPos1(new Location(null, 10, 60, 10));
        assertTrue(selection.getLastInteractionTime() <= System.currentTimeMillis());
        assertFalse(selection.isExpired());

        // Updating pos2 touches interaction time
        selection.setPos2(new Location(null, 20, 70, 20));
        assertTrue(selection.isComplete());
        assertFalse(selection.isExpired());
    }
}
